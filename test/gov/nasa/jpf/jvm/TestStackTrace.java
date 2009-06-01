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

/**
 * stack trace API test
 */
public class TestStackTrace {
  public static void main (String[] args) {
    TestStackTrace t = new TestStackTrace();

    if (args.length > 0) {
      // just run the specified tests
      for (int i = 0; i < args.length; i++) {
        String func = args[i];

        // note that we don't use reflection here because this would
        // blow up execution/test scope under JPF
        if ("testGetStackTrace".equals(func)) {
          t.testGetStackTrace();
        } else {
          throw new IllegalArgumentException("unknown test function");
        }
      }
    } else {
      // that's mainly for our standalone test verification
      t.testGetStackTrace();
    }
  }

  public void testGetStackTrace () {
    getStackTrace_1();
  }

  void getStackTrace_1 () {
    getStackTrace_2();
  }

  void getStackTrace_2 () {
    Throwable           t = new Throwable();
    StackTraceElement[] s = t.getStackTrace();

    System.out.println("## " + s.length);

    for (int i = 0; i < s.length; i++) {
      System.out.println(s[i].getFileName() + ":" + s[i].getLineNumber() + 
                         " = " + s[i].getClassName() + "." + 
                         s[i].getMethodName());
    }
  }
}
