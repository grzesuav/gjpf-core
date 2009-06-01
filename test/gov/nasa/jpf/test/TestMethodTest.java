package gov.nasa.jpf.test;
import java.util.List;

import gov.nasa.jpf.Test;

public class TestMethodTest {

  static double EPS= 0.7;
  static final double D = 43.5;
  
  int id; 
  
  static {
    Math.abs(-1); // so that we reference it before calling round()
  }
  
  TestMethodTest () {
    id = 0;
  }
    
  public TestMethodTest (int id){
    this.id = id;
  }
  
  @Test({"(42) == 42",
         "(-1) throws IllegalArgumentException"})
  int foo (int a) {
    if (a < 0){
      throw new IllegalArgumentException("negative values not supported");
    }
    return a+1;
  }
    
  @Test("({'a', 'b', 'c'}) == 3")
  int strili (List<String> list){
    return list.size();
  }
  
  
  @Test("this(4[23]).('bla') matches '4[23]BLA' ")
  String baz (String s){
    return (id + s.toUpperCase());
  }
  
  @Test("(new Double(1.2)) >= 1.0, satisfies NoAlloc")
  long round (Double d){
    
    return Math.round(d);
  }
  
  @Test({"this(2).(42.0) within 40.0,50.0",
         "(D) within 42.8+-EPS",
         "this(2)|this(3).(.[56]e-10) within 0,20"})
  double func (double d) {
    return d + id;
  }
  
  static Object cache;
  
  @Test("(1) satisfies NoAlloc")
  void allo (int d){
    if (d > 0){
      cache = new double[2];
    }
  }
}
