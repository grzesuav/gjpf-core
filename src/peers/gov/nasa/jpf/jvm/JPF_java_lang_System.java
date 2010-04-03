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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Map;

import org.apache.bcel.util.ClassPath;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.choice.BreakGenerator;

/**
 * MJI NativePeer class for java.lang.System library abstraction
 */
public class JPF_java_lang_System {
  
  // <2do> - now this baby really needs to be fixed. Imagine what it
  // does to an app that works with large vectors
  public static void arraycopy__Ljava_lang_Object_2ILjava_lang_Object_2II__V (MJIEnv env, int clsObjRef,
                                                                              int srcArrayRef, int srcIdx, 
                                                                              int dstArrayRef, int dstIdx,
                                                                              int length) {
    int i;
    
    if ((srcArrayRef == -1) || (dstArrayRef == -1)) {
      env.throwException("java.lang.NullPointerException");
      return;
    }

    if (!env.isArray(srcArrayRef) || !env.isArray(dstArrayRef)) {
      env.throwException("java.lang.ArrayStoreException");
      return;
    }

    int sts = env.getArrayTypeSize(srcArrayRef);
    int dts = env.getArrayTypeSize(dstArrayRef);

    if (sts != dts) {
      env.throwException("java.lang.ArrayStoreException");
      return;
    }

    // ARGHH <2do> pcm - at some point, we should REALLY do this with a block
    // operation (saves lots of state, speed if arraycopy is really used)
    // we could also use the native checks in that case
    int sl = env.getArrayLength(srcArrayRef);
    int dl = env.getArrayLength(dstArrayRef);

    // <2do> - we need detail messages!
    if ((srcIdx < 0) || ((srcIdx + length) > sl)) {
      env.throwException("java.lang.ArrayIndexOutOfBoundsException");
      return;
    }

    if ((dstIdx < 0) || ((dstIdx + length) > dl)) {
      env.throwException("java.lang.ArrayIndexOutOfBoundsException");
      return;
    }
    
    ClassInfo srcArrayCi = env.getClassInfo(srcArrayRef);    
    ClassInfo dstArrayCi = env.getClassInfo(dstArrayRef);      
    
    // finally do the copying
    if (sts == 1) {  // word-size array
      
      // self copy - no checks required
      if (srcArrayRef == dstArrayRef && srcIdx < dstIdx) {
        // bugfix case by peterd; see docs for System.arraycopy
        for (i = length - 1; i >= 0; i--) {
          int v = env.getIntArrayElement(srcArrayRef, srcIdx + i);
          env.setIntArrayElement(dstArrayRef, dstIdx + i, v);
        }
        
      } else { // different array objects

        // check reference types (fix from Milos Gligoric and Tihomir Gvero)
        boolean isDstReferenceArray = dstArrayCi.isReferenceArray();
        if (isDstReferenceArray != srcArrayCi.isReferenceArray()) {
          env.throwException("java.lang.ArrayStoreException");          
        }
        
        if (!isDstReferenceArray) {
          // builtin element types have to be identical
          if (srcArrayCi != dstArrayCi) {
            env.throwException("java.lang.ArrayStoreException");            
          }
          
          for (i = 0; i < length; i++) {
            int v = env.getIntArrayElement(srcArrayRef, srcIdx + i);
            env.setIntArrayElement(dstArrayRef, dstIdx + i, v);
          }
          
        } else {
          ClassInfo dstArrayElementCi = dstArrayCi.getComponentClassInfo();
          
          for (i = 0; i < length; i++) {
            // check element type compatibility
            int srcArrayElementRef = env.getReferenceArrayElement(srcArrayRef, srcIdx + i);
            if (srcArrayElementRef != MJIEnv.NULL) {
              ClassInfo srcArrayElementCi = env.getClassInfo(srcArrayElementRef);
              if (!srcArrayElementCi.isInstanceOf(dstArrayElementCi)) {                   
                env.throwException("java.lang.ArrayStoreException");
                return;            
              }
            }

            env.setIntArrayElement(dstArrayRef, dstIdx + i, srcArrayElementRef);
          }
        }
      }
      
    } else {  // double word size array
      
      if (srcArrayRef == dstArrayRef && srcIdx < dstIdx) {
        // self copy - no checks required
        // bugfix case by peterd; see docs for System.arraycopy
        for (i = length - 1; i >= 0; i--) {
          long v = env.getLongArrayElement(srcArrayRef, srcIdx + i);
          env.setLongArrayElement(dstArrayRef, dstIdx + i, v);
        }    
        
      } else { // different array objects
        
        // builtin element types have to be identical
        if (srcArrayCi != dstArrayCi) {
          env.throwException("java.lang.ArrayStoreException");            
        }
        
        for (i = 0; i < length; i++) {
          long v = env.getLongArrayElement(srcArrayRef, srcIdx + i);
          env.setLongArrayElement(dstArrayRef, dstIdx + i, v);
        }
      }
    }
    
    if (env.hasFieldAttrs(srcArrayRef)){
      if (srcArrayRef == dstArrayRef && srcIdx < dstIdx) { // self copy
        for (i = length - 1; i >= 0; i--) {
          Object a = env.getElementAttr(srcArrayRef, srcIdx+i);
          env.setElementAttr(dstArrayRef, dstIdx+i, a);
        }        
      } else {
        for (i = 0; i < length; i++) {
          Object a = env.getElementAttr(srcArrayRef, srcIdx+i);
          env.setElementAttr(dstArrayRef, dstIdx+i, a);
        }          
      }
    }
  }

