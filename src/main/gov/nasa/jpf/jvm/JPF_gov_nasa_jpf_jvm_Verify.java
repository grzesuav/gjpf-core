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
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.choice.CustomBooleanChoiceGenerator;
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

  static Config config;  // we need to keep this around for CG creation

  // our const ChoiceGenerator ctor argtypes
  static Class[] cgArgTypes = { Config.class, String.class };
  // this is our cache for ChoiceGenerator ctor parameters
  static Object[] cgArgs = { null, null };


  public static void init (Config conf) {

    if (!isInitialized){
      supportIgnorePath = conf.getBoolean("vm.verify.ignore_path");
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
    ClassInfo ci = ClassInfo.getClassInfo(mthClassName);

    return ci.isInstanceOf(refClassName);
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
      cg = ss.getChoiceGenerator();

      assert (cg != null) && (cg instanceof BooleanChoiceGenerator) : "expected BooleanChoiceGenerator, got: " + cg;
      return ((BooleanChoiceGenerator)cg).getNextChoice().booleanValue();
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
      cg = ss.getChoiceGenerator();

      assert (cg != null) && (cg instanceof CustomBooleanChoiceGenerator) : "expected CustomBooleanChoiceGenerator, got: " + cg;
      return ((CustomBooleanChoiceGenerator)cg).getNextChoice().booleanValue();
    }
  }



  public static int getInt__II__I (MJIEnv env, int clsObjRef, int min, int max) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();
    ChoiceGenerator<?> cg;

    if (!ti.isFirstStepInsn()) { // first time around
      if (min == max) return min;
      cg = new IntIntervalGenerator(min,max);
      ss.setNextChoiceGenerator(cg);
      //ti.skipInstructionLogging();
      env.repeatInvocation();
      return 0;  // not used anyways

    } else {
      cg = ss.getChoiceGenerator();

      assert (cg != null) && (cg instanceof IntChoiceGenerator) : "expected IntChoiceGenerator, got: " + cg;
      return ((IntChoiceGenerator)cg).getNextChoice().intValue();
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


  public static void setFieldAttribute__Ljava_lang_Object_2Ljava_lang_String_2I__V (MJIEnv env, int clsRef,
                                                                                    int oRef, int fnRef, int attr){
    if (oRef != env.NULL){
      ElementInfo ei = env.getElementInfo(oRef);

      String fname = env.getStringObject(fnRef);
      FieldInfo fi = ei.getFieldInfo(fname);

      if (fi != null){
        ei.setFieldAttr(fi, new Integer(attr));
      } else {
        env.throwException("java.lang.NoSuchFieldException",
                           ei.getClassInfo().getName() + '.' + fname);
      }
    }
  }

  public static int getFieldAttribute__Ljava_lang_Object_2Ljava_lang_String_2__I (MJIEnv env, int clsRef,
                                                                                    int oRef, int fnRef){
    if (oRef != env.NULL){
      ElementInfo ei = env.getElementInfo(oRef);

      String fname = env.getStringObject(fnRef);
      FieldInfo fi = ei.getFieldInfo(fname);

      if (fi != null){
        Object val = ei.getFieldAttr(fi);
        if (val != null){
          if (val instanceof Integer){
            int ret = ((Integer)val).intValue();
            return ret;
          } else {
            env.throwException("java.lang.RuntimeException",
                               ei.toString() + '.' + fname +
                               " attribute not and int: " + val);
          }
        }

      } else {
        env.throwException("java.lang.NoSuchFieldException",
                           ei.toString() + '.' + fname);
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
    if (oRef != env.NULL){
      ElementInfo ei = env.getElementInfo(oRef);

      if (ei.isArray()){
        if (idx < ei.arrayLength()){
          ei.setElementAttr(idx, new Integer(attr));
        } else {
          env.throwException("java.lang.ArrayIndexOutOfBoundsException",
                             Integer.toString(idx));
        }
      } else {
        env.throwException("java.lang.RuntimeException",
                           "not an array: " + ei);
      }
    }
  }

  public static int getElementAttribute__Ljava_lang_Object_2I__I (MJIEnv env, int clsRef,
                                                                  int oRef, int idx){
    if (oRef != env.NULL){
      ElementInfo ei = env.getElementInfo(oRef);

      if (ei.isArray()){
        if (idx < ei.arrayLength()){
          Object val = ei.getElementAttr(idx);
          if (val != null){
            if (val instanceof Integer){
              int ret = ((Integer)val).intValue();
              return ret;
            } else {
              env.throwException("java.lang.RuntimeException",
                                 ei.toString() + '[' + idx +
                                 "] attribute not and int: " + val);
            }
          }
        } else {
          env.throwException("java.lang.ArrayIndexOutOfBoundsException",
                             Integer.toString(idx));
        }
      } else {
        env.throwException("java.lang.RuntimeException",
                           "not an array: " + ei);
      }
    }

    return 0;
  }

  static ChoiceGenerator<?> createChoiceGenerator (SystemState ss, String id) {
    ChoiceGenerator<?> gen = null;

    cgArgs[0] = config;
    cgArgs[1] = id; // good thing we are not multithreaded (other fields are const)

    String key = id + ".class";
    gen = config.getEssentialInstance(key, ChoiceGenerator.class,
            cgArgTypes, cgArgs);
    ss.setNextChoiceGenerator(gen);

    return gen;
  }


  public static final int getInt__Ljava_lang_String_2__I (MJIEnv env, int clsObjRef, int idRef) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();
    ChoiceGenerator<?> cg;

    if (!ti.isFirstStepInsn()) { // first time around
      String id = env.getStringObject(idRef);
      cg = createChoiceGenerator( ss, id);
      ss.setNextChoiceGenerator(cg);
      //ti.skipInstructionLogging();
      env.repeatInvocation();
      return 0;  // not used anyways

    } else {
      cg = ss.getChoiceGenerator();

      assert (cg != null) && (cg instanceof IntChoiceGenerator) : "expected IntChoiceGenerator, got: " + cg;
      return ((IntChoiceGenerator)cg).getNextChoice().intValue();
    }
  }

  public static int getObject__Ljava_lang_String_2__Ljava_lang_Object_2 (MJIEnv env, int clsObjRef, int idRef) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();
    ChoiceGenerator<?> cg;

    if (!ti.isFirstStepInsn()) { // first time around
      String id = env.getStringObject(idRef);
      cg = createChoiceGenerator( ss, id);
      ss.setNextChoiceGenerator(cg);
      //ti.skipInstructionLogging();
      env.repeatInvocation();
      return 0;  // not used anyways

    } else {
      cg = ss.getChoiceGenerator();

      assert (cg != null) && (cg instanceof ReferenceChoiceGenerator) : "expected ReferenceChoiceGenerator, got: " + cg;
      return ((ReferenceChoiceGenerator)cg).getNextChoice().intValue();
    }
  }

  public static final double getDouble__Ljava_lang_String_2__D (MJIEnv env, int clsObjRef, int idRef) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();
    ChoiceGenerator<?> cg;

    if (!ti.isFirstStepInsn()) { // first time around
      String id = env.getStringObject(idRef);
      cg = createChoiceGenerator( ss, id);
      ss.setNextChoiceGenerator(cg);
      //ti.skipInstructionLogging();
      env.repeatInvocation();
      return 0.0;  // not used anyways

    } else {
      cg = ss.getChoiceGenerator();

      assert (cg != null) && (cg instanceof DoubleChoiceGenerator) : "expected DoubleChoiceGenerator, got: " + cg;
      return ((DoubleChoiceGenerator)cg).getNextChoice().doubleValue();
    }
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
}
