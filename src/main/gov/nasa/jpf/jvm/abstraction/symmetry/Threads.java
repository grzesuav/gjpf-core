package gov.nasa.jpf.jvm.abstraction.symmetry;

import java.util.Arrays;

public class Threads {
  public final Runnable[] targets; 
  protected final int count;
  
  private boolean started = false;

  public Threads(Runnable[] targets) {
    this.targets = targets;
    this.count = targets.length;
  }
  
  public Threads(int count, Runnable singleTarget) {
    this.count = count;
    this.targets = new Runnable[count];
    Arrays.fill(this.targets, singleTarget);
  }
  
  class Worker extends Thread {
    Runnable target;
    
    public Worker(Runnable target) {
      this.target = target;
    }
    
    public void run() {
      synchronized (Threads.this) {
        // barrier no-op
      }
      target.run();
    }
  }
  
  public final void startAll() {
    doStartAll();
  }
  
  synchronized void doStartAll() {
    if (started) throw new IllegalStateException();
    preStart();
    started = true;
    for (int i = 0; i < count; i++) {
      new Worker(targets[i]).start();
    }
    postStart();
    // threads can start after this return
  }
  
  protected synchronized void preStart() { }
  
  protected synchronized void postStart() { }
}

/* better version?:  -pcd

public class Threads {
public final Runnable[] targets; 
protected final int count;

protected int started = 0;

public Threads(Runnable[] targets) {
  this.targets = targets;
  this.count = targets.length;
}

public Threads(int count, Runnable singleTarget) {
  this.count = count;
  this.targets = new Runnable[count];
  Arrays.fill(this.targets, singleTarget);
}

class Worker extends Thread {
  Runnable target;
  
  public Worker(Runnable target) {
    this.target = target;
  }
  
  public void run() {
    synchronized (Threads.this) {
      started++;
      Threads.this.notifyAll();
      while (started < count) {
        try {
          Threads.this.wait();
        } catch (InterruptedException e) { }
      }
    }
    target.run();
  }
}

public final void startAll() {
  doStartAll();
}

synchronized void doStartAll() {
  if (started > 0) throw new IllegalStateException();
  preStart();
  for (int i = 0; i < count; i++) {
    new Worker(targets[i]).start();
    while (started < i + 1) {
      try {
        wait();
      } catch (InterruptedException e) { }
    }
  }
  postStart();
  // threads can start after this return
}

protected synchronized void preStart() { }

protected synchronized void postStart() { }
} */