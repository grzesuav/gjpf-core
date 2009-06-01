package gov.nasa.jpf.mc;

import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.jvm.Verify;

import org.junit.Test;
import org.junit.runner.JUnitCore;

public class TestDataChoiceJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.mc.TestDataChoice";


  public static void main (String[] args) {
    JUnitCore.main("gov.nasa.jpf.mc.TestDataChoiceJPF");
  }


  /**************************** tests **********************************/
  @Test
  public void testIntFromSet () {
    String[] args = { TEST_CLASS,
        "+my_int_from_set.class=gov.nasa.jpf.jvm.choice.IntChoiceFromSet",
        "+my_int_from_set.values=1,2,3,intField,localVar", 
        "testIntFromSet" };
    
    Verify.resetCounter(0);
    noPropertyViolation(args);

    if (Verify.getCounter(0) != 5) {
      fail("wrong number of backtracks");
    }
  }
  
  @Test
  public void testDoubleFromSet () {
    String[] args =  { TEST_CLASS,
        "+my_double_from_set.class=gov.nasa.jpf.jvm.choice.DoubleChoiceFromSet",
        "+my_double_from_set.values=42.0,43.5,doubleField,localVar", 
        "testDoubleFromSet" };

    Verify.resetCounter(0);
    noPropertyViolation(args);

    if (Verify.getCounter(0) != 4) {
      fail("wrong number of backtracks");
    }
  }

  @Test
  public void testTypedObjectChoice () {
    String[] args =  { TEST_CLASS,
        "+my_typed_object.class=gov.nasa.jpf.jvm.choice.TypedObjectChoice",
        "+my_typed_object.type=gov.nasa.jpf.mc.MyType", 
        "testTypedObjectChoice" };

    Verify.resetCounter(0);
    noPropertyViolation(args);

    if (Verify.getCounter(0) != 2) {
      fail("wrong number of backtracks");
    }
    
  }
}
