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
package gov.nasa.jpf.util.test;

import gov.nasa.jpf.util.Reflection;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;




/**
 * common base for all raw tests, i.e. stuff that should also run
 * outside the model checker
 */
public class RawTest {

  // we need this to distinguish between exceptions in the testing framework
  // and exceptions caused by the tested code (which could be anything except
  // of this one)
  public static class Exception extends RuntimeException {

    public Exception(String details, Throwable cause) {
      super(details, cause);
    }
  }


  protected static void runTestsOfThisClass (String... testMethods){
    // intercepted by native peer if we run under JPF

    // this is why we intercept - we don't want this executed by JPF
    Class<? extends RawTest> cls = Reflection.getCallerClass(RawTest.class);
    runTests(cls, testMethods);
  }

  public static void runTests(Class<?> testCls, String... testMethods) {
    Method testMethod = null;
    Object testObject = null;

    try {
      if (testMethods != null && testMethods.length > 0) {
          for (String test : testMethods) {
            try {
             testMethod = testCls.getDeclaredMethod(test);
            } catch (NoSuchMethodException x){
                 throw new RawTest.Exception("method: " + test +
                    "() not in test class: " + testCls.getName(), x);
            }

            testObject = testCls.newInstance();

            System.out.println("-- running test: " + test);
            testMethod.invoke(testObject);
          }

      } else {
        for (Method m : testCls.getDeclaredMethods()) {
          testMethod = m;
          int mod = m.getModifiers();
          if (m.getParameterTypes().length == 0 &&
                  m.getName().startsWith("test") &&
                  Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
            testObject = testCls.newInstance();

            System.out.println("-- running test: " + m.getName());
            testMethod.invoke(testObject);
          }
        }
      }

    } catch (InstantiationException x) {
      throw new RawTest.Exception("error instantiating test class: " + testCls.getName(), x);
    } catch (IllegalAccessException x) {
      throw new RawTest.Exception("no public method: " +
              ((testMethod != null) ? testMethod.getName() : "<init>") +
              " of test class: " + testCls.getName(), x);
    } catch (IllegalArgumentException x) {
      throw new RawTest.Exception("illegal argument for test method: " + testMethod.getName(), x);

    } catch (InvocationTargetException x) {
      throw new RawTest.Exception("failed test method: " + testMethod.getName(), x);
    }
  }

  public static void reset() {
    // only here to be intercepted if run under JPF
  }
}
