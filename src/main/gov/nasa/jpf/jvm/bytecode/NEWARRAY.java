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

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ConstantPool;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.Types;


/**
 * Create new array
 * ..., count => ..., arrayref
 */
public class NEWARRAY extends Instruction {
  protected String type;

  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
    type = Constants.SHORT_TYPE_NAMES[((org.apache.bcel.generic.NEWARRAY) i).getTypecode()];
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int size = ti.pop();
    DynamicArea heap = DynamicArea.getHeap();
    
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
    
    int arrayRef = heap.newArray(type, size, ti);
    ti.push(arrayRef, true);

    ss.checkGC(); // has to happen after we push the new object ref
    
    return getNext(ti);
  }

  public int getLength() {
    return 2; // opcode, atype
  }
  
  public int getByteCode () {
    return 0xBC;
  }
}
