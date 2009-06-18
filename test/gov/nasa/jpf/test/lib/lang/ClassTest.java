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
 * This is a raw test class, which produces AssertionErrors for all
 * cases we want to catch. Make double-sure we don't refer to any
 * JPF class in here, or we start to check JPF recursively.
 * To turn this into a Junt test, you have to write a wrapper
 * TestCase, which just calls the testXX() methods.
 * The Junit test cases run JPF.main explicitly by means of specifying
 * which test case to run, but be aware of this requiring proper
 * state clean up in JPF !
 *
 * KEEP IT SIMPLE - it's already bad enough we have to mimic unit tests
 * by means of system tests (use whole JPF to check if it works), we don't
 * want to make the observer problem worse by means of enlarging the scope
 * JPF has to look at
 *
 * Note that we don't use assert expressions, because those would already
 * depend on working java.lang.Class APIs
 */
package gov.nasa.jpf.test.lib.lang;

import gov.nasa.jpf.util.test.TestJPF;
import java.lang.reflect.Method;
import org.junit.Test;

//interface A {}
//class Base extends TestJPF implements A {}
interface B {}

/**
 * test of java.lang.Class API
 */
public class ClassTest extends TestJPF implements Cloneable, B {
  
  public static void main (String[] args) {
    runTestsOfThisClass(args);
  }


  /**************************** tests **********************************/
  static String clsName = ClassTest.class.getName();

  int data = 42; // that creates a default ctor for our newInstance test


  @Test public void testClassForName () throws ClassNotFoundException {
    if (verifyNoPropertyViolation()) {

      Class<?> clazz = Class.forName(clsName);
      System.out.println("loaded " + clazz.getName());

      if (clazz == null) {
        throw new RuntimeException("Class.forName() returned null object");
      }

      if (!clsName.equals(clazz.getName())) {
        throw new RuntimeException(
                "getName() wrong for Class.forName() acquired class");
      }
    }
  }

  @Test public void testGetClass () {
    if (verifyNoPropertyViolation()) {
      Class<?> clazz = this.getClass();

      if (clazz == null) {
        throw new RuntimeException("Object.getClass() failed");
      }

      if (!clsName.equals(clazz.getName())) {
        throw new RuntimeException(
                "getName() wrong for getClass() acquired class");
      }
    }
  }

  @Test public void testIdentity () {
    if (verifyNoPropertyViolation()) {
      Class<?> clazz1 = null;
      Class<?> clazz2 = ClassTest.class;
      Class<?> clazz3 = this.getClass();

      try {
        clazz1 = Class.forName(clsName);
      } catch (Throwable x) {
      }

      if (clazz1 != clazz2) {
        throw new RuntimeException(
                "Class.forName() and class field not identical");
      }

      if (clazz2 != clazz3) {
        throw new RuntimeException(
                "Object.getClass() and class field not identical");
      }
    }
  }
  
  @Test public void testNewInstance () throws InstantiationException, IllegalAccessException {
    if (verifyNoPropertyViolation()) {
      Class<?> clazz = ClassTest.class;
      ClassTest o = (ClassTest) clazz.newInstance();
      
      System.out.println("new instance: " + o);
      
      if (o.data != 42) {
        throw new RuntimeException(
          "Class.newInstance() failed to call default ctor");        
      }
    }
  }
  
  static class InAccessible {
    private InAccessible() {}
  }
  
  @Test public void testNewInstanceFailAccess () throws IllegalAccessException, InstantiationException {
    if (verifyUnhandledException("java.lang.IllegalAccessException")){
      Class<?> clazz = InAccessible.class;
      Object o = clazz.newInstance();
    }
  }
  
  static abstract class AbstractClass {
  }
    
  @Test public void testNewInstanceFailAbstract () throws IllegalAccessException, InstantiationException {
    if (verifyUnhandledException("java.lang.InstantiationException")){
      Class<?> clazz = AbstractClass.class;
      Object o = clazz.newInstance();
    }
  }
  
  
  @Test public void testIsAssignableFrom () {
    if (verifyNoPropertyViolation()) {
      Class<?> clazz1 = Integer.class;
      Class<?> clazz2 = Object.class;
    
      assert clazz2.isAssignableFrom(clazz1);
  
      assert !clazz1.isAssignableFrom(clazz2); 
    }
  }  
  
