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
package gov.nasa.jpf.vm;

/**
 * test case for the shared object attribute detection, which is required by POR
 * NOTE: these test cases only make sense when executed under JPF, since they
 * depend on race conditions that are most likely not experienced when running on
 * a normal VM
 */
public class TestSharedAttr implements Runnable {
  
  static class SharedOrNot {
    boolean changed;
  }
  
  SharedOrNot o;
  
  public TestSharedAttr (SharedOrNot o) {
    this.o = o;
  }
  
  public void run () {
    boolean b = o.changed;
    o.changed = !b;
    assert o.changed != b : "Argh, somebody changed o under our feet";
  }
  
  /**
   * this on should produce an AssertionError under JPF
   */
  public static void testShared () {
    SharedOrNot s = new SharedOrNot();
    
    Thread t1 = new Thread( new TestSharedAttr(s));
    Thread t2 = new Thread( new TestSharedAttr(s));
    
    t1.start();
    t2.start();
  }
  
  /**
   * and this one shouldn't
   */
  public static void testNonShared () {
    SharedOrNot s = new SharedOrNot();
    Thread t1 = new Thread( new TestSharedAttr(s));
    
    s = new SharedOrNot();
    Thread t2 = new Thread( new TestSharedAttr(s));
    
    t1.start();
    t2.start();
  }

  static TestSharedAttr rStatic = new TestSharedAttr( new SharedOrNot());
  
  public static void testSharedStaticRoot () {
    Thread t = new Thread( rStatic);
    
    t.start();
    
    rStatic.o.changed = false; // why wouldn't 'true' trigger an assertion :)
  }
  
  public static void main (String[] args) {

    if (args.length > 0) {
      // just run the specified tests
      for (int i = 0; i < args.length; i++) {
        String func = args[i];

        // note that we don't use reflection here because this would
        // blow up execution/test scope under JPF
        if ("testShared".equals(func)) { testShared(); }
        else if ( "testNonShared".equals(func)) { testNonShared(); }
        else if ( "testSharedStaticRoot".equals(func)) { testSharedStaticRoot(); }
        else {
          throw new IllegalArgumentException("unknown test function");
        }
      }
    } else {
      testShared();
      testNonShared();
      testSharedStaticRoot();
    }
  }

}
