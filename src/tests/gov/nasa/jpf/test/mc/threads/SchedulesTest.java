package gov.nasa.jpf.test.mc.threads;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;



public class SchedulesTest extends TestJPF {
  
  public static void main (String[] args) {
    runTestsOfThisClass(args);
  }
  
  @Test public void testSleep () {

    if (verifyNoPropertyViolation("+cg.threads.break_all=true",
                                  "+listener=.tools.PathOutputMonitor",
                                  "+pom.all=test/gov/nasa/jpf/test/mc/threads/SchedulesTest-output")) {
      Runnable r = new Runnable() {

        public void run() {
          System.out.println("T started");
          try {
            System.out.println("T sleeping");
            Thread.sleep(100);
          } catch (InterruptedException ix) {
            throw new RuntimeException("unexpected interrupt");
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
}
