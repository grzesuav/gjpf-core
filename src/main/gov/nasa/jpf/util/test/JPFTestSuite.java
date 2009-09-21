//
// Copyright (C) 2009 United States Government as represented by the
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

import gov.nasa.jpf.JPFClassLoader;
import gov.nasa.jpf.JPFClassLoaderException;
import gov.nasa.jpf.Property;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * JPFTestSuite is the base class for (JUnit) tests using JPF outside of
 * JPF projects. Just derive your xxTest classes from JPFTestSuite, copy
 * the standard main method and use

 *  - verifyNoPropertyViolation
 *  - verifyUnhandledException
 *  - verifyAssertionError
 *  - verifyPropertyViolation
 *
 * or whatever JPF run method is appropriate from within your @Test methods
 *
 *
 * <pre>
 *   public static void main (String[] args){
 *     runTestsOfThisClass(args);
 *   }
 *
 *   @Test public void test_1 () {
 *     if (verifyNoPropertyViolation("+cg.enumerate_random", ..){
 *       //... code to be verified by JPF goes here
 *     }
 *   }
 * </pre>
 *
 * Only jpf.jar needs to be added to the external jars of the target project,
 * JPF automatically finds all other JPF related jars from its site configuration
 */
public class JPFTestSuite extends TestJPF {

  static private boolean runClean = false; // new JPF classes for each test method
  private JPFClassLoader jpfLoader = null;

  static final String JPF_RUN = "gov.nasa.jpf.util.test.JPFTestRun";


  JPFClassLoader getLoader() {
    if (jpfLoader == null || runClean) {
      JPFClassLoader cl = new JPFClassLoader();

      cl.addPreloadedClass(JPFTestSuite.class);
      cl.addPreloadedClass(TestJPF.class);

      jpfLoader = cl;
    }
    return jpfLoader;
  }

  /**
   * run JPF expecting no SuT property violations or JPF exceptions
   * @param args JPF main() arguments
   */
  @Override
  public void noPropertyViolation (String... args) {
    try {
      Class<?> cls = getLoader().loadClass(JPF_RUN);

      Class<?>[] argTypes = {TestJPF.class, String[].class};
      Method method = cls.getMethod("noPropertyViolation", argTypes);

      method.invoke(null, new Object[]{this,args});

    } catch (ClassNotFoundException cnfx) {
      fail("cannot find " + JPF_RUN);
    } catch (NoSuchMethodException nsmx) {
      fail("no \"public static void noPropertyViolation(TestJPF, String[])\" method in " + JPF_RUN);
    } catch (IllegalAccessException iax) {
      fail("no \"public static void noPropertyViolation(TestJPF, String[])\" method in " + JPF_RUN);
    } catch (InvocationTargetException ix) {
      ix.getCause().printStackTrace();
      fail(ix.getCause().toString());
      // should already be reported by JPF
    } catch (JPFClassLoaderException ex) {
      fail(ex.toString());
    }
  }

  @Override
  public void propertyViolation (Class<? extends Property> propertyCls, String... args ){
    try {
      JPFClassLoader loader = getLoader();
      loader.addPreloadedClass(propertyCls);

      Class<?> cls = loader.loadClass(JPF_RUN);

      Class<?>[] argTypes = {TestJPF.class, Class.class, String[].class};
      Method method = cls.getMethod("propertyViolation", argTypes);

      method.invoke(null, new Object[]{this,propertyCls,args});

    } catch (ClassNotFoundException cnfx) {
      fail("cannot find " + JPF_RUN);
    } catch (NoSuchMethodException nsmx) {
      fail("no \"public static void propertyViolation(TestJPF,Class<? extends Property>,String[])\" method in " + JPF_RUN);
    } catch (IllegalAccessException iax) {
      fail("no \"public static void propertyViolation(TestJPF,Class<? extends Property>,String[])\" method in " + JPF_RUN);
    } catch (InvocationTargetException ix) {
      ix.getCause().printStackTrace();
      fail(ix.getCause().toString());
      // should already be reported by JPF
    } catch (JPFClassLoaderException ex) {
      fail(ex.toString());
    }
  }

  @Override
  public void unhandledException ( String xClassName, String details, String... args) {
    try {
      Class<?> cls = getLoader().loadClass(JPF_RUN);

      Class<?>[] argTypes = {TestJPF.class, String.class, String.class, String[].class};
      Method method = cls.getMethod("unhandledException", argTypes);

      method.invoke(null, new Object[]{this, xClassName, details, args});

    } catch (ClassNotFoundException cnfx) {
      fail("cannot find " + JPF_RUN);
    } catch (NoSuchMethodException nsmx) {
      fail("no \"public static void unhandledException(TestJPF,String,String,String[])\" method in " + JPF_RUN);
    } catch (IllegalAccessException iax) {
      fail("no \"public static void unhandledException(TestJPF,String,String,String[])\" method in " + JPF_RUN);
    } catch (InvocationTargetException ix) {
      ix.getCause().printStackTrace();
      fail(ix.getCause().toString());
      // should already be reported by JPF
    } catch (JPFClassLoaderException ex) {
      fail(ex.toString());
    }
  }

  @Override
  public void jpfException (Class<? extends Throwable> xCls, String... args) {
    try {
      JPFClassLoader loader = getLoader();
      loader.addPreloadedClass(xCls);

      Class<?> cls = loader.loadClass(JPF_RUN);

      Class<?>[] argTypes = {TestJPF.class, Class.class, String[].class};
      Method method = cls.getMethod("jpfException", argTypes);

      method.invoke(null, new Object[]{this,xCls,args});

    } catch (ClassNotFoundException cnfx) {
      fail("cannot find " + JPF_RUN);
    } catch (NoSuchMethodException nsmx) {
      fail("no \"public static void jpfException(TestJPF,Class<? extends Throwable>,String[])\" method in " + JPF_RUN);
    } catch (IllegalAccessException iax) {
      fail("no \"public static void jpfException(TestJPF,Class<? extends Throwable>,String[])\" method in " + JPF_RUN);
    } catch (InvocationTargetException ix) {
      ix.getCause().printStackTrace();
      fail(ix.getCause().toString());
      // should already be reported by JPF
    } catch (JPFClassLoaderException ex) {
      fail(ex.toString());
    }
  }  
}
