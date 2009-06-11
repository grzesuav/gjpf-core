package gov.nasa.jpf.test.annotation;

import gov.nasa.jpf.Const;

public class TestConst {

  static int s = 42000;
  
  int d;
  
  @Const
  static int doThis() {
    return s;
  }
  
  @Const
  public void dontDoThis() {
    foo();
  }
  
  void foo () {
    d = 42;
  }
  
  void doWhatever() {
    d+= 10;
  }
  
  @Const
  int doThat() {
    return d + 1;
  }
  
  public static void main (String[] args) {
    TestConst t = new TestConst();
    
    doThis(); // Ok, static const
    t.doThat(); // Ok, instance const
    t.doWhatever(); // Ok, non-const write
    t.dontDoThis(); // BANG - (indirect) write from const
  }
}
