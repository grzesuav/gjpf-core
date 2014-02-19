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
package gov.nasa.jpf.jvm.bytecode;

import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;


/**
 * Get length of array 
 * ..., arrayref => ..., length
 */
public class ARRAYLENGTH extends Instruction implements JVMInstruction {
    
  protected int arrayRef;

  /**
   * only makes sense from an executeInstruction() or instructionExecuted() listener,
   * it is undefined outside of insn exec notifications
   */
  public int getArrayRef (ThreadInfo ti){
    if (ti.isPreExec()){
      return peekArrayRef(ti);
    } else {
      return arrayRef;
    }
  }

  public ElementInfo peekArrayElementInfo (ThreadInfo ti){
    int aref = getArrayRef(ti);
    return ti.getElementInfo(aref);
  }
  
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();

    arrayRef = frame.pop();

    if (arrayRef == MJIEnv.NULL){
      return ti.createAndThrowException("java.lang.NullPointerException",
                                        "array length of null object");
    }

    ElementInfo ei = ti.getElementInfo(arrayRef);
    frame.push(ei.arrayLength(), false);

    return getNext(ti);
  }
  
  @Override
  public int getByteCode () {
    return 0xBE;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  protected int peekArrayRef (ThreadInfo ti) {
    return ti.getTopFrame().peek();
  }
}
