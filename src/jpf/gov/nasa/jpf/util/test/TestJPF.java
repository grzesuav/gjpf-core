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
package gov.nasa.jpf.util.test;

import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.Error;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;

import gov.nasa.jpf.Property;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.Reflection;
import java.util.List;
import java.io.PrintStream;


import java.util.Hashtable;
import org.junit.*;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;


/**
 * base class for JPF unit tests. TestJPF mostly includes JPF invocations
 * that check for occurrence or absence of certain execution results
 * 
 * This class can be used in two modes:
 *
 * <ol>
 * <li> wrapping a number of related tests for different SuTs into one class
 * (suite) that calls the various JPF runners with complete argument lists
 * (as in JPF.main(String[]args)) </li>
 *
 * <li> derive a class from TestJPF that uses the "..This" methods, which in
 * turn use reflection to automatically append the test class and method to the
 * JPF.main argument list (based on the calling class / method names). Note that
 * you have to obey naming conventions for this to work:
 *
 * <ul>
 * <li> the SuT class has to be the same as the test class without "Test", e.g.
 * "CastTest" -> "Cast" </li>
 * 
 * <li> the SuT method has to have the same name as the @Test method that
 * invokes JPF, e.g. "CastTest {.. @Test void testArrayCast() ..}" ->
 * "Cast {.. void testArrayCast()..} </li>
 *
 * </li>
 * </ol>
 */
public abstract class TestJPF extends Assert /*extends TestCase*/ {
  static PrintStream out = System.out;

  public static final String UNNAMED_PACKAGE = "";
  public static final String SAME_PACKAGE = null;

  String sutClassName;

  //------ internal methods

  protected void fail (String msg, String[] args, String cause){
    StringBuilder sb = new StringBuilder();

    sb.append(msg);
    if (args != null){
      for (String s : args){
        sb.append(s);
        sb.append(' ');
      }
    }

    if (cause != null){
      sb.append(':');
      sb.append(cause);
    }

    super.fail(sb.toString());
  }

  protected void report (String[] args) {
    out.print("  running jpf with args:");

    for (int i = 0; i < args.length; i++) {
      out.print(' ');
      out.print(args[i]);
    }

    out.println();
  }

  private String[] getArgsForCallerMethod (String[] jpfArgs){
    StackTraceElement callerEntry = Reflection.getCallerElement(2);

    String testMethod = callerEntry.getMethodName();
    String[] args = Misc.appendArray(jpfArgs, sutClassName, testMethod);

    return args;
  }

  /**
   * compute the SuT class name for a given JUnit test class: remove
   * optionally ending "..Test", and replace package (if specified)
   * 
   * @param testClass the JUnit test class
   * @param sutPackage optional SuT package name (without ending '.', null
   * os SAME_PACKAGE means same package, "" or UNNAMED_PACKAGE means unnamed package)
   * @return main class name of system under test
   */
  protected static String getSutClassName (String testClassName, String sutPackage){

    String sutClassName = testClassName;

    int i = sutClassName.lastIndexOf('.');
    if (i >= 0){  // testclass has a package

      if (sutPackage == null){   // use same package
        // nothing to do
      } else if (sutPackage.length() > 0) { // explicit sut package
        sutClassName = sutPackage + sutClassName.substring(i);

      } else { // unnamed sut package
        sutClassName = sutClassName.substring(i+1);
      }

    } else { // test class has no package
      if (sutPackage == null || sutPackage.length() == 0){   // use same package
        // nothing to do
      } else { // explicit sut package
        sutClassName = sutPackage + '.' + sutClassName;
      }
    }

    if (sutClassName.endsWith("JPF")) {
      sutClassName = sutClassName.substring(0, sutClassName.length() - 3);
    }

    return sutClassName;
  }

  // we can't set the sutClassName only from main() called methods (like
  // runTestsOfThisClass()) since main() doesn't get called if this is executed
  // by Ant (via <junit> task)
  // the default ctor is always executed
  public TestJPF () {
    sutClassName = getSutClassName(getClass().getName(), SAME_PACKAGE);
  }


  //------ the API to be used by subclasses

  /**
   * to be used from default ctor of derived class if the SuT is in a different
   * package
   * @param sutClassName the qualified SuT class name to be checked by JPF
   */
  protected TestJPF (String sutClassName){
    this.sutClassName = sutClassName;
  }

  public static boolean isJUnitRun() {
    // intercepted by native peer if this runs under JPF
    Throwable t = new Throwable();
    t.fillInStackTrace();

    for (StackTraceElement se : t.getStackTrace()){
      if (se.getClassName().startsWith("org.junit.")){
        return true;
      }
    }

    return false;
  }

  /**
   * run JUnit test methods of the provided class
   * @param testClass class to test
   * @param testMethods methods to run (all @Test methods if empty or null)
   */
  public static void runTests (Class<? extends Assert> testClass, String... testMethods){
    if (testClass == null ){
      fail("no test class specified");
    }

    if (testMethods == null || testMethods.length == 0){
      JUnitCore.main(testClass.getName());
    } else {
      JUnitCore runner = new JUnitCore();
      for (String mth : testMethods){
        Request req = Request.method(testClass, mth);
        runner.run(req);
      }
    }
  }

