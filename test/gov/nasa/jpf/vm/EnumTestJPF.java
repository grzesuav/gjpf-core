package gov.nasa.jpf.vm;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

public class EnumTestJPF extends TestJPF {

  public static void main (String[] selectedMethods) {
    runTestsOfThisClass(selectedMethods);
  }


  /**************************** tests **********************************/

  @Test
  public void testValueOf () {
    noPropertyViolationThis();
  }
}
