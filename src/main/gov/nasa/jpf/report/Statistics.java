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

package gov.nasa.jpf.report;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.ThreadChoiceGenerator;
import gov.nasa.jpf.search.Search;

/**
 * simple structure to hold statistics info created by Reporters/Publishers
 * this is kind of a second tier SearchListener, which does not
 * explicitly have to be registered
 * 
 * <2do> this should get generic and accessible enough to replace all the
 * other statistics collectors, otherwise there is too much redundancy.
 * If users have special requirements, they should subclass Statistics
 * and set jpf.report.statistics.class accordingly
 * 
 * Note that Statistics might be accessed by a background thread
 * reporting JPF progress, hence we have to synchronize
 */
public class Statistics extends ListenerAdapter implements Cloneable {
    
  // we make these public since we don't want to add a gazillion of
  // getters for these purely informal numbers
  
  public long maxUsed = 0;
  public long newStates = 0;
  public int backtracked = 0;
  public int restored = 0;
  public int processed = 0;
  public int constraints = 0;
  public long visitedStates = 0;
  public long endStates = 0;
  public int maxDepth = 0;
  
  public int gcCycles = 0;
  public int insns = 0;
  public int threadCGs = 0;
  public int dataCGs = 0;
  public int nObjects = 0;
  public int nRecycled = 0;

  public synchronized Statistics clone() {
    try {
      return (Statistics)super.clone();
    } catch (CloneNotSupportedException e) {
      return null; // can't happen
    }
  }
  
  public synchronized void gcBegin (JVM vm) {
    gcCycles++;
  }
  
  public synchronized void instructionExecuted (JVM vm){
    insns++;
  }

  public synchronized void choiceGeneratorSet (JVM vm){
    ChoiceGenerator<?> cg = vm.getChoiceGenerator();
    if (cg instanceof ThreadChoiceGenerator){
      threadCGs++;
    } else {
      dataCGs++;
    }
  }
  
  public synchronized void objectCreated (JVM vm){
    nObjects++;
  }
  
  public synchronized void objectReleased (JVM vm){
    nRecycled++;
  }
  
  public synchronized void stateAdvanced (Search search){
    long m = Runtime.getRuntime().totalMemory();
    if (m > maxUsed) {
      maxUsed = m;
    }

    if (search.isNewState()){
      newStates++;
      int depth = search.getDepth();
      if (depth > maxDepth){
        maxDepth = depth;
      }
    } else {
      visitedStates++;
    }
    if (search.isEndState()){
      endStates++;
    }
  }
  
  public synchronized void stateBacktracked (Search search){
    backtracked++;
  }
  
  public synchronized void stateProcessed (Search search){
    processed++;
  }

  public synchronized void stateRestored (Search search){
    restored++;
  }
    
  public synchronized void searchConstraintHit (Search search){
    constraints++;
  }

}
