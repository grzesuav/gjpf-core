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

package gov.nasa.jpf.util.event;

import java.util.HashMap;

/**
 * a forest of named event trees
 * This class mostly exists for the purpose of tree construction, which happens from respective ctors like
 * 
 *  MyEventForest (){
 *     addDefault(
 *       sequence(
 *         event(..),
 *      ..
 *     );
 * 
 *     addTree( "someState",
 *       sequence(
 *         event(..),
 *         ...
 *     );
 * 
 *     addTree( "someOtherState",
 *       ...
 *   }
 * 
 * Used by CompoundEventChoiceGenerator
 */
public abstract class EventForest extends EventConstructor {

  protected Event defaultTree;
  protected HashMap<String,Event> map = new HashMap<String,Event>();
  
  // map to be populated by subclass ctors
  
  protected void add (String name, Event root){
    map.put(name, root);
  }
  
  protected void addDefault( Event root){
    defaultTree = root;
  }
  
  public Event getDefault(){
    return defaultTree;
  }
  
  public Event get (String name){
    return map.get(name);
  }
}
