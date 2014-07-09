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

import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.OATHash;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * class that represents an external stimulus to the SUT, which is produced by EnvironmentModel instances
 * 
 * Note that albeit concrete EnvironmentModels can provide their own, specialized Event types, this class
 * is generic enough that we don't declare it as abstract
 */
public class Event implements Cloneable {

  static final Object[] NO_ARGUMENTS = new Object[0];
  

  //--- linkage
  protected Event next;
  protected Event prev;
  protected Event alt;

  protected String name;
  protected Object[] arguments;
  
  protected Object source;  // optional, set on demand to keep track of where an event came from

  public Event (String name){
    this( name, NO_ARGUMENTS, null);
  }

  public Event (String name, Object source){
    this( name, NO_ARGUMENTS, source);
  }  
  
  public Event(String name, Object[] arguments) {
    this(name, arguments, null);
  }
  
  public Event(String name, Object[] arguments, Object source) {
    this.name = name;
    this.arguments = arguments != null ? arguments : NO_ARGUMENTS;
    this.source = source;
  }

  
  
  @Override
  public boolean equals (Object o){
    if (o instanceof Event){
      Event other = (Event)o;
      
      if (name.equals(other.name)){
        return Misc.equals(arguments, other.arguments);
      }
    }
    
    return false;
  }
  
  @Override
  public int hashCode(){
    int h = name.hashCode();
    
    for (int i=0; i<arguments.length; i++){
      h = OATHash.hashMixin(h, arguments[i].hashCode());
    }
    
    return OATHash.hashFinalize(h);
  }
  
  protected void setNext (Event e){
    next = e;
    e.prev = this;
  }

  protected void setPrev (Event e){
    prev = e;

    if (alt != null){
      alt.setPrev(e);
    }
  }

  protected void setAlt (Event e){
    alt = e;

    if (prev != null) {
      e.setPrev(prev);
    }
  }

  protected void setSource (Object source){
    this.source = source;
  }
  
  public int getNumberOfAlternatives(){
    int n = 0;
    for (Event e = alt; e != null; e = e.alt) {
      n++;
    }

    return n;
  }

  public Event unlinkedClone(){
    try {
      Event e = (Event)super.clone();
      e.next = e.prev = e.alt = null;
      return e;

    } catch (CloneNotSupportedException x) {
      throw new RuntimeException("event clone failed", x);
    }
    
  }
  
  public Event deepClone(){
    try {
      Event e = (Event)super.clone();

      if (next != null) {
        e.next = next.deepClone();
        e.next.prev = e;

        if (next.alt != null){
          e.next.alt.prev = e;
        }
      }

      if (alt != null) {
        e.alt = alt.deepClone();
      }

      return e;

    } catch (CloneNotSupportedException x) {
      throw new RuntimeException("event clone failed", x);
    }
  }

  public String getName(){
    return name;
  }

  public Object[] getArguments(){
    return arguments;
  }
  
  public Event getNext(){
    return next;
  }
  
  public Event getAlt(){
    return alt;
  }
  
  public Event getPrev(){
    return prev;
  }
  
  public Object getSource(){
    return source;
  }
  
  public Event addNext (Event e){
    boolean first = true;
    for (Event ee : endEvents()){  // this includes alternatives
      if (!first){
        e = e.deepClone();
      } else {
        first = false;      // first one doesn't need a clone
      }
      ee.setNext(e);
      e.setPrev(ee);
    }

    return this;
  }

  public Event addAlternative (Event e){
    Event ea ;
    for (ea = this; ea.alt != null; ea = ea.alt);
    ea.setAlt(e);

    if (next != null){
      e.setNext( next.deepClone());
    }

    return this;
  }
  
  protected static Event createClonedSequence (int firstIdx, int len, Event[] events){
    Event base = events[firstIdx].unlinkedClone();
    Event e = base;

    for (int i = firstIdx+1; i < len; i++) {
      Event ne = events[i].unlinkedClone();
      e.setNext( ne);
      e = ne;
    }
    
    return base;
  }
  
  /**
   * extend this tree with a new path 
   */
  public void addPath (int pathLength, Event... path){
    Event t = this;
    Event pe;
    
    outer:
    for (int i=0; i<pathLength; i++){
      pe = path[i];
      for (Event te = t; te != null; te = te.alt){
        if (pe.equals(te)){      // prefix is in tree
          
          if (te.next == null){  // reached tree leaf
            if (++i < pathLength){ // but the path still has events
              Event tail = createClonedSequence( i, pathLength, path);
              te.setNext(tail);
              tail.setAlt( new NoEvent()); // preserve the tree path
            }
            return;
            
          } else { // there is a next in the tree
            t = te.next;
            
            if (i == pathLength-1){ // but the path is done, add a NoEvent as a next alternative to mark the end
              Event e = t.getLastAlt();
              e.setAlt(new NoEvent());
              return;
              
            } else {
              continue outer;
            }
          }
        }
      }
      
      //--- path prefix was not in tree, append as (last) alternative
      Event tail = createClonedSequence( i, pathLength, path);
      Event e = t.getLastAlt();
      e.setAlt( tail);
      
      return;
    }
  }

  public Event getLastAlt (){
    Event e;
    for (e=this; e.alt != null; e = e.alt);
    return e;
  }
  
  protected void collectEndEvents (List<Event> list, boolean includeNoEvents) {
    if (next != null) {
      next.collectEndEvents(list, includeNoEvents);
      
    } else { // base case: no next
      // strip trailing NoEvents 
      if (prev == null){
        list.add(this); // root NoEvents have to stay
        
      } else { // else we skip trailing NoEvents up to root
        Event ee = this;
        if (!includeNoEvents){
          for (Event e=this; e.prev != null && (e instanceof NoEvent); e = e.prev){
            ee = e.prev;
          }
        }
        list.add(ee);
      }
    }

    if (alt != null) {
      alt.collectEndEvents(list, includeNoEvents);
    }
  }

