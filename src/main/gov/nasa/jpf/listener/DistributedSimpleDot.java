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

package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.MultiProcessChoiceGenerator;

/**
 * This is a Graphviz dot-file generator similar to SimpleDot. It is useful in
 * the case of Multiprocess applications. It distinguishes local choices from global
 * choices.
 * 
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 */
public class DistributedSimpleDot extends SimpleDot {

  static final String MP_START_NODE_ATTRS = "shape=octagon,fillcolor=green";
  static final String MP_NODE_ATTRS = "shape=octagon,fillcolor=azure2";
  
  protected String mpNodeAttrs;
  protected String mpStartNodeAttrs;
  
  public DistributedSimpleDot (Config config, JPF jpf) {
    super(config, jpf);
    
    mpNodeAttrs = config.getString("dot.mp_node_attr", MP_NODE_ATTRS);
    startNodeAttrs = config.getString("dot.mp_start_node_attr", MP_START_NODE_ATTRS);
  }

  @Override
  public void stateAdvanced(Search search){
    int id = search.getStateId();
    long edgeId = ((long)lastId << 32) | id;

    if (id <0 || seenEdges.contains(edgeId)){
      return; // skip the root state and property violations (reported separately)
    }


    if (search.isErrorState()) {
      String eid = "e" + search.getNumberOfErrors();
      printTransition(getStateId(lastId), eid, getLastChoice(), getError(search));
      printErrorState(eid);
      lastErrorId = eid;

    } else if (search.isNewState()) {

      if (search.isEndState()) {
        printTransition(getStateId(lastId), getStateId(id), getLastChoice(), "return");
        printEndState(getStateId(id));
      } else {
        printTransition(getStateId(lastId), getStateId(id), getLastChoice(), getNextCG());
        printMultiProcessState(getStateId(id));
      }

    } else { // already visited state
      String nextCG = null;
      if(!search.isEndState()) {
        nextCG = getNextCG();
      }
      printTransition(getStateId(lastId), getStateId(id), getLastChoice(), nextCG);
    }

    seenEdges.add(edgeId);
    lastId = id;
  }
  
  protected void printMultiProcessState(String stateId){
    if(vm.getNextChoiceGenerator() instanceof MultiProcessChoiceGenerator) {
      pw.print(stateId);

      pw.print(" [");
      pw.print(mpNodeAttrs);
      pw.print(']');

      pw.println("  // multiprc state");
    }
  }
}
