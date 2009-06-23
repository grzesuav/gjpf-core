package gov.nasa.jpf.test.basic;

import gov.nasa.jpf.jvm.NoUncaughtExceptionsProperty;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

/**
 * basic test to test the test harness (how often can you say 'test' in one
 * sentence?)
 */
public class HarnessTest extends TestJPF {

  int d;

  //--- this is standard preamble so that we can execute outside JUnit (also directly)

  public static void main (String[] args){  // "[-d] testMethod...
    runTestsOfThisClass(args);
  }

  //--- here are the test methods

  @Test public void test_1 () {
    if (verifyNoPropertyViolation()){
      d += 42;
      System.out.println("** this is test_1() - it should succeed");
    }
  }

  @Test public void test_2 () {
    if (verifyAssertionErrorDetails("wrong answer..")){
      System.out.println("** this is test_2() - JPF should find an AssertionError");
      assert d == 42 : "wrong answer..";
    }
  }

  @Test public void test_3 () {
    if (verifyUnhandledException("java.lang.NullPointerException")){
      System.out.println("** this is test_3() - JPF should find an NPE");
      String s = null;
      int l = s.length();
    }
  }

  @Test public void test_4 () {
    if (verifyPropertyViolation(NoUncaughtExceptionsProperty.class)){
      System.out.println("** this is test_4() - JPF should find an unhandled exception");
      throw new RuntimeException("Bang!");
    }
  }

  @Test public void test_5 () {
    if (verifyJPFException(Throwable.class, "+vm.class=InvalidVMClass")){
      System.out.println("** JPF should not run, so this should only show when directly executed");
    }
  }

}
