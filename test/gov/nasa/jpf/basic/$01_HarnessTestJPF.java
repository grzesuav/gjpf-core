package gov.nasa.jpf.basic;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

/**
 * the JPF test driver for our harness test
 */
public class $01_HarnessTestJPF extends TestJPF {

  public static void main (String[] selectedTests){
    runTestsOfThisClass(selectedTests);
  }

  @Test
  public void test_1 () {
    System.out.println("@ run JPF on test_1 (should succeed)");
    noPropertyViolationThis();
  }

  @Test
  public void test_2 () {
    System.out.println("@ run JPF on test_2 (should succeed)");
    noPropertyViolationThis();
  }

  @Test
  public void test_3 () {
    System.out.println("@ run JPF on test_3 (should fail with AIOB)");
    unhandledExceptionThis("java.lang.ArrayIndexOutOfBoundsException");
  }

  @Test
  public void test_1_and_2 () {
    System.out.println("@ run JPF on test_1 and test_2 (should succeed)");

    noPropertyViolation("gov.nasa.jpf.basic.$01_HarnessTest", "test_1","test_2");
  }

  @Test
  public void test_all () {
    System.out.println("@ run JPF on all public tests (should fail with AIOB)");
    unhandledException("java.lang.ArrayIndexOutOfBoundsException",
                       "gov.nasa.jpf.basic.$01_HarnessTest");
  }


}
