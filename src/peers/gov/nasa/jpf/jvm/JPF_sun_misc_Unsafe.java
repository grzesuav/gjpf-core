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

import gov.nasa.jpf.JPFException;
import java.io.PrintWriter;

/**
 * we don't want this class! This is a hodgepodge of stuff that shouldn't be in Java, but
 * is handy for some hacks. The reason we have it here - very rudimentary - is that
 * java.util.concurrent makes use of the atomic compare&swap which is in it.
 * The choice was to duplicate a lot of relatively difficult code in the "right" class
 * (java.util.concurrent.locks.AbstractQueuedSynchronizer) or a small amount of straight forward
 * code in the "wrong" class (sun.misc.Unsafe). Knowing a bit about the "library chase" game,
 * we opt for the latter
 *
 * <2do> this might change with better modeling of high level java.util.concurrent constructs
 */
public class JPF_sun_misc_Unsafe {

  public  int getUnsafe____Lsun_misc_Unsafe_2 (MJIEnv env, int clsRef) {
    int objRef = env.getStaticReferenceField("sun.misc.Unsafe", "theUnsafe");
    return objRef;
  }

  public static long objectFieldOffset__Ljava_lang_reflect_Field_2__J (MJIEnv env, int unsafeRef, int fieldRef) {
    return fieldOffset__Ljava_lang_reflect_Field_2__I(env, unsafeRef, fieldRef);
  }

  /**
   * we don't really return an offset here, since that would be useless. What we really want is
   * to identify the corresponding FieldInfo, and that's much easier done with the Field
   * registration id
   */
  public static int fieldOffset__Ljava_lang_reflect_Field_2__I (MJIEnv env, int unsafeRef, int fieldRef) {
    //FieldInfo fi = JPF_java_lang_reflect_Field.getFieldInfo(env, fieldRef);
    //return fi.getStorageOffset();
    return env.getIntField(fieldRef, "regIdx");
  }

  public static boolean compareAndSwapObject__Ljava_lang_Object_2JLjava_lang_Object_2Ljava_lang_Object_2__Z (MJIEnv env, int unsafeRef,
                                                                                                             int objRef, long fieldOffset,
                                                                                                             int expectRef, int updateRef) {
    int actual = getObject__Ljava_lang_Object_2J__Ljava_lang_Object_2(env, unsafeRef, objRef, fieldOffset);
    if (actual == expectRef) {
      putObject__Ljava_lang_Object_2JLjava_lang_Object_2__V(env, unsafeRef, objRef, fieldOffset, updateRef);
      return true;
    }
    return false;
  }

  public static boolean compareAndSwapInt__Ljava_lang_Object_2JII__Z (MJIEnv env, int unsafeRef,
                                                                      int objRef, long fieldOffset, int expect, int update) {
    int actual = getInt__Ljava_lang_Object_2J__I(env, unsafeRef, objRef, fieldOffset);
    if (actual == expect) {
      putInt__Ljava_lang_Object_2JI__V(env, unsafeRef, objRef, fieldOffset, update);
      return true;
    }
    return false;
  }

  public static boolean compareAndSwapLong__Ljava_lang_Object_2JJJ__Z (MJIEnv env, int unsafeRef,
                                                                       int objRef, long fieldOffset, long expect, long update) {
    long actual = getLong__Ljava_lang_Object_2J__J(env, unsafeRef, objRef, fieldOffset);
    if (actual == expect) {
      putLong__Ljava_lang_Object_2JJ__V(env, unsafeRef, objRef, fieldOffset, update);
      return true;
    }
    return false;
  }


  // this is a specialized, native wait that does not require a lock, and that can
  // be turned off by a preceding unpark() call (which is not accumulative)
  // park can be interrupted, but it doesn't throw an InterruptedException, and it doesn't clear the status

