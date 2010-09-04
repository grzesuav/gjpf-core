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


/**
 * common root for MONITORENTER/EXIT
 */
public abstract class LockInstruction extends Instruction {
  int lastLockRef = -1;
  private boolean m_skipLocalSync;          // Can't store this in a static since there might be multiple VM instances.
  private boolean m_skipLocalSyncSet;

  /**
    * only useful post-execution (in an instructionExecuted() notification)
    */
  public int getLastLockRef () {
    return lastLockRef;
  }
  
  public ElementInfo getLastLockElementInfo() {
    if (lastLockRef != -1) {
      return DynamicArea.getHeap().get(lastLockRef);
    }
    
    return null;
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
    if (!getSkipLocalSync(ti)) {
      return true;
    }
    
    return ei.isShared();
  }

  private boolean getSkipLocalSync(ThreadInfo ti) {
    if (!m_skipLocalSyncSet) {
      m_skipLocalSync    = ti.getVM().getConfig().getBoolean("vm.por.skip_local_sync", false);  // Default is false to keep original behavior.
      m_skipLocalSyncSet = true;
    }

    return m_skipLocalSync;
  }

  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
