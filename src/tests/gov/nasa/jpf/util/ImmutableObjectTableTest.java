//
// Copyright (C) 2012 United States Government as represented by the
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

import java.util.HashMap;
import java.util.Random;

import org.junit.Test;

import gov.nasa.jpf.util.test.TestJPF;

/**
 * regression test for ImmutableObjectTable
 */
public class ImmutableObjectTableTest extends TestJPF {

  //--- test  
  static class IntegerProcessor implements Processor<Integer>{
    int count=0;
    
    public void process( Integer i){
      if (count++ > 0){
        System.out.print(',');
      }
      System.out.print(i);
    }
    
    public int getCount(){
      return count;
    }
  }

  static <T> void dump (ImmutableObjectTable<T> t, Processor<T> proc){
    System.out.print( "size=");
    System.out.print(t.size());
    System.out.print(", rsh=" + t.rootShift);
    System.out.print(", values={");
    t.process( proc); 
    System.out.println('}');    
  }

  static <T> void dumpInKeyOrder (ImmutableObjectTable<T> t, Processor<T> proc){
    System.out.print( "size=");
    System.out.print(t.size());
    System.out.print(", values={");
    t.processInKeyOrder( proc); 
    System.out.println('}');    
  }

  ImmutableObjectTable<Integer> insert (ImmutableObjectTable<Integer> t, int key){
    System.out.println();
    System.out.printf("--- add %d:\n", key);
    t = t.set( key, key);
    dump(t, new IntegerProcessor());
    t.printOn(System.out);
    System.out.printf("--- find(%d): %s\n", key, t.get(key)); 
    return t;
  }
  
  @Test
  public void testInsert() {
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    
    System.out.println("------------ 0");
    t = insert( t, 0);
    
    
    System.out.println("\n------------ 1, 32, 33");
    t = new ImmutableObjectTable<Integer>();
    t = insert( t, 1);
    t = insert( t, 32);
    t = insert( t, 33);
    assertTrue( t.size() == 3);

    System.out.println("\n------------ 0x18001, 0x18000");
    t = new ImmutableObjectTable<Integer>();
    t = insert( t, 0x18001);
    t = insert( t, 0x18000);
    assertTrue( t.size() == 2);
  }
  

  @Test
  public void testFullNode(){
    
    System.out.println("\n-------------- BitmapNode[0..30]");
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    for (int i=0; i<31; i++){
      t = t.set(i,  new Integer(i));
    }
    dump(t, new IntegerProcessor());
    t.printOn(System.out);
    assertTrue( t.size() == 31);
   
    
    System.out.println("\n-------------- FullNode[0..31]");
    t = t.set(31,31); // that should promote to full node
    assertTrue( t.size() == 32);
    dump(t, new IntegerProcessor());
    t.printOn(System.out);

    System.out.println("\n-------------- set value[31] = -31");
    t = t.set(31, -31);
    assertTrue( t.size() == 32);
    assertTrue( t.get(31) == -31);
    dump(t, new IntegerProcessor());
    t.printOn(System.out);
    
    System.out.println("\n-------------- remove 31");
    t = t.remove(31); // reduce full node again
    dump(t, new IntegerProcessor());
    t.printOn(System.out);
    assertTrue( t.size() == 31);
  }
  
  @Test
  public void testDenseInsert(){
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    
    int max = 100;
    
    for (int i=0; i<max; i++){
      t = t.set(i,i);
    }
    
    IntegerProcessor proc = new IntegerProcessor();
    dump(t, proc);
    
    assertTrue( t.size() == max);
    assertTrue( proc.getCount() == t.size());
    
    for (int i=0; i<max; i++){
      assertTrue( t.get(i) == i);
    }
    
    t.printOn(System.out);
    
    assertTrue( t.get(max+1) == null);
  }

  @Test
  public void testRemove(){
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    
    t = t.set(42,42);
    dump(t, new IntegerProcessor());
    t.printOn(System.out);

    assertTrue( t.size() == 1);
    ImmutableObjectTable<Integer> tt = t.remove(12345); // should not remove anything
    assertTrue( t == tt);

    System.out.println("\n--------- remove 42");
    t = t.remove(42);
    dump(t, new IntegerProcessor());
    t.printOn(System.out);
    assertTrue( t.size() == 0);    
  }
  
  @Test
  public void testDenseRemove(){
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    
    for (int i=0; i<42; i++){
      t = t.set(i,i);
    }    
    dump(t, new IntegerProcessor());
    assertTrue( t.size == 42);
    
    for (int i=0; i<42; i++){
      t = t.remove(i);
    }
    dump(t, new IntegerProcessor());
    assertTrue( t.size() == 0);    
  }

  @Test
  public void testInsertRemoveInsert(){
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    
    t = t.set(42,42);
    dump(t, new IntegerProcessor());
    assertTrue( t.size() == 1);
  
    t = t.remove(42);
    dump(t, new IntegerProcessor());
    assertTrue( t.size() == 0);

    t = t.set(0,0);
    dump(t, new IntegerProcessor());
    assertTrue( t.size() == 1);
    assertTrue( t.get(0) == 0);
  }
      
