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
package gov.nasa.jpf.test.vm.basic;

import java.lang.annotation.*;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;


public class AnnotationTest extends TestJPF {

  public static void main (String[] args) {
    runTestsOfThisClass(args);
  }

  //--- test methods

  @Test //----------------------------------------------------------------------
  @A1("foo")
  public void testStringValueOk () {
    if (verifyNoPropertyViolation()) {
      try {
        java.lang.reflect.Method method =
                AnnotationTest.class.getMethod("testStringValueOk");
        A1 annotation = method.getAnnotation(A1.class);

        assert ("foo".equals(annotation.value()));
        
      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface A1 {
    String value();
  }


  @Test //----------------------------------------------------------------------
  @A2({"foo", "boo"})
  public void testStringArrayValueOk () {
    if (verifyNoPropertyViolation()) {
      try {
        java.lang.reflect.Method method =
                AnnotationTest.class.getMethod("testStringArrayValueOk");
        A2 annotation = method.getAnnotation(A2.class);

        Object v = annotation.value();
        assert v instanceof String[];

        String[] a = (String[])v;
        assert a.length == 2;

        assert "foo".equals(a[0]);
        assert "boo".equals(a[1]);

      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface A2 {
    String[] value();
  }

  @Test //----------------------------------------------------------------------
  @A3(Long.MAX_VALUE)
  public void testLongValueOk () {
    if (verifyNoPropertyViolation()) {
      try {
        java.lang.reflect.Method method =
                AnnotationTest.class.getMethod("testLongValueOk");
        A3 annotation = method.getAnnotation(A3.class);

        assert (annotation.value() == Long.MAX_VALUE);
      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface A3 {
    long value();
  }


  @Test //----------------------------------------------------------------------
  @A4(a="one",b=42.0)
  public void testNamedParamsOk () {
    if (verifyNoPropertyViolation()) {
      try {
        java.lang.reflect.Method method =
                AnnotationTest.class.getMethod("testNamedParamsOk");
        A4 annotation = method.getAnnotation(A4.class);

        assert ("one".equals(annotation.a()));
        assert ( 42.0 == annotation.b());

        System.out.println(annotation);

      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface A4 {
    String a();
    double b();
  }


  @Test //----------------------------------------------------------------------
  @A5(b="foo")
  public void testPartialDefaultParamsOk () {
    if (verifyNoPropertyViolation()) {
      try {
        java.lang.reflect.Method method =
                AnnotationTest.class.getMethod("testPartialDefaultParamsOk");
        A5 annotation = method.getAnnotation(A5.class);

        assert ("whatever".equals(annotation.a()));

        System.out.println(annotation);

      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface A5 {
    String a() default "whatever";
    String b();
  }

  @Test //----------------------------------------------------------------------
  @A6
  public void testSingleDefaultParamOk () {
    if (verifyNoPropertyViolation()) {
      try {
        java.lang.reflect.Method method =
                AnnotationTest.class.getMethod("testSingleDefaultParamOk");
        A6 annotation = method.getAnnotation(A6.class);

        assert ("whatever".equals(annotation.value()));

        System.out.println(annotation);

      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface A6 {
    String value() default "whatever";
  }

}
