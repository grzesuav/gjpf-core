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

import gov.nasa.jpf.util.Printable;

import java.io.PrintWriter;


/**
 * represents the case of an unhandled exception detected by JPF
 * <2do> control flow exception (non local goto), remove this!
 */
@SuppressWarnings("serial")
public class UncaughtException extends RuntimeException implements Printable {

  ThreadInfo thread;
  int xObjRef;          // the exception object reference (that went uncaught)

  String     xClsName;
  String     details;

  //ArrayList  stackTrace; // unused -pcd

  public UncaughtException (ThreadInfo ti, int objRef) {
    thread = ti;
    xObjRef = objRef;
    
    ElementInfo ei = ti.getElementInfo(xObjRef);
    xClsName = ei.getClassInfo().getName();
    details = ei.getStringField("detailMessage");
  }
  
  public String getRawMessage () {
    return xClsName;
  }
  
  public String getMessage () {
    String s = "uncaught exception in thread " + thread.getName() +
              " #" + thread.getIndex() + " : "
              + xClsName;
    
    if (details != null) {
      s += " : \"" + details + "\"";
    }
    
    return s;
  }

  public void printOn (PrintWriter pw) {
    pw.print("uncaught exception in thread ");
    pw.print( thread.getName());
    pw.print(" #");
    pw.print(thread.index);
    pw.print(" : ");

    thread.printStackTrace(pw, xObjRef);
    pw.flush();
  }
}
