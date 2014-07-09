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

/**
 * abstract class that hold API to create event trees
 * 
 * this factors out constructor methods so that they can be used inside of
 * EventTrees and EventForests
 */
public abstract class EventConstructor {

  //--- overridable event factory method to facilitate creation of specialized event classes

  protected Event event (String name){
    return new Event(name, this);
  }
  
  protected Event event (String name, Object... arguments){
    return new Event(name, arguments, this);
  }

  //--- compound constructors that create sets of events
  
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
    
    Event[] pathBuffer = new Event[n];
    
    // we use the no-event as the anchor
    Event eFirst = new NoEvent();
    
    // now fill in all the remaining combinations
    for (int i=1; i<=max; i++){
      // init the path buffer
      int pathLength=0;
      for (int j=0, m=i; m != 0; j++){
        if ((m & 1) != 0){
          pathBuffer[pathLength++] = events[j];
        }
        m>>>=1;
      }
      
      eFirst.addPath( pathLength, pathBuffer);
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

}
