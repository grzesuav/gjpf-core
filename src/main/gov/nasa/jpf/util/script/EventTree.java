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

package gov.nasa.jpf.util.script;

import java.util.List;

/**
 * an abstract class that creates Event trees
 * 
 * While there is no need to provide specialized Event types or additional event 
 * constructors, concrete subclasses have to provide a createEventTree() implementation.
 * 
 * A typical implementation looks like this
 * 
 *   public Event createEventTree1() {
 *     return sequence(
 *             event("a"),
 *             alternatives(
 *                     event("1"),
 *                     iteration(2,
 *                             event("x")
 *                     )
 *             ),
 *             event("b")
 *     );
 *   }
 */
public class EventTree extends EventConstructor {
  
  public static final String CONFIG_KEY = "event.class";
  
  protected Event root;

  /**
   * this is our purpose in life, which has to be provided by concrete subclasses 
   */
  public Event createEventTree() {
    // nothing here, needs to be overridden by subclass to populate tree
    return null;
  }

  protected EventTree (){
    root = createEventTree();
  }

  protected EventTree (Event root){
    this.root = root;
  }
  
  public Event getRoot(){
    return root;
  }
    
  //--- inspection and debugging

  public List<Event> endEvents(){
    return root.endEvents();
  }
  
  public void printPaths(){
    for (Event es : endEvents()){
      es.printPath(System.out);
      System.out.println('.');
    }
  }

  public void printTree (){
    root.printTree(System.out, 0);
  }

  /**
   * this should be overridden in case we want to check if this is an expected trace
   * The generic form can only check if this is a valid end event.
   * 
   * To check for a whole trace, implementors should keep some sort of expected event specs
   */
  public boolean checkPath (Event lastEvent){
    for (Event ee : root.endEvents()){
      if (ee.equals(lastEvent)){
        return true;
      }
    }
    
    return false;
  }
  
  public boolean checkPath (Event lastEvent, String[] pathSpecs) {
    String trace = lastEvent.getPathString(null);

    for (int i = 0; i < pathSpecs.length; i++) {
      if (trace.equals(pathSpecs[i])) {
        pathSpecs[i] = null;
        return true;
      }
    }

    return false; // unexpected trace
  }
  
  /**
   * override this if the concrete model keeps track of coverage
   * 
   * @return [0.0..1.0]
   */
  public float getPathCoverage (){
    return 0;
  }
  
  /**
   * override this if the concrete model can keep track of coverage
   * call at the end of execution
   */
  public boolean isCompletelyCovered (){
    return true;
  }
  
  /**
   * extend this tree with a new path 
   */
  public void addPath (Event... path){
    root.addPath(path.length, path);
  }
  
  public Event interleave (Event... otherTrees){
    return root.interleave( otherTrees);
  }
  
  public EventTree interleave (EventTree... otherTrees){
    Event[] otherRoots = new Event[otherTrees.length];
    for (int i=0; i<otherRoots.length; i++){
      otherRoots[i] = otherTrees[i].root;
    }
    
    Event resultRoot = root.interleave( otherRoots);
    
    return new EventTree(resultRoot);
  }  
}
