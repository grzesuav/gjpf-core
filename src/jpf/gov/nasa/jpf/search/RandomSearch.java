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
package gov.nasa.jpf.search;


import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.VMState;
import gov.nasa.jpf.util.Debug;


/**
 * this is a straight execution pseudo-search - it doesn't search at
 * all (i.e. it doesn't backtrack), but just behaves like a 'normal' VM,
 * going forward() until there is no next state then it restarts the search 
 * until it hits a certain number of paths executed
 *
 */
public class RandomSearch extends Search {
  int path_limit = 0;
  
  public RandomSearch (Config config, JVM vm) throws Config.Exception {
    super(config, vm);
    
    path_limit = config.getInt("search.RandomSearch.path_limit", 0);
    
    System.out.println("Path Limit = " + path_limit);
    
    Debug.println(Debug.WARNING, "Random Search");
  }
  
  public void search () {
    int maxDepth = getMaxSearchDepth();
    int    depth = 0;
    int paths = 0;
    depth++;
    
    if (hasPropertyTermination()) {
      return;
    }
    
    //vm.forward();
    VMState init_state = vm.getState();
    
    notifySearchStarted();
    while (!done) {
      if (depth < maxDepth && forward() && !isEndState) {
        notifyStateAdvanced();
        if (hasPropertyTermination()) {
          return;
        }
        depth++;
      } else { // no next state or reached depth limit
        // <2do> we could check for more things here. If the last insn wasn't
        // the main return, or a System.exit() call, we could flag a JPFException
        if (depth >= maxDepth) {
          notifySearchConstraintHit(QUEUE_CONSTRAINT);
        }
        isPropertyViolated();
        done = (paths >= path_limit);
        paths++;
        System.out.println("paths = " + paths);
        depth = 1;
        vm.restoreState(init_state);
        vm.resetNextCG();
      }
    }
    notifySearchFinished();
  }
}