  public static void park__ZJ__V (MJIEnv env, int unsafeRef, boolean isAbsoluteTime, long timeout) {
    ThreadInfo ti = env.getThreadInfo();
    int objRef = ti.getThreadObjectRef();
    int permitRef = env.getReferenceField( objRef, "permit");
    ElementInfo ei = env.getElementInfo(permitRef);

    if (ti.isFirstStepInsn()){ // re-executed

      //assert ti.getLockObject() == null : "private 'permit' object locked";

      // notified | timedout | interrupted -> running
      switch (ti.getState()) {
        case NOTIFIED:
        case TIMEDOUT:
        case INTERRUPTED:
          ti.resetLockRef();
          ti.setRunning();
        default:
      }

    } else { // first time

      if (ti.isInterrupted(false)) {
        // there is no lock, so we go directly back to running and therefore
        // have to remove ourself from the contender list
        ei.setMonitorWithoutLocked(ti);
        
        // note that park() does not throw an InterruptedException
        return;
      }

      if (ei.getBooleanField("blockPark")) { // we have to wait, but don't need a lock

        // running -> waiting | timeout_waiting
        ei.wait(ti, timeout, false);

        assert ti.isWaiting();


        // note we pass in the timeout value, since this might determine the type of CG that is created
        ChoiceGenerator<?> cg = env.getSchedulerFactory().createWaitCG(ei, ti, timeout);
        env.setMandatoryNextChoiceGenerator(cg, "no CG on blocking park()");
        env.repeatInvocation();
  
      } else {
        ei.setBooleanField("blockPark", true); // next time
      }
    }
  }

  public static void unpark__Ljava_lang_Object_2__V (MJIEnv env, int unsafeRef, int objRef) {
    ThreadInfo ti = env.getThreadInfo();
    ThreadInfo tiParked = JPF_java_lang_Thread.getThreadInfo(objRef);
    
    if (tiParked.isTerminated()){
      return;
    }
    
    SystemState ss = env.getSystemState();

    int permitRef = env.getReferenceField( objRef, "permit");
    ElementInfo ei = env.getElementInfo(permitRef);

    if (tiParked.getLockObject() == ei){
      ei.notifies(ss, ti, false);
    } else {
      ei.setBooleanField("blockPark", false);
    }
  }

  public static void ensureClassInitialized__Ljava_lang_Class_2__V (MJIEnv env, int unsafeRef, int clsObjRef) {
    // <2do> not sure if we have to do anyting here - if we have a class object, the class should already
    // be initialized
  }

