package gov.nasa.jpf.jvm;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;
import org.junit.runner.JUnitCore;

public class TestJavaTextDateFormatJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.jvm.TestJavaTextDateFormat";

  public static void main (String[] args) {
    JUnitCore.main(TEST_CLASS + "JPF");
  }


  /**************************** tests **********************************/
  @Test
  public void testConversionCycle () {
    String[] args = { TEST_CLASS, "testConversionCycle" };
    noPropertyViolation(args);
  }
}
