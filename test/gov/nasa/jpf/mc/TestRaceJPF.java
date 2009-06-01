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
/**
 * JPF test driver for field race detection
 */
package gov.nasa.jpf.mc;

import org.junit.Test;
import org.junit.runner.JUnitCore;

import gov.nasa.jpf.util.test.TestJPF;

public class TestRaceJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.mc.TestRace";

  public static void main (String[] args) {
    JUnitCore.main("gov.nasa.jpf.mc.TestRaceJPF");
  }

  /**************************** tests **********************************/

  // the first two ones are just to show that the races are for real
  // (programmed so that race is detectable and throws exception)
  @Test
  public void testInstanceRace () {
    String[] args = { TEST_CLASS, "testInstanceRace" };
    unhandledException(args, "java.lang.RuntimeException");
  }
  
  @Test
  public void testStaticRace () {
    String[] args = { TEST_CLASS, "testStaticRace" };
    unhandledException(args, "java.lang.RuntimeException");
  }

  // now we try to find the races early with the PreciseRaceDetector

  @Test
  public void testInstanceRaceListener () {
    String[] args = { "+jpf.listener=.tools.PreciseRaceDetector", TEST_CLASS, "testInstanceRaceNoThrow" };
    propertyViolation(args, gov.nasa.jpf.tools.PreciseRaceDetector.class);
  }

  @Test
  public void testStaticRaceListener () {
    String[] args = { "+jpf.listener=.tools.PreciseRaceDetector", TEST_CLASS, "testStaticRaceNoThrow" };
    propertyViolation(args, gov.nasa.jpf.tools.PreciseRaceDetector.class);
  }

  @Test
  public void testInstanceRaceListenerExclude () {
    String[] args = { "+jpf.listener=.tools.PreciseRaceDetector", "+race.exclude=gov.nasa.jpf.mc.SharedObject.*",
                      TEST_CLASS, "testInstanceRaceNoThrow" };
    noPropertyViolation(args);
  }

  @Test
  public void testInstanceRaceListenerInclude () {
    String[] args = { "+jpf.listener=.tools.PreciseRaceDetector", "+race.include=gov.nasa.jpf.mc.SharedObject.instanceField",
                      TEST_CLASS, "testInstanceRaceNoThrow" };
    propertyViolation(args, gov.nasa.jpf.tools.PreciseRaceDetector.class);
  }

  @Test
  public void testStaticRaceListenerIncludeOther () {
    String[] args = { "+jpf.listener=.tools.PreciseRaceDetector", "+race.include=gov.nasa.jpf.mc.SharedObject.instanceField",
                      TEST_CLASS, "testStaticRaceNoThrow" };
    noPropertyViolation(args);
  }

  
  // the rest is to filter out false positives

  @Test
  public void testSameInsnOtherObject () {
    String[] args = { "+jpf.listener=.tools.PreciseRaceDetector", TEST_CLASS, "testSameInsnOtherObject" };
    noPropertyViolation(args);
  }

  @Test
  public void testSameObjectOtherField () {
    String[] args = { "+jpf.listener=.tools.PreciseRaceDetector", TEST_CLASS, "testSameObjectOtherField" };
    noPropertyViolation(args);
  }

}
