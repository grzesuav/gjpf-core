package gov.nasa.jpf.util;

import org.junit.Test;

import gov.nasa.jpf.util.PersistentIntMapTest.EvenPredicate;
import gov.nasa.jpf.util.PersistentIntMapTest.IntegerProcessor;
import gov.nasa.jpf.util.PersistentIntMapTest.IntervalPredicate;
import gov.nasa.jpf.util.PersistentMsbIntMap.BitmapNode;
import gov.nasa.jpf.util.PersistentMsbIntMap.FullNode;
import gov.nasa.jpf.util.PersistentMsbIntMap.OneNode;
import gov.nasa.jpf.util.test.TestJPF;

public class PersistentStagingMsbIntMapTest extends TestJPF {

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
  
  static <T> void dumpInKeyOrder (PersistentIntMap<T> map, Processor<T> proc){
    PersistentStagingMsbIntMap<T> t = (PersistentStagingMsbIntMap<T>) map; // we don't want to force all methods to be ret - covariant
    System.out.print( "size=");
    System.out.print(t.size());
    if (t.stagingNode != null) {
      System.out.print(", staging=");
      System.out.print(t.stagingNode.getClass().getSimpleName() + '@'
          + Integer.toHexString(t.stagingNode.hashCode()));
    }
    System.out.print(", values={");
    t.processInKeyOrder( proc); 
    System.out.println('}');    
  }
  
  PersistentIntMap<Integer> insert (PersistentIntMap<Integer> t, int key){
    System.out.println();
    System.out.printf("--- add %d:\n", key);
    t = t.set( key, key);
    dumpInKeyOrder(t, new IntegerProcessor());
    t.printOn(System.out);
    System.out.printf("--- find(%d): %s\n", key, t.get(key)); 
    return t;
  }
  
  @Test
  public void testStagingNodeInsert() {
    PersistentIntMap<Integer> t = new PersistentStagingMsbIntMap<Integer>();
    assertTrue( t.isEmpty());
    
    t = insert( t, 42);
    dumpInKeyOrder( t, new IntegerProcessor());
    assertTrue( t.size() == 1);
    
    t = insert( t, 43);
    dumpInKeyOrder( t, new IntegerProcessor());
    assertTrue( t.size() == 2);    
  }
  
  @Test
  public void testStagingNodeMerge() {
    PersistentIntMap<Integer> t = new PersistentStagingMsbIntMap<Integer>();
    assertTrue( t.isEmpty());
    
    t = insert( t, 1);
    dumpInKeyOrder( t, new IntegerProcessor());
    assertTrue( t.size() == 1);
    
    // mod stagingNode, that should also set a new root
    t = insert( t, 2);
    dumpInKeyOrder( t, new IntegerProcessor());
    assertTrue( t.size() == 2);
    
    // new node, but since stagingNode == root no merge required
    t = insert( t, 42);
    dumpInKeyOrder( t, new IntegerProcessor());
    assertTrue( t.size() == 3);

    // mod stagingNode
    t = insert( t, 43);
    dumpInKeyOrder( t, new IntegerProcessor());
    assertTrue( t.size() == 4);

    // back to first value node, now the stagingNode needs to be merged
    t = insert( t, 3);
    dumpInKeyOrder( t, new IntegerProcessor());
    assertTrue( t.size() == 5);
  }
  
  @Test
  public void testDenseInsert(){
    PersistentIntMap<Integer> t = new PersistentStagingMsbIntMap<Integer>();
    
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
    
    t.printOn(System.out);
    
    assertTrue( t.get(max+1) == null);
  }
  
  @Test
  public void testRemove() {
    PersistentIntMap<Integer> t = new PersistentStagingMsbIntMap<Integer>();

    System.out.println("--- add 1,2,42,43,3");
    t = t.set(1, 1);
    t = t.set(2, 2);
    t = t.set(42, 42);
    t = t.set(43, 43);    
    t = t.set(3, 3);
    dumpInKeyOrder(t, new IntegerProcessor());
    t.printOn(System.out);
    
    System.out.println("--- remove 3 (mod staging node)");
    t = t.remove(3);
    dumpInKeyOrder(t, new IntegerProcessor());
    t.printOn(System.out);
    
    System.out.println("--- remove 42 (new staging node and merge old one)");
    t = t.remove(43);
    dumpInKeyOrder(t, new IntegerProcessor());
    t.printOn(System.out);
  }

  @Test
  public void testBlockRemove(){
    int N = 150;
    
    PersistentIntMap<Integer> t = new PersistentStagingMsbIntMap<Integer>();
    for (int i=0; i<N; i++){
      t = t.set(i,i);
    }
    PersistentIntMap<Integer> t0 = t;

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
  
  @Test
  public void testLargeTable() {
    long t1, t2;
    int N = 40000; // table size

    //--- create
    System.out.println("-------- creating table with " + N + " entries");
    Runtime.getRuntime().gc();
    t1 = System.currentTimeMillis();
    PersistentIntMap<Integer> t = new PersistentStagingMsbIntMap<Integer>();
    for (int i=0; i<N; i++){
if (i == 32) {
  System.out.println("@@ at 32");
}
try {
      t = t.set(i,  new Integer(i));
} catch (Throwable x) {
  System.out.println("@@ X at " + i);
  x.printStackTrace();
  System.exit(0);
}
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
    
System.out.printf("OneNodes: %d, BitmapNodes: %d, FullNodes: %d\n", OneNode.nNodes, BitmapNode.nNodes, FullNode.nNodes);
  }
}
