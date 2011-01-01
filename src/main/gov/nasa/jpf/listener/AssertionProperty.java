//
// Copyright (C) 2007 United States Government as represented by the
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
package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.Heap;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.ATHROW;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.search.Search;

/**
 * this is a property listener that turns thrown AssertionErrors into
 * property violations before they are caught (i.e. potentially
 * change the stack).
 * Besides serving the purpose of eliminating the "catch(Throwable)" case,
 * it can be used in conjunction with "search.multiple_errors=true" to
 * report assertions but otherwise ignore them and go on searching the
 * same path (otherwise, multiple_errors would cause a backtrack)
 */
public class AssertionProperty extends PropertyListenerAdapter {

  boolean goOn;
  boolean caughtAssertion = false;
  String msg;
  
  public AssertionProperty (Config config) {
    goOn = config.getBoolean("ap.go_on",false);
  }
  
  public boolean check(Search search, JVM vm) {
    return !caughtAssertion;
  }

  public String getErrorMessage() {
    return msg;
  }

  protected void warn (String details, Instruction insn){
    // should probably use logging
    System.err.print("WARNING - AssertionError");

    if (details != null){
      System.err.print(": ");
      System.err.print(details);
    }
    System.err.println();

    System.err.print("\tat ");
    System.err.println(insn.getSourceLocation());
  }

  public void executeInstruction (JVM vm){
    Instruction insn = vm.getLastInstruction();
    
    if (insn instanceof ATHROW) {
      ThreadInfo ti = vm.getLastThreadInfo();
      
      Heap heap = vm.getHeap();
      int xobjref = ti.peek();
      ElementInfo ei = heap.get(xobjref);
      ClassInfo ci = ei.getClassInfo();
      if (ci.getName().equals("java.lang.AssertionError")) {
        int msgref = ei.getIntField("detailMessage");
        ElementInfo eiMsg = heap.get(msgref);

        // Ok, arm ourselves
        caughtAssertion = true;
        if (eiMsg != null) {
          msg = eiMsg.asString();
        } else {
          msg = null;
        }

        if (goOn) {
          warn(msg, insn);

          ti.pop(); // ensure operand stack integrity (ATHROW pops)
          ti.skipInstruction(insn.getNext());

        } else {
          ti.skipInstruction(insn);
          ti.breakTransition();
        }
      }
    }
  }
  
  public void reset() {
    caughtAssertion = false;
    msg = null;
  }
}
