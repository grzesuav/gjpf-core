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
package gov.nasa.jpf.test.vm.threads;

import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;


/**
 * regression test for suspend/resume (adapted patch from Nathan Reynolds)
 */
@SuppressWarnings("deprecation")
public class SuspendResumeTest extends TestJPF {

  private final static Thread s_waiter = new Thread(new Runnable() {

    public void run() {
      waiter();
    }
  });
  private final static Object s_lock = new Object();
  private final static Runnable s_suspend = new Runnable() {

    public void run() {
      s_waiter.suspend();
    }
    private final String operation = "Suspend";
  };
  private final static Runnable s_resume = new Runnable() {

    public void run() {
      s_waiter.resume();
    }
    private final String operation = "Resume";
  };
  private final static Runnable s_notify = new Runnable() {

    public void run() {
      s_lock.notify();
    }
    private final String operation = "Notify";
  };
  private final static Runnable s_verifyRunnable = new VerifyState(Thread.State.RUNNABLE);
  private final static Runnable s_verifyBlocked = new VerifyState(Thread.State.BLOCKED);
  private final static Runnable s_verifyWaiting = new VerifyState(Thread.State.WAITING);
  private final static Runnable s_verifyTerminated = new VerifyState(Thread.State.TERMINATED);
  private final static Runnable s_assertRunnable = new AssertState(Thread.State.RUNNABLE);
  private final static Runnable s_assertBlocked = new VerifyState(Thread.State.BLOCKED);
  private final static Runnable s_assertWaiting = new VerifyState(Thread.State.WAITING);
  private final static Runnable s_assertTerminated = new VerifyState(Thread.State.TERMINATED);
  private final static Runnable s_tests[][] = {
    new Runnable[]{
      new LockedSteps(new Runnable[]{
        s_verifyBlocked,
        s_suspend,
        s_assertBlocked,
        s_resume,
        s_assertBlocked,}),
      s_verifyWaiting,
      new LockedSteps(new Runnable[]{
        s_assertWaiting,
        s_notify,
        s_verifyWaiting,
        s_verifyBlocked,}),
      s_verifyTerminated
    },
    new Runnable[]{
      new LockedSteps(new Runnable[]{
        s_verifyWaiting,
        s_suspend,
        s_assertWaiting,
        s_notify,
        s_assertWaiting,}),
      s_assertWaiting,
      s_resume,
      s_verifyTerminated
    },
    new Runnable[]{
      new LockedSteps(new Runnable[]{
        s_verifyWaiting,
        s_suspend,
        s_assertWaiting,
        s_notify,
        s_assertWaiting,
        s_resume,
        s_verifyWaiting,
        s_verifyBlocked,}),
      s_verifyTerminated
    },
    new Runnable[]{
      new LockedSteps(new Runnable[]{
        s_verifyWaiting,
        s_suspend,
        s_assertWaiting,
        s_resume,
        s_assertWaiting,
        s_notify,
        s_verifyWaiting,
        s_verifyBlocked,}),
      s_verifyTerminated
    },
    new Runnable[]{
      new LockedSteps(new Runnable[]{
        s_verifyWaiting,
        s_notify,
        s_verifyWaiting,
        s_suspend,
        s_verifyWaiting,}),
      s_assertWaiting,
      s_resume,
      s_verifyTerminated
    },
    new Runnable[]{
      new LockedSteps(new Runnable[]{
        s_verifyWaiting,
        s_notify,
        s_verifyWaiting,
        s_suspend,
        s_verifyWaiting,
        s_resume,
        s_verifyWaiting,
        s_verifyBlocked,}),
      s_verifyTerminated
    },
    new Runnable[]{
      new LockedSteps(new Runnable[]{
        s_verifyWaiting,
        s_notify,
        s_verifyBlocked,
        s_suspend,
        s_assertBlocked,}),
      s_assertBlocked,
      s_resume,
      s_verifyTerminated
    },
    new Runnable[]{
      new LockedSteps(new Runnable[]{
        s_verifyWaiting,
        s_notify,
        s_verifyBlocked,
        s_suspend,
        s_assertBlocked,
        s_resume,
        s_assertBlocked,}),
      s_verifyTerminated
    },};

  public static int getTestCount() {
    return (s_tests.length);
  }


  private static void test(int index) {
    Verify.resetCounter(0);

    System.out.println("Test #" + index);

    s_waiter.setDaemon(false);
    s_waiter.setName("Waiter");
    s_waiter.start();

    test(s_tests[index]);

    Verify.incrementCounter(0);
    assert Verify.getCounter(0) == 1;
  }

  private static void test(Runnable steps[]) {
    int i;

    for (i = 0; i < steps.length; i++) {
      steps[i].run();
    }
  }

  private static void waiter() {
    synchronized (s_lock) {
      try {
        s_lock.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
        assert false;
      }
    }
  }

  private static class VerifyState implements Runnable {

    private final String m_operation = "Verify";
    private final Thread.State m_state;

    VerifyState(Thread.State state) {
      m_state = state;
    }

    public void run() {
      Verify.ignoreIf(s_waiter.getState() != m_state);
    }
  }

  private static class AssertState implements Runnable {

    private final String m_operation = "Assert";
    private final Thread.State m_state;

    AssertState(Thread.State state) {
      m_state = state;
    }

    public void run() {
      assert s_waiter.getState() == m_state;
    }
  }

  private static class LockedSteps implements Runnable {

    private final String m_operation = "Locked Steps";
    private final Runnable m_steps[];

    LockedSteps(Runnable steps[]) {
      m_steps = steps;
    }

    public void run() {
      int i;

      synchronized (s_lock) {
        for (i = 0; i < m_steps.length - 1; i++) {
          m_steps[i].run();
        }
      }
    }
  }


  //--- the test methods

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  @Test
  public void test () {
    if (verifyNoPropertyViolation()){
      test(Verify.getInt(0, s_tests.length - 1));
    }
  }
}
