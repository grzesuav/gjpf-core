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

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;


/**
 * Enter monitor for object
 * ..., objectref => ...
 */
public class MONITORENTER extends LockInstruction {

  public Instruction execute (ThreadInfo ti) {
    Scheduler scheduler = ti.getScheduler();
    StackFrame frame = ti.getTopFrame();

    int objref = frame.peek();      // Don't pop yet before we know we really enter
    if (objref == MJIEnv.NULL){
      return ti.createAndThrowException("java.lang.NullPointerException", "Attempt to acquire lock for null object");
    }

    lastLockRef = objref;
    ElementInfo ei = ti.getModifiableElementInfo(objref);    
    ei = scheduler.updateObjectSharedness(ti, ei, null); // locks most likely belong to shared objects
    
    if (!ti.isLockOwner(ei)){ // we only need to register, block and/or reschedule if this is not a recursive lock
      if (ei.canLock(ti)) {
        // record that this thread would lock the object upon next execution if we break the transition
        // (note this doesn't re-add if already registered)
        ei.registerLockContender(ti);
        if (scheduler.setsLockAcquisitionCG(ti, ei)) { // optional scheduling point
          return this;
        }
        
      } else { // we need to block
        ei.block(ti); // this means we only re-execute once we can acquire the lock
        if (scheduler.setsBlockedThreadCG(ti, ei)){ // mandatory scheduling point
          return this;
        }
        throw new JPFException("blocking MONITORENTER without transition break");            
      }
    }
    
    //--- bottom half or lock acquisition succeeded without transition break
    frame = ti.getModifiableTopFrame(); // now we need to modify it
    frame.pop();
    
    ei.lock(ti);  // mark object as locked, increment the lockCount, and set the thread as owner
    
    return getNext(ti);
  }  

  public int getByteCode () {
    return 0xC2;
  }
  
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
