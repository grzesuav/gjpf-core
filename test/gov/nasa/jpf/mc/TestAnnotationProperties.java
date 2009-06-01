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

import java.lang.reflect.InvocationTargetException;

import gov.nasa.jpf.util.test.RawTest;
import gov.nasa.jpf.NonShared;
import gov.nasa.jpf.Const;

/**
 * raw test for Java Annotation based JPF properties
 */
public class TestAnnotationProperties extends RawTest {
  public static void main (String[] args) throws InvocationTargetException {
    TestAnnotationProperties t = new TestAnnotationProperties();
    if (!runSelectedTest(args, t)){
      //t.testNonShared();
      t.testConst();
    }
  }

  //--- our annotated test class
  @NonShared
  static class A {
    int d;
    
    @Const
    void foo () {
      d = 42;
    }
  }
  
  //--- the test methods
  
  public void testNonShared () {
    final A a = new A();
    
    Runnable r = new Runnable() {
      public void run () {
        a.d = 42;
      }
    };
    Thread t = new Thread(r);
    t.start();
  }
  
  public void testConst () {
    A a = new A();
    a.foo();
  }
}
