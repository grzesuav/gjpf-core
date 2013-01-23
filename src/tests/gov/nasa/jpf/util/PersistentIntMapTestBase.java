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
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import gov.nasa.jpf.util.PersistentIntMap.FullNode;
import gov.nasa.jpf.util.PersistentIntMap.BitmapNode;
import gov.nasa.jpf.util.PersistentIntMap.OneNode;
import gov.nasa.jpf.util.test.TestJPF;

/**
 * regression test base for concrete PersistentIntMap classes 
 */
public abstract class PersistentIntMapTestBase extends TestJPF {

  
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
  
  protected abstract PersistentIntMap<Integer> createPersistentIntMap();

  protected void dump (PersistentIntMap<Integer> t, Processor<Integer> proc){
    System.out.print( "size=");
    System.out.print(t.size());
    if (t instanceof PersistentMsbIntMap){
      System.out.print(", rsh=" + ((PersistentMsbIntMap)t).rootShift);
    }
    System.out.print(", values={");
    t.process( proc); 
    System.out.println('}');    
  }

  PersistentIntMap<Integer> insert (PersistentIntMap<Integer> t, int key){
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
    PersistentIntMap<Integer> t;

    System.out.println("------------ 0");
    t = createPersistentIntMap();
    t = insert( t, 0);
        
    System.out.println("\n------------ 1, 32, 33");
    t = createPersistentIntMap();
    t = insert( t, 1);
    t = insert( t, 32);
    t = insert( t, 33);
    assertTrue( t.size() == 3);

    System.out.println("\n------------ 0x18001, 0x18000");
    t = createPersistentIntMap();
    t = insert( t, 0x18001);
    t = insert( t, 0x18000);
    assertTrue( t.size() == 2);
  }
  
  @Test
  public void testNodeValueInsert() {
    PersistentIntMap<Integer> t = createPersistentIntMap();
    for (int i=0; i<33; i++) {
      t = t.set( i, i);
      assertTrue( t.size() == i+1);
    }
    dump(t, new IntegerProcessor());
    t.printOn(System.out);
  }

  @Test
  public void testFullNode(){
    
    System.out.println("\n-------------- BitmapNode[0..30]");
    PersistentIntMap<Integer> t = createPersistentIntMap();
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
    PersistentIntMap<Integer> t = createPersistentIntMap();
    
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
    PersistentIntMap<Integer> t = createPersistentIntMap();
    
    t = t.set(42,42);
    dump(t, new IntegerProcessor());
    t.printOn(System.out);

    assertTrue( t.size() == 1);
    PersistentIntMap<Integer> tt = t.remove(12345); // should not remove anything
    assertTrue( t == tt);

    System.out.println("\n--------- remove 42");
    t = t.remove(42);
    dump(t, new IntegerProcessor());
    t.printOn(System.out);
    assertTrue( t.size() == 0);    
  }
  
  @Test
  public void testDenseRemove(){
    PersistentIntMap<Integer> t = createPersistentIntMap();
    
    for (int i=0; i<42; i++){
      t = t.set(i,i);
    }    
    dump(t, new IntegerProcessor());
    assertTrue( t.size() == 42);
    
    for (int i=0; i<42; i++){
      t = t.remove(i);
    }
    dump(t, new IntegerProcessor());
    assertTrue( t.size() == 0);    
  }

