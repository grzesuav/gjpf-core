//
// Copyright (C) 2009 United States Government as represented by the
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

package gov.nasa.jpf.vm;

/**
 * base class for atomic field updaters
 */
public class AtomicFieldUpdater extends NativePeer {
  
  
  protected FieldInfo getFieldInfo (ElementInfo eiUpdater, ElementInfo eiTarget){
    int fidx = eiUpdater.getIntField("fieldId");
    return eiTarget.getClassInfo().getInstanceField(fidx);
  }
  
  /**
   * note - we are not interested in sharedness/interleaving of the AtomicUpdater object 
   * but in the object that is accessed by the updater
   */
  protected boolean reschedulesAccess (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    Scheduler scheduler = ti.getScheduler();
    Instruction insn = ti.getPC();
    
    if (scheduler.canHaveSharedObjectCG( ti, insn, ei, fi)){
      ei = scheduler.updateObjectSharedness( ti, ei, fi);
      return scheduler.setsSharedObjectCG( ti, insn, ei, fi);
    }
    
    return false;
  }

}