  protected static void runTestsOfThisClass (String[] testMethods){
    // needs to be at the same stack level, so we can't delegate
    Class<? extends Assert> testClass = Reflection.getCallerClass(Assert.class);
    runTests(testClass, testMethods);
  }


  /**
   * run JPF expecting a AssertionError in the SuT
   * @param args JPF main() arguments
   */
  public void assertionError (String... args) {
    unhandledException("java.lang.AssertionError", args );
  }
  protected void assertionErrorThis (String... jpfArgs){
    assertionError(getArgsForCallerMethod(jpfArgs));
  }


  /**
   * run JPF expecting no SuT property violations or JPF exceptions
   * @param args JPF main() arguments
   */
  public void noPropertyViolation (String... args) {
    ExceptionInfo xi = null;
    
    report(args);

    try {
      Config conf = JPF.createConfig(args);
      
      if (conf.getTargetArg() != null) {
        JPF jpf = new JPF(conf);
        jpf.run();
        
        List<Error> errors = jpf.getSearchErrors();      
        if ((errors != null) && (errors.size() > 0)) {
          fail("JPF found unexpected errors: " + (errors.get(0)).getDescription());
        }

        JVM vm = jpf.getVM();
        if (vm != null) {
          xi = vm.getPendingException();
        }
      }
    } catch (Throwable t) {
      // we get as much as one little hickup and we declare it failed
      fail("JPF internal exception executing: ", args, t.toString());
    }

    if (xi != null) {
      fail("JPF caught exception executing: ", args, xi.getExceptionClassname());
    }
  }
  protected void noPropertyViolationThis (String... jpfArgs){
    noPropertyViolation(getArgsForCallerMethod(jpfArgs));
  }



  /**
   * run JPF expecting an unhandled exception to occur in the SuT
   * @param args JPF main() arguments
   */
  public void unhandledException ( String xClassName, String... args) {
    ExceptionInfo xi = null;
    
    report(args);

    try {
      // run JPF on our target test func
      gov.nasa.jpf.JPF.main(args);

      xi = JVM.getVM().getPendingException();
      if (xi == null){
        fail("JPF failed to catch exception executing: ", args, ("expected " + xClassName));
      } else {
        String xn = xi.getExceptionClassname();
        if (!xn.equals(xClassName)){
          if (xn.equals(RawTest.Exception.class.getName())){
            xn = xi.getCauseClassname();
            if (!xn.equals(xClassName)){
              fail("JPF caught wrong exception: " + xn + ", expected: " + xClassName);            
            }
          } else {
            fail("JPF caught wrong exception: " + xn + ", expected: " + xClassName);          
          }
        }
      }
    } catch (Throwable x) {
      fail("JPF internal exception executing: ", args, x.toString());
    }
  }
  protected void unhandledExceptionThis (String exceptionClsName, String... jpfArgs){
    unhandledException(exceptionClsName, getArgsForCallerMethod(jpfArgs));
  }


  /**
   * run JPF expecting it to throw an exception
   * NOTE - xClassName needs to be the concrete exception, not a super class
   * @param args JPF main() arguments
   */
  public void jpfException (String xClassName, String[] args) {
    report(args);

    try {
      // run JPF on our target test func
      gov.nasa.jpf.JPF.main(args);
      
      fail("JPF failed to produce exception, expected: " + xClassName);
      
    } catch (Throwable x) {
      String xn = x.getClass().getName();
      if (!xn.equals(xClassName)){
        fail("JPF produced wrong exception: " + xn + ", expected: " + xClassName);
      }
    }
  }
  protected void jpfExceptionThis (Class<? extends Throwable> exceptionCls, String... jpfArgs){
    jpfException(exceptionCls.getName(), getArgsForCallerMethod(jpfArgs));
  }

  
  
  /**
   * run JPF expecting a property violation of the SuT
   * @param args JPF main() arguments
   */
  public void propertyViolation (Class<?> propertyCls, String[] args ){
    report(args);
    
    try {
      Config conf = JPF.createConfig(args);
      
      if (conf.getTargetArg() != null) {
        JPF jpf = new JPF(conf);
        jpf.run();
        
        List<Error> errors = jpf.getSearchErrors();
        
        if (errors != null) {
          for (Error e : errors) {          
            if (propertyCls == e.getProperty().getClass()) {
              System.out.println("found error: " + propertyCls.getName());
              return; // success, we got the sucker
            }
          }
        }          
      }
    } catch (Throwable x) {
      x.printStackTrace();
      fail("JPF internal exception executing: ", args, x.toString());
    }
    
    fail("JPF failed to detect error: " + propertyCls.getName());    
  }
  protected void propertyViolationThis (Class<? extends Property> propertyCls, String... jpfArgs){
    propertyViolation(propertyCls, getArgsForCallerMethod(jpfArgs));
  }


  /**
   * run JPF expecting a deadlock in the SuT
   * @param args JPF main() arguments
   */
  public void deadlock (String... args) {
    propertyViolation(NotDeadlockedProperty.class, args );
  }
  protected void deadlockThis (String... jpfArgs){
    propertyViolation(NotDeadlockedProperty.class, getArgsForCallerMethod(jpfArgs));
  }

}
