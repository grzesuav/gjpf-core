package gov.nasa.jpf.jvm;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;
import org.junit.runner.JUnitCore;

public class TestRuntimeJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.jvm.TestRuntime";

  public static void main (String[] args) {
    JUnitCore.main( TEST_CLASS + "JPF");
  }


  /**************************** tests **********************************/

  @Test
  public void testAvailableProcessors() {
    String[] args = { "+cg.max_processors=2", TEST_CLASS, "testAvailableProcessors" };
    
    Verify.resetCounter(0);
    
    noPropertyViolation(args);

    if (Verify.getCounter(0) != 2) {
      fail("wrong number of backtracks: " + Verify.getCounter(0));
    }

  }
}
