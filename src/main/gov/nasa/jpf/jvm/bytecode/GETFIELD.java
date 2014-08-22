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
import gov.nasa.jpf.vm.Scheduler;
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
    int objRef = frame.peek(); // don't pop yet, we might re-enter
    lastThis = objRef;

    //--- check for obvious exceptions
    if (objRef == MJIEnv.NULL) {
      return ti.createAndThrowException("java.lang.NullPointerException",
              "referencing field '" + fname + "' on null object");
    }

    ElementInfo eiFieldOwner = ti.getElementInfo(objRef);
    FieldInfo fieldInfo = getFieldInfo();
    if (fieldInfo == null) {
      return ti.createAndThrowException("java.lang.NoSuchFieldError",
              "referencing field '" + fname + "' in " + eiFieldOwner);
    }

    //--- check for potential transition breaks (be aware everything above gets re-executed)
    Scheduler scheduler = ti.getScheduler();
    if (scheduler.canHaveSharedObjectCG( ti, this, eiFieldOwner, fieldInfo)){
      eiFieldOwner = scheduler.updateObjectSharedness( ti, eiFieldOwner, fieldInfo);
      if (scheduler.setsSharedObjectCG( ti, this, eiFieldOwner, fieldInfo)){
        return this; // re-execute
      }
    }
    
    frame.pop(); // Ok, now we can remove the object ref from the stack
    Object fieldAttr = eiFieldOwner.getFieldAttr(fieldInfo);

    // We could encapsulate the push in ElementInfo, but not the GET, so we keep it at the same level
    if (fieldInfo.getStorageSize() == 1) { // 1 slotter
      int ival = eiFieldOwner.get1SlotField(fieldInfo);
      lastValue = ival;
      
      if (fieldInfo.isReference()){
        frame.pushRef(ival);
        
      } else {
        frame.push(ival);
      }
      
      if (fieldAttr != null) {
        frame.setOperandAttr(fieldAttr);
      }

    } else {  // 2 slotter
      long lval = eiFieldOwner.get2SlotField(fieldInfo);
      lastValue = lval;

      frame.pushLong( lval);
      if (fieldAttr != null) {
        frame.setLongOperandAttr(fieldAttr);
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
