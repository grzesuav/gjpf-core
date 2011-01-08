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

import gov.nasa.jpf.jvm.*;
import org.apache.bcel.classfile.ConstantPool;


/**
 * Exit monitor for object 
 * ..., objectref => ... 
 */
public class MONITOREXIT extends LockInstruction {

  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int objref = ti.peek();
    if (objref == -1) {
      return ti.createAndThrowException("java.lang.NullPointerException",
                                        "attempt to release lock for null object");
    }

    lastLockRef = objref;
    ElementInfo ei = ks.heap.get(objref);

    if (!ti.isFirstStepInsn()){
      int lockCount = ei.getLockCount();
      // unlock before registering any potential CG so that unblocked threads
      // can be considered too. Make sure this is only executed in the top half
      if (lockCount > 0){ // maybe lock acquisition was skipped
        ei.unlock(ti);

        if (lockCount == 1) { // this is about to unlock
          if (ei.isShared()) { // the monitor_enter should have set it shared
            ChoiceGenerator cg = ss.getSchedulerFactory().createMonitorExitCG(ei, ti);
            if (cg != null) {
              if (ss.setNextChoiceGenerator(cg)){
                return this;
              }
            }
          }
        }
      }
    }

    ti.pop();

    return getNext(ti);
  }


  public int getByteCode () {
    return 0xC3;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