  public Event endEvent() {
    if (next != null) {
      return next.endEvent();
    } else {
      return this;
    }
  }

  public List<Event> visibleEndEvents(){
    List<Event> list = new ArrayList<Event>();
    collectEndEvents(list, false);
    return list;
  }
 
  
  public List<Event> endEvents(){
    List<Event> list = new ArrayList<Event>();
    collectEndEvents(list, true);
    return list;
  }
  
 
  private void interleave (Event a, Event b, Event[] path, int pathLength, int i, Event result){
    if (a == null && b == null){ // base case
      result.addPath(pathLength, path);
      
    } else {
      if (a != null) {
        path[i] = a;
        interleave(a.prev, b, path, pathLength, i - 1, result);
      }

      if (b != null) {
        path[i] = b;
        interleave(a, b.prev, path, pathLength, i - 1, result);
      }
    }
  }
  
  /**
   * this creates a new tree that contains all paths resulting from
   * all interleavings of all paths of this tree with the specified other events
   * 
   * BEWARE: this is a combinatorial bomb that should only be used if we know all
   * paths are short
   */
  public Event interleave (Event... otherEvents){
    Event t = new NoEvent(); // we need a root for the new tree
    
    Event[] pathBuffer = new Event[32];
    int mergedTrees = 0;
    
    for (Event o : otherEvents){
      List<Event> endEvents = (mergedTrees++ > 0) ? t.visibleEndEvents() : visibleEndEvents();

      for (Event ee1 : endEvents) {
        for (Event ee2 : o.visibleEndEvents()) {
          int n = ee1.getPathLength() + ee2.getPathLength();
          if (n > pathBuffer.length){
            pathBuffer = new Event[n];
          }

          interleave(ee1, ee2, pathBuffer, n, n - 1, t);
        }
      }
    }
        
    return t.alt;
  }
  
  
  
  private void removeSource (Object src, Event[] path, int i, Event result){
    
    if (alt != null){
      alt.removeSource(src, path, i, result);
    }
    
    if (source != src){
      path[i++] = this;
    }
    
    if (next != null){
      next.removeSource(src, path, i, result);
      
    } else { // done, add path to result
      result.addPath( i, path);
    }
  }
  
  /**
   * remove all events from this tree that are from the specified source 
   */
  public Event removeSource (Object src){
    Event base = new NoEvent(); // we need a root to add to
    int maxDepth = getMaxDepth();
    Event[] pathBuffer = new Event[maxDepth];
    
    removeSource( src, pathBuffer, 0, base);
    
    return base.alt;
  }
  
  private void printPath (PrintStream ps, boolean isLast){
    if (prev != null){
      prev.printPath(ps, false);
    }
    
    if (!isNoEvent()){
      ps.print(name);
      if (!isLast){
        ps.print(',');
      }
    }
  }
  
  public void printPath (PrintStream ps){
    printPath(ps, true);
  }

  public String toString(){
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    if (arguments != NO_ARGUMENTS) {
      sb.append('(');
      boolean first = true;
      for (Object a : arguments) {
        if (first){
          first = false;
        } else {
          sb.append(',');
        }
        sb.append(a.toString());
      }
      sb.append(')');
    }
    return sb.toString();
  }

  
  /**
   * upwards path length 
   */
  public int getPathLength(){
    int n=0;
    
    for (Event e=this; e != null; e = e.prev){
      n++;
    }
    
    return n;
  }
  
  
  private int getMaxDepth (int depth){
    int maxAlt = depth;
    int maxNext = depth;
    
    if (alt != null){
      maxAlt = alt.getMaxDepth(depth);
    }
    
    if (next != null){
      maxNext = next.getMaxDepth(depth + 1);
    }
    
    if (maxAlt > maxNext){
      return maxAlt;
    } else {
      return maxNext;
    }
  }
  
  /**
   * maximum downwards tree depth 
   */
  public int getMaxDepth(){
    return getMaxDepth(1);
  }
  
  public Event[] getPath(){
    int n = getPathLength();
    Event[] trace = new Event[n];
    
    for (Event e=this; e != null; e = e.prev){
      trace[--n] = e;
    }
    
    return trace;
  }
  
  public void printTree (PrintStream ps, int level) {
    for (int i = 0; i < level; i++) {
      ps.print(". ");
    }
    
    ps.print(this);
    //ps.print(" [" + prev + ']');
    ps.println();

    if (next != null) {
      next.printTree(ps, level + 1);
    }

    if (alt != null) {
      alt.printTree(ps, level);
    }
  }
  
  public boolean isEndOfTrace (String[] eventNames){
    int n = eventNames.length-1;
    
    for (Event e=this; e!= null; e = e.prev){
      if (e.getName().equals(eventNames[n])){
        n--;
      } else {
        return false;
      }
    }
    
    return (n == 0);
  }
  
  protected void collectTrace (StringBuilder sb, String separator, boolean isLast){
    if (prev != null){
      prev.collectTrace(sb, separator, false);    
    }

    if (!isNoEvent()){
      sb.append(toString());
      
      if (!isLast && separator != null){
        sb.append(separator);        
      }
    }
  }
  
  public String getPathString (String separator){
    StringBuilder sb = new StringBuilder();
    collectTrace( sb, separator, true);
    return sb.toString();
  }
  
  public boolean isNoEvent(){
    return false;
  }
}
