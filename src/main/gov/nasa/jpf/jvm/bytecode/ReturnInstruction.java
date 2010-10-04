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
import gov.nasa.jpf.jvm.NativeMethodInfo;
import gov.nasa.jpf.jvm.NativeStackFrame;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * abstraction for the various return instructions
 */
public abstract class ReturnInstruction extends Instruction {

  // to store where we came from
  protected StackFrame returnFrame;

  // note these are only callable from within the same execute - thread interleavings
  // would cause races
  abstract protected void storeReturnValue (ThreadInfo th);
  abstract protected void pushReturnValue (ThreadInfo th);

  public abstract Object getReturnValue(ThreadInfo ti);

  public StackFrame getReturnFrame() {
    return returnFrame;
  }

  public void setReturnFrame(StackFrame frame){
    returnFrame = frame;
  }

  // override if this is a void or double word return
  public Object getReturnAttr (ThreadInfo ti) {
    return ti.getOperandAttr();
  }

  public void setReturnAttr (ThreadInfo ti, Object attr){
    if (attr != null){
      ti.setOperandAttrNoClone(attr);
    }
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {

    if (!ti.isFirstStepInsn()) {
      mi.leave(ti);  // takes care of unlocking before potentially creating a CG

      if (mi.isSynchronized()) {
        int objref = ti.getThis();
        ElementInfo ei = ks.da.get(objref);

        ChoiceGenerator<ThreadInfo> cg = ss.getSchedulerFactory().createSyncMethodExitCG(ei, ti);
        if (cg != null) {
          ss.setNextChoiceGenerator(cg);
          ti.skipInstructionLogging();
          return this; // re-execute
        }
      }
    }

    Object attr = getReturnAttr(ti);
    storeReturnValue(ti);
    returnFrame = ti.getTopFrame();

    if (ti.getStackDepth() == 1) { // done - last stackframe in this thread
      int objref = ti.getThreadObjectRef();
      ElementInfo ei = ks.da.get(objref);

      // beware - this notifies all waiters on this thread (e.g. in a join())
      // hence it has to be able to acquire the lock
      if (!ei.canLock(ti)) {
        // block first, so that we don't get this thread in the list of CGs
        ei.block(ti);

        ChoiceGenerator<ThreadInfo> cg = ss.getSchedulerFactory().createMonitorEnterCG(ei, ti);
        if (cg != null) { // Ok, break here
          ss.setNextChoiceGenerator(cg);
        }
        return this; // we have to come back later, when we can acquire the lock

      } else { // Ok, we can get the lock, time to die
        // notify waiters on thread termination

        if (!ti.holdsLock(ei)){
          // we only need to increase the lockcount if we don't own the lock yet,
          // as is the case for synchronized run() in anonymous threads (otherwise
          // we have a lockcount > 1 and hence do not unlock upon return)
          ei.lock(ti);
        }

        ei.notifiesAll();
        ei.unlock(ti);

        ti.finish(); // cleanup

        ChoiceGenerator<ThreadInfo> cg = ss.getSchedulerFactory().createThreadTerminateCG(ti);
        if (cg != null) {
          ss.clearAtomic();  // no carry over of atomic levels between threads
          ss.setNextChoiceGenerator(cg);
        }

        ti.popFrame(); // we need to do this *after* setting the CG (so that we still have a CG insn)
        return null;
      }

    } else { // there are still frames on the stack
      ti.popFrame(); // do this *before* we push the return value

      Instruction nextPC = ti.getReturnFollowOnPC();
      if (nextPC != ti.getPC()) {
        // remove args and push return value
        ti.removeArguments(mi);
        pushReturnValue(ti);

        if (attr != null){
          setReturnAttr(ti, attr);
        }
      } else {
        // don't remove args and push return value, we repeat this insn!
      }

      return nextPC;
    }
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
