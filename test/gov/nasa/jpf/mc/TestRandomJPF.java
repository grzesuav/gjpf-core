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
package gov.nasa.jpf.mc;

import org.junit.Test;
import org.junit.runner.JUnitCore;

import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.jvm.Verify;

/**
 * JPF driver of gov.nasa.jpf.jvm.Verify test for nondeterministic
 * data initialization
 */
public class TestRandomJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.mc.TestRandom";


  public static void main (String[] args) {
    JUnitCore.main("gov.nasa.jpf.mc.TestRandomJPF");
  }


  /**************************** tests **********************************/
  @Test
  public void testRandom () {
    String[] args = { "+cg.enumerate_random=true", TEST_CLASS, "testRandom", "3" };
    
    Verify.resetCounter(0);
    
    noPropertyViolation(args);

    if (Verify.getCounter(0) != 4) {
      fail("wrong number of backtracks: " + Verify.getCounter(0));
    }
  }
  
  @Test
  public void testRandomBFS () {
    String[] args = { "+search.class=gov.nasa.jpf.search.heuristic.BFSHeuristic",
        "+cg.enumerate_random=true",
        TEST_CLASS, "testRandom", "3" };

    Verify.resetCounter(0);

    noPropertyViolation(args);

    if (Verify.getCounter(0) != 4) {
      fail("wrong number of backtracks: " + Verify.getCounter(0));
    }
  }
  
  @Test
  public void testJavaUtilRandomBFS () {
    String[] args = { "+search.class=gov.nasa.jpf.search.heuristic.BFSHeuristic",
        "+cg.enumerate_random=true",
        TEST_CLASS, "testJavaUtilRandom"};

    unhandledException(args, "java.lang.ArithmeticException");
  }
  
}
