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

import gov.nasa.jpf.util.InstructionState;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.SharednessPolicy;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.bytecode.WriteInstruction;
import gov.nasa.jpf.vm.choice.ExposureCG;

/**
 * Set field in object
 * ..., objectref, value => ...
 */
public class PUTFIELD extends JVMInstanceFieldInstruction implements WriteInstruction {

  public PUTFIELD(String fieldName, String clsDescriptor, String fieldDescriptor){
    super(fieldName, clsDescriptor, fieldDescriptor);
  }  

  @Override
  public int getObjectSlot (StackFrame frame){
    return frame.getTopPos() - size;
  }

  /**
   * where do we get the value from?
   * NOTE: only makes sense in a executeInstruction() context 
   */
  public int getValueSlot (StackFrame frame){
    return frame.getTopPos();
  }

  
  /**
   * where do we write to?
   * NOTE: this should only be used from a executeInstruction()/instructionExecuted() context
   */
  public ElementInfo getElementInfo(ThreadInfo ti){
    if (isCompleted(ti)){
      return ti.getElementInfo(lastThis);
    } else {
      return peekElementInfo(ti); // get it from the stack
    }
  }

  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();
    int objRef = frame.peek( size);
    lastThis = objRef;
    
    if (objRef == MJIEnv.NULL) {
      return ti.createAndThrowException("java.lang.NullPointerException", "referencing field '" + fname + "' on null object");
    }

    ElementInfo eiFieldOwner = ti.getModifiableElementInfo(objRef);
    FieldInfo fieldInfo = getFieldInfo();
    if (fieldInfo == null) {
      return ti.createAndThrowException("java.lang.NoSuchFieldError", "no field " + fname + " in " + eiFieldOwner);
    }
    
    //--- check scheduling point due to shared object access
    Scheduler scheduler = ti.getScheduler();
    if (scheduler.canHaveSharedObjectCG(ti,this,eiFieldOwner,fieldInfo)){
      eiFieldOwner = scheduler.updateObjectSharedness( ti, eiFieldOwner, fi);
      if (scheduler.setsSharedObjectCG( ti, this, eiFieldOwner, fieldInfo)){
        return this; // re-execute
      }
    }
    
    // this might be re-executed
    if (frame.getAndResetFrameAttr(InstructionState.class) == null){
      lastValue = PutHelper.setField(ti, frame, eiFieldOwner, fieldInfo);
    }
    
    //--- check scheduling point due to exposure through shared object
    if (isReferenceField()){
      int refValue = frame.peek();
      if (refValue != MJIEnv.NULL){
        ElementInfo eiExposed = ti.getElementInfo(refValue);
        if (scheduler.setsSharedObjectExposureCG(ti, this, eiFieldOwner, fi, eiExposed)){
          frame.addFrameAttr( InstructionState.processed);
          return this; // re-execute AFTER assignment
        }
      }
    }
    
    popOperands(frame);      
    return getNext();
  }

  protected void popOperands (StackFrame frame){
    if (size == 1){
      frame.pop(2); // .. objref, val => ..
    } else {
      frame.pop(3); // .. objref, highVal,lowVal => ..    
    }
  }
    
  public ElementInfo peekElementInfo (ThreadInfo ti) {
    FieldInfo fi = getFieldInfo();
    int storageSize = fi.getStorageSize();
    int objRef = ti.getTopFrame().peek( (storageSize == 1) ? 1 : 2);
    ElementInfo ei = ti.getElementInfo( objRef);

    return ei;
  }


  public int getLength() {
    return 3; // opcode, index1, index2
  }

  public int getByteCode () {
    return 0xB5;
  }

  public boolean isRead() {
    return false;
  }

  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}



