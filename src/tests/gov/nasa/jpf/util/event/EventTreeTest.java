//
// Copyright (C) 2013 United States Government as represented by the
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

package gov.nasa.jpf.util.event;

import gov.nasa.jpf.util.event.EventTree;
import gov.nasa.jpf.util.event.Event;
import gov.nasa.jpf.util.test.TestJPF;
import static gov.nasa.jpf.util.test.TestJPF.fail;
import org.junit.Test;

/**
 * regression test for EventTree
 */
public class EventTreeTest extends TestJPF {
  
  protected boolean checkGeneratedPaths (EventTree m, String[] expected){
    System.out.println("event tree: ");
    m.printTree();
    
    int nMatches = 0;
    for (Event ee : m.visibleEndEvents()){
      String trace = ee.getPathString(null);
      System.out.print("checking path: \"" + trace + '"');
      
      if (!m.checkPath(ee, expected)){
        System.out.println("   UNEXPECTED");
        return false;
      } else {
        System.out.println("   OK");
      }
      
      nMatches++;
    }
    
    if (nMatches != expected.length){
      System.out.println("UNCOVERED PATH: ");
      for (int i=0; i<expected.length; i++){
        if (expected[i] != null){
          System.err.println(expected[i]);
        }
      }
      return false;
    }
    
    return true;
  }
  
  
  //--------------------------------------------------------------------
    
  static class SimpleTree extends EventTree {    
    @Override
    public Event createEventTree() {
      return 
        sequence(
          event("a"),
          alternatives(
            event("1"),
            iteration(2,
              event("x")
            )
          ),
          event("b")
        );
    }
  }

  @Test
  public void testSimpleTree(){
    SimpleTree m = new SimpleTree();
    
    String[] expected = {
        "a1b",
        "axxb"     
    };
    
    if (!checkGeneratedPaths(m, expected)){
      fail("failed to match traces");
    }
  }
  
  //--------------------------------------------------------------------
  public static class CombinationTree extends EventTree {    
    @Override
    public Event createEventTree() {
      return anyCombination(
               event("a"),
               event("b"),
               event("c"),
               event("d")
             );
    }    
  }
  
  @Test
  public void testCombinationTree(){
    CombinationTree t = new CombinationTree();
    //t.printPaths();

    String[] expected = {
        "",
        "a",
        "b",
        "ab",
        "c",
        "ac",
        "bc",
        "abc",
        "d",
        "ad",
        "bd",
        "abd",
        "cd",
        "acd",
        "bcd",
        "abcd"
    };
    
    if (!checkGeneratedPaths(t, expected)){
      fail("failed to match traces");
    }
  }  

  static class SimpleCombinationTree extends EventTree {
    @Override
    public Event createEventTree() {
      return anyCombination(
               event("a"),
               event("b")
             );
    }
  }

  //@Test
  public void testSimpleCombinationTree(){
    SimpleCombinationTree t = new SimpleCombinationTree();
    System.out.println("--- tree:");
    t.printTree();
    System.out.println("--- paths:");
    t.printPaths();
  }

  //--------------------------------------------------------------------
 
  static class EmbeddedCombinationTree extends EventTree {
    @Override
    public Event createEventTree() {
      return sequence(
                event("1"),
                anyCombination(
                   event("a"),
                   event("b")),
                event("2"));
    }
  }

  //@Test
  public void testEmbeddedCombinationTree(){
    EventTree t = new EmbeddedCombinationTree();
    System.out.println("--- tree:");
    t.printTree();
    System.out.println("--- paths:");
    t.printPaths();
  }
    
  
  //--------------------------------------------------------------------
  static class DT extends EventTree {    
    @Override
    public Event createEventTree() {
      return sequence(
              event("a"),
              alternatives(
                  event("1"),
                  sequence(
                      event("X"),
                      event("Y")
                  ),
                  iteration(3,
                      event("r")
                  )
              ),
              event("b"));
    }    
  }

  
  @Test
  public void testMaxDepth(){
    DT t = new DT();
    t.printTree();
    t.printPaths();
    
    int maxDepth = t.getMaxDepth();
    System.out.println("max depth: " + maxDepth);
    
    assertTrue( maxDepth == 5);
  }

