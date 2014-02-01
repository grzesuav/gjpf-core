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
 * While there is no need to provide specialized Event types or compound constructors
 * other than sequence(), alternative() and iteration(), concrete subclasses
 * have to provide a createEventTree() implementation.
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
public abstract class EventTree {
  
  public static final String CONFIG_KEY = "event.class";
  
  protected Event root;

  /**
   * this is our purpose in life, which has to be provided by concrete subclasses 
   */
  public abstract Event createEventTree();

  protected EventTree (){
    root = createEventTree();
  }

  public Event getRoot(){
    return root;
  }
  
  //--- event tree creation
  
  /**
   * factory method to facilitate creation of specialized event classes
   */
  protected Event event (String name){
    return new Event(name);
  }
  
  protected Event event (String name, Object... arguments){
    return new Event(name, arguments);
  }

  protected Event alternatives (Event... events){
    Event last = events[0];
    for (int i = 1; i < events.length; i++) {
      Event e = events[i];
      last.setAlt(e);
      last = e;
    }
    return events[0];
  }

  protected Event sequence (Event... events) {
    Event base = events[0];

    for (int i = 1; i < events.length; i++) {
      base.addNext(events[i]);
    }
    return base;
  }

  protected Event iteration (int count, Event... events) {
    Event seq = sequence(events);
    Event[] it = new Event[count];

    it[0] = seq;
    for (int i=1; i<count; i++){
      it[i] = seq.deepClone();
    }

    return sequence(it);
  }

  /**
   * an alterative of all combinations of the specified events (regardless of order) 
   */
  protected Event anyCombination (Event... events){
    int n = events.length;
    int max = 0;
    for (int i=0; i<n; i++){
      max = (max << 1) | 1;
    }
    
    // we use the no-event as the anchor
    Event eFirst = new NoEvent();
    Event eLast = eFirst;
    
    // now fill in all the remaining combinations
    for (int i=1; i<=max; i++){
      Event eAlt=null;
      
      // build and add the next sequence as alternative
      int m = i;
      for (int j=0; m != 0; j++){
        if ((m & 1) != 0){
          Event e = events[j].deepClone();
          if (eAlt == null){
            eAlt = e;
            eLast.setAlt(e);
            eLast = e;
          } else {
            eAlt.setNext(e);
            eAlt = e;
          }
        }
        m >>>= 1;
      }
    }
    
    return eFirst;
  }
  
  
  protected void generatePermutation (int length, Event[] events, Event anchor, Event perm){
    if (length == 0){
      anchor.addAlternative(perm);
      
    } else {
      outer:
      for (Event e : events){
        if (perm != null){
          // check if e is already in there
          for (Event ee = perm; ee != null; ee = ee.getNext()){
            if (ee.equals(e)){
              continue outer;
            }
          }          
          e = perm.deepClone().addNext(e.deepClone());
          
        } else {
          e = e.deepClone();
        }
        
        generatePermutation( length-1, events, anchor, e);
      }
    }
  }
  
  /**
   * generate tree with all event permutations without repetitions.
   * <2do> this is not particularly optimized
   */
  protected Event anyPermutation (Event... events){
    Event a = new NoEvent();
    generatePermutation( events.length, events, a, null);
    return a.getAlt();
  }
  
  
  //--- inspection and debugging

  public List<Event> endEvents(){
    return root.endEvents();
  }
  
  public void printTraces(){
    for (Event es : endEvents()){
      es.printTrace(System.out);
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
  public boolean checkTrace (Event lastEvent){
    for (Event ee : root.endEvents()){
      if (ee.equals(lastEvent)){
        return true;
      }
    }
    
    return false;
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
}
