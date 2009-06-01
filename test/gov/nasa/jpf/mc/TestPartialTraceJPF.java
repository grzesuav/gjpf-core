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

import java.io.File;

import org.junit.Test;
import org.junit.runner.JUnitCore;

import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.jvm.Verify;

/**
 * test case to show how to use partial traces
 */
public class TestPartialTraceJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.mc.TestPartialTrace";


  public static void main (String[] args) {
    JUnitCore.main( TEST_CLASS + "JPF");
  }

  /**************************** tests **********************************/
  @Test
  public void testPartialTrace () {
    String tfName = "testPartialTrace.trace";
    String[] args0 = { TEST_CLASS, "testPartialTrace" };
    String[] args1 = { "+jpf.listener=.tools.ChoiceSelector",
        "+choice.use_trace=" + tfName, TEST_CLASS, "testPartialTrace" };

    File tf = new File(tfName);
    if (tf.exists()) {
      tf.delete();
    }

    try {
      Verify.resetCounter(0);
      noPropertyViolation(args0);

      if (Verify.getCounter(0) != 1) {
        fail("wrong number of backtracks on non-replay run: " + Verify.getCounter(0));
      }

      Verify.resetCounter(0);
      noPropertyViolation(args1);

      if (Verify.getCounter(0) != 5) {
        fail("wrong number of backtracks on replay run: " + Verify.getCounter(0));
      }

    } finally {
      if (tf.exists()) {
        tf.delete();
      }
    }
  }

  
  @Test
  public void testReplay () {

    String tfName = "testReplay.trace";
    String[] args0 = { "+jpf.listener=.tools.TraceStorer", "+trace.file=" + tfName, TEST_CLASS, "testReplay" };
    String[] args1 = { "+jpf.listener=.tools.ChoiceSelector",
        "+choice.use_trace=" + tfName, TEST_CLASS, "testReplay" };

    File tf = new File(tfName);
    if (tf.exists()) {
      tf.delete();
    }

    try {
      unhandledException(args0, "java.lang.AssertionError");
      unhandledException(args1, "java.lang.AssertionError");
      
    } finally {
      if (tf.exists()) {
        tf.delete();
      }
    }
  }
}
