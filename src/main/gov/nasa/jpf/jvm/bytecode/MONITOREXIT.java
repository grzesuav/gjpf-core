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
    ElementInfo ei = ks.da.get(objref);      
    
    if (!ti.isFirstStepInsn()) {
      // do that before potentially creating the CG, but don't pop yet, since then we've
      // lost the lock object (also in RETURN)
      ei.unlock(ti);

      ChoiceGenerator cg = ss.getSchedulerFactory().createMonitorExitCG(ei, ti);
      if (cg != null) {
        ss.setNextChoiceGenerator(cg);
        //ti.skipInstructionLogging();
        return this; // re-execute
      }
    }    

    ti.pop(); // now we can safely purge the lock object, the unlocking alreday is done
    
    return getNext(ti);
  }
  
  public int getByteCode () {
    return 0xC3;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
