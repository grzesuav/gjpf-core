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
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.SharednessPolicy;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.bytecode.WriteInstruction;
import gov.nasa.jpf.vm.choice.ExposureCG;


/**
 * Set static field in class
 * ..., value => ...
 */
public class PUTSTATIC extends JVMStaticFieldInstruction implements WriteInstruction {

  public PUTSTATIC(String fieldName, String clsDescriptor, String fieldDescriptor){
    super(fieldName, clsDescriptor, fieldDescriptor);
  }

  /**
   * where do we get the value from?
   * NOTE: only makes sense in a executeInstruction() context 
   */
  public int getValueSlot (StackFrame frame){
    return frame.getTopPos();
  }
    
  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();
    FieldInfo fieldInfo;
    
    //--- check if this causes a class load by a user defined classloader
    try {
      fieldInfo = getFieldInfo();
    } catch (LoadOnJPFRequired lre) {
      return ti.getPC();
    }
    
    if (fieldInfo == null) {
      return ti.createAndThrowException("java.lang.NoSuchFieldError", (className + '.' + fname));
    }

    //--- check if this has to trigger class initialization
    ClassInfo ciField = fieldInfo.getClassInfo();
    if (!mi.isClinit(ciField) && ciField.pushRequiredClinits(ti)) {
      return ti.getPC(); // this returns the next insn in the topmost clinit that just got pushed
    }
    ElementInfo eiFieldOwner = ciField.getModifiableStaticElementInfo();

    //--- check scheduling point due to shared class access
    Scheduler scheduler = ti.getScheduler();
    if (scheduler.canHaveSharedClassCG( ti, this, eiFieldOwner, fieldInfo)){
      eiFieldOwner = scheduler.updateClassSharedness(ti, eiFieldOwner, fi);
      if (scheduler.setsSharedClassCG( ti, this, eiFieldOwner, fieldInfo)){
        return this; // re-execute
      }
    }
    
    // check if this gets re-executed from a exposure CG (which already did the assignment
    if (frame.getAndResetFrameAttr(InstructionState.class) == null){
      lastValue = PutHelper.setField(ti, frame, eiFieldOwner, fieldInfo);
    }
      
    //--- check scheduling point due to exposure through shared class
    if (isReferenceField()){
      int refValue = frame.peek();
      if (refValue != MJIEnv.NULL){
        ElementInfo eiExposed = ti.getElementInfo(refValue);
        if (scheduler.setsSharedClassExposureCG(ti,this,eiFieldOwner,fieldInfo,eiExposed)){
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
      frame.pop(); // .. val => ..
    } else {
      frame.pop(2);  // .. highVal, lowVal => ..
    }
  }
  
  public int getLength() {
    return 3; // opcode, index1, index2
  }

  public int getByteCode () {
    return 0xB3;
  }

  public boolean isRead() {
    return false;
  }
  
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
