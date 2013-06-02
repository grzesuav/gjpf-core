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
import sun.misc.Hashing;

/**
 * simple forwarding sun.misc.Hashing peer to speed up execution and shorten traces
 */
public class JPF_sun_misc_Hashing extends NativePeer {

  @MJI
  public int murmur3_32___3I__I (MJIEnv env, int clsRef, int dataRef){
    int[] data = env.getIntArrayObject(dataRef);
    return Hashing.murmur3_32(0, data, 0, data.length);
  }
  
  @MJI 
  public int murmur3_32__I_3III__I (MJIEnv env, int clsRef, int seed, int dataRef, int offset, int len){
    int[] data = env.getIntArrayObject(dataRef);
    return Hashing.murmur3_32(seed, data, offset, len);
  }
  
  //... <2do> and many more
}
