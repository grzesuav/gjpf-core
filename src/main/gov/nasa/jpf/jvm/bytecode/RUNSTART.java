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

import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;

import org.apache.bcel.classfile.ConstantPool;

/**
 * this is an artificial instruction that is automatically prepended to
 * a run() method. Comes in handy to avoid CG confusion if the first insn
 * otherwise is using a CG, and can also be used to grab the lock if this
 * is a synchronized run
 *
 * NOTE we can't do the full CG handling here because there is no way
 * telling if this is reexecuted - ThreadInfo.isFirstStepInsn() will always
 * return true, since this is always the first insn after a context switch.
 * thread starting is a bit weird
 */
public class RUNSTART extends Instruction {

  public RUNSTART (MethodInfo runMth) {
    this.mi = runMth;
    this.offset = -1;
    this.position = -1;
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {

    ti.setState(ThreadInfo.State.RUNNING);  // The thread is now running.  Need to update the thread state accordingly.

    // if this is the first insn in a synchronized run(), we also have to
    // grab the lock
    if (mi.isSynchronized()) {
      ElementInfo ei = ti.getVM().getDynamicArea().get(ti.getThis());
      ei.lock(ti);
    }

    return mi.getInstruction(0);
  }


  public static final int OPCODE = 257;

  public int getByteCode () {
    return OPCODE;
  }

  public boolean isExtendedInstruction() {
    return true;
  }

  protected void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
    // nothing - this is initialized explicitly, not from BCEL loaded insns
  }



}
