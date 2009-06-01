package gov.nasa.jpf.basic;


import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

/**
 * raw test to test the test harness (how often can you say 'test' in one
 * sentence?)
 */
public class $02_HarnessTestJPF extends TestJPF {

  int d;

  public static void main (String[] args){  // "[-jpf] testMethod...
    runTestsOfThisClass(args);
  }

  @Test public void test_1 () {
    if (runJPF()){  // either if it's under JUnit, or the '-jpf' arg was present
      noPropertyViolationThis();
      return;
    }

    d += 42;
    System.out.println("** this is test_1() - it should succeed");
  }

  @Test public void test_3 () {
    System.out.println("** this is test_3() - it should fail");
    int[] a = { 1, 2 };
    a[3] = 0;
  }

}
