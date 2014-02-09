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

package gov.nasa.jpf.util.script;

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
    
    for (Event ee : m.endEvents()){
      String trace = ee.getPathString(null);
      System.out.println("checking path: " + trace);
      
      if (!m.checkPath(ee, expected)){
        System.out.print("unexpected path");
        return false;
      }
    }
    
    if (!m.isCompletelyCovered()){
      System.out.println("uncovered path: ");
      for (int i=0; i<expected.length; i++){
        if (expected[i] != null){
          System.out.println(expected[i]);
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
  static class CombinationTree extends EventTree {    
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
        "NONE",
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
}
