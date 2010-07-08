//
// Copyright (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.jvm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.choice.CustomBooleanChoiceGenerator;
import gov.nasa.jpf.jvm.choice.DoubleChoiceFromSet;
import gov.nasa.jpf.jvm.choice.IntChoiceFromSet;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;
import gov.nasa.jpf.util.RunListener;
import gov.nasa.jpf.util.RunRegistry;

/**
 * native peer class for programmatic JPF interface (that can be used inside
 * of apps to verify - if you are aware of the danger that comes with it)
 */
public class JPF_gov_nasa_jpf_jvm_Verify {
  static final int MAX_COUNTERS = 10;

  static boolean isInitialized;
  static int[] counter;
  static boolean supportIgnorePath;
  static boolean breakSingleChoice;

  static Config config;  // we need to keep this around for CG creation

  // our const ChoiceGenerator ctor argtypes
  static Class[] cgArgTypes = { Config.class, String.class };
  // this is our cache for ChoiceGenerator ctor parameters
  static Object[] cgArgs = { null, null };


  public static void init (Config conf) {

    if (!isInitialized){
      supportIgnorePath = conf.getBoolean("vm.verify.ignore_path");
      breakSingleChoice = conf.getBoolean("cg.break_single_choice");

      counter = null;
      config = conf;

      Verify.setPeerClass( JPF_gov_nasa_jpf_jvm_Verify.class);

      RunRegistry.getDefaultRegistry().addListener( new RunListener() {
        public void reset (RunRegistry reg){
          isInitialized = false;
        }
      });
    }
  }

  public static final int getCounter__I__I (MJIEnv env, int clsObjRef, int counterId) {
    if ((counter == null) || (counterId < 0) || (counterId >= counter.length)) {
      return 0;
    }

    return counter[counterId];
  }

  public static final void resetCounter__I__V (MJIEnv env, int clsObjRef, int counterId) {
    if ((counter == null) || (counterId < 0) || (counterId >= counter.length)) {
      return;
    }
    counter[counterId] = 0;
  }

  public static final int incrementCounter__I__I (MJIEnv env, int clsObjRef, int counterId) {
    if (counterId < 0) {
      return 0;
    }

    if (counter == null) {
      counter = new int[(counterId >= MAX_COUNTERS) ? counterId+1 : MAX_COUNTERS];
    } else if (counterId >= counter.length) {
      int[] newCounter = new int[counterId+1];
      System.arraycopy(counter, 0, newCounter, 0, counter.length);
      counter = newCounter;
    }

    return ++counter[counterId];
  }

  public static final long currentTimeMillis____J (MJIEnv env, int clsObjRef) {
    return System.currentTimeMillis();
  }

  public static String getType (int objRef, MJIEnv env) {
    DynamicArea da = env.getDynamicArea();

    return Types.getTypeName(da.get(objRef).getType());
  }

  public static void addComment__Ljava_lang_String_2__V (MJIEnv env, int clsObjRef, int stringRef) {
    SystemState ss = env.getSystemState();
    String      cmt = env.getStringObject(stringRef);
    ss.getTrail().setAnnotation(cmt);
  }

  /** deprectated, just use assert */
  public static void assertTrue__Z__V (MJIEnv env, int clsObjRef, boolean b) {
    if (!b) {
      env.throwException("java.lang.AssertionError", "assertTrue failed");
    }
  }

  // those are evil - use with extreme care
  public static void beginAtomic____V (MJIEnv env, int clsObjRef) {
    env.getSystemState().incAtomic();
  }
  public static void endAtomic____V (MJIEnv env, int clsObjRef) {
    env.getSystemState().decAtomic();
  }

  public static void busyWait__J__V (MJIEnv env, int clsObjRef, long duration) {
    // nothing required here (we systematically explore scheduling
    // sequences anyway), but we need to intercept the call
  }

  public static void ignoreIf__Z__V (MJIEnv env, int clsObjRef, boolean cond) {
    if (supportIgnorePath) {
      env.getSystemState().setIgnored(cond);
    }
  }

  public static void interesting__Z__V (MJIEnv env, int clsObjRef, boolean cond) {
    env.getSystemState().setInteresting(cond);
  }

