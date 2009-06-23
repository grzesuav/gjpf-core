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

import gov.nasa.jpf.jvm.*;

import org.apache.bcel.classfile.ConstantPool;


/**
 * Create new array of reference
 * ..., count => ..., arrayref
 */
public class ANEWARRAY extends Instruction {
  protected String type;

  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
    type = cp.constantToString(cp.getConstant(
                                     ((org.apache.bcel.generic.ANEWARRAY) i).getIndex()));

    if (!type.startsWith("[")) {
      type = "L" + type + ";";
    }
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    DynamicArea heap = DynamicArea.getHeap();    
    int size = ti.pop();
    
    // there is no clinit for array classes, but we still have  to create a class object
    String clsName = "[" + type;
    ClassInfo ci = ClassInfo.getClassInfo(clsName);
    if (!ci.isInitialized()) {
      ci.loadAndInitialize(ti);
    }
    
    if (heap.getOutOfMemory()) { // simulate OutOfMemoryError
      return ti.createAndThrowException("java.lang.OutOfMemoryError",
                                        "trying to allocate new " +
                                          Types.getTypeName(type) +
                                        "[" + size + "]");
    }
    
    // pushes the object reference on the top stack frame
    ti.push(heap.newArray(type, size, ti), true);

    ss.checkGC(); // has to happen after we push the new object ref
    
    return getNext(ti);
  }

  public int getLength () {
    return 3; // opcode, index1, index2
  }
  
  public int getByteCode () {
    return 0xBD;
  }
}
