
package gov.nasa.jpf.util.test;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.jvm.ExceptionInfo;
import gov.nasa.jpf.jvm.JVM;
import java.io.PrintWriter;
import java.util.List;
import gov.nasa.jpf.Error;
import gov.nasa.jpf.Property;

public class JPFTestRun {

  static JPF createAndRunJPF (TestJPF test, String[] args){
    JPF jpf = null;
    Config conf = JPF.createConfig(args);

    if (conf.getTarget() != null) {
      jpf = new JPF(conf);

      if (test.showConfig()) {
        conf.print(new PrintWriter(System.out));
      }

      JPF_gov_nasa_jpf_util_test_TestJPF.init();

      jpf.run();
    }

    return jpf;
  }

  public static void propertyViolation (TestJPF test, Class<? extends Property> propertyCls, String... args ){
    JPF jpf = null;

    test.report(args);

    try {
      jpf = createAndRunJPF(test, args);
    } catch (Throwable x) {
      test.fail("JPF internal exception executing: ", args, x.toString());
    }

    List<Error> errors = jpf.getSearchErrors();
    if (errors != null) {
      for (Error e : errors) {
        if (propertyCls == e.getProperty().getClass()) {
          return; // success, we got the sucker
        }
      }
    }

    test.fail("JPF failed to detect error: " + propertyCls.getName());
  }


  public static void unhandledException (TestJPF test, String xClassName, String details, String... args) {

    test.report(args);

    try {
      createAndRunJPF(test, args);
    } catch (Throwable x) {
      test.fail("JPF internal exception executing: ", args, x.toString());
    }

    ExceptionInfo xi = JVM.getVM().getPendingException();
    if (xi == null) {
      test.fail("JPF failed to catch exception executing: ", args, ("expected " + xClassName));
    } else {
      String xn = xi.getExceptionClassname();
      if (!xn.equals(xClassName)) {
        if (xn.equals(TestException.class.getName())) {
          xn = xi.getCauseClassname();
          if (!xn.equals(xClassName)) {
            test.fail("JPF caught wrong exception: " + xn + ", expected: " + xClassName);
          }
          if (details != null) {
            String xd = xi.getCauseDetails();
            if (!details.equals(xd)) {
              test.fail("wrong exception details: " + xd + ", expected: " + details);
            }
          }
        } else {
          test.fail("JPF caught wrong exception: " + xn + ", expected: " + xClassName);
        }
      }
    }
  }

  public static void noPropertyViolation (TestJPF test, String[] args) {
    JPF jpf = null;

    test.report(args);

    try {
      jpf = createAndRunJPF(test,args);
    } catch (Throwable t) {
      // we get as much as one little hickup and we declare it failed
      test.fail("JPF internal exception executing: ", args, t.toString());
    }

    List<Error> errors = jpf.getSearchErrors();
    if ((errors != null) && (errors.size() > 0)) {
      test.fail("JPF found unexpected errors: " + (errors.get(0)).getDescription());
    }

    JVM vm = jpf.getVM();
    if (vm != null) {
      ExceptionInfo xi = vm.getPendingException();
      if (xi != null) {
        test.fail("JPF caught exception executing: ", args, xi.getExceptionClassname());
      }
    }
  }

  public static void jpfException (TestJPF test, Class<? extends Throwable> xCls, String... args) {
    Throwable exception = null;

    test.report(args);

    try {
      createAndRunJPF(test, args);
    } catch (JPF.ExitException xx) {
      exception = xx.getCause();
    } catch (Throwable x) {
      exception = x;
    }

    if (exception != null){
      if (!xCls.isAssignableFrom(exception.getClass())){
        test.fail("JPF produced wrong exception: " + exception + ", expected: " + xCls.getName());
      }
    } else {
      test.fail("JPF failed to produce exception, expected: " + xCls.getName());
    }
  }

}