  public static void breakTransition____V (MJIEnv env, int clsObjRef){
    ThreadInfo ti = env.getThreadInfo();
    ti.breakTransition();
  }

  public static boolean isCalledFromClass__Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef,
                                           int clsNameRef) {
    String refClassName = env.getStringObject(clsNameRef);
    ThreadInfo ti = env.getThreadInfo();
    int        stackDepth = ti.countStackFrames();

    if (stackDepth < 2) {
      return false;      // native methods don't have a stackframe
    }

    String mthClassName = ti.getCallStackClass(1);
    ClassInfo ci = ClassInfo.getResolvedClassInfo(mthClassName);

    return ci.isInstanceOf(refClassName);
  }


  static <T extends ChoiceGenerator<?>> T createChoiceGenerator (Class<T> cgClass, SystemState ss, String id) {
    T gen = null;

    cgArgs[0] = config;
    cgArgs[1] = id; // good thing we are not multithreaded (other fields are const)

    String key = id + ".class";
    gen = config.getEssentialInstance(key, cgClass, cgArgTypes, cgArgs);

    return gen;
  }

  static <T> T registerChoiceGenerator (MJIEnv env, SystemState ss, ThreadInfo ti, ChoiceGenerator<T> cg, T dummyVal){

    int n = cg.getTotalNumberOfChoices();
    if (n == 0) {
      ss.setIgnored(true);
      ti.breakTransition();

    } else if (n == 1 && !breakSingleChoice) {
      // no choice -> no CG optimization
      cg.advance();
      return cg.getNextChoice();

    } else {
      ss.setNextChoiceGenerator(cg);
      env.repeatInvocation();
    }

    return dummyVal;
  }

  static <T,C extends ChoiceGenerator<T>> T getNextChoice (SystemState ss, Class<C> cgClass, Class<T> choiceClass){
    ChoiceGenerator<?> cg = ss.getChoiceGenerator();

    assert (cg != null) && (cgClass.isAssignableFrom(cg.getClass())) :
          "expected ChoiceGenerator of type " + cgClass.getName() + ", got: " + cg.getClass().getName();
    return ((ChoiceGenerator<T>)cg).getNextChoice();
  }

  public static final boolean getBoolean____Z (MJIEnv env, int clsObjRef) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();
    ChoiceGenerator<?> cg;

    if (!ti.isFirstStepInsn()) { // first time around
      cg = new BooleanChoiceGenerator(config, "boolean");
      ss.setNextChoiceGenerator(cg);
      env.repeatInvocation();
      return true;  // not used anyways

    } else {  // this is what really returns results
      return getNextChoice(ss,BooleanChoiceGenerator.class,Boolean.class);
    }
  }

  public static final boolean getBoolean__Z__Z (MJIEnv env, int clsObjRef, boolean falseFirst) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();
    ChoiceGenerator<?> cg;

    if (!ti.isFirstStepInsn()) { // first time around
      cg = new CustomBooleanChoiceGenerator(falseFirst, "boolean");
      ss.setNextChoiceGenerator(cg);
      env.repeatInvocation();
      return true;  // not used anyways

    } else {  // this is what really returns results
      return getNextChoice(ss,BooleanChoiceGenerator.class,Boolean.class);
    }
  }



  public static int getInt__II__I (MJIEnv env, int clsObjRef, int min, int max) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();

    if (!ti.isFirstStepInsn()) { // first time around

      if (min > max){
        int t = max;
        max = min;
        min = t;
      }

      IntChoiceGenerator cg = new IntIntervalGenerator(min,max);
      return registerChoiceGenerator(env,ss,ti,cg,0);

    } else {
      return getNextChoice(ss,IntChoiceGenerator.class,Integer.class);
    }
  }

  public static int getIntFromSet___3I__I (MJIEnv env, int clsObjRef, int valArrayRef){
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();

    if (!ti.isFirstStepInsn()) { // first time around
      int[] values = env.getIntArrayObject(valArrayRef);

      IntChoiceGenerator cg = new IntChoiceFromSet(values);
      return registerChoiceGenerator(env,ss,ti,cg,0);

    } else {
      return getNextChoice(ss,IntChoiceGenerator.class,Integer.class);
    }
  }


  public static int getInt__Ljava_lang_String_2__I (MJIEnv env, int clsObjRef, int idRef) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();


    if (!ti.isFirstStepInsn()) { // first time around
      String id = env.getStringObject(idRef);
      IntChoiceGenerator cg = createChoiceGenerator( IntChoiceGenerator.class, ss, id);
      return registerChoiceGenerator(env,ss,ti,cg, 0);

    } else {
      return getNextChoice(ss,IntChoiceGenerator.class,Integer.class);
    }
  }

  public static int getObject__Ljava_lang_String_2__Ljava_lang_Object_2 (MJIEnv env, int clsObjRef, int idRef) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();

    if (!ti.isFirstStepInsn()) { // first time around
      String id = env.getStringObject(idRef);
      ReferenceChoiceGenerator cg = createChoiceGenerator( ReferenceChoiceGenerator.class, ss, id);
      return registerChoiceGenerator(env,ss,ti,cg, 0);

    } else {
      return getNextChoice(ss,ReferenceChoiceGenerator.class,Integer.class);
    }
  }

  public static double getDouble__Ljava_lang_String_2__D (MJIEnv env, int clsObjRef, int idRef) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();

    if (!ti.isFirstStepInsn()) { // first time around
      String id = env.getStringObject(idRef);
      DoubleChoiceGenerator cg = createChoiceGenerator( DoubleChoiceGenerator.class, ss, id);
      return registerChoiceGenerator(env,ss,ti,cg, 0.0);

    } else {
      return getNextChoice(ss,DoubleChoiceGenerator.class,Double.class);
    }
  }

  public static double getDoubleFromSet___3D__D (MJIEnv env, int clsObjRef, int valArrayRef){
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();

    if (!ti.isFirstStepInsn()) { // first time around
      double[] values = env.getDoubleArrayObject(valArrayRef);
      DoubleChoiceGenerator cg = new DoubleChoiceFromSet(values);
      return registerChoiceGenerator(env,ss,ti,cg, 0.0);

    } else {
      return getNextChoice(ss,DoubleChoiceFromSet.class,Double.class);
    }
  }



  public static void print__Ljava_lang_String_2I__V (MJIEnv env, int clsRef, int sRef, int val){
    String s = env.getStringObject(sRef);
    System.out.println(s + " : " + val);
  }

  public static void print__Ljava_lang_String_2Z__V (MJIEnv env, int clsRef, int sRef, boolean val){
    String s = env.getStringObject(sRef);
    System.out.println(s + " : " + val);
  }

  public static void print___3Ljava_lang_String_2__V (MJIEnv env, int clsRef, int argsRef){
    int n = env.getArrayLength(argsRef);
    for (int i=0; i<n; i++){
      int aref = env.getReferenceArrayElement(argsRef, i);
      String s = env.getStringObject(aref);
      System.out.print(s);
    }
  }

  public static void println____V (MJIEnv env, int clsRef){
    System.out.println();
  }

  public static void setObjectAttribute__Ljava_lang_Object_2I__V (MJIEnv env, int clsRef, int oRef, int attr){
    if (oRef != MJIEnv.NULL){
      ElementInfo ei = env.getElementInfo(oRef);
      ei.setObjectAttr(attr);
    }
  }

  public static int getObjectAttribute__Ljava_lang_Object_2__I (MJIEnv env, int clsRef, int oRef){
    if (oRef != MJIEnv.NULL){
      ElementInfo ei = env.getElementInfo(oRef);
      Integer a = ei.getObjectAttr(Integer.class);
      if (a != null){
        return a.intValue();
      }
    }

    return 0;
  }

  public static void setFieldAttribute__Ljava_lang_Object_2Ljava_lang_String_2I__V (MJIEnv env, int clsRef,
                                                                                    int oRef, int fnRef, int attr){
    if (oRef != MJIEnv.NULL){
      ElementInfo ei = env.getElementInfo(oRef);
      if (ei != null){
        String fname = env.getStringObject(fnRef);
        FieldInfo fi = ei.getFieldInfo(fname);

        if (fi != null) {
          ei.setFieldAttr(fi, new Integer(attr));
        } else {
          env.throwException("java.lang.NoSuchFieldException",
                  ei.getClassInfo().getName() + '.' + fname);
        }
      } else {
        env.throwException("java.lang.RuntimeException", "illegal reference value: " + oRef);
      }
    }
  }

  public static int getFieldAttribute__Ljava_lang_Object_2Ljava_lang_String_2__I (MJIEnv env, int clsRef,
                                                                                    int oRef, int fnRef){
    if (oRef != MJIEnv.NULL){
      ElementInfo ei = env.getElementInfo(oRef);
      if (ei != null){
        String fname = env.getStringObject(fnRef);
        FieldInfo fi = ei.getFieldInfo(fname);

        if (fi != null) {
          Integer a = ei.getFieldAttr(Integer.class, fi);
          if (a != null){
            return a.intValue();
          }
        } else {
          env.throwException("java.lang.NoSuchFieldException",
                  ei.toString() + '.' + fname);
        }
      } else {
        env.throwException("java.lang.RuntimeException", "illegal reference value: " + oRef);
      }
    }

    return 0;
  }

  public static void setLocalAttribute__Ljava_lang_String_2I__V (MJIEnv env, int clsRef, int snRef, int attr) {
    String slotName = env.getStringObject(snRef);
    ThreadInfo ti = env.getThreadInfo();
    StackFrame frame = ti.getTopFrame();

    if (!frame.getMethodInfo().isStatic() &&  slotName.equals("this")) {
      frame.setLocalAttr(0, new Integer(attr)); // only for instance methods of course

    } else {
      int slotIdx = frame.getLocalVariableOffset(slotName);
      if (slotIdx >= 0) {
        frame.setLocalAttr(slotIdx, new Integer(attr));
      } else {
        env.throwException("java.lang.RuntimeException", "local variable not found: " + slotName);
      }
    }
  }

  public static int getLocalAttribute__Ljava_lang_String_2__I (MJIEnv env, int clsRef, int snRef) {
    String slotName = env.getStringObject(snRef);
    ThreadInfo ti = env.getThreadInfo();
    StackFrame frame = ti.getTopFrame();

    int slotIdx = frame.getLocalVariableOffset(slotName);
    if (slotIdx >= 0) {
      Object val = frame.getLocalAttr(slotIdx);
      if (val instanceof Integer) {
        return (Integer)val;
      } else {
        env.throwException("java.lang.RuntimeException", "attribute for local var: "
                           + slotName + " not an int: " + val);
        return 0;
      }
    } else {
      env.throwException("java.lang.RuntimeException", "local variable not found: " + slotName);
      return 0;
    }
  }


  public static void setElementAttribute__Ljava_lang_Object_2II__V (MJIEnv env, int clsRef,
                                                                    int oRef, int idx, int attr){
    if (oRef != MJIEnv.NULL){
      ElementInfo ei = env.getElementInfo(oRef);

      if (ei != null){
        if (ei.isArray()) {
          if (idx < ei.arrayLength()) {
            ei.setElementAttr(idx, new Integer(attr));
          } else {
            env.throwException("java.lang.ArrayIndexOutOfBoundsException",
                    Integer.toString(idx));
          }
        } else {
          env.throwException("java.lang.RuntimeException",
                  "not an array: " + ei);
        }
      } else {
        env.throwException("java.lang.RuntimeException", "illegal reference value: " + oRef);
      }
    }
  }

  public static int getElementAttribute__Ljava_lang_Object_2I__I (MJIEnv env, int clsRef,
                                                                  int oRef, int idx){
    if (oRef != MJIEnv.NULL){
      ElementInfo ei = env.getElementInfo(oRef);

      if (ei != null) {
        if (ei.isArray()) {
          if (idx < ei.arrayLength()) {
            Integer a = ei.getElementAttr(Integer.class, idx);
            if (a != null){
              return a.intValue();
            }
          } else {
            env.throwException("java.lang.ArrayIndexOutOfBoundsException",
                    Integer.toString(idx));
          }
        } else {
          env.throwException("java.lang.RuntimeException",
                  "not an array: " + ei);
        }
      } else {
        env.throwException("java.lang.RuntimeException", "illegal reference value: " + oRef);
      }
    }

    return 0;
  }


  /**
   *  deprecated, use getBoolean()
   */
  public static final boolean randomBool (MJIEnv env, int clsObjRef) {
    //SystemState ss = env.getSystemState();
    //return (ss.random(2) != 0);

    return getBoolean____Z(env, clsObjRef);
  }



  /**
   * deprecated, use getInt
   */
  public static final int random__I__I (MJIEnv env, int clsObjRef, int x) {
   return getInt__II__I( env, clsObjRef, 0, x);
  }

  static void boring__Z__V (MJIEnv env, int clsObjRef, boolean b) {
    env.getSystemState().setBoring(b);
  }

  protected static int[] arrayOfObjectsOfType (DynamicArea da, String type) {
    int[] map = new int[0];
    int   map_size = 0;

    for (int i = 0; i < da.getLength(); i++) {
      if (da.get(i) != null) {
        if ((Types.getTypeName(da.get(i).getType())).equals(type)) {
          if (map_size >= map.length) {
            int[] n = new int[map_size + 1];
            System.arraycopy(map, 0, n, 0, map.length);
            map = n;
          }

          map[map_size] = i;
          map_size++;
        }
      }
    }

    return map;
  }

  public static boolean isRunningInJPF____Z(MJIEnv env, int clsObjRef) {
    return true;
  }

  public static boolean vmIsMatchingStates____Z(MJIEnv env, int clsObjRef) {
    return env.getVM().getStateSet() != null;
  }

  public static void storeTrace__Ljava_lang_String_2Ljava_lang_String_2__V (MJIEnv env, int clsObjRef,
                                      int filenameRef, int commentRef) {
    String fileName = env.getStringObject(filenameRef);
    String comment = env.getStringObject(commentRef);
    env.getVM().storeTrace(fileName, comment, config.getBoolean("trace.verbose", false));
  }

  public static void terminateSearch____V (MJIEnv env, int clsObjRef) {
    JPF jpf = env.getVM().getJPF();
    jpf.getSearch().terminate();
  }

  public static boolean isTraceReplay____Z (MJIEnv env, int clsObjRef) {
    return env.getVM().isTraceReplay();
  }

  public static void setProperties___3Ljava_lang_String_2__V (MJIEnv env, int clsObjRef, int argRef) {
    if (argRef != MJIEnv.NULL) {
      Config conf = env.getConfig();

      int n = env.getArrayLength(argRef);
      for (int i=0; i<n; i++) {
        int pRef = env.getReferenceArrayElement(argRef, i);
        if (pRef != MJIEnv.NULL) {
          String p = env.getStringObject(pRef);
          config.parse(p);
        }
      }
    }
  }

  public static int getProperty__Ljava_lang_String_2__Ljava_lang_String_2 (MJIEnv env, int clsObjRef, int keyRef) {
    if (keyRef != MJIEnv.NULL){
      Config conf = env.getConfig();

      String key = env.getStringObject(keyRef);
      String val = config.getString(key);

      if (val != null){
        return env.newString(val);
      } else {
        return MJIEnv.NULL;
      }

    } else {
      return MJIEnv.NULL;
    }
  }


  public static void printPathOutput__ZLjava_lang_String_2__V (MJIEnv env, int clsObjRef, boolean cond, int msgRef){
    if (cond){
      printPathOutput__Ljava_lang_String_2__V(env,clsObjRef,msgRef);
    }
  }

  public static void printPathOutput__Ljava_lang_String_2__V (MJIEnv env, int clsObjRef, int msgRef){
    JVM vm = env.getVM();

    System.out.println();
    if (msgRef != MJIEnv.NULL){
      String msg = env.getStringObject(msgRef);
      System.out.println("~~~~~~~~~~~~~~~~~~~~~~~ begin program output at: " + msg);
    } else {
      System.out.println("~~~~~~~~~~~~~~~~~~~~~~~ begin path output");
    }

    for (Transition t : vm.getPath()) {
      String s = t.getOutput();
      if (s != null) {
        System.out.print(s);
      }
    }

    // we might be in the middle of a transition that isn't stored yet in the path
    String s = vm.getPendingOutput();
    if (s != null) {
      System.out.print(s);
    }

    System.out.println("~~~~~~~~~~~~~~~~~~~~~~~ end path output");
  }
}
