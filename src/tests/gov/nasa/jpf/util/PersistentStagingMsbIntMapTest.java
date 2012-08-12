package gov.nasa.jpf.util;

import org.junit.Test;

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
  
  static <T> void dumpInKeyOrder (PersistentStagingMsbIntMap<T> t, Processor<T> proc){
    System.out.print( "size=");
    System.out.print(t.size());
    System.out.print( ", staging=");
    System.out.print( t.stagingNode.getClass().getSimpleName() + '@' + Integer.toHexString(t.stagingNode.hashCode()));
    System.out.print(", values={");
    t.processInKeyOrder( proc); 
    System.out.println('}');    
  }
  
  PersistentStagingMsbIntMap<Integer> insert (PersistentStagingMsbIntMap<Integer> t, int key){
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
    PersistentStagingMsbIntMap<Integer> t = new PersistentStagingMsbIntMap<Integer>();
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
    PersistentStagingMsbIntMap<Integer> t = new PersistentStagingMsbIntMap<Integer>();
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

}
