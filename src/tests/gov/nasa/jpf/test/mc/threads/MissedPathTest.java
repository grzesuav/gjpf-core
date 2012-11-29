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
package gov.nasa.jpf.test.mc.threads;

import org.junit.Test;

import gov.nasa.jpf.util.test.TestJPF;

/**
 * test for missed paths in concurrent threads with very little interaction
 */
public class MissedPathTest extends TestJPF {

  static class X {
    boolean pass;
  }
  
  static class MissedPath extends Thread {
    X myX;

    public void run() {
      if (myX != null){
        System.out.println("T: accessed global myX");
        if (!myX.pass){  // (2) won't fail unless main is between (0) and (1)
          throw new AssertionError("gotcha");
        }
      }
    }    
  }

  @Test
  public void testGlobalTracking () {
    if (verifyAssertionErrorDetails("gotcha", "+vm.thread_tracking.class=.vm.GlobalTrackingPolicy")) {
      MissedPath mp = new MissedPath();
      mp.start();
      
      X x = new X();
      System.out.println("M: new " + x);
      mp.myX = x;        // (0) x not shared until this GOT executed
     
      //Thread.yield();  // this would expose the error
      System.out.println("M: x.pass=true");
      x.pass = true;     // (1) need to break BEFORE assignment or no error
    }
  }
}