  @Test public void testInstanceOf () {
    if (verifyNoPropertyViolation()) {
      assert this instanceof Cloneable;
      assert this instanceof TestJPF;
      assert this instanceof Object;

      if (this instanceof Runnable) {
        assert false : "negative instanceof test failed";
      }
    }
  }
  
  public void testAsSubclass () {
    if (verifyNoPropertyViolation()) {
      Class<?> clazz1 = Float.class;
    
      Class<? extends Number> clazz2 = clazz1.asSubclass(Number.class); 
      assert clazz2 != null;
      
      try {
        Class<? extends String> clazz3 = clazz1.asSubclass(String.class);
        assert false : "testAsSubclass() failed to throw ClassCastException";
      } catch (ClassCastException ccx) {
        
      } 
    }    
  }
  
  @SuppressWarnings("null")
  @Test public void testClassField () {
    if (verifyNoPropertyViolation()) {

      Class<?> clazz = ClassTest.class;

      if (clazz == null) {
        throw new RuntimeException("class field not set");
      }

      if (!clsName.equals(clazz.getName())) {
        throw new RuntimeException("getName() wrong for class field");
      }
    }
  }

  @Test public void testInterfaces () {
    if (verifyNoPropertyViolation()) {
      Class<?>[] ifc = ClassTest.class.getInterfaces();
      if (ifc.length != 2) {
        throw new RuntimeException("wrong number of interfaces: " + ifc.length);
      }

      int n = ifc.length;
      String[] ifcs = {"java.lang.Cloneable", B.class.getName()};
      for (int i = 0; i < ifcs.length; i++) {
        for (int j = 0; j < ifc.length; j++) {
          if (ifc[j].getName().equals(ifcs[i])) {
            n--;
            break;
          }
        }
      }

      if (n != 0) {
        throw new RuntimeException("wrong interface types: " + ifc[0].getName() + ',' + ifc[1].getName());
      }
    }
  }
  
  
  static class TestClassBase {
    protected TestClassBase() {}
    public void foo () {}
  }
  
  interface TestIfc {
    void boo();
    void foo();
  }
  
  static abstract class TestClass extends TestClassBase implements TestIfc {
    public TestClass() {}
    public TestClass (int a) {}
    public void foo() {}
    void bar() {}
    public static void baz () {}
    
  }
  
  @Test public void testMethods() {
    if (verifyNoPropertyViolation()) {

      Class<?> cls = TestClass.class;
      Method[] methods = cls.getMethods();

      if (methods.length <= 4) {
        // the java.lang.Object methods + TestClassBase + TestClass
        throw new RuntimeException("wrong number of getMethods() elements (>4): " + methods.length);
      }

      // we should have two foo's() and one baz()
      for (int i = 0; i < methods.length; i++) {
        Method m = methods[i];
        Class<?> declCls = m.getDeclaringClass();
        String mname = m.getName();

        // we don't care about the Object methods
        if (declCls == Object.class) {
          methods[i] = null;
          continue;
        }

        if (declCls == TestClassBase.class) {
          if (mname.equals("foo")) {
            methods[i] = null;
            continue;
          }
        }

        if (declCls == TestClass.class) {
          if (mname.equals("foo")) {
            methods[i] = null;
            continue;
          }
          if (mname.equals("baz")) {
            methods[i] = null;
            continue;
          }
        }

        if (declCls == TestIfc.class) {
          if (mname.equals("boo")) {
            methods[i] = null;
            continue;
          }
        }
      }

      for (int i = 0; i < methods.length; i++) {
        if (methods[i] != null) {
          throw new RuntimeException("unexpected method in getMethods(): " +
                  methods[i].getDeclaringClass().getName() + " : " + methods[i]);
        }
      }
    }
  }
}
