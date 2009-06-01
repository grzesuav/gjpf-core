//
// Copyright (C) 2006 United States Government as represented by the
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
package gov.nasa.jpf.mc;

import org.junit.Test;
import org.junit.runner.JUnitCore;

import gov.nasa.jpf.util.test.TestJPF;


/**
 * JPF driver for breadth first search test
 */
public class TestOldClassicJPF extends TestJPF {
  static final String TEST_CLASS = "gov.nasa.jpf.mc.oldclassic";


  public static void main (String[] args) {
    JUnitCore.main("gov.nasa.jpf.mc.TestOldClassicJPF");
  }


  /**************************** tests **********************************/
  
  /**
   * Tests running the Crossing example with no heuristics, i.e., with the
   * default DFS.
   */
  @Test
  public void testDFSearch () {
    String[] args = { TEST_CLASS };
    deadlock(args);
  }

  /**
   * Tests running the Crossing example with BFS heuristic.
   */
  @Test
  public void testBFSHeuristic () {
    String[] args = { "+search.class=gov.nasa.jpf.search.heuristic.BFSHeuristic", TEST_CLASS };

    deadlock(args);
  }

}
