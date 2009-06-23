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

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * Set static field in class
 * ..., value => ...
 */
public class PUTSTATIC extends StaticFieldInstruction implements StoreInstruction
{
  protected long lastValue;

  public long getLastValue() {
    return lastValue;
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    FieldInfo fi = getFieldInfo();
    if (fi == null) {
      return ti.createAndThrowException("java.lang.NoSuchFieldException",
          (className + '.' + fname));
    }

    ClassInfo ci = fi.getClassInfo();

    // this tries to avoid endless recursion, but is too restrictive, and
    // causes NPE's with the infamous, synthetic  'class$0' fields
    if (!mi.isClinit() && requiresClinitCalls(ti, ci)) {
      return ti.getPC();
    }

    ElementInfo ei = ks.sa.get(ci.getName());

    if (isNewPorFieldBoundary(ti)) {
      if (createAndSetFieldCG(ss, ei, ti)) {
        return this;
      }
    }

    Object attr = null; // attr handling has to be consistent with PUTFIELD

    switch (fi.getStorageSize()) {
      case 1:
        attr = ti.getOperandAttr();

        int ival = ti.pop();
        lastValue = ival;

        if (fi.isReference()) {
          ei.setReferenceField(fi, ival);
        } else {
          ei.setIntField(fi, ival);
        }
        break;

      case 2:
        attr = ti.getLongOperandAttr();

        long lval = ti.longPop();
        lastValue = lval;

        ei.setLongField(fi, lval);
        break;
      default:
        throw new JPFException("invalid field type");
    }

    // this is kind of policy, but it seems more natural to overwrite
    // (if we want to accumulate, this has to happen in ElementInfo/Fields
    ei.setFieldAttrNoClone(fi, attr);

    return getNext(ti);
  }

  public int getLength() {
    return 3; // opcode, index1, index2
  }

  public int getByteCode () {
    return 0xB3;
  }

}