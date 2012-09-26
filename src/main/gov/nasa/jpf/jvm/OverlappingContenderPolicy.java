//
// Copyright (C) 2012 United States Government as represented by the
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
package gov.nasa.jpf.jvm;

/**
 * a thread tracking policy that uses ThreadInfoSets which are
 * search global from the point of object creation. Each object
 * allocation gets a new ThreadInfo set which contains only the
 * allocating thread. 
 * 
 * Note this can miss sharedness due to non-overlapping thread execution.
 * Most real world systems have enough interaction points (sync,
 * field access within loops etc.) to avoid this, but short living threads
 * that only have single field access interaction points can run into
 * this effect: T1 creates O, creates & starts T2, accesses O and terminates
 * before T2 runs. When T2 runs, it only sees access to O from an already
 * terminated thread and therefore treats this as a clean handover. Even if
 * T2 would break at the access, there is no CG that would bring T1 back
 * into the state between creation and access of O, hence T1 never breaks
 * on that access.
 * Unfortunately, this case has a tendency to happen in simple race examples  
 */
public class OverlappingContenderPolicy extends ThreadTrackingPolicy {

  @Override
  public ThreadInfoSet getThreadInfoSet(ThreadInfo allocThread, DynamicElementInfo ei) {
    return new TidSet(allocThread);
  }

  @Override
  public ThreadInfoSet getThreadInfoSet(ThreadInfo allocThread, StaticElementInfo ei) {
    return new TidSet(allocThread);
  }

  @Override
  public boolean isShared(ThreadInfoSet set) {
    return set.hasMultipleLiveThreads();
  }

  @Override
  public Memento<ThreadInfoSet> getMemento(ThreadInfoSet set) {
    return set.getMemento();
  }

  @Override
  public void cleanupThreadTermination(ThreadInfo ti) {
    // nothing, we keep the thread id in the set. Note this requires
    // ids to NOT being reused
  }

}
