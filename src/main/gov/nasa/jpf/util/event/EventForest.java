//
// Copyright (C) 2014 United States Government as represented by the
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
