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
package gov.nasa.jpf.jvm.bytecode;

import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * Exit monitor for object 
 * ..., objectref => ... 
 */
public class MONITOREXIT extends LockInstruction {

  public Instruction execute (ThreadInfo ti) {
    boolean didUnblock = false;
    StackFrame frame = ti.getTopFrame();
    Scheduler scheduler = ti.getScheduler();
    
    int objref = frame.peek();
    if (objref == MJIEnv.NULL) {
      return ti.createAndThrowException("java.lang.NullPointerException", "attempt to release lock for null object");
    }

    lastLockRef = objref;
    ElementInfo ei = ti.getElementInfo(objref);
    ei = scheduler.updateObjectSharedness(ti, ei, null); // locks most likely belong to shared objects
    
    if (!ti.isFirstStepInsn()){
      ei = ei.getModifiableInstance();
      // we only do this in the top half of the first execution, but before potentially creating
      // a CG so that blocked threads are runnable again
      didUnblock = ei.unlock(ti);
    }
    
    if (ei.getLockCount() == 0) { // might still be recursively locked, which wouldn't be a release
      if (scheduler.setsLockReleaseCG(ti, ei, didUnblock)){ // scheduling point
        return this;
      }
    }

    // bottom half or monitorexit proceeded
    frame = ti.getModifiableTopFrame();
    frame.pop();

    return getNext(ti);
  }

  public int getByteCode () {
    return 0xC3;
  }
  
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
