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

import gov.nasa.jpf.vm.ChoiceGeneratorBase;

/**
 * ChoiceGenerator for Events.
 * This is basically just a pointer into the event tree
 */
public class EventGenerator extends ChoiceGeneratorBase<Event> {

  protected Event base;
  protected Event cur;
  protected int nProcessed;
  
  public EventGenerator (String id, Event base){
    super(id);
    this.base = base;
  }
  
  public EventGenerator getSuccessor (String id){
    if (cur == null){
      return new EventGenerator(id, base.getNext());
    } else {
      Event next = cur.getNext();
      if (next != null){
        return new EventGenerator( id, next);
      } else {
        return null; // done
      }
    }
  }
  
  public Event getNextChoice () {
    return cur;
  }

  public Class<Event> getChoiceType () {
    return Event.class;
  }

  public boolean hasMoreChoices () {
    if (cur == null){
      return (nProcessed == 0);
    } else {
      return (cur.getAlt() != null);
    }
  }

  public void advance () {
    if (cur == null){
      if (nProcessed == 0){
        cur = base;
        nProcessed = 1;
      }
    } else {
      cur = cur.getAlt();
      nProcessed++;
    }
  }

  public void reset () {
    isDone = false;
    cur = null;
    nProcessed = 0;
  }

  public int getTotalNumberOfChoices () {
    return base.getNumberOfAlternatives();
  }

  public int getProcessedNumberOfChoices () {
    return nProcessed;
  }

}
