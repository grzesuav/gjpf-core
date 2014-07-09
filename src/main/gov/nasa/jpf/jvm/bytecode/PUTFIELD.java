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
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
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
    
    if (!ti.isFirstStepInsn()) { // top half
      // check for obvious exceptions
      if (objRef == MJIEnv.NULL) {
        return ti.createAndThrowException("java.lang.NullPointerException",
                                   "referencing field '" + fname + "' on null object");
      }
      
      ElementInfo eiFieldOwner = ti.getModifiableElementInfo(objRef);
      FieldInfo fi = getFieldInfo();
      if (fi == null) {
        return ti.createAndThrowException("java.lang.NoSuchFieldError", 
            "no field " + fname + " in " + eiFieldOwner);
      }

      if (ti.isInstanceSharednessRelevant(this, eiFieldOwner, fi)) {

        // check for non-lock protected shared object access, breaking before the field is written
        eiFieldOwner = ti.checkSharedInstanceFieldAccess(this, eiFieldOwner, fi);
        if (ti.hasNextChoiceGenerator()) {
          return this;
        }

        // if this is an object exposure (non-shared object gets stored in field of
        // shared object and hence /could/ become shared itself), we have to change the
        // field value /before/ we do an exposure break. However, we should only change it
        // once, if we re-write during re-execution we change program behavior in
        // case there is a race for this field, leading to false positives etc.
        if (isReferenceField()) {
          int refValue = frame.peek();
          ti.checkInstanceFieldObjectExposure(this, eiFieldOwner, fi, refValue);
          if (ti.hasNextChoiceGenerator()) {
            lastValue = PutHelper.setReferenceField(ti, frame, eiFieldOwner, fi);
            ti.markExposure(frame); // make sure we don't overwrite external changes in bottom half
            return this;
          }
        }
      }
      
      // regular case - non shared or lock protected field, no re-execution
      lastValue = PutHelper.setField( ti, frame, eiFieldOwner, fi);
      
    } else { // bottom-half - re-execution
      // check if the value was already set in the top half
      // NOTE - we can also get here because of a non-exposure CG (thread termination, interrupt etc.)
      if (!ti.checkAndResetExposureMark(frame)){
        ElementInfo eiFieldOwner = ti.getModifiableElementInfo(objRef);
        lastValue = PutHelper.setField(ti, frame, eiFieldOwner, fi);
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



