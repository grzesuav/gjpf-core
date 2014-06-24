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

/**
 * a threshold FieldLockInfo with a set of lock candidates
 * 
 * this is the destructive update version. Override singleLockThresholdFli() and lockSetthresholdFli() for
 * a persistent version
 */
public class LockSetThresholdFli extends ThresholdFieldLockInfo {

  protected int[] lockRefSet;

  // this is only used once during prototype generation
  public LockSetThresholdFli(ThreadInfo ti, int[] currentLockRefs, int checkThreshold) {
    super(checkThreshold);

    tiLastCheck = ti;
    lockRefSet = currentLockRefs;
  }

  protected int[] getCandidateLockSet() {
    return lockRefSet;
  }

  /**
   * override this for search global FieldLockInfos
   */
  protected SingleLockThresholdFli singleLockThresholdFli (ThreadInfo ti, int lockRef, int remainingChecks) {
    return new SingleLockThresholdFli(ti, lockRef, remainingChecks);
  }
  
  protected LockSetThresholdFli lockSetThresholdFli (ThreadInfo ti, int[] lockRefs, int remainingChecks){
    this.lockRefSet = lockRefs;
    this.tiLastCheck = ti;
    this.remainingChecks = remainingChecks;
    
    return this;
  }
  
  public FieldLockInfo checkProtection(ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
    int[] currentLockRefs = ti.getLockedObjectReferences();
    int nLocks = currentLockRefs.length;
    int nRemaining = Math.max(0, remainingChecks--);

    if (nLocks == 0) { // no current locks, so intersection is empty
      checkFailedLockAssumption(ti, ei, fi);
      return empty;

    } else { // we had a lock set, and there currently is at least one lock held
      int l = 0;
      int[] newLset = new int[lockRefSet.length];

      for (int i = 0; i < nLocks; i++) { // get the set intersection
        int leidx = currentLockRefs[i];

        for (int j = 0; j < lockRefSet.length; j++) {
          if (lockRefSet[j] == leidx) {
            newLset[l++] = leidx;
            break; // sets don't contain duplicates
          }
        }
      }

      if (l == 0) { // intersection empty
        checkFailedLockAssumption(ti, ei, fi);
        return empty;

      } else if (l == 1) { // only one candidate left 
        return singleLockThresholdFli(ti, newLset[0], nRemaining);

      } else if (l < newLset.length) { // candidate set did shrink
        int[] newLockRefSet = new int[l];
        System.arraycopy(newLset, 0, newLockRefSet, 0, l);
        return lockSetThresholdFli(ti, newLockRefSet, nRemaining);

      } else {
        return lockSetThresholdFli(ti, lockRefSet, nRemaining);
      }
    }
  }

  /**
   * only called at the end of the gc on all live objects. The recycled ones are
   * either already nulled in the heap, or are not marked as live
   */
  public FieldLockInfo cleanUp(Heap heap) {
    int[] newSet = null;
    int l = 0;

    if (lockRefSet != null) {
      for (int i = 0; i < lockRefSet.length; i++) {
        ElementInfo ei = heap.get(lockRefSet[i]);

        if (!heap.isAlive(ei)) { // we got a stale one, so we have to change us
          if (newSet == null) { // first one, copy everything up to it
            newSet = new int[lockRefSet.length - 1];
            if (i > 0) {
              System.arraycopy(lockRefSet, 0, newSet, 0, i);
              l = i;
            }
          }

        } else {
          if (newSet != null) { // we already had a dangling ref, now copy the live ones
            newSet[l++] = lockRefSet[i];
          }
        }
      }
    }

    if (l == 1) {
      assert (newSet != null);
      return new SingleLockThresholdFli(tiLastCheck, newSet[0], remainingChecks);

    } else {
      if (newSet != null) {
        if (l == newSet.length) { // we just had one stale ref
          lockRefSet = newSet;
        } else { // several stales - make a new copy
          if (l == 0) {
            return empty;
          } else {
            lockRefSet = new int[l];
            System.arraycopy(newSet, 0, lockRefSet, 0, l);
          }
        }
      }
      return this;
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("LockSetThresholdFli {");
    sb.append("remainingChecks=");
    sb.append(remainingChecks);
    sb.append(",lset=[");
    if (lockRefSet != null) {
      for (int i = 0; i < lockRefSet.length; i++) {
        if (i > 0) {
          sb.append(',');
        }
        sb.append(lockRefSet[i]);
      }
    }
    sb.append("]}");

    return sb.toString();
  }
}
