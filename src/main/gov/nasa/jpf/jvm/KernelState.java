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
package gov.nasa.jpf.jvm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.Debug;
import gov.nasa.jpf.util.HashData;

import java.util.Stack;


/**
 * This class represents the SUT program state (statics, heap and threads)
 */
public class KernelState {

  /** The area containing static fields and  classes */
  public StaticArea sa;

  /** The area containing the heap */
  public DynamicArea da;

  /** The list of the threads */
  public ThreadList tl;

  /**
   * current listeners waiting for notification of next change.
   */
  private Stack<ChangeListener> listeners = new Stack<ChangeListener>();

  /**
   * Creates a new kernel state object.
   */
  public KernelState (Config config) {
    Class<?>[] argTypes = { Config.class, KernelState.class };
    Object[] args = { config, this };

    sa = config.getEssentialInstance("vm.static_area.class", StaticArea.class, argTypes, args);
    da = config.getEssentialInstance("vm.dynamic_area.class", DynamicArea.class, argTypes, args);
    tl = config.getEssentialInstance("vm.thread_list.class", ThreadList.class, argTypes, args);
  }

  public StaticArea getStaticArea() {
    return sa;
  }

  public DynamicArea getDynamicArea() {
    return da;
  }

  public Heap getHeap() {
    return da;
  }

  public ThreadList getThreadList() {
    return tl;
  }

  /**
   * interface for getting notified of changes to KernelState and everything
   * "below" it.
   */
  public interface ChangeListener {
    void kernelStateChanged(KernelState ks);
  }

  /**
   * called by internals to indicate a change in KernelState.  list of listeners
   * is emptied.
   */
  public void changed() {
    while (!listeners.empty()) {
      listeners.pop().kernelStateChanged(this);
    }
  }

  /**
   * push a listener for notification of the next change.  further notification
   * requires re-pushing.
   */
  public void pushChangeListener(ChangeListener cl) {
    if (cl instanceof IncrementalChangeTracker && listeners.size() > 0) {
      for (ChangeListener l : listeners) {
        if (l instanceof IncrementalChangeTracker) {
          throw new IllegalStateException("Only one IncrementalChangeTracker allowed!");
        }
      }
    }
    listeners.push(cl);
  }

  boolean isDeadlocked () {
    return tl.isDeadlocked();
  }

  /**
   * The program is terminated if there are no alive threads, and there is no nonDaemon left.
   * 
   * NOTE - this is only approximated in real life. Daemon threads can still run for a few cycles
   * after the last non-daemon died, which opens an interesting source of errors we
   * actually might want to check for
   */
  public boolean isTerminated () {
    //return !tl.anyAliveThread();
    return !tl.hasMoreThreadsToRun();
  }

  public int getThreadCount () {
    return tl.length();
  }

  @Deprecated
  public ThreadInfo getThreadInfo (int index) {
    return tl.get(index);
  }


  public void gc () {
        
    da.gc();

    // we might have stored stale references in live objects
    da.cleanUpDanglingReferences();
    sa.cleanUpDanglingReferences();
  }



  public void hash (HashData hd) {
    da.hash(hd);
    sa.hash(hd);

    for (int i = 0, l = tl.length(); i < l; i++) {
      tl.get(i).hash(hd);
    }
  }
}
