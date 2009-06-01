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


import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;
import org.junit.runner.JUnitCore;


/**
 * JPF driver for thread test
 */
public class TestThreadJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.jvm.TestThread";


  public static void main (String[] args) {
    JUnitCore.main("gov.nasa.jpf.jvm.TestThreadJPF");
  }

  /**************************** tests **********************************/

  @Test
  public void testDaemon () {
    String[] args = { TEST_CLASS, "testDaemon" };
    noPropertyViolation(args);
  }

  @Test
  public void testDaemonTermination () {
    String[] args = { "+cg.threads.break_all=true", TEST_CLASS, "testDaemonTermination" };
    noPropertyViolation(args);
  }

  
  @Test
  public void testMain () {
    String[] args = { TEST_CLASS, "testMain" };
    noPropertyViolation(args);
  }

  @Test
  public void testName () {
    String[] args = { TEST_CLASS, "testName" };
    noPropertyViolation(args);
  }

  @Test
  public void testPriority () {
    String[] args = { TEST_CLASS, "testPriority" };
    noPropertyViolation(args);
  }
  
  @Test
  public void testSyncRunning () {
    String[] args = { TEST_CLASS, "testSyncRunning" };
    noPropertyViolation(args);
  }
  
  @Test
  public void testInterrupt () {
    String[] args = { TEST_CLASS, "testInterrupt" };
    noPropertyViolation(args);
  }

  @Test
  public void testSimpleThreadGroup () {
    String[] args = { TEST_CLASS, "testSimpleThreadGroup" };
    noPropertyViolation(args);
  }

  @Test
  public void testYield () {
    String[] args = { "+cg.threads.break_all=true", TEST_CLASS, "testYield" };
    noPropertyViolation(args);
  }
  
}
