package gov.nasa.jpf.vm;

import gov.nasa.jpf.util.test.RawTest;

public class EnumTest extends RawTest {

  //--- helper type
  enum A {
    ELEM;
  }

  
  public static void main (String[] selectedMethods) {
    runTests( new EnumTest(), selectedMethods);
  }

  public void testValueOf () {
    assert A.valueOf("ELEM") == A.ELEM;
  }

}

