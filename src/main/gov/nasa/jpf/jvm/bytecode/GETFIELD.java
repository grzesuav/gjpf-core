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
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.SharednessPolicy;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.bytecode.ReadInstruction;


/**
 * Fetch field from object
 * ..., objectref => ..., value
 */
public class GETFIELD extends JVMInstanceFieldInstruction implements ReadInstruction {

  public GETFIELD (String fieldName, String classType, String fieldDescriptor){
    super(fieldName, classType, fieldDescriptor);
  }
  
  
  @Override
  public int getObjectSlot (StackFrame frame){
    return frame.getTopPos();
  }
  
  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();
    ElementInfo eiFieldOwner;
    int objRef = frame.peek(); // don't pop yet, we might re-enter
    lastThis = objRef;

    if (!ti.isFirstStepInsn()){
      if (objRef == MJIEnv.NULL) {
        return ti.createAndThrowException("java.lang.NullPointerException",
                "referencing field '" + fname + "' on null object");
      }

      eiFieldOwner = ti.getElementInfo(objRef);
      FieldInfo fi = getFieldInfo();
      if (fi == null) {
        return ti.createAndThrowException("java.lang.NoSuchFieldError",
                "referencing field '" + fname + "' in " + eiFieldOwner);
      }

      if (ti.isInstanceSharednessRelevant(this, eiFieldOwner, fi)) {
        // check for non-lock protected shared object access, breaking before the field is written
        eiFieldOwner = ti.checkSharedInstanceFieldAccess(this, eiFieldOwner, fi);
        if (ti.getVM().hasNextChoiceGenerator()) {
          return this;
        }
      }
      
    } else {
      eiFieldOwner = ti.getElementInfo(objRef);      
    }

    frame.pop(); // Ok, now we can remove the object ref from the stack
    Object attr = eiFieldOwner.getFieldAttr(fi);

    // We could encapsulate the push in ElementInfo, but not the GET, so we keep it at a similiar level
    if (fi.getStorageSize() == 1) { // 1 slotter
      int ival = eiFieldOwner.get1SlotField(fi);
      lastValue = ival;
      
      if (fi.isReference()){
        frame.pushRef(ival);
        
      } else {
        frame.push(ival);
      }
      
      if (attr != null) {
        frame.setOperandAttr(attr);
      }

    } else {  // 2 slotter
      long lval = eiFieldOwner.get2SlotField(fi);
      lastValue = lval;

      frame.pushLong( lval);
      if (attr != null) {
        frame.setLongOperandAttr(attr);
      }
    }

    return getNext(ti);
  }

  public ElementInfo peekElementInfo (ThreadInfo ti) {
    StackFrame frame = ti.getTopFrame();
    int objRef = frame.peek();
    ElementInfo ei = ti.getElementInfo(objRef);
    return ei;
  }

  @Override
  public boolean isMonitorEnterPrologue(){
    return GetHelper.isMonitorEnterPrologue(mi, insnIndex);
  }
  
  public int getLength() {
    return 3; // opcode, index1, index2
  }

  public int getByteCode () {
    return 0xB4;
  }

  public boolean isRead() {
    return true;
  }

  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
