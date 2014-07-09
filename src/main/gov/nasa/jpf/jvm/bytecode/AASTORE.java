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

import gov.nasa.jpf.vm.ArrayIndexOutOfBoundsExecutiveException;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;


/**
 * Store into reference array
 * ..., arrayref, index, value  => ...
 */
public class AASTORE extends ArrayStoreInstruction {

  int value;

  @Override
  public boolean isReferenceArray() {
    return true;
  }
  
  protected void popValue(StackFrame frame){
    value = frame.pop();
  }

  protected void setField (ElementInfo ei, int index) throws ArrayIndexOutOfBoundsExecutiveException {
    ei.checkArrayBounds(index);
    ei.setReferenceElement(index, value);
  }
  
  /**
   * overridden because AASTORE can cause ArrayStoreExceptions and exposure CGs 
   */
  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();
    int refValue = frame.peek();
    int idx = frame.peek(1);
    int aref = frame.peek(2);
    ElementInfo eiArray = ti.getElementInfo(aref);
        
    if (!ti.isFirstStepInsn()){ // first execution, top half
      //--- runtime exceptions
      if (aref == MJIEnv.NULL) {
        return ti.createAndThrowException("java.lang.NullPointerException");
      }
      Instruction xInsn = checkArrayStoreException(ti, frame, eiArray);
      if (xInsn != null){
        return xInsn;
      }
    
      //--- shared access CG
      boolean skipExposure = false;
      if (ti.isArraySharednessRelevant(this, eiArray)){
        eiArray = ti.checkSharedArrayAccess(this, eiArray, idx);
        if (ti.hasNextChoiceGenerator()) {
          return this;
        }
      } else {
        skipExposure = true;
      }
           
      try {
        setArrayElement( ti, frame, eiArray); // this pops operands
      } catch (ArrayIndexOutOfBoundsExecutiveException ex) { // at this point, the AIOBX is already processed
        return ex.getInstruction();
      }
      
      //--- exposure
      if (!skipExposure){
        ti.checkArrayObjectExposure(this, eiArray, idx, refValue);
        if (ti.hasNextChoiceGenerator()) {
          ti.markExposure(frame);
          return this;
        }
      }

      return getNext(ti);
      
    } else { // re-execution, bottom half (was either shared array or object exposure)
      if (!ti.checkAndResetExposureMark(frame)){
        // if this wasn't an exposure we still have to set the array element
        try {
          setArrayElement( ti, frame, eiArray); // this pops operands
        } catch (ArrayIndexOutOfBoundsExecutiveException ex) { // at this point, the AIOBX is already processed
          return ex.getInstruction();
        }        
      }
      // otherwise the element has already been set
      return getNext(ti);      
    }
  }

  protected Instruction checkArrayStoreException(ThreadInfo ti, StackFrame frame, ElementInfo ei){
    ClassInfo c = ei.getClassInfo();
    int refVal = frame.peek();
    
    if (refVal != MJIEnv.NULL) { // no checks for storing 'null'
      ClassInfo elementCi = ti.getClassInfo(refVal);
      ClassInfo arrayElementCi = c.getComponentClassInfo();
      if (!elementCi.isInstanceOf(arrayElementCi)) {
        String exception = "java.lang.ArrayStoreException";
        String exceptionDescription = elementCi.getName();
        return ti.createAndThrowException(exception, exceptionDescription);
      }
    }

    return null;
  }


  public int getByteCode () {
    return 0x53;
  }

  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
