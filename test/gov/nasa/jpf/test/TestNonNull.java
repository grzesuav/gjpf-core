package gov.nasa.jpf.test;

import gov.nasa.jpf.NonNull;

public class TestNonNull {

  public static void main (String[] args) {
    TestNonNull o = new TestNonNull();
    
    o.foo();  // @NonNull return value violated
    
    //new T1(); // @NonNull instance field w/o ctor
    //new T2();   // has ctor, but doesn't init the @NonNull field
    //new T3().dontDoThis(); // inits instance field, but then nullifies
    
    //new T4(); // uninitialized static field w/o clinit
    //new T5(); // uninitialized static field with clinit
    //T6.dontDoThis(); // inits static field, but then nullifies
  }
  
  void foo () {
    Object ret = computeSomething(42);
    System.out.print("computeSomething(42) returned ");
    System.out.println(ret);
    
    ret = computeSomething(0);
    System.out.print("computeSomething(0) returned ");
    System.out.println(ret);
  }
  
  @NonNull
  Integer computeSomething(int a) {
    if (a == 0) {
      return null;
    } else {
      return a;
    }
  }

  static class T1 {
    @NonNull
    String s;
    // no ctor
  }
  
  static class T2 {
    @NonNull
    String s;
    
    T2() {}
  }
  
  static class T3 {
    @NonNull
    String s = "blah";
    void dontDoThis() { s = null; }
  }
  
  static class T4 {
    @NonNull
    static String s;
  }
  
  static class T5 {
    @NonNull
    static String s; 
    static { s = null; }
  }
  
  static class T6 {
    @NonNull
    static String s = "blah";
    static void dontDoThis() { s = null; }
  }

}
