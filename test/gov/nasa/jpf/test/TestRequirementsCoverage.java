package gov.nasa.jpf.test;

import gov.nasa.jpf.Requirement;

public class TestRequirementsCoverage {

  static class SomeOtherClass {
    
    @Requirement("1.1.1")
    double computeValue() {
      return 42.0;
    }
  }
  
  @Requirement({"1.1.1", "1.2.7"})
  public double doSomething (double d){
    if (d > 42.0){
      return 42.0;
    } else {
      return -42.0;
    }
  }
  
  @Requirement("1.2.7")
  int foo (int a, int b) {
    if (a > b) {
      return 1;
    } else {
      return -1;
    }
  }
  
  @Requirement("1.1.2") // not covered
  int doWhatever (boolean b) {
    if (b) {
      return 42;
    } else {
      return -42;
    }
  }
  
  public void testDoSomething() {
    doSomething(1.1);
    
    SomeOtherClass o = new SomeOtherClass();
    o.computeValue();
  }
  
  public static void main (String[] args){
    TestRequirementsCoverage t = new TestRequirementsCoverage();
    
    t.testDoSomething();
  }
}
