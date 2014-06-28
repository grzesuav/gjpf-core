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

import gov.nasa.jpf.vm.bytecode.StoreInstruction;
import gov.nasa.jpf.vm.ArrayIndexOutOfBoundsExecutiveException;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;


/**
 * abstraction for all array store instructions
 *
 *  ... array, index, <value> => ...
 */
public abstract class ArrayStoreInstruction extends JVMArrayElementInstruction implements StoreInstruction, JVMInstruction {

  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();
    int idx = peekIndex(ti);
    int aref = peekArrayRef(ti); // need to be polymorphic, could be LongArrayStore
    ElementInfo eiArray = ti.getElementInfo(aref);
        
    if (!ti.isFirstStepInsn()){ // first execution, top half
      //--- runtime exceptions
      if (aref == MJIEnv.NULL) {
        return ti.createAndThrowException("java.lang.NullPointerException");
      }
    
      //--- shared access CG
      eiArray = ti.checkSharedArrayAccess( this, eiArray, idx);
      if (ti.hasNextChoiceGenerator()) {
        return this;
      }      
    }
    
    try {
      setArrayElement(ti, frame, eiArray); // this pops operands
    } catch (ArrayIndexOutOfBoundsExecutiveException ex) { // at this point, the AIOBX is already processed
      return ex.getInstruction();
    }

    return getNext(ti);
  }

  protected void setArrayElement (ThreadInfo ti, StackFrame frame, ElementInfo eiArray) throws ArrayIndexOutOfBoundsExecutiveException {
    int esize = getElementSize();
    Object attr = esize == 1 ? frame.getOperandAttr() : frame.getLongOperandAttr();
    
    popValue(frame);
    index = frame.pop();
    // don't set 'arrayRef' before we do the CG checks (would kill loop optimization)
    arrayRef = frame.pop();

    eiArray = eiArray.getModifiableInstance();
    setField(eiArray, index);
    eiArray.setElementAttrNoClone(index,attr); // <2do> what if the value is the same but not the attr?
  }
  
  /**
   * this is for pre-exec use
   */
  @Override
  public int peekArrayRef(ThreadInfo ti) {
    return ti.getTopFrame().peek(2);
  }

  @Override
  public int peekIndex(ThreadInfo ti){
    return ti.getTopFrame().peek(1);
  }

  protected abstract void popValue(StackFrame frame);
 
  protected abstract void setField (ElementInfo e, int index)
                    throws ArrayIndexOutOfBoundsExecutiveException;


  @Override
  public boolean isRead() {
    return false;
  }
  
  @Override
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

}
