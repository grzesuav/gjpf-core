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
package java.lang;

import sun.nio.ch.Interruptible;

/**
 * MJI model class for java.lang.Thread library abstraction
 */
public class Thread implements Runnable {

  static int              threadNum;

  public static final int MIN_PRIORITY = 1;
  public static final int NORM_PRIORITY = 5;
  public static final int MAX_PRIORITY = 10;

  // this is for the dreaded Unsafe.park() synchronization. I hope it would vanish
  static class Permit {
    boolean isTaken = true;
  }

  // initialized in init(), except of the main thread (which gets explicitly initialized by the VM)
  ThreadGroup         group;
  Runnable            target;
  String              name;
  int                 priority;
  boolean             isDaemon;
  Permit              permit;

  // this is an explicit thread state that gets set on a call of interrupt(), but
  // only if the thread is not blocked. If it is, we only change the status.
  // this gets cleared by calling interrupted()
  boolean             interrupted;


  // <2do> those two seem to be the only interfaces to the ThreadLocal
  // implementation. Replace once we have our own
  // ThreadLocal / InhertitableThreadLocal classes
  ThreadLocal.ThreadLocalMap threadLocals;
  ThreadLocal.ThreadLocalMap inheritableThreadLocals;

  // referenced by the dreaded java.util.concurrent.locks.LockSupport via sun.misc.Unsafe
  volatile Object parkBlocker;

  public enum State { BLOCKED, NEW, RUNNABLE, TERMINATED, TIMED_WAITING, WAITING }

  public Thread () {
    init(group, target, name, 0L);
  }

  public Thread (Runnable target) {
    init(group, target, name, 0L);
  }

  public Thread (Runnable target, String name) {
    init(group, target, name, 0L);
  }

  public Thread (String name) {
    init(group, target, name, 0L);
  }

  public Thread (ThreadGroup group, Runnable target) {
    init(group, target, name, 0L);
  }

  public Thread (ThreadGroup group, Runnable target, String name) {
    init(group, target, name, 0L);
  }

  public Thread (ThreadGroup group, Runnable target, String name,
                 long stackSize) {
    init(group, target, name, 0L);
  }

  public Thread (ThreadGroup group, String name) {
    init(group, target, name, 0L);
  }

  public static int activeCount () {
    return 0;
  }

  public void setContextClassLoader (ClassLoader cl) {
  }

  public ClassLoader getContextClassLoader () {
    // <NSY>
    return null;
  }

  public synchronized void setDaemon (boolean isDaemon) {
    this.isDaemon = isDaemon;
    setDaemon0(isDaemon);
  }

  public boolean isDaemon () {
    return isDaemon;
  }

  public native long getId();

  public StackTraceElement[] getStackTrace() {
    return null; // not yet implemented
  }

  public native int getState0();

  public Thread.State getState() {
    int i = getState0();
    switch (i) {
    case 0: return State.BLOCKED;
    case 1: return State.NEW;
    case 2: return State.RUNNABLE;
    case 3: return State.TERMINATED;
    case 4: return State.TIMED_WAITING;
    case 5: return State.WAITING;
    }

    return null; // shoudl be intercepted by a getState0 assertion
  }

  public synchronized void setName (String name) {
    if (name == null) {
      throw new IllegalArgumentException("thread name can't be null");
    }

    this.name = name;
    setName0(name);
  }

  public String getName () {
    return name;
  }

  public void setPriority (int priority) {
    if ((priority < MIN_PRIORITY) || (priority > MAX_PRIORITY)) {
      throw new IllegalArgumentException("thread priority out of range");
    }

    this.priority = priority;
    setPriority0(priority);
  }

  public int getPriority () {
    return priority;
  }

  public ThreadGroup getThreadGroup () {
    return group;
  }

  public void checkAccess () {
    // <NSY>
  }

  public native int countStackFrames ();

  public static native Thread currentThread ();

  public void destroy () {
  }

  public static void dumpStack () {
  }

  public static int enumerate (Thread[] tarray) {
    Thread cur = currentThread();

    return cur.group.enumerate(tarray);
  }

  public static native boolean holdsLock (Object obj);

  // this one needs to be native because it might change the thread status
  public native void interrupt ();

  // those don't have to be native, but we keep it symmetric
  public static native boolean interrupted ();
  public native boolean isInterrupted ();

  public native boolean isAlive ();


  public synchronized void join () throws InterruptedException {
    while (isAlive()) {
      wait();
    }
  }

  //public native synchronized void join () throws InterruptedException;

  public synchronized void join (long millis) throws InterruptedException {
    while (isAlive()) {
      wait(millis);
    }
  }

  public synchronized void join (long millis, int nanos) throws InterruptedException {
    while (isAlive()) {
      wait(millis);
    }
  }

  public void run () {
  }

  public static void sleep (long millis) throws InterruptedException {
    sleep(millis, 0);
  }

  public static native void sleep (long millis, int nanos)
                            throws InterruptedException;

  public native void start ();

  public void stop () {
    // deprecated, <NSY>
  }

  public native void suspend();
  public native void resume();

  public String toString () {
    return ("Thread[" + name + ',' + priority + ',' + group.getName() + ']');
  }

  public static native void yield ();

  native void setDaemon0 (boolean on);

  native void setName0 (String name);

  native void setPriority0 (int priority);

  void init (ThreadGroup group, Runnable target, String name, long stackSize) {
    Thread cur = currentThread();

    if (group == null) {
      this.group = cur.getThreadGroup();
    } else {
      this.group = group;
    }

    this.group.add(this);

    if (name == null) {
      this.name = "Thread-" + threadNum++;
    } else {
      this.name = name;
    }

    this.permit = new Permit();

    // those are always inherited from the current thread
    this.priority = cur.getPriority();
    this.isDaemon = cur.isDaemon();

    this.target = target;

    // do our associated native init
    init0(this.group, target, this.name, stackSize);
  }

  native void init0 (ThreadGroup group, Runnable target, String name,
                     long stackSize);

  /**
   * automatically called by system upon thread termination to clean up
   * references.
   * NOTE - we clean up atomically during ThreadInfo.finish(), to avoid any
   * additional states
   */
  private void exit () {
    // apparently some older javac on Solaris chokes on this, but it's perfectly fine
    //group.remove(this);
    //group = null;

    //threadLocals = null;
    //inheritableThreadLocals = null;
    //parkBlocker = null;
  }

  // some Java 6 mojo
  // <2do> not implemented yet
  native void blockedOn (Interruptible b);
}
