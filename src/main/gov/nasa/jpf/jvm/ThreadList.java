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
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Contains the list of all currently active threads.
 *
 * Note that this list may both shrink or (re-) grow on backtrack. This imposes
 * a challenge for keeping ThreadInfo identities, which are otherwise nice for
 * directly storing ThreadInfo references in Monitors and/or listeners.
 */
public class ThreadList implements Cloneable, Iterable<ThreadInfo>, Restorable<ThreadList> {

  /** all threads (including terminated ones) */
  protected ThreadInfo[] threads;

  /** reference of the kernel state this thread list belongs to */
  public KernelState ks;  // <2do> bad backlink, remove!


  static class TListMemento implements Memento<ThreadList> {
    // note that ThreadInfo mementos are also identity preserving
    Memento<ThreadInfo>[] tiMementos;

    TListMemento(ThreadList tl) {
      ThreadInfo[] threads = tl.threads;
      int len = threads.length;

      tiMementos = new Memento[len];
      for (int i=0; i<len; i++){
        ThreadInfo ti = threads[i];
        Memento<ThreadInfo> m = null;

        if (!ti.hasChanged()){
          m = ti.cachedMemento;
        }
        if (m == null){
          m = ti.getMemento();
          ti.cachedMemento = m;
        }
        tiMementos[i] = m;
      }
    }

    public ThreadList restore(ThreadList tl){
      int len = tiMementos.length;
      ThreadInfo[] threads = new ThreadInfo[len];
      for (int i=0; i<len; i++){
        Memento<ThreadInfo> m = tiMementos[i];
        ThreadInfo ti = m.restore(null);
        ti.cachedMemento = m;
        threads[ti.getIndex()] = ti;
      }
      tl.threads = threads;

      return tl;
    }
  }


  protected ThreadList() {
    // nothing here
  }

  /**
   * Creates a new empty thread list.
   */
  public ThreadList (Config config, KernelState ks) {
    this.ks = ks;
    threads = new ThreadInfo[0];
  }

