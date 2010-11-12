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

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;

import org.apache.bcel.classfile.ConstantPool;


/**
 * Enter monitor for object
 * ..., objectref => ...
 */
public class MONITORENTER extends LockInstruction {

  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int objref = ti.peek();      // Don't pop yet before we know we really execute

    if (objref == -1){
      return ti.createAndThrowException("java.lang.NullPointerException", "Attempt to acquire lock for null object");
    }

    lastLockRef = objref;
    ElementInfo ei = ks.heap.get(objref);
    
    if (!isLockOwner(ti, ei)){           // If the object isn't already owned by this thread, then consider a choice point
      if (isShared(ti, ei)){             // If the object is shared, then consider a choice point
        if (!ti.isFirstStepInsn()){      // First time around - reexecute if the scheduling policy gives us a choice point
          if (executeChoicePoint(ss, ti, ei)){
            return this;                // Repeat execution.  Keep instruction on the stack.
          }
        }
      }
    }
    
    ti.pop();
    ei.lock(ti);  // Still have to increment the lockCount
    
    return getNext(ti);
  }  
  
  private boolean executeChoicePoint(SystemState ss, ThreadInfo ti, ElementInfo ei) {
    if (!ei.canLock(ti)){
      ei.block(ti);          // block first, so that we don't get this thread in the list of CG choices
    }

    ChoiceGenerator<?> cg = ss.getSchedulerFactory().createMonitorEnterCG(ei, ti);

    if (cg == null) {                   // Ok, break here
      assert !ti.isBlocked() : "scheduling policy did not return ChoiceGenerator for blocking MONITOR_ENTER";
      return(false);
    }

    if (!ti.isBlocked()){
      ei.registerLockContender(ti);  // Record that this thread would lock the object upon next execution
    }
 
    ss.setNextChoiceGenerator(cg);
    
    return(true);
  }

  public int getByteCode () {
    return 0xC2;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
