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
    if (verifyNoPropertyViolation()){  // either if it's under JUnit, or the '-jpf' arg was present
      // this code is only run under JPF
      d += 42;
      System.out.println("** this is test_1() - it should succeed");
    }
  }


}
