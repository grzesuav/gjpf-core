//
// Copyright (C) 2013 United States Government as represented by the
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

package gov.nasa.jpf.vm;

import gov.nasa.jpf.annotation.MJI;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * simple forwarding sun.misc.Hashing peer to speed up execution and shorten traces, especially
 * for system initialization
 * 
 * NOTE - this peer is only used under Java 7. Since Java 8 doesn't have a sun.misc.Hashing class,
 * we have to use reflection and MethodHandles for delegation
 */
public class JPF_sun_misc_Hashing extends NativePeer {
    
  MethodHandle byte_murmur3_32;
  MethodHandle char_murmur3_32;
  MethodHandle int_murmur3_32;
  MethodHandle stringHash32;
  MethodHandle randomHashSeed;
  
  
  public JPF_sun_misc_Hashing () throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
    Class<?> hashingCls = Class.forName("sun.misc.Hashing");
    MethodHandles.Lookup mhLookup = MethodHandles.lookup();

    MethodType mt = MethodType.methodType( int.class,  int.class,byte[].class,int.class,int.class);
    byte_murmur3_32 = mhLookup.findStatic( hashingCls, "murmur3_32", mt);
    
    mt = MethodType.methodType(int.class,  int.class,char[].class,int.class,int.class);
    char_murmur3_32 = mhLookup.findStatic( hashingCls, "murmur3_32", mt);

    mt = MethodType.methodType(int.class,  int.class,int[].class,int.class,int.class);
    int_murmur3_32 = mhLookup.findStatic( hashingCls, "murmur3_32", mt);

    mt = MethodType.methodType(int.class, String.class);
    stringHash32 = mhLookup.findStatic( hashingCls, "stringHash32", mt);
    
    mt = MethodType.methodType(int.class, Object.class);
    randomHashSeed = mhLookup.findStatic( hashingCls, "randomHashSeed", mt);
  }
  
  @MJI 
  public int murmur3_32__I_3BII__I (MJIEnv env, int clsRef, int seed, int dataRef, int offset, int len) throws Throwable {
    byte[] data = env.getByteArrayObject(dataRef);
    return (Integer)byte_murmur3_32.invoke( seed, data, offset, len);
  }
  
  @MJI 
  public int murmur3_32__I_3CII__I (MJIEnv env, int clsRef, int seed, int dataRef, int offset, int len) throws Throwable {
    char[] data = env.getCharArrayObject(dataRef);
    return (Integer)char_murmur3_32.invoke( seed, data, offset, len);
  }
  
  @MJI 
  public int murmur3_32__I_3III__I (MJIEnv env, int clsRef, int seed, int dataRef, int offset, int len) throws Throwable {
    int[] data = env.getIntArrayObject(dataRef);
    return (Integer)int_murmur3_32.invoke( seed, data, offset, len);
  }

  @MJI
  public int stringHash32__Ljava_lang_String_2__I (MJIEnv env, int clsRef, int sRef) throws Throwable {
    String s = env.getStringObject(sRef);
    return (Integer)stringHash32.invoke(s);
  }
  
  @MJI
  public int randomHashSeed__Ljava_lang_Object_2__I (MJIEnv env, int clsRef, int objRef) throws Throwable {
    // <2do> since the original implementation uses things like System.currentTimeMillis()
    // we have to model this to make it reproducible between JPF runs
    return (Integer)randomHashSeed.invokeExact(Integer.valueOf(objRef));
  }

}