  //--------------------------------------------------------------------
  static class PermutationTree extends EventTree {
    @Override
    public Event createEventTree(){
      return anyPermutation(
               event("a"),
               event("b"),
               event("c")
              );
    }
  }

  @Test
  public void testPermutationTree(){
    PermutationTree t = new PermutationTree();
    //t.printPaths();
    
    String[] expected = {
        "abc",
        "acb",
        "bac",
        "bca",
        "cab",
        "cba"
      };
    
    if (!checkGeneratedPaths(t, expected)){
      fail("failed to match traces");
    }
  }
  
  //--------------------------------------------------------------------
  static class AddPathTree extends EventTree {        
    @Override
    public Event createEventTree(){
      return sequence(
               event("a"),
               event("b"),
               event("c")
              );
    } 
  }
  
  @Test
  public void testAddPath () {
    AddPathTree t = new AddPathTree();
    t.addPath(
            new Event("a"), 
            new Event("b"), 
            new Event("3"));

    String[] expected = { "abc", "ab3" };
    
    if (!checkGeneratedPaths(t, expected)){
      fail("failed to match traces");
    }
  }
    
  //-------------------------------------------------------------------
  static class MT1 extends EventTree {
    @Override
    public Event createEventTree(){
      return sequence(
               event("a"),
               event("b"),
               event("c")
              );
    }
  }
  
  static class MT2 extends EventTree {
    @Override
    public Event createEventTree(){
      return sequence(
               event("1"),
               event("2"),
               event("3")
              );
    }
  }

  static class MT3 extends EventTree {
    @Override
    public Event createEventTree(){
      return sequence(
               event("X"),
               event("Y")
              );
    }
  }

  
  @Test
  public void testMerge (){
    MT1 t1 = new MT1();
    MT2 t2 = new MT2();
    //MT3 t3 = new MT3();
    
    EventTree t = t1.interleave( t2);
    // t.printPaths();
    
    String[] expected = {
      "a123bc",
      "a12b3c",
      "a12bc3",
      "a1b23c",
      "a1b2c3",
      "a1bc23",
      "ab123c",
      "ab12c3",
      "ab1c23",
      "abc123",
      "123abc",
      "12a3bc",
      "12ab3c",
      "12abc3",
      "1a23bc",
      "1a2b3c",
      "1a2bc3",
      "1ab23c",
      "1ab2c3",
      "1abc23"
    };
    
    if (!checkGeneratedPaths(t, expected)){
      fail("failed to match traces");
    }
  }
  
  //-------------------------------------------------------------------
  static class SMT1 extends EventTree {
    @Override
    public Event createEventTree(){
      return sequence(
               event("a"),
               event("b")
              );
    }
  }
  
  static class SMT2 extends EventTree {
    @Override
    public Event createEventTree(){
      return sequence(
               event("1"),
               event("2")
              );
    }
  }

  //@Test
  public void testSimpleMerge (){
    SMT1 t1 = new SMT1();
    SMT2 t2 = new SMT2();
    //MT3 t3 = new MT3();
    
    EventTree t = t1.interleave( t2);
    System.out.println("--- merged tree:");
    t.printTree();
    System.out.println("--- merged paths:");
    t.printPaths();
  }
    

  //-------------------------------------------------------------------
  static class RT1 extends EventTree {
    @Override
    public Event createEventTree(){
      return sequence(
               event("a"),
               event("b")
              );
    }
  }
  
  static class RT2 extends EventTree {
    @Override
    public Event createEventTree(){
      return sequence(
               event("1"),
               event("2")
              );
    }
  }

  
  @Test
  public void testRemove (){
    RT1 t1 = new RT1();
    RT2 t2 = new RT2();
    
    EventTree t = t1.interleave( t2);
    System.out.println("merged tree: ");
    //t.printTree();
    t.printPaths();
    
    t = new EventTree( t.removeSource(t2));
    System.out.println("reduced tree: ");
    //t.printTree();
    //t.printPaths();
    
    String[] expected = { "ab" };
    if (!checkGeneratedPaths(t, expected)){
      fail("failed to match traces");
    }    
  }
}
