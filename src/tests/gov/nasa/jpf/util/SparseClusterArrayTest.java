//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.util;

import org.junit.Test;
import gov.nasa.jpf.util.test.TestJPF;
import java.util.HashMap;
import java.util.Random;
import static gov.nasa.jpf.util.SparseClusterArray.*;

/**
 * unit test for gov.nasa.jpf.util.SparseClusterArray
 */
public class SparseClusterArrayTest extends TestJPF {

  public static void main (String[] args){

    // our performance evals
    if (args.length == 1){
      String mthName = args[0];
      if (mthName.equals("evalHashMap")){
        evalHashMap();
        return;
      } else if (mthName.equals("evalSparseClusterArray")){
        evalSparseClusterArray();
        return;
      }
    }

    // the regression tests
    runTestsOfThisClass(args);
  }


  @Test
  public void testBasic() {
    SparseClusterArray<Object> arr = new SparseClusterArray<Object>();
    Object v;
    int ref;

    ref = (1 << S1) | 42;
    arr.set(ref, (v = new Integer(ref)));

    Object o = arr.get(ref);
    System.out.println(o);
    assert o.equals(v);

    ref = (2 << S1);
    arr.set(ref, new Integer(ref));

    System.out.println("cardinality = " + arr.cardinality());
    assert arr.cardinality() == 2;

    for (Object e : arr) {
      System.out.println(e);
    }
  }

  @Test
  public void testNextNull () {
    Object e = new Integer(42);
    SparseClusterArray<Object> arr = new SparseClusterArray<Object>();
    int k;
    int limit = 10000000;

    arr.set(0, e);
    k = arr.firstNullIndex(0, limit);
    System.out.println("k=" + k);  // 1
    assert k == 1;

    arr.set(0,null);
    k = arr.firstNullIndex(0, limit);
    System.out.println("k=" + k);  // 0
    assert k == 0;

    arr.set(511, 511);

    int i=0;
    for (;i<512; i++) {
      arr.set(i, e);
    }
    System.out.println(arr.get(511));
    System.out.println(arr.get(512));
    k = arr.firstNullIndex(0, limit);
    assert k == 512;
    
    long t1 = System.currentTimeMillis();
    for (int j=0; j<100000; j++) {
      k = arr.firstNullIndex(0, limit);
    }
    long t2 = System.currentTimeMillis();
    System.out.println("k=" + k + ", 100000 lookups in: " + (t2 - t1)); // 512

    for (;i<2048;i++) {
      arr.set(i, e);
    }
    k = arr.firstNullIndex(0, limit);
    System.out.println("k=" + k);  // 2048 (no chunk)
    assert k == 2048;

    k = arr.firstNullIndex(0, 2048);
    System.out.println("k=" + k); // -1
    assert k == -1;

    arr.set(2048, e);
    arr.set(2048,null);
    k = arr.firstNullIndex(0, limit);
    System.out.println("k=" + k);  // 2048 (new chunk)
    assert k == 2048;

    for (; i<2500; i++) {
      arr.set(i, e);
    }
    k = arr.firstNullIndex(0, limit);
    System.out.println("k=" + k);  // 2500
    assert k == 2500;
  }

  @Test
  public void testClone() {
    SparseClusterArray<Integer> arr = new SparseClusterArray<Integer>();

    arr.set(0, new Integer(0));
    arr.set(42, new Integer(42));
    arr.set(6762, new Integer(6762));
    arr.set(6762, null);

    Cloner<Integer> cloner = new Cloner<Integer>() {
      public Integer clone (Integer other) {
        return new Integer(other);
      }
    };
    SparseClusterArray<Integer> newArr = arr.deepCopy(cloner);
    for (Integer i : newArr) {
      System.out.println(i);
    }

    assert newArr.cardinality() == 2;
    assert newArr.get(0) == 0;
    assert newArr.get(42) == 42;
    assert newArr.get(6762) == null;
  }

