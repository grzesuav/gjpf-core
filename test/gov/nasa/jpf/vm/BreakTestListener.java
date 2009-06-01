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
package gov.nasa.jpf.vm;

import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;

public class BreakTestListener extends ListenerAdapter {
  
  public static int nCG; // braindead, just to check from outside
  
  public BreakTestListener() {
    nCG = 0;
  }
  
  public void instructionExecuted (JVM vm) {
    Instruction insn = vm.getLastInstruction();
    
    if (insn instanceof PUTFIELD) {  // break on field access
      FieldInfo fi = ((PUTFIELD)insn).getFieldInfo();
      if (fi.getClassInfo().getName().equals("gov.nasa.jpf.vm.BreakTest")) {
        System.out.println("# breaking after: " + insn);
        vm.getLastThreadInfo().reschedule(true);
      }
      
    } else if (insn instanceof InvokeInstruction){ // break on method call
      InvokeInstruction call = (InvokeInstruction)insn;
      if ("foo".equals(call.getInvokedMethod().getName())){
        System.out.println("# breaking (&pruning) after: " + insn);
        vm.getLastThreadInfo().reschedule(true);
        vm.getSystemState().setIgnored(true); // ??? D&C's bug
      }
    }
  }
  
  public void choiceGeneratorSet (JVM vm) {
    //System.out.println("CG set: " + vm.getLastChoiceGenerator());
    nCG++;
  }
  
  public void choiceGeneratorAdvanced (JVM vm) {
    System.out.println("CG advanced: " + vm.getLastChoiceGenerator());    
  }
}
