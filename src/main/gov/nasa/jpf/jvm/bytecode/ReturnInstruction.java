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
        int objref = mi.isStatic() ? mi.getClassInfo().getClassObjectRef() : ti.getThis();
        ElementInfo ei = ti.getElementInfo(objref);

        if (ei.getLockCount() == 0){
          if (ei.checkUpdatedSharedness(ti)) {
            ChoiceGenerator<ThreadInfo> cg = ss.getSchedulerFactory().createSyncMethodExitCG(ei, ti);
            if (cg != null) {
              if (ss.setNextChoiceGenerator(cg)) {
                ti.skipInstructionLogging();
                return this; // re-execute
              }
            }
          }
        }
      }
    }

    returnFrame = ti.getTopFrame();
    Object attr = getReturnAttr(ti); // do this before we pop
    storeReturnValue(ti);

    if (ti.getStackDepth() == 1) { // done - last stackframe in this thread
      int objref = ti.getThreadObjectRef();
      ElementInfo ei = ks.heap.get(objref);

      // beware - this notifies all waiters on this thread (e.g. in a join())
      // hence it has to be able to acquire the lock
      if (!ei.canLock(ti)) {
        // block first, so that we don't get this thread in the list of CGs
        ei.block(ti);

        // if we can't acquire the lock, it means there needs to be another thread alive,
        // but it might not be runnable (deadlock) and we don't want to mask that error
        ChoiceGenerator<ThreadInfo> cg = ss.getSchedulerFactory().createMonitorEnterCG(ei, ti);
        ss.setMandatoryNextChoiceGenerator(cg, "blocking thread termination without CG: ");
        return this;

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

        ss.clearAtomic();
        if (ti.hasOtherRunnables()){
          ChoiceGenerator<ThreadInfo> cg = ss.getSchedulerFactory().createThreadTerminateCG(ti);
          ss.setMandatoryNextChoiceGenerator(cg, "thread terminated without CG: ");
        }

        ti.popFrame(); // we need to do this *after* setting the CG (so that we still have a CG insn)
        return null;
      }

    } else { // there are still frames on the stack
      StackFrame top = ti.popFrame();

      // remove args, push return value and continue with next insn
      // (DirectCallStackFrames don't use this)
      ti.removeArguments(mi);
      pushReturnValue(ti);

      if (attr != null) {
        setReturnAttr(ti, attr);
      }

      return top.getPC().getNext();
    }
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
