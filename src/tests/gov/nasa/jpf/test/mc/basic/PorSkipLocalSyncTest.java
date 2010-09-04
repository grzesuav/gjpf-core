//
// Copyright (C) 2009 United States Government as represented by the
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
package gov.nasa.jpf.test.mc.basic;

import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.util.test.*;
import org.junit.*;

/**
 * tests POR to make sure synchronize on a local object doesn't cause a state explosion
 */
public class PorSkipLocalSyncTest extends TestJPF
{
   private static final int COUNTER_THREAD_1 = 0;
   private static final int COUNTER_THREAD_2 = 1;
   private static final int COUNTER_FINISHED = 2;
   
   public static void main(String args[])
   {
      runTestsOfThisClass(args);
   }
   
   @Test
   public void porOnLocalSync() throws InterruptedException
   {
      if (verifyNoPropertyViolation())
      {
         Verify.setProperties("vm.por.skip_local_sync=false");

         test(3);    // With POR boundaries from the synchronized statement, each thread should have 2 execution paths through run().
      }
   }

   @Test
   public void noPorOnLocalSync() throws InterruptedException
   {
      if (verifyNoPropertyViolation())
      {
         Verify.setProperties("vm.por.skip_local_sync=true");  
         
         test(1);    // With no POR boundaries from the synchronized statement, the threads should just execute straight through run() with no POR boundaries.
      }
   }
   
   private void test(int expectedCount) throws InterruptedException
   {
      Thread thread;
      Runnable task;
      int counter1, counter2, finished;

      Verify.resetCounter(COUNTER_THREAD_1);
      Verify.resetCounter(COUNTER_THREAD_2);
      Verify.resetCounter(COUNTER_FINISHED);

      if (Verify.getBoolean(true))
      {
         finished = Verify.getCounter(COUNTER_FINISHED);
         
         //Verify.print("Finished = " + finished);
         //Verify.println();
         
         assert finished == 1;
         return;
      }

      //Verify.setProperties("listener=gov.nasa.jpf.listener.StateSpaceAnalyzer");

      task   = new Runnable() {public void run() {sync(COUNTER_THREAD_2);}};
      thread = new Thread(task);
      
      thread.start();
      sync(COUNTER_THREAD_1);
      thread.join();

      counter1 = Verify.getCounter(COUNTER_THREAD_1);
      counter2 = Verify.getCounter(COUNTER_THREAD_2);

      //Verify.print("Counter1 = " + counter1);
      //Verify.println();

      //Verify.print("Counter2 = " + counter2);
      //Verify.println();
      
      Verify.ignoreIf(counter1 != expectedCount);
      Verify.ignoreIf(counter2 != expectedCount);
      Verify.incrementCounter(COUNTER_FINISHED);
   }
   
   private static void sync(int index)
   {
      Object sync;

      sync = new Object();

      synchronized (sync)  // sync can only be reached by the local thread.  There really is no need to put a POR boundary here.
      {
      }

      Verify.incrementCounter(index);
   }
}
