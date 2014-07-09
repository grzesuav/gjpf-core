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

import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.MJIEnv;
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
    
    if (!ti.isFirstStepInsn()) { // top half
      FieldInfo fieldInfo;
    
      // handle class loading
      try {
        fieldInfo = getFieldInfo();
      } catch(LoadOnJPFRequired lre) {
        return ti.getPC();
      }
      
      if (fieldInfo == null) {
        return ti.createAndThrowException("java.lang.NoSuchFieldError", (className + '.' + fname));
      }
      
      // handle static class initialization
      ClassInfo ciField = fi.getClassInfo();
      if (!mi.isClinit(ciField) && ciField.pushRequiredClinits(ti)) {
        // note - this returns the next insn in the topmost clinit that just got pushed
        return ti.getPC();
      }
            
      ElementInfo eiFieldOwner = ciField.getModifiableStaticElementInfo();
      
      if (ti.isStaticSharednessRelevant(this, eiFieldOwner, fieldInfo)) {
        // check for non-lock protected shared object access, breaking before the field is written
        eiFieldOwner = ti.checkSharedStaticFieldAccess(this, eiFieldOwner, fieldInfo);
        if (ti.hasNextChoiceGenerator()) {
          return this;
        }

        // handle object exposure (see PUTFIELD)
        if (isReferenceField()) {
          int refValue = frame.peek();
          ti.checkStaticFieldObjectExposure(this, eiFieldOwner, fieldInfo, refValue);
          if (ti.hasNextChoiceGenerator()) {
            lastValue = PutHelper.setReferenceField(ti, frame, eiFieldOwner, fieldInfo);
            ti.markExposure(frame); // make sure we don't overwrite external changes in bottom half
            return this;
          }
        }
      }
        
      // regular case - non shared or lock protected field
      lastValue = PutHelper.setField( ti, frame, eiFieldOwner, fieldInfo);
      
    } else { // bottom-half - re-execution
      // check if the value was already set in the top half
      // NOTE - we can also get here because of a non-exposure CG (thread termination, interrupt etc.)
      if (!ti.checkAndResetExposureMark(frame)){
        ClassInfo ciField = fi.getClassInfo();
        ElementInfo eiFieldOwner = ciField.getModifiableStaticElementInfo();
        lastValue = PutHelper.setField(ti, frame, eiFieldOwner, fi);
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
