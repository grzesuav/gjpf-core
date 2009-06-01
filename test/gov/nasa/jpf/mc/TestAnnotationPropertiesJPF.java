//
// Copyright (C) 2007 United States Government as represented by the
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
import gov.nasa.jpf.tools.ObjectTracker;

/**
 * JPF test driver for Java Annotation based properties
 */
public class TestAnnotationPropertiesJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.mc.TestAnnotationProperties";

  public static void main (String[] args) {
    JUnitCore.main( TEST_CLASS + "JPF");
  }

  /**************************** tests **********************************/
  @Test
  public void testInstanceRace () {
    String[] args = { "+jpf.listener=.tools.ObjectTracker",
        "+jpf.listener.autoload=",    // we don't want any additional listeners
        "+ot.include=*$A", "+ot.check_shared=true",
        "+jpf.report.console.property_violation=error,snapshot",
        TEST_CLASS, "testNonShared" };
    propertyViolation(args, ObjectTracker.class);
  }

  @Test
  public void testConst () {
    String[] args = { "+jpf.listener=.tools.ObjectTracker",
        "+jpf.listener.autoload=",    // we don't want any additional listeners
        "+ot.include=*$A", "+ot.check_const=true",
        "+jpf.report.console.property_violation=error,snapshot",
        TEST_CLASS, "testConst" };
    propertyViolation(args, ObjectTracker.class);
  }
}
