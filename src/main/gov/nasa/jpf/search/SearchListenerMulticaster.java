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
package gov.nasa.jpf.search;

import gov.nasa.jpf.JPFListener;



/**
 * helper class to avoid indirection if there is just one observer
 * (the usual case) Typical 'Container' pattern implementation
 */
public class SearchListenerMulticaster implements SearchListener {
    
  SearchListener head;
  SearchListener tail;

  public static SearchListener add (SearchListener oldListener, SearchListener newListener) {
    if (newListener == null) {
      return oldListener;
    }
    if (oldListener == null) {
      return newListener;
    }
    if (newListener == oldListener) {
      return oldListener;
    }
    
    if (oldListener instanceof SearchListenerMulticaster) {
      // filter out multiple registrations
      SearchListenerMulticaster c = (SearchListenerMulticaster)oldListener;
      while (c != null) {
        if (c.tail == newListener) {
          return oldListener;
        }
        if (c.head == newListener) {
          return oldListener;
        } else {
          if (c.head instanceof SearchListenerMulticaster) {
            c = (SearchListenerMulticaster) c.head;
          } else {
            break;
          }
        }
      }
    }
    
    return new SearchListenerMulticaster(oldListener, newListener);
  }

  public static boolean containsType (SearchListener listener, Class<?> type) {
    if (listener == null) {
      return false;
    }
    
    if (listener instanceof SearchListenerMulticaster) {
      SearchListenerMulticaster l = (SearchListenerMulticaster)listener;
      while (l != null) {
        if (type.isAssignableFrom(l.tail.getClass())) {
          return true;
        }
        if (l.head instanceof SearchListenerMulticaster) {
          l = (SearchListenerMulticaster)l.head;          
        } else {
          return type.isAssignableFrom(l.head.getClass());
        }
      }
      return false;
      
    } else {
      return type.isAssignableFrom(listener.getClass());
    }
  }

  
  public static SearchListener remove (SearchListener oldListener, SearchListener removeListener){
    if (oldListener == removeListener) {
      return null;
    }
    if (oldListener instanceof SearchListenerMulticaster){
      return ((SearchListenerMulticaster)oldListener).remove( removeListener);
    }
    
    return oldListener;
  }
  
  protected SearchListener remove (SearchListener listener) {
    if (listener == head) {
      return tail;
    }
    if (listener == tail){
      return head;
    }
    
    SearchListenerMulticaster h,t;
    if (head instanceof SearchListenerMulticaster) {
      h = (SearchListenerMulticaster)head;
      if (tail instanceof SearchListenerMulticaster){
        t = (SearchListenerMulticaster)tail;
        return new SearchListenerMulticaster( h.remove(listener),t.remove(listener));
      } else {
        return new SearchListenerMulticaster( h.remove(listener), tail);
      }
    } else if (tail instanceof SearchListenerMulticaster) {
      t = (SearchListenerMulticaster)tail;      
      return new SearchListenerMulticaster( head, t.remove(listener));
    }
    
    return this;
  }

  
  public SearchListenerMulticaster (SearchListener h, SearchListener t) {
    head = h;
    tail = t;
  }

  public void stateAdvanced (Search search) {
    head.stateAdvanced(search);
    tail.stateAdvanced(search);
  }

  public void stateProcessed (Search search) {
    head.stateProcessed(search);
    tail.stateProcessed(search);
  }
  
  public void stateBacktracked (Search search) {
    head.stateBacktracked(search);
    tail.stateBacktracked(search);
  }

  public void statePurged (Search search) {
    head.statePurged(search);
    tail.statePurged(search);
  }


  public void stateStored (Search search) {
    head.stateStored(search);
    tail.stateStored(search);
  }
  
  public void stateRestored (Search search) {
    head.stateRestored(search);
    tail.stateRestored(search);
  }
  
  public void propertyViolated (Search search) {
    head.propertyViolated(search);
    tail.propertyViolated(search);
  }

  public void searchStarted(Search search) {
    head.searchStarted(search);
    tail.searchStarted(search);
  }
  
  
  public void searchFinished(Search search) {
    // this one is a little odd - we notify in reverse order
    // so that we can make sure system listeners are executing
    // after all the other ones
    tail.searchFinished(search);
    head.searchFinished(search);
    //tail.searchFinished(search);
  }
  
  public void searchConstraintHit(Search search) {
    head.searchConstraintHit(search);
    tail.searchConstraintHit(search);
  }

}
