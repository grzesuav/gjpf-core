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
package gov.nasa.jpf.vm;

import static gov.nasa.jpf.vm.FieldLockInfo.empty;

/**
 * a threshold FieldLockInfo with a single lock candidate
 * This is the base version that does destructive updates. Override singleLockThresholdFli for a persistent version
 */
public class SingleLockThresholdFli extends ThresholdFieldLockInfo {
    protected int lockRef;
    
    SingleLockThresholdFli (ThreadInfo ti, int lockRef, int remainingChecks) {
      super( remainingChecks);
      
      tiLastCheck = ti;
      this.lockRef = lockRef;
    }

    protected int[] getCandidateLockSet() {
      int[] set = { lockRef };
      return set;
    }

    /**
     * override this for path local flis
     */
    protected SingleLockThresholdFli singleLockThresholdFli (ThreadInfo ti, int lockRef, int remainingChecks){
      this.lockRef = lockRef;
      this.tiLastCheck = ti;
      this.remainingChecks = remainingChecks;

      return this;
    }
    
    public FieldLockInfo checkProtection (ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
      int[] currentLockRefs = ti.getLockedObjectReferences();
      int nLocks = currentLockRefs.length;
      int nRemaining = Math.max(0, remainingChecks--);
            
      for (int i=0; i<nLocks; i++) {
        if (currentLockRefs[i] == lockRef) {
          return singleLockThresholdFli( ti, lockRef, nRemaining);
        }
      }
      
      checkFailedLockAssumption(ti, ei, fi);
      return empty;
    }

    /**
     * only called at the end of the gc on all live objects. The recycled ones
     * are either already nulled in the heap, or are not marked as live
     */
    public FieldLockInfo cleanUp (Heap heap) {
      ElementInfo ei = heap.get(lockRef);
      if (!heap.isAlive(ei)) {
        return FieldLockInfo.empty;
      } else {
        return this;
      }
    }

    public String toString() {
      return ("SingleLockThresholdFli {remainingChecks="+remainingChecks+",lock="+lockRef + '}');
    }  

}
