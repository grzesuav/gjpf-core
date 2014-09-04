//
// Copyright (C) 2014 United States Government as represented by the
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

package gov.nasa.jpf.vm.bytecode;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * abstract base for InvokeInstructions
 */
public abstract class InvokeInstruction extends Instruction {

  public abstract MethodInfo getInvokedMethod();
  
  public abstract String getInvokedMethodName();
  public abstract String getInvokedMethodSignature();
  public abstract String getInvokedMethodClassName();
  
  /**
   * this does the lock registration/acquisition and respective transition break 
   * return true if the caller has to re-execute
   */
  protected boolean reschedulesLockAcquisition (ThreadInfo ti, ElementInfo ei){
    Scheduler scheduler = ti.getScheduler();
    ei = ei.getModifiableInstance();
    
    if (!ti.isLockOwner(ei)){ // we only need to register, block and/or reschedule if this is not a recursive lock
      if (ei.canLock(ti)) {
        // record that this thread would lock the object upon next execution if we break the transition
        // (note this doesn't re-add if already registered)
        ei.registerLockContender(ti);
        if (scheduler.setsLockAcquisitionCG(ti, ei)) { // optional scheduling point
          return true;
        }
        
      } else { // we need to block
        ei.block(ti); // this means we only re-execute once we can acquire the lock
        if (scheduler.setsBlockedThreadCG(ti, ei)){ // mandatory scheduling point
          return true;
        }
        throw new JPFException("blocking synchronized call without transition break");            
      }
    }
    
    // locking will be done by ti.enter()
    return false;
  }

}