  public Memento<ThreadList> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }
  public Memento<ThreadList> getMemento(){
    return new TListMemento(this);
  }

  public Object clone() {
    ThreadList other = new ThreadList();
    other.ks = ks;
    other.threads = new ThreadInfo[threads.length];

    for (int i=0; i<threads.length; i++) {
      other.threads[i] = (ThreadInfo) threads[i].clone();
    }

    return other;
  }

  public int add (ThreadInfo ti) {
    int n = threads.length;

    // check if it's already there
    for (ThreadInfo t : threads) {
      if (t == ti) {
        return t.getIndex();
      }
    }

    // append it
    ThreadInfo[] newList = new ThreadInfo[n+1];
    System.arraycopy(threads, 0, newList, 0, n);
    newList[n] = ti;
    threads = newList;
    return n; // the index where we added
  }


  public boolean hasAnyAliveThread () {
    for (int i = 0, l = threads.length; i < l; i++) {
      if (threads[i].isAlive()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns the array of threads.
   */
  public ThreadInfo[] getThreads() {
    return threads.clone();
  }

  /**
   * Returns a specific thread.
   */
  public ThreadInfo get (int index) {
    return threads[index];
  }

  /**
   * Returns the length of the list.
   */
  public int length () {
    return threads.length;
  }

  /**
   * Replaces the array of ThreadInfos.
   */
  public void setAll(ThreadInfo[] threads) {
    this.threads = threads;
  }

  public ThreadInfo locate (int objref) {
    for (int i = 0, l = threads.length; i < l; i++) {
      if (threads[i].getThreadObjectRef() == objref) {
        return threads[i];
      }
    }

    return null;
  }

  public void markRoots (Heap heap) {
    for (int i = 0, l = threads.length; i < l; i++) {
      if (threads[i].isAlive()) {
        threads[i].markRoots(heap);
      }
    }
  }
  
  /**
   * return if there are still runnables, and there is at least one
   * non-daemon thread left 
   */
  public boolean hasMoreThreadsToRun() {
    int nonDaemons = 0;
    int runnables = 0;

    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      if (!ti.isDaemon() && !ti.isTerminated()) {
        nonDaemons++;
      }
      if (ti.isTimeoutRunnable()) {
        runnables++;
      }
    }
    
    return (nonDaemons > 0) && (runnables > 0);
  }

  public int getNonDaemonThreadCount () {
    int nd = 0;

    for (int i = 0; i < threads.length; i++) {
      if (!threads[i].isDaemon()) {
        nd++;
      }
    }

    return nd;
  }

  public int getRunnableThreadCount () {
    int n = 0;

    for (int i = 0; i < threads.length; i++) {
      if (threads[i].isTimeoutRunnable()) {
        n++;
      }
    }

    return n;
  }

  public ThreadInfo[] getRunnableThreads() {
    int nRunnable = getRunnableThreadCount();
    ThreadInfo[] list = new ThreadInfo[nRunnable];

    for (int i = 0, j=0; i < threads.length; i++) {
      ThreadInfo t = threads[i];
      if (t.isTimeoutRunnable()) {
        list[j++] = t;
        if (j == nRunnable) {
          break;
        }
      }
    }

    return list;
  }

  public ThreadInfo[] getRunnableThreadsWith (ThreadInfo ti) {
    int nRunnable = getRunnableThreadCount();
    ThreadInfo[] list =  new ThreadInfo[ti.isRunnable() ? nRunnable : nRunnable+1];

    for (int i = 0, j=0; i < threads.length; i++) {
      ThreadInfo t = threads[i];
      if (t.isTimeoutRunnable() || (t == ti)) {
        list[j++] = t;
        if (j == list.length) {
          break;
        }
      }
    }

    return list;
  }

  public ThreadInfo[] getRunnableThreadsWithout( ThreadInfo ti) {
    int nRunnable = getRunnableThreadCount();

    if (ti.isRunnable()) {
      nRunnable--;
    }
    ThreadInfo[] list = new ThreadInfo[nRunnable];

    for (int i = 0, j=0; i < threads.length; i++) {
      ThreadInfo t = threads[i];
      if (t.isTimeoutRunnable() && (ti != t)) {
        list[j++] = t;
        if (j == nRunnable) {
          break;
        }
      }
    }

    return list;
  }


  public int getLiveThreadCount () {
    int n = 0;

    for (int i = 0; i < threads.length; i++) {
      if (threads[i].isAlive()) {
        n++;
      }
    }

    return n;
  }

  boolean hasOtherRunnablesThan (ThreadInfo ti) {
    int n = threads.length;

    for (int i=0; i<n; i++) {
      ThreadInfo t = threads[i];
      if (t != ti) {
        if (t.isRunnable()) {
          return true;
        }
      }
    }

    return false;
  }

  boolean hasOtherNonDaemonRunnablesThan (ThreadInfo ti) {
    int n = threads.length;

    for (int i=0; i<n; i++) {
      ThreadInfo t = threads[i];
      if (t != ti) {
        if (t.isRunnable() && !t.isDaemon()) {
          return true;
        }
      }
    }

    return false;
  }



  boolean isDeadlocked () {
    boolean hasBlockedThreads = false;

    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      // if there's at least one runnable, we are not deadlocked
      if (ti.isTimeoutRunnable()) { // willBeRunnable() ?
        return false;
      }

      if (ti.isAlive()){
        // means it is not NEW or TERMINATED, i.e. live & blocked
        hasBlockedThreads = true;
      }
    }

    return hasBlockedThreads;
  }

  public void dump () {
    int i=0;
    for (ThreadInfo t : threads) {
      System.err.println("[" + i++ + "] " + t);
    }
  }

  public Iterator<ThreadInfo> iterator() {
    return new Iterator() {
      int i = 0;

      public boolean hasNext() {
        return threads != null && threads.length>0 && i<threads.length;
      }

      public ThreadInfo next() {
        if (threads != null && threads.length>0 && i<threads.length){
          return threads[i++];
        } else {
          throw new NoSuchElementException();
        }
      }

      public void remove() {
        throw new UnsupportedOperationException("Iterator<ThreadInfo>.remove()");
      }
    };
  }


  /**
   * only for debugging purposes, this is expensive
   */
  public void checkConsistency(boolean isStore) {
    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      
      ti.checkConsistency(isStore);
    }
  }
}
