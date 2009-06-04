package gov.nasa.jpf.basic;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

/**
 * basic test to test the test harness (how often can you say 'test' in one
 * sentence?)
 */
public class t01_HarnessTestJPF extends TestJPF {

  int d;

  public static void main (String[] args){  // "[-jpf] testMethod...
    runTestsOfThisClass(args);
  }

  @Test public void test_1 () {
    if (verifyNoPropertyViolation()){
      d += 42;
      System.out.println("** this is test_1() - it should succeed");
    }
  }

  @Test public void test_2 () {
    if (verifyAssertionError("wrong answer..")){
      System.out.println("** this is test_2() - JPF should find an AssertionError");
      assert d == 42 : "wrong answer..";
    }
  }

  @Test public void test_3 () {
    if (verifyUnhandledException("java.lang.NullPointerException")){
      System.out.println("** this is test_3() - JPF should find an unhandled exception");
      String s = null;
      int l = s.length();
    }
  }

}