  @Test
  public void testSnapshot() {
    SparseClusterArray<Integer> arr = new SparseClusterArray<Integer>();

    arr.set(0, new Integer(0));
    arr.set(42, new Integer(42));
    arr.set(4095, new Integer(4095));
    arr.set(4096, new Integer(4096));
    arr.set(7777, new Integer(7777));
    arr.set(67620, new Integer(67620));
    arr.set(67620, null);
    arr.set(7162827, new Integer(7162827));

    Transformer<Integer,String> transformer = new Transformer<Integer,String>() {
      public String transform (Integer n) {
        return n.toString();
      }
      public Integer restore (String s) {
        return new Integer( Integer.parseInt(s));
      }
    };

    Snapshot<Integer,String> snap = arr.getSnapshot(transformer);
    for (Entry<String> e = snap.first; e != null; e = e.next) {
      System.out.println("a[" + e.index + "] = " + e.value);
    }

    arr.set(42,null);
    arr.set(87, new Integer(87));
    arr.set(7162827, new Integer(-1));

    arr.restoreSnapshot(snap, transformer);
    for (Integer i : arr) {
      System.out.println(i);
    }

    assert arr.cardinality() == 6;
    assert arr.get(0) == 0;
    assert arr.get(42) == 42;
    assert arr.get(4095) == 4095;
    assert arr.get(4096) == 4096;
    assert arr.get(7777) == 7777;
    assert arr.get(7162827) == 7162827;
  }

  @Test
  public void testChanges() {
    SparseClusterArray<Integer> arr = new SparseClusterArray<Integer>();

    arr.set(42, new Integer(42));
    arr.set(6276, new Integer(6276));

    arr.trackChanges();

    arr.set(0, new Integer(0));
    arr.set(42, new Integer(-1));
    arr.set(4095, new Integer(4095));
    arr.set(4096, new Integer(4096));
    arr.set(7777, new Integer(7777));
    arr.set(7162827, new Integer(7162827));

    Entry<Integer> changes = arr.getChanges();
    arr.revertChanges(changes);

    for (Integer i : arr) {
      System.out.println(i);
    }

    assert arr.cardinality() == 2;
    assert arr.get(42) == 42;
    assert arr.get(6276) == 6276;
  }


   //--- the performance sectopm

  final static int MAX_ROUNDS = 1000;
  final static int MAX_N = 10000;
  final static int MAX_T = 8;


  static void evalSparseClusterArray() {
    Random random = new Random(0);
    Object elem = new Object();
    long t1, t2;
    int n = 0;

    t1 = System.currentTimeMillis();
    SparseClusterArray<Object> arr = new SparseClusterArray<Object>();

    for (int i=0; i<MAX_ROUNDS; i++) {
      int seg = random.nextInt(MAX_T) << S1;
      for (int j=0; j<MAX_N; j++) {
        int ref = seg | random.nextInt(MAX_N);
        //ref |= j;
        arr.set(ref, elem);
        if (arr.get(ref) == null) throw new RuntimeException("element not set: " + i);
      }
    }
    t2 = System.currentTimeMillis();
    System.out.println("SparseArray random write/read of " + arr.cardinality() + " elements: "+ (t2 - t1));

    n=0;
    t1 = System.currentTimeMillis();
    for (Object e : arr) {
      n++;
    }
    t2 = System.currentTimeMillis();
    System.out.println("SparseArray iteration over " + n + " elements: " + (t2 - t1));
  }

  static void evalHashMap() {
    Random random = new Random(0);
    Object elem = new Object();
    long t1, t2;

    t1 = System.currentTimeMillis();
    HashMap<Integer,Object> arr = new HashMap<Integer,Object>();

    for (int i=0; i<MAX_ROUNDS; i++) {
      int seg = random.nextInt(MAX_T) << S1;
      for (int j=0; j<MAX_N; j++) {
        int ref = seg | random.nextInt(MAX_N);
        //ref |= j;
        arr.put(ref, elem);
        if (arr.get(ref) == null) throw new RuntimeException("element not set: " + i);
      }
    }
    t2 = System.currentTimeMillis();
    System.out.println("HashMap random write/read of " + arr.size() + " elements: " + (t2 - t1));

    int n=0;
    t1 = System.currentTimeMillis();
    for (Object e : arr.values()) {
      n++;
    }
    t2 = System.currentTimeMillis();
    System.out.println("HashMap iteration over " + n + " elements: " + (t2 - t1));
  }
}
