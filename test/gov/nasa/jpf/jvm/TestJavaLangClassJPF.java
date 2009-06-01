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
 * JPF driver for java.lang.Class test
 */
public class TestJavaLangClassJPF extends TestJPF {
  static final String  TEST_CLASS = "gov.nasa.jpf.jvm.TestJavaLangClass";

  public static void main (String[] args) {
    JUnitCore.main("gov.nasa.jpf.jvm.TestJavaLangClassJPF");
  }



  /**************************** tests **********************************/
  @Test
  public void testClassField () {
    String[] args = { TEST_CLASS, "testClassField" };
    noPropertyViolation(args);
  }

  @Test
  public void testClassForName () {
    String[] args = { TEST_CLASS, "testClassForName" };
    noPropertyViolation(args);
  }

  @Test
  public void testGetClass () {
    String[] args = { TEST_CLASS, "testGetClass" };
    noPropertyViolation(args);
  }

  @Test
  public void testIdentity () {
    String[] args = { TEST_CLASS, "testIdentity" };
    noPropertyViolation(args);
  }

  @Test
  public void testNewInstance () {
    String[] args = { TEST_CLASS, "testNewInstance" };
    noPropertyViolation(args);
  }

  @Test
  public void testNewInstanceFailAccess () {
    String[] args = { TEST_CLASS, "testNewInstanceFailAccess" };
    unhandledException(args, "java.lang.IllegalAccessException");
  }

  @Test
  public void testNewInstanceFailAbstract () {
    String[] args = { TEST_CLASS, "testNewInstanceFailAbstract" };
    unhandledException(args, "java.lang.InstantiationException");
  }
  
  @Test
  public void testIsAssignableFrom () {
    String[] args = { TEST_CLASS, "testIsAssignableFrom" };
    noPropertyViolation(args);
  }

  @Test
  public void testInstanceOf () {
    String[] args = { TEST_CLASS, "testInstanceOf" };
    noPropertyViolation(args);
  }
  
  @Test
  public void testAsSubclass () {
    String[] args = { TEST_CLASS, "testAsSubclass" };
    noPropertyViolation(args);
  }

  @Test
  public void testInterfaces () {
    String[] args = { TEST_CLASS, "testInterfaces" };
    noPropertyViolation(args);
  }

  @Test
  public void testMethods () {
    String[] args = { TEST_CLASS, "testMethods" };
    noPropertyViolation(args);
  }
  
}
