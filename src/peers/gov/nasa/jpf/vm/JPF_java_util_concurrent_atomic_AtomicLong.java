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
package gov.nasa.jpf.vm;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;

/**
 * native peer for java.util.concurrent.atomic.AtomicLong
 * this implementation just cuts off native methods
 */
public class JPF_java_util_concurrent_atomic_AtomicLong extends NativePeer {
  @MJI
  public void $clinit____V (MJIEnv env, int rcls) {
    // don't let this one pass, it calls native methods from non-public Sun classes
  }

  @MJI
  public boolean compareAndSet__JJ__Z (MJIEnv env, int objRef, long expect, long update){
    long value = env.getLongField(objRef, "value");
    if (value == expect){
      env.setLongField(objRef, "value", update);
      return true;
    } else {
      return false;
    }
  }
  
  @MJI
  public long getAndIncrement____J (MJIEnv env, int objRef){
    long value = env.getLongField(objRef, "value");
    env.setLongField(objRef, "value", value + 1);
    return value;
  }
  
  @MJI
  public long getAndDecrement____J (MJIEnv env, int objRef){
    long value = env.getLongField(objRef, "value");
    env.setLongField(objRef, "value", value - 1);
    return value;
  }

  @MJI
  public long getAndAdd__J__J (MJIEnv env, int objRef, long delta) {
    long value = env.getIntField(objRef, "value");
    env.setLongField(objRef, "value", value + delta);
    return value;
  }
  
  @MJI
  public long incrementAndGet____J (MJIEnv env, int objRef) {
    long value = env.getIntField(objRef, "value");
    value++;
    env.setLongField(objRef, "value", value);
    return value;
  }
  
  @MJI
  public long decrementAndGet____J (MJIEnv env, int objRef) {
    long value = env.getIntField(objRef, "value");
    value--;
    env.setLongField(objRef, "value", value);
    return value;
  }
  
  @MJI
  public long addAndGet__J__J (MJIEnv env, int objRef, long delta) {
    long value = env.getIntField(objRef, "value");
    value += delta;
    env.setLongField(objRef, "value", value);
    return value;
  }
}
