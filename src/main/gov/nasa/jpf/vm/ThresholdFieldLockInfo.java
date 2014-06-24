//
// Copyright (C) 2008 United States Government as represented by the
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

import gov.nasa.jpf.JPFException;

/**
 * a FieldLockInfo that assumes lock protection after n accesses with
 * non-empty lock sets
 */
public abstract class ThresholdFieldLockInfo extends FieldLockInfo implements Cloneable {
  protected int remainingChecks;

  protected ThresholdFieldLockInfo(int remainingChecks) {
    this.remainingChecks = remainingChecks;
  }

  public boolean isProtected() {
    // otherwise this would have turned into a EmptyFieldLockInfo
    return (remainingChecks == 0);
  }

  protected void checkFailedLockAssumption(ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
    if (remainingChecks == 0) {
      // with no locks remaining this would have been demoted to an
      // EmptyFieldLockInfo by now
      lockAssumptionFailed(ti, ei, fi);
    }
  }
  
  /**
   * this implements a path-local FieldLockInfo that are never mutated
   * this has to be overridden for search global FieldLockInfos
   */
  protected FieldLockInfo getInstance (int nRemaining){
    try {
      ThresholdFieldLockInfo fli = (ThresholdFieldLockInfo)clone();
      fli.remainingChecks = nRemaining;
      return fli;
              
    } catch (CloneNotSupportedException cnsx){
      throw new JPFException("clone of ThresholdFieldLockInfo failed: " + this);
    }
  }
}
