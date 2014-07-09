//
// Copyright (C) 2010 United States Government as represented by the
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


import gov.nasa.jpf.annotation.NeverBreak;
import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.Verify;

import org.junit.Test;

/**
 * regression test for Thread.stop()
 */
@SuppressWarnings("deprecation")
public class ThreadStopTest extends TestJPF {

  @Test
  public void testStopNewThread(){
    if (!isJPFRun()){
      Verify.resetCounter(0);
    }

    if (verifyNoPropertyViolation()){
      Thread t = new Thread(){
        public void run(){
           Verify.println("# t running, that's bad");
          fail("t should never run");
        }
      };

      t.stop();

       Verify.println("# now starting the stopped thread");
      t.start();
      Thread.yield();
      assertFalse( "t not terminated yet", t.isAlive());
       Verify.println("# main got past start of stopped thread");
      Verify.incrementCounter(0);

    } else {
      assertTrue( "main did not get past starting stopped thread", Verify.getCounter(0) > 0);
    }
  }

  @Test
  public void testStopNewSyncThread(){
    if (!isJPFRun()){
      Verify.resetCounter(0);
    }

    if (verifyNoPropertyViolation()){
      Thread t = new Thread(){
        public synchronized void run(){
           Verify.println("# t running, that's bad");
          fail("t should never run");
        }
      };

      t.stop();

      synchronized (t){
         Verify.println("# now starting the stopped thread");
        t.start(); // this should terminate the thread
         Verify.println("# main got past start of stopped sync thread");
      }
       Verify.println("# main released lock for stopped sync thread");
      Thread.yield();
      assertFalse( "t not terminated yet", t.isAlive());
      Verify.incrementCounter(0);


    } else {
      assertTrue( "main did not get past starting stopped thread", Verify.getCounter(0) > 0);
    }
  }

  @Test
  public void testStopSelf(){
    if (!isJPFRun()){
      Verify.resetCounter(0);
      Verify.resetCounter(1);
    }

    if (verifyNoPropertyViolation()){
      Thread t = new Thread(){
        public synchronized void run(){
          Verify.incrementCounter(1);

           Verify.println("# t running");
          foo();
          fail("t should not have gotten past self stop");
        }

        void foo() {
          stop();
        }
      };

       Verify.println("# main now starting the thread");
      t.start();

      try {
         Verify.println("# main now joining the thread..");
        t.join();
         Verify.println("# main joined thread");
        assertFalse( "t not terminated yet", t.isAlive());
        Verify.incrementCounter(0);

      } catch (InterruptedException ix){
        assert false : "main should not get an InterruptedException while joining";
      }

    } else {
      assertTrue( "t did not run", Verify.getCounter(1) > 0);
      assertTrue( "main did not get past joining stopped thread", Verify.getCounter(0) > 0);
    }
  }


  // some sync helpers
  @NeverBreak
  static boolean isRunning;

  @Test
  public void testStopRunning () {
    if (!isJPFRun()) {
      Verify.resetCounter(0);
      Verify.resetCounter(1);
    }

    if (verifyNoPropertyViolation()) {
      isRunning = false;

      Thread t = new Thread() {
        public synchronized void run () {
          isRunning = true;
          Verify.incrementCounter(1);

          Verify.println("# t running");

          while (true) { // while(true) will cause no return insn !
            // keep it alive
            Thread.yield();
          }
        }
      };

      Verify.println("# main now starting t");
      t.start();

      while (!isRunning) {
        Thread.yield();
      }

      Verify.println("# main now stopping t");
      t.stop();

      try {
        Verify.println("# main now joining the stopped thread..");
        t.join();
        Verify.println("# main joined thread");
        assertFalse("t not terminated yet", t.isAlive());
        Verify.incrementCounter(0);

      } catch (InterruptedException ix) {
        assert false : "main should not get an InterruptedException while joining";
      }

    } else {
      assertTrue("t did not run", Verify.getCounter(1) > 0);
      assertTrue("main did not get past join", Verify.getCounter(0) > 0);
    }
  }

