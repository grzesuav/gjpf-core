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
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * Invoke instance method; special handling for superclass, private,
 * and instance initialization method invocations
 * ..., objectref, [arg1, [arg2 ...]] => ...
 *
 * invokedMethod is supposed to be constant (ClassInfo can't change)
 */
public class INVOKESPECIAL extends InstanceInvocation {

  private boolean m_skipLocalSync;    // Can't store this in a static since there might be multiple VM instances.
  private boolean m_skipLocalSyncSet;

  public INVOKESPECIAL () {}

  public int getByteCode () {
    return 0xB7;
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int argSize = getArgSize();
    int objRef = ti.getCalleeThis( argSize);
    lastObj = objRef;

    // we don't have to check for NULL objects since this is either a ctor, a 
    // private method, or a super method

    MethodInfo mi = getInvokedMethod(ti);

    if (mi == null){
      return ti.createAndThrowException("java.lang.NoSuchMethodException", "Calling " + cname + "." + mname);
    }

    ElementInfo ei = ks.heap.get(objRef);

    if (mi.isSynchronized())
      if (!isLockOwner(ti, ei))           // If the object isn't already owned by this thread, then consider a choice point
        if (isShared(ti, ei)) {           // If the object is shared, then consider a choice point
          ChoiceGenerator<?> cg = getSyncCG(objRef, mi, ss, ks, ti);
          if (cg != null) {
            ss.setNextChoiceGenerator(cg);
            return this;   // repeat exec, keep insn on stack
          }
        }

    return mi.execute(ti);
  }

  /**
   * If the current thread already owns the lock, then the current thread can go on.
   * For example, this is a recursive acquisition.
   */
  protected boolean isLockOwner(ThreadInfo ti, ElementInfo ei) {
    return ei.getLockingThread() == ti;
  }

  /**
   * If the object will still be owned, then the current thread can go on.
   * For example, all but the last monitorexit for the object.
   */
  protected boolean isLastUnlock(ElementInfo ei) {
    return ei.getLockCount() == 1;
  }

  /**
   * If the object isn't shared, then the current thread can go on.
   * For example, this object isn't reachable by other threads.
   */
  protected boolean isShared(ThreadInfo ti, ElementInfo ei) {
    if (!getSkipLocalSync(ti))
      return true;

    //return ei.isShared();
    return ei.checkUpdatedSchedulingRelevance(ti);
  }

  private boolean getSkipLocalSync(ThreadInfo ti) {
    if (!m_skipLocalSyncSet) {
      m_skipLocalSync = ti.getVM().getConfig().getBoolean("vm.por.skip_local_sync", false); // Default is false to keep original behavior.
      m_skipLocalSyncSet = true;
    }

    return m_skipLocalSync;
  }

  /**
    * we can do some more caching here - the MethodInfo should be const
    */
  public MethodInfo getInvokedMethod (ThreadInfo th) {

    // since INVOKESPECIAL is only used for private methods and ctors,
    // we don't have to deal with null object calls

    if (invokedMethod == null) {
      ClassInfo ci = ClassInfo.getResolvedClassInfo(cname);
      invokedMethod = ci.getMethod(mname, true);
    }

    return invokedMethod; // we can store internally
  }

  public String toString() {
    MethodInfo callee = getInvokedMethod();

    return "invokespecial " + ((callee != null) ? callee.getFullName() : "?");
  }

  @Override
  public Object getFieldValue (String id, ThreadInfo ti) {
    int objRef = getCalleeThis(ti);
    ElementInfo ei = ti.getElementInfo(objRef);

    Object v = ei.getFieldValueObject(id);

    if (v == null){ // try a static field
      v = ei.getClassInfo().getStaticFieldValueObject(id);
    }

    return v;
  }

  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
