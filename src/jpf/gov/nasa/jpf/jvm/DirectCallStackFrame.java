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

import gov.nasa.jpf.jvm.bytecode.Instruction;

/**
 * this is a StackFrame that can dynamically grow its operand stack size (and associated
 * operand attributes). To be used for floating calls, where we don't want to mis-use the
 * stack of the currently executing bytecode method, since it might not have enough
 * operand stack space. This class is basically an inheritance-decorator for StackFrame
 * 
 * Note that these frames do not appear in a Thread's call stack!
 */
public class DirectCallStackFrame extends StackFrame {

  // in what (linear) increments do we grow the operand and local size
  static final int OPERAND_INC = 5;
  static final int LOCAL_INC = 5;
  
  Instruction nextPc;
    
  public DirectCallStackFrame (MethodInfo stub) {
    super(stub, null);
  }
  
  public DirectCallStackFrame (MethodInfo stub, Instruction nextInsn) {
    super(stub, null);
    
    nextPc = nextInsn;
  }

  private DirectCallStackFrame () {
    // just here for cloning purposes
  }
  
  public void reset() {
    pc = mi.getInstruction(0);
  }
  
  public Instruction getNextPC() {
    return nextPc;
  }
  
  public boolean isDirectCallFrame() {
    return true;
  }
      
  public String getClassName() {
    return "<direct call>";
  }
  
  public String getSourceFile () {
    return "<direct call>"; // we don't have any
  }
  
  private void growOperands () {
    int newLen = operands.length + OPERAND_INC; // should grow linearly

    int[] newOperands = new int[newLen];
    System.arraycopy(operands, 0, newOperands, 0, operands.length);
    operands = newOperands;
        
    boolean[] newIsOperandRef = new boolean[newLen];
    System.arraycopy(isOperandRef, 0, newIsOperandRef, 0, isOperandRef.length);
    isOperandRef = newIsOperandRef;
    
    if (operandAttr != null){
      Object[] newOperandAttr = new Object[newLen];
      System.arraycopy(operandAttr, 0, newOperandAttr, 0, operandAttr.length);
      operandAttr = newOperandAttr;
    }
  }
    
  public void push (int v, boolean ref) {
    if (top >= (operands.length-1)) {
      growOperands();
    }
    super.push(v,ref);
  }

  // those are of less interest, unless somebody creates a method on the fly
  
  private void growLocals (int idx) {
    int newLen = idx + LOCAL_INC;
    int[] newLocals = new int[newLen];
    System.arraycopy(locals, 0, newLocals, 0, locals.length);
    locals = newLocals;
    
    boolean[] newIsLocalRef = new boolean[newLen];
    System.arraycopy(isLocalRef, 0, newIsLocalRef, 0, isLocalRef.length);
    isLocalRef = newIsLocalRef;
    
    if (localAttr != null){
      Object[] newLocalAttr = new Object[newLen];
      System.arraycopy(localAttr, 0, newLocalAttr, 0, localAttr.length);
      localAttr = newLocalAttr;
    }

  }

  public void setLocalVariable (int index, int v, boolean ref) {
    if (index >= locals.length) {
      growLocals(index);
    }
    super.setLocalVariable( index, v, ref);
  }
  
  public void setLongLocalVariable (int index, long v) {
    if (index+1 >= locals.length) {
      growLocals(index+1);
    }
    super.setLongLocalVariable(index, v);
  }  
  
  public void dup () {
    if (top >= (operands.length-1)) {
      growOperands();
    }
    super.dup();
  }
  
  public void dup2 () {
    if (top >= (operands.length-2)) {
      growOperands();
    }
    super.dup2();    
  }
  
  // <2do> and a couple more we still have to do
}
