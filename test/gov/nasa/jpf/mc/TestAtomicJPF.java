package gov.nasa.jpf.mc;

import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;
import org.junit.runner.JUnitCore;

public class TestAtomicJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.mc.TestAtomic";

  public static void main (String[] args) {
    JUnitCore.main(TEST_CLASS + "JPF");
  }


  /**************************** tests **********************************/

  @Test
  public void testNoRace () {
    String[] args = { TEST_CLASS, "testNoRace" };
    noPropertyViolation(args);
  }

  @Test
  public void testDataCG () {
    String[] args = { TEST_CLASS, "testDataCG" };
    noPropertyViolation(args);
  }

}
