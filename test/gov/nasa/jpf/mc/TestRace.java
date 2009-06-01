//
// Copyright (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
/**
 * this is a raw test class for detection of thread-shared fields, i.e.
 * it executes the garbage collection based reachability analysis
 */

package gov.nasa.jpf.mc;

import java.lang.reflect.InvocationTargetException;
import gov.nasa.jpf.util.test.RawTest;

class SharedObject {
  int instanceField;
  int whatEver;
}


public class TestRace extends RawTest {

  public static void main (String[] args) throws InvocationTargetException {
    TestRace t = new TestRace();
    if (!runSelectedTest(args, t)){
      t.testInstanceRace();
    }
  }

  static int staticField;

  public void testStaticRace () {
    Runnable r1 = new Runnable() {
      public void run() {
        staticField = 1;
        if (staticField != 1) {
          throw new RuntimeException("r1 detected race!");
        }
      }
    };

    Runnable r2 = new Runnable() {
      public void run() {
        staticField = 0;
        if (staticField != 0) {
          throw new RuntimeException("r2 detected race!");
        }
      }
    };

    Thread t1 = new Thread(r1);
    Thread t2 = new Thread(r2);

    t1.start();
    t2.start();
  }

  public void testStaticRaceNoThrow () {
    Runnable r1 = new Runnable() {
      public void run() {
        staticField = 1;
      }
    };

    Runnable r2 = new Runnable() {
      public void run() {
        staticField = 0;
      }
    };

    Thread t1 = new Thread(r1);
    Thread t2 = new Thread(r2);

    t1.start();
    t2.start();
  }
  
  
  public void testInstanceRace () {
    final SharedObject o = new SharedObject();

    Runnable r1 = new Runnable() {
      SharedObject d = o;
      public void run() {
        d.instanceField = 1;
        if (d.instanceField != 1) {
          throw new RuntimeException("r1 detected race!");
        }
      }
    };

    Runnable r2 = new Runnable() {
      SharedObject d = o;
      public void run() {
        d.instanceField = 0;
        if (d.instanceField != 0) {
          throw new RuntimeException("r2 detected race!");
        }
      }
    };

    Thread t1 = new Thread(r1);
    Thread t2 = new Thread(r2);

    t1.start();
    t2.start();
  }

  public void testInstanceRaceNoThrow () {
    final SharedObject o = new SharedObject();

    Runnable r1 = new Runnable() {
      SharedObject d = o;
      public void run() {
        d.instanceField = 1;
      }
    };

    Runnable r2 = new Runnable() {
      SharedObject d = o;
      public void run() {
        d.instanceField = 0;
      }
    };

    Thread t1 = new Thread(r1);
    Thread t2 = new Thread(r2);

    t1.start();
    t2.start();
  }
  
  
  //--- these are tests to check false positives

  static class SameInsnRunnable implements Runnable {
    SharedObject o = new SharedObject();

    public void run () {
      o.instanceField = 42;  // same insn, different 'o', no race
    }
  }

  public void testSameInsnOtherObject () {
    SameInsnRunnable r1 = new SameInsnRunnable();
    SameInsnRunnable r2 = new SameInsnRunnable();

    Thread t = new Thread(r1);
    t.start();

    r2.run();
  }

  public void testSameObjectOtherField() {
    final SharedObject o = new SharedObject();

    Runnable r = new Runnable() {
      public void run () {
        o.instanceField = 42;
      }
    };

    Thread t = new Thread(r);

    o.whatEver = -42;  // different field, no race
  }
  
  
  //--- try variations of locks
  
  class AnotherSharedObject {
    Object lock1 = new Object();
    Object lock2 = new Object();
    
    int x;
  }
  
  public void testNoSync() {
    final AnotherSharedObject o = new AnotherSharedObject();
    Runnable r = new Runnable() {
      public void run () {
        o.x++;
        if (o.x == 0) {
          throw new RuntimeException("testNoSync race");
        }
      }
    };
    Thread t = new Thread(r);
    t.start();
    
    o.x--;
  }
  
  
  public void testTSync() {
    final AnotherSharedObject o = new AnotherSharedObject();
    Runnable r = new Runnable() {
      public void run () {
        synchronized(o.lock1) {
          o.x++;
          if (o.x == 0) {
            throw new RuntimeException("testT1Sync race");
          }
        }
      }
    };
    Thread t = new Thread(r);
    t.start();
    
    // no sync
    o.x--;
  }
  
  public void testMainSync () {
    final AnotherSharedObject o = new AnotherSharedObject();
    Runnable r = new Runnable() {
      public void run () {
        // not synchronized
        o.x++;
        if (o.x == 0) {
          throw new RuntimeException("testMainSync race");
        }
      }
    };
    Thread t = new Thread(r);
    t.start();
    
    synchronized(o.lock1) {
      o.x--;
    }
  }
  
  public void testBothSync () {
    final AnotherSharedObject o = new AnotherSharedObject();
    Runnable r = new Runnable() {
      public void run () {
        synchronized(o.lock1) {
          o.x++;
          if (o.x == 0) {
            throw new RuntimeException("testBothSync race??");
          }
        }
      }
    };
    Thread t = new Thread(r);
    t.start();
    
    synchronized(o.lock1) {
      o.x--;
    }
  }

  public void testWrongSync () {
    final AnotherSharedObject o = new AnotherSharedObject();
    
    Runnable r = new Runnable() {
      public void run () {
        synchronized(o.lock1) {
          o.x++;
          if (o.x == 0) {
            throw new RuntimeException("testWrongSync race");
          }
        }
      }
    };
    Thread t = new Thread(r);
    t.start();
    
    synchronized(o.lock2) {
      o.x--;
    }
  }
  
  
}


