package gov.nasa.jpf.test.annotation;

import gov.nasa.jpf.test.*;
import gov.nasa.jpf.NonShared;
import gov.nasa.jpf.JPFConfig;
import gov.nasa.jpf.jvm.Verify;

import java.util.Random;

@NonShared
class VeryPrivate {
  void foo() {
    System.out.print("### VeryPrivate.foo() called from thread: ");
    System.out.println(Thread.currentThread());
  }
}


// set JPF properties declaratively 
//@JPFConfig ({"jpf.listener+=.tools.SharedChecker", "cg.enumerate_random=true"})
public class TestNonShared implements Runnable {
  
  int ri;
  VeryPrivate vp;
  
  public void run() {
    if (ri == 2){
      vp.foo();
    }
  }
  
  public static void main (String[] args){
    Random rand = new Random();
    
    // set JPF properties programmatically
    Verify.setProperties("jpf.listener+=.tools.SharedChecker", "cg.enumerate_random=true");
    
    TestNonShared r = new TestNonShared();
    Thread t = new Thread(r);
    
    r.ri = rand.nextInt(3); // add some backtracking here
    System.out.println("### round: " + r.ri);
    r.vp = new VeryPrivate();
    
    t.start();
  }

}
