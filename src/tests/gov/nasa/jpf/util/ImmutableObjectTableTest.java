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

  
  @Test
  public void testSimpleInsert(){
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    
    t = t.set(42,42);
    
    dump(t, new IntegerProcessor());
    assertTrue( t.size() == 1);
    assertTrue( t.get(42) == 42);
  }
  
  @Test
  public void testValueToNodePromotion(){
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    
    t = t.set(1,1);
    t = t.set(33,33);
    
    dump(t, new IntegerProcessor());
    
    assertTrue( t.get(33) == 33);
    assertTrue( t.get(42) == null);
  }

  @Test
  public void testFullNode(){
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    for (int i=0; i<31; i++){
      t = t.set(i,  new Integer(i));
    }
    dump(t, new IntegerProcessor());
    assertTrue( t.size() == 31);
   
    t = t.set(31,31); // that should promote to full node
    assertTrue( t.size() == 32);
    dump(t, new IntegerProcessor());

    t = t.set(31, -31);
    assertTrue( t.size() == 32);
    assertTrue( t.get(31) == -31);
    dump(t, new IntegerProcessor());
    
    t = t.remove(31); // reduce full node again
    dump(t, new IntegerProcessor());
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
    dumpInKeyOrder(t, proc);
    
    assertTrue( t.size() == max);
    assertTrue( proc.getCount() == t.size());
    
    for (int i=0; i<max; i++){
      assertTrue( t.get(i) == i);
    }
    
    assertTrue( t.get(max+1) == null);
  }

  @Test
  public void testSimpleRemove(){
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    
    t = t.set(42,42);
    dump(t, new IntegerProcessor());
    assertTrue( t.size() == 1);
    ImmutableObjectTable<Integer> tt = t.remove(12345); // should not remove anything
    assertTrue( t == tt);
    
    t = t.remove(42);
    dump(t, new IntegerProcessor());
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
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    ImmutableObjectTable<Integer> t = new ImmutableObjectTable<Integer>();
    for (int i=0; i<N; i++){
      t = t.set(i,  new Integer(i));
    }
    t2 = System.currentTimeMillis();
    System.out.println("creation: " + (t2 - t1));
    assertTrue(t.size() == N);

    t1 = System.currentTimeMillis();
    for (int i=0; i<N; i++){
      assertTrue(t.get(i) != null);
    }
    t2 = System.currentTimeMillis();
    System.out.println("lookup: " + (t2 - t1));
    
    t1 = System.currentTimeMillis();
    for (int i=0; i<N; i++){
       t = t.remove(i);
       assertTrue( t.size() == (N-i-1));
    }
    t2 = System.currentTimeMillis();
    System.out.println("remove: " + (t2 - t1));
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
