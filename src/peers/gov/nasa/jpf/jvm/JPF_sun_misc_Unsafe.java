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


  // these just encapsulate waits on a very private Thread field object ("permit"). wait() requires
  // owning the lock, but the trick is to not expose any lock that would cause JPF to break
  // the transition and add additional states (e.g. by synchronizing on the Unsafe object itself),
  // hence we have to do the lock/unlock explicitly from within park/unpark. In case of the
  // park(), this is slightly more complicated since it might get re-executed

  public static void park__ZJ__V (MJIEnv env, int unsafeRef, boolean isAbsoluteTime, long timeout) {
    ThreadInfo ti = env.getThreadInfo();
    int objRef = ti.getThreadObjectRef();

    if (ti.isInterrupted(false)) {
      return;
    }

    int permitRef = env.getReferenceField( objRef, "permit");
    ElementInfo ei = env.getElementInfo(permitRef);

    // NOTE - this means the native Object.wait() bottom half has to handle
    // being called in a RUNNING state (which otherwise won't happen), or we
    // get invalid thread state exceptions

    if (ei.getBooleanField("isTaken")) { // we have to block
      ei.lock(ti); // otherwise a subsequent wait will blow up
      JPF_java_lang_Object.wait__J__V(env,permitRef,timeout);

    } else {
      if (ti.isFirstStepInsn()) {  // somebody might have notified us
        JPF_java_lang_Object.wait__J__V(env,permitRef,timeout);
        ei.unlock(ti); // simulate the accompanying MONITOR_EXIT
      }
      ei.setBooleanField("isTaken", true);
    }
  }

  public static void unpark__Ljava_lang_Object_2__V (MJIEnv env, int unsafeRef, int objRef) {
    ThreadInfo ti = env.getThreadInfo();

    ThreadInfo t1 = JPF_java_lang_Thread.getThreadInfo(env, objRef); // needed? -pcd
    int permitRef = env.getReferenceField( objRef, "permit");
    ElementInfo ei = env.getElementInfo(permitRef);

    ei.lock(ti);
    ei.setBooleanField("isTaken", false);
    ei.notifies(env.getSystemState(), ti);
    ei.unlock(ti);
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
      return ei.getElement((int)fieldOffset);
    }
  }

  public static void putObject__Ljava_lang_Object_2JLjava_lang_Object_2__V (MJIEnv env, int unsafeRef,
                                                                            int objRef, long fieldOffset, int valRef) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      ei.setReferenceField(fi, valRef);
    } else {
      ei.setElement((int)fieldOffset, valRef);
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
    return Types.intToBoolean(getInt__Ljava_lang_Object_2J__I(env, unsafeRef, objRef, fieldOffset));
  }
  
  public static void putBoolean__Ljava_lang_Object_2JZ__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, boolean val){
    putInt__Ljava_lang_Object_2JI__V(env, unsafeRef, objRef, fieldOffset, Types.booleanToInt(val));
  }

  public static byte getByte__Ljava_lang_Object_2J__B(MJIEnv env,
                                                      int unsafeRef,
                                                      int objRef,
                                                      long fieldOffset) {
    return (byte) getInt__Ljava_lang_Object_2J__I(env, unsafeRef, objRef, fieldOffset);
  }
  
  public static void putByte__Ljava_lang_Object_2JB__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, byte val){
    putInt__Ljava_lang_Object_2JI__V(env, unsafeRef, objRef, fieldOffset, val);
  }

  public static char getChar__Ljava_lang_Object_2J__C(MJIEnv env,
                                                      int unsafeRef,
                                                      int objRef,
                                                      long fieldOffset) {
    return (char) getInt__Ljava_lang_Object_2J__I(env, unsafeRef, objRef, fieldOffset);
  }
  
  public static void putChar__Ljava_lang_Object_2JC__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, char val){
    putInt__Ljava_lang_Object_2JI__V(env, unsafeRef, objRef, fieldOffset, val);
  }

  public static short getShort__Ljava_lang_Object_2J__S(MJIEnv env,
                                                        int unsafeRef,
                                                        int objRef,
                                                        long fieldOffset) {
    return (short) getInt__Ljava_lang_Object_2J__I(env, unsafeRef, objRef, fieldOffset);
  }

  public static void putShort__Ljava_lang_Object_2JS__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, short val){
    putInt__Ljava_lang_Object_2JI__V(env, unsafeRef, objRef, fieldOffset, val);
  }

  public static int getInt__Ljava_lang_Object_2J__I(MJIEnv env, int unsafeRef,
                                                    int objRef, long fieldOffset) {
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      return ei.getIntField(fi);
    } else {
      return ei.getElement((int)fieldOffset);
    }
  }

  public static void putInt__Ljava_lang_Object_2JI__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, int val){
    ElementInfo ei = env.getElementInfo(objRef);
    if (!ei.isArray()) {
      FieldInfo fi = getRegisteredFieldInfo(fieldOffset);
      ei.setIntField(fi, val);
    } else {
      ei.setElement((int)fieldOffset, val);
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
    return Types.intToFloat(getInt__Ljava_lang_Object_2J__I(env, unsafeRef, objRef, fieldOffset));
  }

  public static void putFloat__Ljava_lang_Object_2JF__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, float val){
    putInt__Ljava_lang_Object_2JI__V(env, unsafeRef, objRef, fieldOffset, Types.floatToInt(val));
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
    return Types.longToDouble(getLong__Ljava_lang_Object_2J__J(env, unsafeRef, objRef, fieldOffset));
  }

  public static void putDouble__Ljava_lang_Object_2JD__V (MJIEnv env, int unsafeRef,
                                                       int objRef, long fieldOffset, double val){
    putLong__Ljava_lang_Object_2JJ__V(env, unsafeRef, objRef, fieldOffset, Types.doubleToLong(val));
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

