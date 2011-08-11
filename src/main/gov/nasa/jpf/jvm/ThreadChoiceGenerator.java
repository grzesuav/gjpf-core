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
package gov.nasa.jpf.jvm;

import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class ThreadChoiceGenerator extends ChoiceGenerator<ThreadInfo> {
  
  /** that's not the best solution, but we can have Thread CGs which
   * are used to switch threads, and others which just return objects.
   * We don't want to double all subclasses based on that, so we bite
   * the bullet and have a field. We could also do this with a decorator,
   * but the normal case is a scheduling point, and we don't want two
   * objects for each of it
   */
  boolean isSchedulingPoint;
    
  protected ThreadChoiceGenerator (String id, boolean isSchedulingPoint) {
    super(id);
    this.isSchedulingPoint = isSchedulingPoint;
  }

  public abstract ThreadInfo getNextChoice ();
  
  public Class<ThreadInfo> getChoiceType() {
    return ThreadInfo.class;
  }
  
  public String toString () {
    StringWriter sw = new StringWriter(100);
    PrintWriter pw = new PrintWriter(sw);
    printOn(pw);
    
    return sw.toString();
  }
  
  public abstract void printOn (PrintWriter pw);
  
  public ThreadChoiceGenerator randomize () {
    return this;
  }

  @Override
  public boolean isSchedulingPoint () {
    return isSchedulingPoint;
  }

  public abstract boolean contains (ThreadInfo ti);
}