  public static int getenv__Ljava_lang_String_2__Ljava_lang_String_2 (MJIEnv env, int clsObjRef,
                                                                         int keyRef){
    String k = env.getStringObject(keyRef);
    String v = System.getenv(k);
    
    if (v == null){
      return MJIEnv.NULL;
    } else {
      return env.newString(v);
    }
  }

  
  static int createPrintStream (MJIEnv env, int clsObjRef){
    ThreadInfo ti = env.getThreadInfo();
    Instruction insn = ti.getPC();

    ClassInfo ci = ClassInfo.getResolvedClassInfo("gov.nasa.jpf.ConsoleOutputStream");

    // it's not really used, but it would be hack'ish to use a class whose
    // super class hasn't been initialized yet
    if (!ci.isRegistered()) {
      ci.registerClass(ti);
    }

    if (!ci.isInitialized()) {
      if (ci.initializeClass(ti, insn)) {
        env.repeatInvocation();
        return MJIEnv.NULL;
      }
    }

    return env.newObject(ci);
  }
  
  public static int createSystemOut____Ljava_io_PrintStream_2 (MJIEnv env, int clsObjRef){
    try {
      return createPrintStream(env,clsObjRef);

    } catch (NoClassInfoException cx){
      env.throwException("java.lang.NoClassDefFoundError", cx.getMessage());
      return MJIEnv.NULL;
    }
  }
  
  public static int createSystemErr____Ljava_io_PrintStream_2 (MJIEnv env, int clsObjRef){
    try {
      return createPrintStream(env,clsObjRef);

    } catch (NoClassInfoException cx){
      env.throwException("java.lang.NoClassDefFoundError", cx.getMessage());
      return MJIEnv.NULL;
    }
  }
  
  static int getProperties (MJIEnv env, Properties p){
    int n = p.size() * 2;
    int aref = env.newObjectArray("Ljava/lang/String;", n);
    int i=0;
    
    for (Map.Entry<Object,Object> e : p.entrySet() ){
      env.setReferenceArrayElement(aref,i++, 
                                   env.newString(e.getKey().toString()));
      env.setReferenceArrayElement(aref,i++,
                                   env.newString(e.getValue().toString()));
    }
    
    return aref;
  }

  static int getSysPropsFromHost (MJIEnv env){
    return getProperties(env, System.getProperties());
  }
  
  static int getSysPropsFromFile (MJIEnv env){
    Config conf = env.getConfig();
    
    String cf = conf.getString("vm.sysprop.file", "system.properties");
    if (cf != null){
      try {
        Properties p = new Properties();
        FileInputStream fis = new FileInputStream(cf);
        p.load(fis);
        
        return getProperties(env, p);
        
      } catch (IOException iox){
        return MJIEnv.NULL;
      }
    }
    //.. not yet
    return MJIEnv.NULL;
  }
  