  @NeverBreak
  static Object lock = new Object();

  @Test
  public void testStopBlocked () {
    if (!isJPFRun()) {
      Verify.resetCounter(0);
      Verify.resetCounter(1);
    }

    if (verifyNoPropertyViolation()) {
      isRunning = false;
      Thread t = new Thread() {
        public synchronized void run () {
          isRunning = true;
          Verify.incrementCounter(1);
          Verify.println("# t running, now blocking on lock..");
          synchronized (lock) {
            fail("t should never get here");
          }
        }
      };

      synchronized (lock) {
        Verify.println("# main now starting t");
        t.start();

        while (!isRunning) {
          Thread.yield();
        }
        assertTrue("t not blocked", t.getState() == Thread.State.BLOCKED);

        Verify.println("# main now stopping t");
        t.stop();

        assertTrue("t dead despite main not giving up lock", t.isAlive());

        Verify.println("# main now releasing lock");
      }

      try {
        Verify.println("# main now joining the stopped thread..");
        t.join();
        Verify.println("# main joined thread");
        assertFalse("t not terminated yet", t.isAlive());
        Verify.incrementCounter(0);

      } catch (InterruptedException ix) {
        assert false : "main should not get an InterruptedException while joining";
      }

    } else {
      assertTrue("t did not run", Verify.getCounter(1) > 0);
      assertTrue("main did not get past join", Verify.getCounter(0) > 0);
    }
  }

  @Test
  public void testStopWaiting(){
    if (!isJPFRun()){
      Verify.resetCounter(0);
      Verify.resetCounter(1);
    }

    if (verifyNoPropertyViolation()){
      isRunning = false;

      Thread t = new Thread(){
        public synchronized void run(){
          isRunning = true;
          Verify.incrementCounter(1);

           Verify.println("# t running, now waiting on lock");

          synchronized(lock){
            try {
              lock.wait();
            } catch (InterruptedException ix){
              fail("should not get interrupted");
            }
          }
        }
      };

       Verify.println("# main now starting t");
      t.start();

      while (t.getState() != Thread.State.WAITING) {
        Thread.yield();
      }

       Verify.println("# main now stopping t");
      t.stop();

      Thread.yield();
      assertTrue("t dead despite main not notifying", t.isAlive());

       Verify.println("# main now notifying");
      synchronized (lock) {
        lock.notifyAll();
      }

      try {
         Verify.println("# main now joining the stopped thread..");
        t.join();
         Verify.println("# main joined thread");
        assertFalse( "t not terminated yet", t.isAlive());
        Verify.incrementCounter(0);

      } catch (InterruptedException ix){
        assert false : "main should not get an InterruptedException while joining";
      }

    } else {
      assertTrue( "t did not run", Verify.getCounter(1) > 0);
      assertTrue( "main did not get past join", Verify.getCounter(0) > 0);
    }
  }

  static boolean wasHandled;

  @Test
  public void testStopHandler() {
    if (!isJPFRun()){
      Verify.resetCounter(0);
      Verify.resetCounter(1);
    }

    if (verifyNoPropertyViolation()){
      isRunning = false;
      wasHandled = false;

      Thread t = new Thread(){
        public synchronized void run(){
          isRunning = true;
          Verify.incrementCounter(1);

           Verify.println("# t running, now waiting on lock");

          try {
            synchronized (lock) {
              try {
                lock.wait();
              } catch (InterruptedException ix) {
                fail("should not get interrupted");
              }
            }
          } catch (ThreadDeath td){
            // usually not a good style, but can happen for exit processing
             Verify.println("# t caught ThreadDeath");
            wasHandled = true;
            throw td; // rethrow to continue with kill
          }
        }
      };

       Verify.println("# main now starting t");
      t.start();

      while (t.getState() != Thread.State.WAITING) {
        Thread.yield();
      }

       Verify.println("# main now stopping t");
      t.stop();

      Thread.yield();
      assertTrue("t dead despite main not notifying", t.isAlive());

       Verify.println("# main now notifying");
      synchronized (lock) {
        lock.notifyAll();
      }

      try {
         Verify.println("# main now joining the stopped thread..");
        t.join();
         Verify.println("# main joined thread");

        assertFalse( "t not terminated yet", t.isAlive());
        assertTrue("t did not handle ThreadDeath", wasHandled);

        Verify.incrementCounter(0);

      } catch (InterruptedException ix){
        assert false : "main should not get an InterruptedException while joining";
      }

    } else {
      assertTrue( "t did not run", Verify.getCounter(1) > 0);
      assertTrue( "main did not get past join", Verify.getCounter(0) > 0);
    }
  }

