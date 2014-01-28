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

  public Event (String name){
    this.name = name;
    this.arguments = NO_ARGUMENTS;
  }

  public Event(String name, Object[] arguments) {
    this.name = name;
    this.arguments = arguments != null ? arguments : NO_ARGUMENTS;
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

  public int getNumberOfAlternatives(){
    int n = 0;
    for (Event e = alt; e != null; e = e.alt) {
      n++;
    }

    return n;
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
  
  public Event addNext (Event e){
    boolean first = true;
    for (Event ee : endEvents()){
      if (!first){
        e = e.deepClone();
      } else {
        first = false;      // first one doesn't need a clone
      }
      ee.setNext(e);
      e.setPrev(ee);

      for (Event ea = e.alt; ea != null; ea = ea.alt){
        ea.setPrev(ee);
      }
    }

    for (Event ea = alt; ea != null; ea = ea.alt){
      e = e.deepClone();
      ea.setNext(e);
      e.setPrev(ea);
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


  protected void collectEndStates (List<Event> list) {
    if (next != null) {
      next.collectEndStates(list);
    } else {
      list.add(this);
    }

    if (alt != null) {
      alt.collectEndStates(list);
    }
  }

  public Event endEvent() {
    if (next != null) {
      return next.endEvent();
    } else {
      return this;
    }
  }

  public List<Event> endEvents(){
    List<Event> list = new ArrayList<Event>();
    collectEndStates(list);
    return list;
  }

  public void printTrace (PrintStream ps){
    if (prev != null){
      prev.printTrace(ps);
      ps.print(',');
    }
    ps.print(name);
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

  
  public int getTraceLength(){
    int n=0;
    
    for (Event e=this; e != null; e = e.prev){
      n++;
    }
    
    return n;
  }
  
  public Event[] getTrace(){
    int n = getTraceLength();
    Event[] trace = new Event[n];
    
    for (Event e=this; e != null; e = e.prev){
      trace[--n] = e;
    }
    
    return trace;
  }
  
  public void printTree (PrintStream ps, int level) {
    for (int i = 0; i < level; i++) {
      ps.print("⋅ ");
    }
    ps.println(this);

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
  
  protected void collectTrace (StringBuilder sb, String separator){
    if (prev != null){
      prev.collectTrace(sb, separator);
      
      if (separator != null){
        sb.append(separator);
      }
    }
    
    sb.append(toString());
  }
  
  public String getTrace (String separator){
    StringBuilder sb = new StringBuilder();
    collectTrace( sb, separator);
    return sb.toString();
  }
}