  public static int getObject__Ljava_lang_Object_2J__Ljava_lang_Object_2 (MJIEnv env, int unsafeRef,
                                                                          int objRef, long fieldOffset) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      return ei.getReferenceField(fi);
    } else {
      return ei.getReferenceElement((int)fieldOffset);
    }
  }

  public static void putObject__Ljava_lang_Object_2JLjava_lang_Object_2__V (MJIEnv env, int unsafeRef,
                                                                            int objRef, long fieldOffset, int valRef) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      ei.setReferenceField(fi, valRef);
    } else {
      ei.setReferenceElement((int)fieldOffset, valRef);
    }
  }

  public static void putOrderedObject__Ljava_lang_Object_2JLjava_lang_Object_2__V(
                                                                                  MJIEnv env,
                                                                                  int unsafeRef,
                                                                                  int objRef,
                                                                                  long fieldOffset,
                                                                                  int valRef) {
    putObject__Ljava_lang_Object_2JLjava_lang_Object_2__V(env, unsafeRef, objRef, fieldOffset, valRef);
  }
  
  public static boolean getBoolean__Ljava_lang_Object_2J__Z(MJIEnv env,
                                                            int unsafeRef,
                                                            int objRef,
                                                            long fieldOffset) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      return ei.getBooleanField(fi);
    } else {
      return ei.getBooleanElement((int)fieldOffset);
    }
  }
  
  public static void putBoolean__Ljava_lang_Object_2JZ__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, boolean val){
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      ei.setBooleanField(fi, val);
    } else {
      ei.setBooleanElement((int)fieldOffset, val);
    }
  }

  public static byte getByte__Ljava_lang_Object_2J__B(MJIEnv env,
                                                      int unsafeRef,
                                                      int objRef,
                                                      long fieldOffset) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      return ei.getByteField(fi);
    } else {
      return ei.getByteElement((int)fieldOffset);
    }
  }
  
  public static void putByte__Ljava_lang_Object_2JB__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, byte val){
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      ei.setByteField(fi, val);
    } else {
      ei.setByteElement((int)fieldOffset, val);
    }
  }

  public static char getChar__Ljava_lang_Object_2J__C(MJIEnv env,
                                                      int unsafeRef,
                                                      int objRef,
                                                      long fieldOffset) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      return ei.getCharField(fi);
    } else {
      return ei.getCharElement((int)fieldOffset);
    }
  }
  
  public static void putChar__Ljava_lang_Object_2JC__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, char val){
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      ei.setCharField(fi, val);
    } else {
      ei.setCharElement((int)fieldOffset, val);
    }
  }

  public static short getShort__Ljava_lang_Object_2J__S(MJIEnv env,
                                                        int unsafeRef,
                                                        int objRef,
                                                        long fieldOffset) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      return ei.getShortField(fi);
    } else {
      return ei.getShortElement((int)fieldOffset);
    }
  }

  public static void putShort__Ljava_lang_Object_2JS__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, short val){
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      ei.setShortField(fi, val);
    } else {
      ei.setShortElement((int)fieldOffset, val);
    }
  }

  public static int getInt__Ljava_lang_Object_2J__I(MJIEnv env, int unsafeRef,
                                                    int objRef, long fieldOffset) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      return ei.getIntField(fi);
    } else {
      return ei.getIntElement((int)fieldOffset);
    }
  }

  public static void putInt__Ljava_lang_Object_2JI__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, int val){
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      ei.setIntField(fi, val);
    } else {
      ei.setIntElement((int)fieldOffset, val);
    }
  }

  public static void putOrderedInt__Ljava_lang_Object_2JI__V(MJIEnv env,
                                                             int unsafeRef,
                                                             int objRef,
                                                             long fieldOffset,
                                                             int val) {
    // volatile?
    putInt__Ljava_lang_Object_2JI__V(env, unsafeRef, objRef, fieldOffset, val);
  }

  public static float getFloat__Ljava_lang_Object_2J__F(MJIEnv env,
                                                        int unsafeRef,
                                                        int objRef,
                                                        long fieldOffset) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      return ei.getFloatField(fi);
    } else {
      return ei.getFloatElement((int)fieldOffset);
    }
  }

  public static void putFloat__Ljava_lang_Object_2JF__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, float val){
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      ei.setFloatField(fi, val);
    } else {
      ei.setFloatElement((int)fieldOffset, val);
    }
  }

  public static long getLong__Ljava_lang_Object_2J__J(MJIEnv env,
                                                      int unsafeRef,
                                                      int objRef,
                                                      long fieldOffset) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      return ei.getLongField(fi);
    } else {
      return ei.getLongElement((int)fieldOffset);
    }
  }

  public static void putLong__Ljava_lang_Object_2JJ__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, long val){
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      ei.setLongField(fi, val);
    } else {
      ei.setLongElement((int)fieldOffset, val);
    }
  }

  public static void putOrderedLong__Ljava_lang_Object_2JJ__V (MJIEnv env, int unsafeRef,
                                                        int objRef, long fieldOffset, long val) {
    putLong__Ljava_lang_Object_2JJ__V(env, unsafeRef, objRef, fieldOffset, val);
  }

  public static double getDouble__Ljava_lang_Object_2J__D(MJIEnv env,
                                                         int unsafeRef,
                                                         int objRef,
                                                         long fieldOffset) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      return ei.getDoubleField(fi);
    } else {
      return ei.getDoubleElement((int)fieldOffset);
    }
  }

  public static void putDouble__Ljava_lang_Object_2JD__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, double val){
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      ei.setDoubleField(fi, val);
    } else {
      ei.setDoubleElement((int)fieldOffset, val);
    }
  }

  public static int arrayBaseOffset__Ljava_lang_Class_2__I (MJIEnv env, int unsafeRef, int clazz) {
    return 0;
  }

  public static int arrayIndexScale__Ljava_lang_Class_2__I (MJIEnv env, int unsafeRef, int clazz) {
    return 1;
  }

  private static FieldInfo getRegisteredFieldInfo(long fieldOffset) {
    return JPF_java_lang_reflect_Field.getRegisteredFieldInfo((int)fieldOffset);
  }

}

