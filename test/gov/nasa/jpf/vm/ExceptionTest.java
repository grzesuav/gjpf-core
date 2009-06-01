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
package gov.nasa.jpf.vm;

import gov.nasa.jpf.util.test.RawTest;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * JPF unit test for exception handling
 */
@SuppressWarnings("null")
public class ExceptionTest extends RawTest {
  int data;

  void foo () {
  }
  
  static void bar () {
    ExceptionTest o = null;
    o.foo();
  }
  
  public static void main (String[] selectedMethods) {
    runTests( new ExceptionTest(), selectedMethods);
  }

  public static void testNPE () {
    ExceptionTest o = null;
    o.data = -1;

    assert false : "should never get here";
  }
  
  public static void testNPECall () {
    ExceptionTest o = null;
    o.foo();

    assert false : "should never get here";
  }

  public static void testArrayIndexOutOfBoundsLow () {
    int[] a = new int[10];
    a[-1] = 0;

    assert false : "should never get here";
  }

  public static void testArrayIndexOutOfBoundsHigh () {
    int[] a = new int[10];
    a[10] = 0;

    assert false : "should never get here";
  }

  public static void testLocalHandler () {
    try {
      ExceptionTest o = null;
      o.data = 0;
    } catch (IllegalArgumentException iax) {
      assert false : "should never get here";
    } catch (NullPointerException npe) {
      return;
    } catch (Exception x) {
      assert false : "should never get here";
    }
    
    assert false : "should never get here";
  }

  public static void testCallerHandler () {
    try {
      bar();
    } catch (Throwable t) {
      return;
    }
    
    assert false : "should never get here";
  }
  
  public static void testEmptyHandler () {
    try {
      throw new RuntimeException("should be empty-handled");
    } catch (Throwable t) {
      // nothing
    }
  }
  
  public static void testEmptyTryBlock () {
    try {
      // nothing
    } catch (Throwable t) {
      assert false : "should never get here";
    }
  }
  
  public static void testPrintStackTrace() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    
    Throwable x = new Throwable();
    x.printStackTrace(pw);
    String s = sw.toString();
    
    assert s != null && s.length() > 0 : "no stacktrace printout"; 

    /*
java.lang.Throwable
        at gov.nasa.jpf.jvm.Exceptions.testPrintStackTrace(Exceptions.java:117)
        at java.lang.reflect.Method.invoke(Native Method)
        at gov.nasa.jpf.testing.RawTest.runSelectedTest(RawTest.java:115)
        at gov.nasa.jpf.testing.RawTest.runTests(RawTest.java:44)
        at gov.nasa.jpf.jvm.Exceptions.main(Exceptions.java:41)
     */

    String[] lines = s.split("[\n\r]");
    assert lines.length == 6 : "wrong number of lines in stacktrace printout (expected 6) : " + lines.length;


    System.out.println("------- begin stacktrace printout");
    System.out.print(s);
    System.out.println("------- end stacktrace printout");
  }
  
}

