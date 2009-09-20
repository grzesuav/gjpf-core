package gov.nasa.jpf.util.test;

import gov.nasa.jpf.JPFClassLoader;
import gov.nasa.jpf.JPFClassLoaderException;
import gov.nasa.jpf.Property;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class JPFTestCase extends TestJPF {

  static boolean runClean; // new classes for each test method
  static String JPF_RUN = "gov.nasa.jpf.util.test.JPFTestRun";

  JPFClassLoader jpfLoader = null;

  JPFClassLoader getLoader() {
    if (jpfLoader == null || runClean) {
      JPFClassLoader cl = new JPFClassLoader();

      cl.addPreloadedClass(JPFTestCase.class);
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
      fail("no \"public static void jpfException(TestJPF,String[])\" method in " + JPF_RUN);
    } catch (InvocationTargetException ix) {
      ix.getCause().printStackTrace();
      fail(ix.getCause().toString());
      // should already be reported by JPF
    } catch (JPFClassLoaderException ex) {
      fail(ex.toString());
    }
  }  
}