  @Test
  public void testLargeTable() {
    long t1, t2;
    int N = 20000; // table size
    int M = 5000000; // lookup

    //--- create
    System.out.println("-------- creating table with " + N + " entries");
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    for (int i=0; i<N; i++){
      t = t.set(i,  new Integer(i));
    }
    t2 = System.currentTimeMillis();
    System.out.println("creation: " + (t2 - t1));
    assertTrue(t.size() == N);

    System.out.println("-------- retrieving each entry");
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    for (int i=0; i<N; i++){
      Object v = t.get(i);
      assertTrue( v != null);
    }
    t2 = System.currentTimeMillis();
    System.out.println("lookup: " + (t2 - t1));
    
    System.out.println("-------- removing each entry");
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    for (int i=0; i<N; i++){
       t = t.remove(i);
       assertTrue( t.size() == (N-i-1));
    }
    t2 = System.currentTimeMillis();
    System.out.println("remove: " + (t2 - t1));
  }
  
  static class EvenPredicate implements Predicate<Integer>{
    public boolean isTrue (Integer i){
      return (i % 2) == 0;
    }
  }
  
  static class IntervalPredicate implements Predicate<Integer>{
    int min, max;
    
    public IntervalPredicate (int min, int max){
      this.min = min;
      this.max = max;
    }
    
    public boolean isTrue (Integer i) {
      return (min <= i) && (max >= i);
    }
  }
  
  @Test
  public void testBlockRemove(){
    int N = 150;
    
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();    
    for (int i=0; i<N; i++){
      t = t.set(i,i);
    }
    ImmutableObjectTable<Integer> t0 = t;

    System.out.println("original table");
    dumpInKeyOrder(t, new IntegerProcessor());

    System.out.println("\nremoving all even");
    t = t.removeAllSatisfying(new EvenPredicate());
    dumpInKeyOrder(t, new IntegerProcessor());

    int min = 10, max = 140;
    System.out.printf("\nremoving [%d..%d] from original table\n", min, max);
    t = t0.removeAllSatisfying(new IntervalPredicate(min, max));
    dumpInKeyOrder(t, new IntegerProcessor());
  }
  
  public void blockRemoveBenchmark() {
    int N = 500;
    int M = 100000;
    long t1, t2;
    
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();    
    for (int i=0; i<N; i++){
      t = t.set(i,i);
    }
    ImmutableObjectTable<Integer> t0 = t;

    System.out.println("original table");
    //dumpInKeyOrder(t, new IntegerProcessor());

    int min = 50;
    int max = 300;
    
    Runtime.getRuntime().gc();
    Predicate<Integer> pred = new IntervalPredicate(min, max);
    t1 = System.currentTimeMillis();
    for (int j=0; j<M; j++) {
      t = t0.removeAllSatisfying(pred); 
    }
    t2 = System.currentTimeMillis();
    System.out.printf("block remove of %d = %d\n", t.getSizeChange(), (t2 - t1));
    
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    for (int j=0; j<M; j++) {
      t = t0;      
      for (int i=min; i<=max; i++) {
        t = t.remove(i);
      }
    }
    t2 = System.currentTimeMillis();
    System.out.printf("explicit remove of %d = %d\n", (max-min+1), (t2 - t1));
    
    
    //--- HashMap
    HashMap<Integer,Integer> h0 = new HashMap<Integer,Integer>();
    for (int i=0; i<N; i++){
      h0.put(i, i);
    }

    long td = 0, t3, t4;
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    for (int j=0; j<M; j++) {
      HashMap<Integer,Integer> h = (HashMap<Integer,Integer>)h0.clone();
      t3 = System.currentTimeMillis();
      for (int i=min; i<=max; i++) {
        h.remove(i);
      }
      t4 = System.currentTimeMillis();
      td += (t4-t3);
    }
    t2 = System.currentTimeMillis();
    System.out.printf("HashMap remove of %d = %d (%d with restore)\n", (max-min+1), td, (t2 - t1));
    
    
  }
  
  //@Test
  public void benchmark (){
    long t1, t2;
    int N = 20000; // table size
    int M = 5000000; // lookup

    //--- create
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    for (int i=0; i<N; i++){
      t = t.set(i,  new Integer(i));
    }
    t2 = System.currentTimeMillis();
    System.out.println("table creation: " + (t2 - t1));

    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    HashMap<Integer,Integer> h = new HashMap<Integer,Integer>();
    for (int i=0; i<N; i++){
      h.put(i, i);
    }
    t2 = System.currentTimeMillis();
    System.out.println("HashMap creation: " + (t2 - t1));

    //--- lookup
    Random r = new Random(42);
    t1 = System.currentTimeMillis();
    for (int i=0; i<M; i++){
      int k = r.nextInt(N);
      t.get(k);
    }
    t2 = System.currentTimeMillis();
    System.out.println("table lookup: " + (t2 - t1));
    
    r = new Random(42);
    t1 = System.currentTimeMillis();
    for (int i=0; i<M; i++){
      int k = r.nextInt(N);
      h.get(k);
    }
    t2 = System.currentTimeMillis();
    System.out.println("HashMap lookup: " + (t2 - t1));

    //--- remove
    t1 = System.currentTimeMillis();
    for (int i=0; i<N; i++){
      t = t.remove(i);
    }
    t2 = System.currentTimeMillis();
    System.out.println("table remove: " + (t2 - t1));
    
    t1 = System.currentTimeMillis();
    for (int i=0; i<N; i++){
      h.remove(i);
    }
    t2 = System.currentTimeMillis();
    System.out.println("HashMap remove: " + (t2 - t1));
  }

}