  static String JAVA_CLASS_PATH = "java.class.path";
  
  static int getSelectedSysPropsFromHost (MJIEnv env){
    Config conf = env.getConfig();
    String keys[] = conf.getStringArray("vm.sysprop.keys");

    if (keys == null){
      String[] defKeys = {
        "path.separator",
        "line.separator", 
        "file.separator",
        "user.name",
        "user.dir",
        "user.timezone",
        "user.country",
        "java.home",
        "java.version",
        JAVA_CLASS_PATH
        //... and probably some more
        // <2do> what about -Dkey=value commandline options
      };
      keys = defKeys;
    }
    
    int aref = env.newObjectArray("Ljava/lang/String;", keys.length * 2);
    int i=0;
    
    for (String s : keys) {
      String v;
      
      int idx = s.indexOf('/');
      if (idx >0){ // this one is an explicit key/value pair from vm.system.properties
        v = s.substring(idx+1);
        s = s.substring(0,idx);
        
      } else {
        // the special beasts first (if they weren't overridden with vm.system.properties)
        if (s == JAVA_CLASS_PATH) {
          // maybe we should just use vm.classpath
          ClassPath cp = ClassInfo.getModelClassPath();
          // <2do> should be consistent with path.separator (this is host VM notation)
          v = cp.toString();
          
        } else { // we bluntly grab it from the host VM properties
          v = System.getProperty(s);
        }
      }
            
      if (v != null){
        env.setReferenceArrayElement(aref,i++, env.newString(s));
        env.setReferenceArrayElement(aref,i++, env.newString(v));
      }
    }
        
    return aref;
  }
  
  static String SYSPROP_SRC_KEY = "vm.sysprop.source";
  static enum SYSPROP_SRC { selected, file, host };

  public static int getKeyValuePairs_____3Ljava_lang_String_2 (MJIEnv env, int clsObjRef){
    Config conf = env.getConfig();
    SYSPROP_SRC sysPropSrc = conf.getEnum(SYSPROP_SRC_KEY, SYSPROP_SRC.values(), SYSPROP_SRC.selected);

    // <2do> temporary workaround - a switch would cause an anonymous inner class that gets
    // erroneously associated with a different ..$1 class (anonymous JavaLangAccess) from our model
    if (sysPropSrc == SYSPROP_SRC.file){
      return getSysPropsFromFile(env);
    } else if (sysPropSrc == SYSPROP_SRC.host){
      return getSysPropsFromHost(env);
    } else if (sysPropSrc == SYSPROP_SRC.selected){
      return getSelectedSysPropsFromHost(env);
    } else {
        throw new JPFConfigException("unsupported system properties source: " + conf.getString(SYSPROP_SRC_KEY));
    }
  }
  
  // <2do> - this break every app which uses time delta thresholds
  // (sort of "if ((t2 - t1) > d)"). Ok, we can't deal with
  // real time, but we could at least give some SystemState dependent
  // increment
  public static long currentTimeMillis____J (MJIEnv env, int clsObjRef) {
    // <2do> this should be configurable policy
    //return env.getVM().getTime();
    return System.currentTimeMillis();
  }

  // <2do> - likewise. Java 1.5's way to measure relative time
  public static long nanoTime____J (MJIEnv env, int clsObjRef) {
    return env.getVM().getTime();
  }  
  
  // this works on the assumption that we sure break the transition, and
  // then the search determines that it is an end state (all terminated)
  public static void exit__I__V (MJIEnv env, int clsObjRef, int ret) {
    SystemState ss = env.getSystemState();
    ThreadList tl = env.getVM().getThreadList();

    for (int i = 0; i < tl.length(); i++) {
      ThreadInfo ti = tl.get(i);

      // keep the stack frames around, so that we can inspect the snapshot
      ti.setTerminated();
    }
    
    ss.setNextChoiceGenerator( new BreakGenerator(env.getThreadInfo(), true));
  }

  public static void gc____V (MJIEnv env, int clsObjRef) {
    env.getSystemState().activateGC();
  }

  public static int identityHashCode__Ljava_lang_Object_2__I (MJIEnv env, int clsObjRef, int objref) {
    return (objref ^ 0xABCD);
  }
  
}
