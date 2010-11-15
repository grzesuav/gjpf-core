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
import org.junit.*;

/**
 * regression test for suspend/resume
 */
@SuppressWarnings("deprecation")
public class SuspendResumeTest extends TestJPF implements Runnable
{
   private final Object m_lock   = new Object();
   private final Thread m_waiter = new Thread(this);
      
   public void run()
   {
      synchronized (m_lock)
      {
         try
         {
            m_lock.wait();
         }
         catch (InterruptedException e)
         {
            e.printStackTrace();

            assert false;
         }
      }
   }
   
   @Before
   public void before()
   {
      if (!Verify.isRunningInJPF())
         return;
      
      Verify.resetCounter(0);
      
      m_waiter.setDaemon(false);
      m_waiter.setName("Waiter");
      m_waiter.start();
   }
   
   @After
   public void after()
   {
      if (!Verify.isRunningInJPF())
         return;

      Verify.ignoreIf(m_waiter.getState() != Thread.State.TERMINATED);

      Verify.incrementCounter(0);

      assert Verify.getCounter(0) == 1;
   }
   
   @Test
   public void suspendResumeUnlockLockNotify()
   {
      if (verifyNoPropertyViolation())
      {
         synchronized (m_lock)
         {
            Verify.ignoreIf(m_waiter.getState() != Thread.State.BLOCKED);
         
            m_waiter.suspend();
         
            assert m_waiter.getState() == Thread.State.BLOCKED;
         
            m_waiter.resume();
            
            assert m_waiter.getState() == Thread.State.BLOCKED;
         }
         
         Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
         
         synchronized (m_lock)
         {
            assert m_waiter.getState() == Thread.State.WAITING;
            
            m_lock.notify();
            
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            Verify.ignoreIf(m_waiter.getState() != Thread.State.BLOCKED);
         }
      }
   }
   
   @Test
   public void suspendNotifyUnlockResume()
   {
      if (verifyNoPropertyViolation())
      {
         synchronized (m_lock)
         {
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            
            m_waiter.suspend();
            
            assert m_waiter.getState() == Thread.State.WAITING;
            
            m_lock.notify();
            
            assert m_waiter.getState() == Thread.State.WAITING;
         }
         
         assert m_waiter.getState() == Thread.State.WAITING;
         
         m_waiter.resume();
      }
   }
   
   @Test
   public void suspendNotifyResumeUnlock()
   {
      if (verifyNoPropertyViolation())
      {
         synchronized (m_lock)
         {
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            
            m_waiter.suspend();
            
            assert m_waiter.getState() == Thread.State.WAITING;
            
            m_lock.notify();
            
            assert m_waiter.getState() == Thread.State.WAITING;
            
            m_waiter.resume();
            
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            Verify.ignoreIf(m_waiter.getState() != Thread.State.BLOCKED);
         }
      }
   }
   
   @Test
   public void suspendResumeNotifyUnlock()
   {
      if (verifyNoPropertyViolation())
      {
         synchronized (m_lock)
         {
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            
            m_waiter.suspend();
            
            assert m_waiter.getState() == Thread.State.WAITING;
            
            m_waiter.resume();
            
            assert m_waiter.getState() == Thread.State.WAITING;
            
            m_lock.notify();
            
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            Verify.ignoreIf(m_waiter.getState() != Thread.State.BLOCKED);
         }
      }
   }
   
   @Test
   public void notifySuspendUnlockResumeWaiting()
   {
      if (verifyNoPropertyViolation())
      {
         synchronized (m_lock)
         {
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            
            m_lock.notify();

            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            
            m_waiter.suspend();
            
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
         }
         
         assert m_waiter.getState() == Thread.State.WAITING;

         m_waiter.resume();
      }
   }
   
   @Test
   public void notifySuspendResumeWaitingBlockedUnlock()
   {
      if (verifyNoPropertyViolation())
      {
         synchronized (m_lock)
         {
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            
            m_lock.notify();

            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            
            m_waiter.suspend();
            
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);

            m_waiter.resume();

            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            Verify.ignoreIf(m_waiter.getState() != Thread.State.BLOCKED);
         }
      }
   }
   
   @Test
   public void notifyBlockedSuspendUnlockResume()
   {
      if (verifyNoPropertyViolation())
      {
         synchronized (m_lock)
         {
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            
            m_lock.notify();

            Verify.ignoreIf(m_waiter.getState() != Thread.State.BLOCKED);
            
            m_waiter.suspend();
            
            assert m_waiter.getState() == Thread.State.BLOCKED;
         }
         
         assert m_waiter.getState() == Thread.State.BLOCKED;

         m_waiter.resume();
      }
   }

   @Test
   public void notifyBlockedSuspendResumeUnlock()
   {
      if (verifyNoPropertyViolation())
      {
         synchronized (m_lock)
         {
            Verify.ignoreIf(m_waiter.getState() != Thread.State.WAITING);
            
            m_lock.notify();

            Verify.ignoreIf(m_waiter.getState() != Thread.State.BLOCKED);
            
            m_waiter.suspend();
            
            assert m_waiter.getState() == Thread.State.BLOCKED;

            m_waiter.resume();

            assert m_waiter.getState() == Thread.State.BLOCKED;
         }
      }
   }
}
