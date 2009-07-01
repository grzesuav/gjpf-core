package gov.nasa.jpf.test.java.concurrent;

import gov.nasa.jpf.util.test.TestJPF;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

// <2do> there seems to be some rather nasty state explosion - revisit
// once we model java.util.concurrent
public class CountDownLatchTest extends TestJPF {

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  @Test
  public void testCountDown() {
    if (verifyNoPropertyViolation()) {

      final int n = 2; // <2do> bump up once we model java.util.concurrent

      final CountDownLatch done = new CountDownLatch(n);
      final Exchanger<Object> exchanger = new Exchanger<Object>();
      final ExecutorService es = Executors.newFixedThreadPool(3);
      for (int i = 0; i < n; i++) {
        es.submit(new Runnable() {

          public void run() {
            try {
              exchanger.exchange(new Object(), 1L, TimeUnit.SECONDS);
              done.countDown();
            } catch (Throwable e) {
              throw new Error(e);
            }
          }
        });
      }
      try {
        done.await();
        es.shutdown();
      } catch (InterruptedException ix) {
      }
    }
  }
}
