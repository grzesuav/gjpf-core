package gov.nasa.jpf.mc;

import org.junit.Test;
import org.junit.runner.JUnitCore;

import gov.nasa.jpf.util.test.TestJPF;

public class TestSchedulesJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.mc.TestSchedules";


  public static void main (String[] args) {
    JUnitCore.main("gov.nasa.jpf.mc.TestSchedulesJPF");
  }

  /**************************** tests **********************************/

  @Test
  public void testSleep () {
    String[] args = {
        "+cg.threads.break_all=true",
        "+jpf.listener=.tools.PathOutputMonitor",
        "+pom.all=test/gov/nasa/jpf/mc/TestSchedules-output",
        TEST_CLASS, "testSleep" };
    noPropertyViolation(args);
  }
  
}