  @Test
  public void testStopTerminated(){
    if (!isJPFRun()){
      Verify.resetCounter(0);
      Verify.resetCounter(1);
    }

    if (verifyNoPropertyViolation()){
      Thread t = new Thread(){
        public void run(){
          Verify.incrementCounter(1);
           Verify.println("# t running");
        }
      };

       Verify.println("# main now starting t");
      t.start();

      while (t.isAlive()){
        Thread.yield();
      }

      assertFalse("t is a zombie", t.isAlive());

       Verify.println("# main now stopping dead t");
      t.stop();
       Verify.println("# main survived stopping t");

      assertFalse("t is a zombie", t.isAlive());
      Verify.incrementCounter(0);

    } else {
      assertTrue( "t did not run", Verify.getCounter(1) > 0);
      assertTrue( "main did not get past join", Verify.getCounter(0) > 0);
    }
  }

  static Thread waitee;

  @Test
  public void testStopJoin() {
    // join() is a lockfree wait, which makes it interesting

    if (!isJPFRun()){
      Verify.resetCounter(0);
      Verify.resetCounter(1);
      Verify.resetCounter(2);
    }

    if (verifyNoPropertyViolation()){
      waitee = new Thread(){
        public void run(){
          Verify.incrementCounter(2);
           Verify.println("# waitee running");
          synchronized(lock){
            try {
               Verify.println("# waitee now waiting for main to signal..");
              lock.wait();
               Verify.println("# waitee terminating");
            } catch (InterruptedException ix) {
              fail("waitee should not get interrupted");
            }
          }
        }
      };
      waitee.start();
      while (waitee.getState() != Thread.State.WAITING){
        Thread.yield();
      }

      Thread t = new Thread(){
        public synchronized void run(){
          Verify.incrementCounter(1);
          try {
             Verify.println("# t now joining waitee..");
            waitee.join();

            fail("t should never get here");

          } catch (InterruptedException ix){
            fail("t should not get interrupted");
          }
        }
      };

       Verify.println("# main now starting t");
      t.start();

      while (t.getState() != Thread.State.WAITING){
        Thread.yield();
      }

      assertTrue("waitee is a zombie", waitee.isAlive());
      assertTrue("t is a zombie", t.isAlive());

       Verify.println("# main now stopping t");
      t.stop();
      assertTrue("t should not have terminated since waitee not notified yet", t.isAlive());


       Verify.println("# main now notifying waitee");
      synchronized(lock){
        lock.notifyAll();
      }

      try {
         Verify.println("# main now joining waitee");
        waitee.join();
      } catch (InterruptedException ix){
        fail("main should not get interupted joining waitee");
      }
      assertFalse("waitee is a zombie", waitee.isAlive());

      try {
         Verify.println("# main now joining t");
        t.join();
      } catch (InterruptedException ix){
        fail("main should not get interupted joining t");
      }
      assertFalse("t is a zombie", t.isAlive());

      Verify.incrementCounter(0);

    } else {
      assertTrue( "waitee did not run", Verify.getCounter(2) > 0);
      assertTrue( "t did not run", Verify.getCounter(1) > 0);
      assertTrue( "main did not get past t join", Verify.getCounter(0) > 0);
    }
  }

  // and a lot more.. (e.g. stopping interrupted or timedout threads)
}
