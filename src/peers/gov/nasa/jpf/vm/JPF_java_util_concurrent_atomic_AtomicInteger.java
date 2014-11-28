//
//Copyright (C) 2009 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.
//
//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.
//
//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.vm;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;

/**
* native peer for java.util.concurrent.atomic.AtomicInteger
* this implementation just cuts off native methods
*/
public class JPF_java_util_concurrent_atomic_AtomicInteger extends NativePeer {

  @MJI
  public void $clinit____V (MJIEnv env, int rcls) {
    // don't let this one pass, it calls native methods from non-public Sun classes
  }
 
  @MJI
  public boolean compareAndSet__II__Z (MJIEnv env, int objRef, int expect, int update){
    int value = env.getIntField(objRef, "value");
    if (value == expect){
      env.setIntField(objRef, "value", update);
      return true;
    } else {
      return false;
    }
  }
  
  @MJI
  public int getAndAdd__I__I (MJIEnv env, int objRef, int delta) {
    int value = env.getIntField(objRef, "value");
    env.setIntField(objRef, "value", value + delta);
    return value;
  }
  
  @MJI
  public int getAndIncrement____I (MJIEnv env, int objRef) {
    int value = env.getIntField(objRef, "value");
    env.setIntField(objRef, "value", value + 1);
    return value;
  }
  
  @MJI
  public int getAndDecrement____I (MJIEnv env, int objRef) {
    int value = env.getIntField(objRef, "value");
    env.setIntField(objRef, "value", value - 1);
    return value;
  }
  
  @MJI
  public void lazySet__I__V (MJIEnv env, int objRef, int newValue) {
    env.setIntField(objRef, "value", newValue);
  }

  @MJI
  public int getAndSet__I__I (MJIEnv env, int objRef, int newValue) {
    int value = env.getIntField(objRef, "value");
    env.setIntField(objRef, "value", newValue);
    return value;
  }

  @MJI
  public boolean weakCompareAndSet__II__Z (MJIEnv env, int objRef, int expect, int update) {
    int value = env.getIntField(objRef, "value");
    if (value == expect){
      env.setIntField(objRef, "value", update);
      return true;
    } else {
      return false;
    }
  }
  
  @MJI
  public int incrementAndGet____I (MJIEnv env, int objRef) {
    int value = env.getIntField(objRef, "value");
    value++;
    env.setIntField(objRef, "value", value);
    return value;
  }
  
  @MJI
  public int decrementAndGet____I (MJIEnv env, int objRef) {
    int value = env.getIntField(objRef, "value");
    value--;
    env.setIntField(objRef, "value", value);
    return value;
  }
  
  @MJI
  public int addAndGet__I__I (MJIEnv env, int objRef, int delta) {
    int value = env.getIntField(objRef, "value");
    value += delta;
    env.setIntField(objRef, "value", value);
    return value;
  }
}
