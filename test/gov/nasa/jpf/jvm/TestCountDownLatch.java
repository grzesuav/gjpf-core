package gov.nasa.jpf.jvm;

import gov.nasa.jpf.util.test.RawTest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestCountDownLatch extends RawTest {

  public static void main (String[] args) {
    TestCountDownLatch t = new TestCountDownLatch();

    if (!runSelectedTest(args, t)){
      runAllTests(args,t);
    }
  }
  
  public void testCountDown () {
    final int n = 4;
    final CountDownLatch done = new CountDownLatch(n);
    final Exchanger<Object> exchanger = new Exchanger<Object>();
    final ExecutorService es = Executors.newFixedThreadPool(3);
    for (int i = 0; i < n; i++){
        es.submit(new Runnable() { public void run() {
            try {
                exchanger.exchange(new Object(), 1L, TimeUnit.SECONDS);
                done.countDown();
            } catch (Throwable e) { throw new Error(e); }}});
    }
    try {
      done.await();
      es.shutdown();
    } catch (InterruptedException ix) {}
  }
}
