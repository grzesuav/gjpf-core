package gov.nasa.jpf.jvm.abstraction.symmetry;


public class JoinableThreads extends Threads {
  private boolean started = false;
  private int aliveCount = 0;
  private boolean allCompleted = false;
  
  public JoinableThreads(Runnable[] targets) {
    super(targets);
  }
  
  public JoinableThreads(int count, Runnable singleTarget) {
    super(count,singleTarget);
  }
  
  private class JoinableWorker extends Threads.Worker {
    public JoinableWorker(Runnable target) {
      super(target);
    }
    
    public void run() {
      synchronized (JoinableThreads.this) {
        // barrier no-op
      }
      try {
        target.run();
      } finally {
        synchronized (JoinableThreads.this) {
          aliveCount--;
          if (aliveCount == 0) {
            allCompleted = true;
            postCompletion();
            JoinableThreads.this.notifyAll();
          }
        }
      }
    }
  }
  
  void doStartAll() {
    if (started) throw new IllegalStateException();
    preStart();
    started = true;
    aliveCount = count;
    for (int i = 0; i < count; i++) {
      new JoinableWorker(targets[i]).start();
    }
    postStart();
    // threads can start after this return
  }
  
  protected synchronized void postCompletion() { }
  
  public final void joinAll() throws InterruptedException {
    synchronized (this) {
      while (!allCompleted) {
        this.wait();
      }
    }
  }

  public final void joinAllUninterrupted() {
    synchronized (this) {
      while (!allCompleted) {
        try {
          this.wait();
        } catch (InterruptedException e) { }
      }
    }
  }
}
