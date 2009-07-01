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
package sun.misc;

import java.lang.reflect.Field;

/**
 * Unsafe = unwanted. See comments in the native peer. We only have it because
 * it is required by java.util.concurrent.
 *
 * Note that in the real world, this class is only callable from the system library,
 * not application code
 */

public class Unsafe {
  private static final Unsafe singleton = new Unsafe();

  public static final int INVALID_FIELD_OFFSET = -1;

  public static Unsafe getUnsafe() {
    return singleton;
    //return new Unsafe();
  }

  // field offsets are completely useless between VMs, we just return
  // a numeric id for the corresponding FieldInfo here
  public native int fieldOffset (Field f);
  public native long objectFieldOffset (Field f);

  // those do the usual CAS magic
  public native boolean compareAndSwapObject (Object oThis, long offset, Object expect, Object update);
  public native boolean compareAndSwapInt (Object oThis, long offset, int expect, int update);
  public native boolean compareAndSwapLong (Object oThis, long offset, long expect, long update);

  // that looks like some atomic conditional wait
  public native void park (boolean dontknow, long timeout);
  public native void unpark (Object thread);

  // various accessors
  public native int getInt(Object obj, long l);

  public native void putInt(Object obj, long l, int i);

  public native Object getObject(Object obj, long l);

  public native void putObject(Object obj, long l, Object obj1);

  public native boolean getBoolean(Object obj, long l);

  public native void putBoolean(Object obj, long l, boolean flag);

  public native byte getByte(Object obj, long l);

  public native void putByte(Object obj, long l, byte byte0);

  public native short getShort(Object obj, long l);

  public native void putShort(Object obj, long l, short word0);

  public native char getChar(Object obj, long l);

  public native void putChar(Object obj, long l, char c);

  public native long getLong(Object obj, long l);

  public native void putLong(Object obj, long l, long l1);

  public native float getFloat(Object obj, long l);

  public native void putFloat(Object obj, long l, float f);

  public native double getDouble(Object obj, long l);

  public native void putDouble(Object obj, long l, double d);

  public native void ensureClassInitialized(Class<?> cls);
}
