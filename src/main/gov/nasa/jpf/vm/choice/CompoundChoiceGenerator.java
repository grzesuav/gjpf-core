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

package gov.nasa.jpf.vm.choice;

import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ChoiceGeneratorBase;

/**
 * an abstract choice generator that is just a list of choice generators
 */
public abstract class CompoundChoiceGenerator<T> extends ChoiceGeneratorBase<T> {

  //--- helper to implement ad hoc linked lists
  protected class Entry {
    ChoiceGenerator<T> cg;
    Entry next;
    
    Entry (ChoiceGenerator<T> cg, Entry next){
      this.cg = cg;
      this.next = next;
    }
  }
  
  protected Entry base;
  protected Entry cur;

  protected CompoundChoiceGenerator (String id){
    super(id);
  }
  
  //--- to be called from derived ctors
  
  protected void setBase (ChoiceGenerator<T> cg){
    base = cur = new Entry( cg, null);
  }
  
  protected void add (ChoiceGenerator<T> cg){
    base = cur = new Entry( cg, cur);
  }
  
  //--- the public ChoiceGenerator interface
  
  public T getNextChoice () {
    if (cur != null){
      return cur.cg.getNextChoice();
    } else {
      return null;
    }
  }

  public boolean hasMoreChoices () {
    if (cur != null){
      if (cur.cg.hasMoreChoices()){
        return true;
      } else {
        for (Entry e = cur.next; e != null; e = e.next){
          if (e.cg.hasMoreChoices()){
            return true;
          }
        }
        
        return false;
      }
      
    } else {
      return false;
    }
  }

  public void advance () {
    if (cur != null){
      if (cur.cg.hasMoreChoices()){
        cur.cg.advance();
      } else {
        cur = cur.next;
        advance();
      }
    }
  }

  public void reset () {
    cur = base;
    
    for (Entry e = base; e != null; e = e.next){
      e.cg.reset();
    }
  }

  public int getTotalNumberOfChoices () {
    int n = 0;
    
    for (Entry e = base; e != null; e = e.next){
      n += e.cg.getTotalNumberOfChoices();
    }
    
    return n;
  }

  public int getProcessedNumberOfChoices () {
    int n=0;
    
    for (Entry e = base; e != null; e = e.next){
      n += e.cg.getProcessedNumberOfChoices();
      if (e == cur){
        break;
      }
    }
    
    return n;
  }

}
