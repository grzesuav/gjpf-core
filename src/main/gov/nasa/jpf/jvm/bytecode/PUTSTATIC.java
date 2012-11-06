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

import gov.nasa.jpf.jvm.ClassLoaderInfo;
import gov.nasa.jpf.jvm.Instruction;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.LoadOnJPFRequired;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * Set static field in class
 * ..., value => ...
 */
public class PUTSTATIC extends StaticFieldInstruction implements StoreInstruction {

  public PUTSTATIC() {}

  public PUTSTATIC(String fieldName, String clsDescriptor, String fieldDescriptor){
    super(fieldName, clsDescriptor, fieldDescriptor);
  }

  public Instruction execute (ThreadInfo ti) {
    ClassInfo clsInfo;

    // resolve the class of the referenced field first
    ClassInfo cls = ti.getMethod().getClassInfo();
    try {
      ci = cls.resolveReferencedClass(className);
    } catch(LoadOnJPFRequired lre) {
      return ti.getPC();
    }

    FieldInfo fieldInfo = getFieldInfo();
    if (fieldInfo == null) {
      return ti.createAndThrowException("java.lang.NoSuchFieldError",
          (className + '.' + fname));
    }
    
    // this can be actually different (can be a base)
    clsInfo = fi.getClassInfo();

    // this tries to avoid endless recursion, but is too restrictive, and
    // causes NPE's with the infamous, synthetic  'class$0' fields
    if (!mi.isClinit(clsInfo) && requiresClinitExecution(ti, clsInfo)) {
      return ti.getPC();
    }

    ElementInfo ei = clsInfo.getModifiableStaticElementInfo();

    if (isNewPorFieldBoundary(ti)) {
      if (createAndSetFieldCG( ei, ti)) {
        return this;
      }
    }

    StackFrame frame = ti.getModifiableTopFrame();
    Object attr = null; // attr handling has to be consistent with PUTFIELD

    if (fi.getStorageSize() == 1) {
      attr = ti.getOperandAttr();

      int ival = frame.pop();
      lastValue = ival;

      if (fi.isReference()) {
        ei.setReferenceField(fi, ival);
      } else {
        ei.set1SlotField(fi, ival);
      }

    } else {
      attr = frame.getLongOperandAttr();

      long lval = frame.popLong();
      lastValue = lval;

      ei.set2SlotField(fi, lval);
    }

    // this is kind of policy, but it seems more natural to overwrite
    // (if we want to accumulate, this has to happen in ElementInfo/Fields
    ei.setFieldAttr(fi, attr);

    return getNext(ti);
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
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
