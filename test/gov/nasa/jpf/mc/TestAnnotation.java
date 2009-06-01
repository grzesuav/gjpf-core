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

import gov.nasa.jpf.util.test.RawTest;

public class TestAnnotation extends RawTest {

  public static void main (String[] args) {
    TestAnnotation t = new TestAnnotation();

    if (!runSelectedTest(args, t)){
      runAllTests(args,t);
    }
  }
  
  @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
  @interface A3 {
    long value();
  }
  
  @A3(value = Long.MAX_VALUE)
  public void testLongElementAnnotation () {
    try {
      java.lang.reflect.Method method =
      TestAnnotation.class.getMethod("testLongElementAnnotation");
      A3 annotation = method.getAnnotation(A3.class);

      assert (annotation.value() == Long.MAX_VALUE);
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
  }
}
