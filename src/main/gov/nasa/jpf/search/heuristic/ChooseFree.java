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
package gov.nasa.jpf.search.heuristic;

import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.Config;

/**
 * Heuristic state prioritizer that favors executions that are not 
 * nondeterministic, i.e. deterministic. When analysing abstracted programs
 * this has the effect of prioritizing "must"-transitions over "may"-transitions. 
 * An error detected using this heuristic has a good likelihood of being non-spurious.  
 */
public class ChooseFree extends SimplePriorityHeuristic {

  public ChooseFree (Config config, JVM vm)  {
    super(config, vm);
  }

  protected int computeHeuristicValue () {
    int h_value = 1 + vm.getAbstractionNonDeterministicThreadCount();

    return h_value;
  }
}
