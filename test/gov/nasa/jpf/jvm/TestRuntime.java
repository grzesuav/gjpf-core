package gov.nasa.jpf.jvm;

import gov.nasa.jpf.util.test.RawTest;

public class TestRuntime extends RawTest {

  public static void main (String[] args) {
    TestRuntime t = new TestRuntime();
    
    if (!runSelectedTest(args, t)){
      runAllTests(args,t);
    }
  }

  public void testAvailableProcessors() {
    Runtime rt = Runtime.getRuntime();
    
    int n = rt.availableProcessors();
    System.out.println("-- available processors: " + n);
    Verify.incrementCounter(0);
  }
}
