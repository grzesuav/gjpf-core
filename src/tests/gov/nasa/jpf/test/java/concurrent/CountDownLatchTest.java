package gov.nasa.jpf.test.java.concurrent;

import gov.nasa.jpf.util.test.*;
import java.util.concurrent.*;
import org.junit.Test;

// <2do> there seems to be some rather nasty state explosion - revisit
// once we model java.util.concurrent
public class CountDownLatchTest extends TestJPF {

  private static final int N = 2;  // <2do> bump up once we model java.util.concurrent

  private final CountDownLatch    latch     = new CountDownLatch(N);
  private final Exchanger<Object> exchanger = new Exchanger<Object>();
  private final ExecutorService   service   = Executors.newFixedThreadPool(N);

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  @Test
  public void testCountDown() throws InterruptedException
  {
    if (verifyNoPropertyViolation("+vm.time.model=ConstantZero")) {

      Runnable task = new Runnable() {
        public void run() {
          base();
        }
      };       

      for (int i = 0; i < N; i++)
        service.execute(task);
      
      latch.await();
      service.shutdown();
    }
  }
  
  private void base()
   {
    try {
      Object source = new Object();
      Object result = exchanger.exchange(source, 1L, TimeUnit.SECONDS);
      assert source != result : "source != result";
      assert result != null : "result != null";
      latch.countDown();
    } catch (TimeoutException e) {
      latch.countDown();
    } catch (Exception e) {
      throw new Error(e);
    }
  }
}
