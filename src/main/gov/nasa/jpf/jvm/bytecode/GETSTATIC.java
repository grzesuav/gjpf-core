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

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.SharednessPolicy;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.bytecode.ReadInstruction;


/**
 * Get static fieldInfo from class
 * ..., => ..., value 
 */
public class GETSTATIC extends JVMStaticFieldInstruction  implements ReadInstruction {

  public GETSTATIC(String fieldName, String clsDescriptor, String fieldDescriptor){
    super(fieldName, clsDescriptor, fieldDescriptor);
  }

  @Override
  public Instruction execute (ThreadInfo ti) {
    FieldInfo fieldInfo;

    //--- check if this causes a class load by a user defined classloader
    try {
      fieldInfo = getFieldInfo();
    } catch (LoadOnJPFRequired lre) {
      return ti.getPC();
    }
    
    if (fieldInfo == null) {
      return ti.createAndThrowException("java.lang.NoSuchFieldError",
              (className + '.' + fname));
    }

    //--- check if this has to trigger class initialization
    ClassInfo ciField = fieldInfo.getClassInfo();
    if (!mi.isClinit(ciField) && ciField.pushRequiredClinits(ti)) {
      // note - this returns the next insn in the topmost clinit that just got pushed
      return ti.getPC();
    }
    ElementInfo eiFieldOwner = ciField.getStaticElementInfo();

    //--- check if this breaks the transition
    Scheduler scheduler = ti.getScheduler();
    if (scheduler.canHaveSharedClassCG( ti, this, eiFieldOwner, fieldInfo)){
      eiFieldOwner = scheduler.updateClassSharedness(ti, eiFieldOwner, fieldInfo);
      if (scheduler.setsSharedClassCG( ti, this, eiFieldOwner, fieldInfo)){
        return this; // re-execute
      }
    }
        
    Object fieldAttr = eiFieldOwner.getFieldAttr(fieldInfo);
    StackFrame frame = ti.getModifiableTopFrame();

    if (size == 1) {
      int ival = eiFieldOwner.get1SlotField(fieldInfo);
      lastValue = ival;

      if (fieldInfo.isReference()) {
        frame.pushRef(ival);
      } else {
        frame.push(ival);
      }
      
      if (fieldAttr != null) {
        frame.setOperandAttr(fieldAttr);
      }

    } else {
      long lval = eiFieldOwner.get2SlotField(fieldInfo);
      lastValue = lval;
      
      frame.pushLong(lval);
      
      if (fieldAttr != null) {
        frame.setLongOperandAttr(fieldAttr);
      }
    }
        
    return getNext(ti);
  }
  
  @Override
  public boolean isMonitorEnterPrologue(){
    return GetHelper.isMonitorEnterPrologue(mi, insnIndex);
  }
  
  public int getLength() {
    return 3; // opcode, index1, index2
  }
  
  public int getByteCode () {
    return 0xB2;
  }

  public boolean isRead() {
    return true;
  }
  
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
