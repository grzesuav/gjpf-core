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
package gov.nasa.jpf.vm;

import gov.nasa.jpf.Config;

/**
 * policy encapsulation for keeping track of threads referencing objects/static fields
 * This is mostly used by POR to detect shared objects/classes
 */
public abstract class ThreadTrackingPolicy {

  protected static ThreadTrackingPolicy singleton;
  
  public static boolean init (Config config) {
    singleton = config.getEssentialInstance("vm.thread_tracking.class", ThreadTrackingPolicy.class);
    return true;
  }

  public static ThreadTrackingPolicy getPolicy() {
    return singleton;
  }
  
  //--- creators
  public abstract ThreadInfoSet getThreadInfoSet (ThreadInfo allocThread, DynamicElementInfo ei);
  public abstract ThreadInfoSet getThreadInfoSet (ThreadInfo allocThread, StaticElementInfo ei);
  
  //--- sharedness check
  public abstract boolean isShared (ThreadInfoSet set);
  
  //--- state management
  public abstract Memento<ThreadInfoSet> getMemento (ThreadInfoSet set);
  
  //--- housekeeping
  public abstract void cleanupThreadTermination (ThreadInfo ti);
  
}
