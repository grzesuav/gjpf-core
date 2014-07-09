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

import gov.nasa.jpf.Config;

/**
 * a SharednessPolicy implementation that only computes and keeps sharedness
 * along the same path, i.e. not search global.
 * 
 * This is the policy that should be used to create traces that can be replayed.
 * 
 * This policy uses PersistentTids, i.e. it does not modify existing ThreadInfoSet
 * instances but replaces upon add/remove with new ones. This ensures that ThreadInfoSets
 * are path local, but it also means that operations that add/remove ThreadInfos
 * have to be aware of the associated ElementInfo cloning and don't keep/use references
 * to old ElementInfos. The upside is that sharedness should be the same along any
 * given path, regardless of which paths were executed before. The downside is that all
 * callers of ThreadInfoSet updates have to be aware of that the ElementInfo might change.
 * 
 * Note that without additional transition breaks this can miss races due 
 * to non-overlapping thread execution along all paths. Most real world systems
 * have enough scheduling points (sync, field access within loops etc.) to avoid this,
 * but short living threads that only have single field access interaction points can
 * run into this effect: T1 creates O, starts T2, accesses O and
 * terminates before T2 runs. When T2 runs, it only sees access to O from an already
 * terminated thread and therefore treats this as a clean handover. Even if
 * T2 would break at its O access, there is no CG that would bring T1 back
 * into a state between creation and access of O, hence races caused by the O access
 * of T1 are missed (see .test.mc.threads.MissedPathTest).
 * Two additional scheduling points help to prevent this case: thread start CGs and
 * object exposure CGs (/after/ assignment in reference field PUTs where the owning
 * object is shared, but the referenced object isn't yet). Both are conservative by
 * nature, i.e. might introduce superfluous states for the sake of not missing paths to 
 * race points. Both can be controlled by configuration options ('cg.threads.break_start'
 * and 'vm.por.break_on_exposure').
 * 
 * Note also that shared-ness is not the same along all paths, because our goal
 * is just to find potential data races. As long as JPF explores /one/ path that
 * leads into a race we are fine - we don't care how many paths don't detect a race.
 */
public class PathSharednessPolicy extends GenericSharednessPolicy {
  
  public PathSharednessPolicy (Config config){
    super(config);
  }
  
  @Override
  public void initializeSharedness (ThreadInfo allocThread, DynamicElementInfo ei) {
    ei.setReferencingThreads( new PersistentTidSet(allocThread));
  }

  @Override
  public void initializeSharedness (ThreadInfo allocThread, StaticElementInfo ei) {
    ei.setReferencingThreads( new PersistentTidSet(allocThread));
    ei.setExposed(); // static fields are per se exposed
  }
  
  @Override
  protected FieldLockInfo createFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    int[] lockRefs = ti.getLockedObjectReferences();
    switch (lockRefs.length){
      case 0:
        return FieldLockInfo.getEmptyFieldLockInfo();
      case 1:
        return new PersistentSingleLockThresholdFli(ti, lockRefs[0], lockThreshold);
      default: 
        return new PersistentLockSetThresholdFli(ti, lockRefs, lockThreshold);
    }
  }
}