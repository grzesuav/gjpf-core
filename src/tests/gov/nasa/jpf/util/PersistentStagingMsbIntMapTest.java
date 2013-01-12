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

import org.junit.Test;


/**
 * regression test for PersistentMsbIntMap
 */
public class PersistentStagingMsbIntMapTest extends PersistentIntMapTestBase {
  
  protected void dump (PersistentIntMap<Integer> map, Processor<Integer> proc){
    PersistentStagingMsbIntMap<Integer> t = (PersistentStagingMsbIntMap<Integer>) map; // we don't want to force all methods to be ret - covariant
    System.out.print( "size=");
    System.out.print(t.size());
    if (t.stagingNode != null) {
      System.out.print(", staging=");
      System.out.print(t.stagingNode.getClass().getSimpleName() + '@'
          + Integer.toHexString(t.stagingNode.hashCode()));
    }
    System.out.print(", values={");
    t.process( proc); 
    System.out.println('}');    
  }
    
  protected PersistentIntMap<Integer> createPersistentIntMap(){
    return new PersistentStagingMsbIntMap<Integer>();
  }
  
  //--- additional test methods
  
  @Test
  public void testStagingNodeInsert() {
    PersistentIntMap<Integer> t = new PersistentStagingMsbIntMap<Integer>();
    assertTrue( t.isEmpty());
    
    t = insert( t, 42);
    dump( t, new IntegerProcessor());
    assertTrue( t.size() == 1);
    
    t = insert( t, 43);
    dump( t, new IntegerProcessor());
    assertTrue( t.size() == 2);    
  }
  
  @Test
  public void testStagingMerge() {
    PersistentIntMap<Integer> t = new PersistentStagingMsbIntMap<Integer>();
    assertTrue( t.isEmpty());
    
    t = insert( t, 1);
    dump( t, new IntegerProcessor());
    assertTrue( t.size() == 1);
    
    // mod stagingNode, that should also set a new root
    t = insert( t, 2);
    dump( t, new IntegerProcessor());
    assertTrue( t.size() == 2);
    
    // new node, but since stagingNode == root no merge required
    t = insert( t, 42);
    dump( t, new IntegerProcessor());
    assertTrue( t.size() == 3);

    // mod stagingNode
    t = insert( t, 43);
    dump( t, new IntegerProcessor());
    assertTrue( t.size() == 4);

    // back to first value node, now the stagingNode needs to be merged
    t = insert( t, 3);
    dump( t, new IntegerProcessor());
    assertTrue( t.size() == 5);
  }
  
  @Test
  public void testStagingRemove() {
    PersistentIntMap<Integer> t = new PersistentStagingMsbIntMap<Integer>();

    System.out.println("--- add 1,2,42,43,3");
    t = t.set(1, 1);
    t = t.set(2, 2);
    t = t.set(42, 42);
    t = t.set(43, 43);    
    t = t.set(3, 3);
    dump(t, new IntegerProcessor());
    t.printOn(System.out);
    
    System.out.println("--- remove 3 (mod staging node)");
    t = t.remove(3);
    dump(t, new IntegerProcessor());
    t.printOn(System.out);
    
    System.out.println("--- remove 42 (new staging node and merge old one)");
    t = t.remove(43);
    dump(t, new IntegerProcessor());
    t.printOn(System.out);
  }

}
