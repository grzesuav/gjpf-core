package gov.nasa.jpf.mc;

import gov.nasa.jpf.util.test.RawTest;

public class TestSchedules extends RawTest {
  
  public static void main (String[] args) {
    TestSchedules t = new TestSchedules();

    if (!runSelectedTest(args, t)){
      runAllTests(args,t);
    }
  }
  
  public void testSleep () {
    Runnable r = new Runnable() {
      public void run () {
        System.out.println("T started");
        try {
          System.out.println("T sleeping");
          Thread.sleep(100);
        } catch (InterruptedException ix) {
          throw new RuntimeException ("unexpected interrupt");
        }
        System.out.println("T finished");
      }
    };

    Thread t = new Thread(r);
    System.out.println("main starting T");
    t.start();
    
    System.out.println("main finished");
  }
  
}
