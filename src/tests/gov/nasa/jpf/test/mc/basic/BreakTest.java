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
package gov.nasa.jpf.test.mc.basic;

import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;


/**
 * simple test application to break transitions from listeners
 */
public class BreakTest extends TestJPF {

  static final String LISTENER = "+listener=.test.mc.basic.BreakTestListener";

  int data;
  
  public static void main (String[] args) {
    runTestsOfThisClass(args);
  }

  @Test public void testSimpleBreak () {
    if (verifyNoPropertyViolation(LISTENER)) {
      int i = 42;
      data = i; // we break after that
      i = 0;

    } else {
      if (BreakTestListener.nCG != 2) { // that's really simplistic
        fail("wrong number of CGs: " + BreakTestListener.nCG);
      }
    }
  }
  
  @Test public void testNestedBreak () {
    if (verifyNoPropertyViolation(LISTENER)) {
      if (Verify.getBoolean()) {
        System.out.println("foo,bar branch");
        foo(); // breaks it
        bar();
      } else {
        System.out.println("bar,foo branch");
        bar();
        foo(); // breaks it
      }
    }
  }
  
  void foo () {
    System.out.println("foo");
  }
  
  void bar () {
    System.out.println("bar");
  }
}