  @Test
  public void testInsertRemoveInsert(){
    PersistentIntMap<Integer> t = createPersistentIntMap();
    
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
  public void testIterator() {
    PersistentIntMap<Integer> t = createPersistentIntMap();
    
    //--- empty map test
    System.out.println("check empty map iterator");
    for (Integer v : t) {
      fail("map should be empty");
    }
    
    //--- populate a dense map
    int max = 446; //100;
    for (int i=0; i<max; i++) {
      t = t.set(i, Integer.valueOf(i));
    }

    //--- check for the right number of entries 
    System.out.println("check map with size: " + max);
    int n=0;
    for (Integer v : t) {
      n++;
      if (n > 0) {
        System.out.print(',');
      }
      System.out.print(v);
    }
    System.out.println();
    assertTrue( n == max);
    
  }
  
  @Test
  public void testLargeTable() {
    long t1, t2;
    int N = 40000; // table size

    //--- create
    System.out.println("-------- creating table with " + N + " entries");
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    PersistentIntMap<Integer> t = createPersistentIntMap();
    PersistentIntMap.Result<Integer> res = t.createResult();
    for (int i=0; i<N; i++){
      t = t.set(i,  new Integer(i), res);
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
    
    PersistentIntMap<Integer> t0 = t;
    
    System.out.println("-------- removing each entry");
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    for (int i=0; i<N; i++){
       t = t.remove(i,res);
       assertTrue( t.size() == (N-i-1));
    }
    t2 = System.currentTimeMillis();
    System.out.println("remove: " + (t2 - t1));
    
    System.out.println("-------- block remove of entries");
    Predicate<Integer> pred = new Predicate<Integer>(){
      public boolean isTrue (Integer o) {
        int i = o.intValue();
        return (i > 20000 && i < 30000);
      }
    };
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    t = t0.removeAllSatisfying(pred,res);
    t2 = System.currentTimeMillis();
    System.out.println("block remove of " + Math.abs(res.changeCount) + " values: " + (t2 - t1));
    assertTrue( (t0.size() + res.changeCount) == t.size() );
    
    
    System.out.printf("OneNodes: %d, BitmapNodes: %d, FullNodes: %d\n", OneNode.nNodes, BitmapNode.nNodes, FullNode.nNodes);
  }
  
  public void benchmarkHashMap() {
    long t1, t2;
    int N = 40000; // table size

    //--- create
    System.out.println("-------- creating HashMap with " + N + " entries");
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    HashMap<Integer,Integer> t = new HashMap<Integer,Integer>();
    for (int i=0; i<N; i++){
      t.put(i, i);
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
       
    System.out.println("-------- clone HashMap");
    Runtime.getRuntime().gc();
    HashMap<Integer,Integer> t0 = null;
    t1 = System.currentTimeMillis();
    for (int i=0; i<1000; i++) {
      t0 = (HashMap<Integer,Integer>)t.clone();
    }
    t2 = System.currentTimeMillis();
    System.out.println("cloned 1000 times (corresponds to state storage): " + (t2 - t1));
    
    System.out.println("-------- removing each entry");
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    for (int i=0; i<N; i++){
       t.remove(i);
       assertTrue( t.size() == (N-i-1));
    }
    t2 = System.currentTimeMillis();
    System.out.println("remove: " + (t2 - t1));
    
    System.out.println("-------- block remove of entries");
    
    Predicate<Integer> pred = new Predicate<Integer>(){
      public boolean isTrue (Integer o) {
        int i = o.intValue();
        return (i > 20000 && i < 30000);
      }
    };
    Runtime.getRuntime().gc();
    int nRemoved=0;
    t1 = System.currentTimeMillis();
    for (Iterator<Map.Entry<Integer,Integer>> it = t0.entrySet().iterator(); it.hasNext();) {
      Map.Entry<Integer, Integer> e = it.next();
      if (pred.isTrue(e.getValue())) {
        it.remove();
        nRemoved++;
      }      
    }
    t2 = System.currentTimeMillis();
    System.out.println("block remove of " + nRemoved + " values: " + (t2 - t1));
    
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
    
    PersistentIntMap<Integer> t = createPersistentIntMap();    
    for (int i=0; i<N; i++){
      t = t.set(i,i);
    }
    PersistentIntMap<Integer> t0 = t;

    System.out.println("original table");
    dump(t, new IntegerProcessor());

    System.out.println("\nremoving all even");
    t = t.removeAllSatisfying(new EvenPredicate());
    dump(t, new IntegerProcessor());

    int min = 10, max = 140;
    System.out.printf("\nremoving [%d..%d] from original table\n", min, max);
    t = t0.removeAllSatisfying(new IntervalPredicate(min, max));
    dump(t, new IntegerProcessor());
  }
  

  public void test3457() {
    PersistentIntMap<Integer> t = createPersistentIntMap();
    t = t.set(0, 0);
    t = t.set(3456, 3456);
    t = t.set(3457, 3457);
    t = t.set(3458, 3458);
    System.out.println("3457: " + t.get(3457));

    t.printOn(System.out);
    
    t = t.remove(3456);
    //t = t.remove(3458);
    
    t = t.removeAllSatisfying(new Predicate<Integer>() {
      public boolean isTrue(Integer i) {
        return (i == 3458);
      }
    });
    
    t.printOn(System.out);
    System.out.println("3457: " + t.get(3457));
  }
  

  final static int NSTATES = 20000;
  final static int NOBJECTS = 2000;
  final static int NGC = 400;
  
  
  public void benchmark (){
    long t1, t2;

    //--- PersistentIntMap
    Predicate<Integer> pred = new Predicate<Integer>() {
      public boolean isTrue (Integer o) {
        int i = o.intValue();
        return (i < NGC);
      }
    };
    
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    for (int l=0; l<NSTATES; l++) {
      PersistentIntMap<Integer> t = createPersistentIntMap();
      PersistentIntMap.Result<Integer> res = t.createResult();  
      
      //--- allocations
      for (int i=0; i<NOBJECTS; i++){
        t = t.set(i,  new Integer(i), res);
      }

      //--- lookup
      for (int i=0; i<NOBJECTS; i++) {
        Integer o = t.get(i);
      }
      
      //--- gc
      t = t.removeAllSatisfying(pred, res);
      
      //--- no store/backtrack costs for container
    }
    t2 = System.currentTimeMillis();
    System.out.println("PersistentIntMap (" + NSTATES + " cycles): " + (t2 - t1));
  

    //--- HashMap
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    for (int l=0; l<NSTATES; l++) {
      HashMap<Integer,Integer> m = new HashMap<Integer,Integer>();
      //--- allocations
      for (int i=0; i<NOBJECTS; i++){
        m.put(i, i);
      }

      //--- lookup
      for (int i=0; i<NOBJECTS; i++) {
        Integer o = m.get(i);
      }
      
      //--- gc
      for (Iterator<Map.Entry<Integer,Integer>> it = m.entrySet().iterator(); it.hasNext();) {
        Map.Entry<Integer, Integer> e = it.next();
        if (pred.isTrue(e.getValue())) {
          it.remove();
        }      
      }
      
      //--- 2xclone (upon store and backtrack)
      m = (HashMap<Integer,Integer>)m.clone();
      m = (HashMap<Integer,Integer>)m.clone();
    }
    t2 = System.currentTimeMillis();
    System.out.println("HashMap (" + NSTATES + " cycles): " + (t2 - t1));

    //--- ObjVector (needs to be adjusted for holes -> increased size)
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    for (int l=0; l<NSTATES; l++) {
      ObjVector<Integer> v = new ObjVector<Integer>();
      //--- allocations
      for (int i=0; i<NOBJECTS; i++){
        v.set(i, i);
      }

      //--- lookup
      for (int i=0; i<NOBJECTS; i++) {
        Integer o = v.get(i);
      }
      
      //--- gc
      v.clearAllSatisfying(pred);
      
      //--- snap & restore
      ObjVector.Snapshot<Integer> snap = v.getSnapshot();
      v.restore(snap);
    }
    t2 = System.currentTimeMillis();
    System.out.println("ObjVector (" + NSTATES + " cycles): " + (t2 - t1));

  }

  //--- debugging helpers
  static PersistentIntMap<Integer> addRemove (PersistentIntMap<Integer> t, int[] keys){
    for (int i = 0; i<keys.length; i++){
      int k = keys[i];
      if (k < 0){
        t = t.remove( -k);
      } else {
        t = t.set( k, Integer.valueOf(k));
      }
    }
    
    return t;
  }
  
  static PersistentIntMap<Integer> set (PersistentIntMap<Integer> t, int i){
    return t.set(i, Integer.valueOf(i));
  }

}
