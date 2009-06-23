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
 * Get static field from class
 * ..., => ..., value 
 */
public class GETSTATIC extends StaticFieldInstruction {
  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    FieldInfo fi = getFieldInfo();
    if (fi == null) {
      return ti.createAndThrowException("java.lang.NoSuchFieldException",
          (className + '.' + fname));
    }
    
    ClassInfo ci = fi.getClassInfo();
    if (!mi.isClinit(ci) && requiresClinitCalls(ti, ci)) {
      return ti.getPC();
    }

    ElementInfo ei = ks.sa.get(ci.getName());

    if (isNewPorFieldBoundary(ti)) {
      if (createAndSetFieldCG(ss, ei, ti)) {
        return this;
      }
    }
   
    switch (size) {
      case 1:
        int ival = ei.getIntField(fi);
        ti.push(ival, fi.isReference());
        break;
      case 2:
        long lval = ei.getLongField(fi);
        ti.longPush(lval);
        break;
      default:
        throw new JPFException("invalid field type");
    }
    
    Object attr = ei.getFieldAttr(fi);
    if (attr != null){
      ti.setOperandAttrNoClone(attr);
    }
    
    return getNext(ti);
  }
  
  public int getLength() {
    return 3; // opcode, index1, index2
  }
  
  public int getByteCode () {
    return 0xB2;
  }
  
}
