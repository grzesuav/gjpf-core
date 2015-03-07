/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package gov.nasa.jpf.search.heuristic;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.VM;


/**
 * heuristic state prioritizer that uses fields of the Main class under test
 * to determine priorities (i.e. priorities can be set by the program under test)
 *  
 * <2do> pcm - does this still make sense in light of MJI ? If we keep it, this
 * has to be moved to the Verify interface!
 */
public class UserHeuristic extends SimplePriorityHeuristic {
  static final int defaultValue = 1000;

  public UserHeuristic (Config config, VM vm) {
    super(config, vm);
  }

  @Override
  protected int computeHeuristicValue () {
    
    // <2do> pcm - BAD, this is WAY too hardwired
    ClassLoaderInfo systemLoader = ClassLoaderInfo.getCurrentSystemClassLoader();
    ElementInfo ei = systemLoader.getElementInfo("Main");
    if (ei != null) {
      // this code is ugly because of the Reference interface
      ElementInfo b = ei.getObjectField("buffer");

      if (b != null) {
        int current = b.getIntField("current");
        int capacity = b.getIntField("capacity");

        return (capacity - current);
      }
    }

    return defaultValue;
  }
}
