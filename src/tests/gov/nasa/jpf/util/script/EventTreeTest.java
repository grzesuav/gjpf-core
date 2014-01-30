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
  
  protected boolean checkGeneratedTraces (TestEventTree m){
    System.out.println("event tree: ");
    m.printTree();
    
    for (Event ee : m.endEvents()){
      String trace = ee.getTrace(null);
      System.out.println("checking trace: " + trace);
      
      if (!m.checkTrace(ee)){
        System.out.print("unexpected trace");
        return false;
      }
    }
    
    if (!m.isCompletelyCovered()){
      System.out.println("uncovered traces: ");
      String[] expected = m.expected;
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
    
  static class SimpleTree extends TestEventTree {
    SimpleTree (){
      expected = new String[] {
        "a1b",
        "axxb"
      };
    }
    
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
    
    if (!checkGeneratedTraces(m)){
      fail("failed to match traces");
    }
  }
  
  //--------------------------------------------------------------------
  static class CombinationTree extends TestEventTree {
    CombinationTree (){
      expected = new String[] {
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
    }
    
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
    //t.printTraces();
    
    if (!checkGeneratedTraces(t)){
      fail("failed to match traces");
    }
  }  

  //--------------------------------------------------------------------
  static class PermutationTree extends TestEventTree {
    public PermutationTree(){
      expected = new String[] {
        "abc",
        "acb",
        "bac",
        "bca",
        "cab",
        "cba"
      };
    }
    
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
    //t.printTraces();
    
    if (!checkGeneratedTraces(t)){
      fail("failed to match traces");
    }
  }
}
