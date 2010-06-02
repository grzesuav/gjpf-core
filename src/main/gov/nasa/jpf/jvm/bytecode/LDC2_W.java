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
import gov.nasa.jpf.jvm.Types;

import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Type;


/**
 * Push long or double from runtime constant pool (wide index)
 * ... => ..., value
 */
public class LDC2_W extends Instruction {
  protected Type type;
  protected long value;

  public void setPeer (org.apache.bcel.generic.Instruction insn, ConstantPool cp) {
    ConstantPoolGen cpg = ClassInfo.getConstantPoolGen(cp);
    
    org.apache.bcel.generic.LDC2_W ldc2_w = (org.apache.bcel.generic.LDC2_W) insn;
    int index = ldc2_w.getIndex();
    
    type = ldc2_w.getType(cpg);

    if (type == Type.LONG) {
      value = ((ConstantLong) cp.getConstant( index)).getBytes();
    } else {
      value = Types.doubleToLong(((ConstantDouble) cp.getConstant( index)).getBytes());
    }
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo th) {
    th.longPush(value);

    return getNext(th);
  }

  public int getLength() {
    return 3; // opcode, index1, index2
  }
  
  public int getByteCode () {
    return 0x14;
  }
  
  public Type getType() {
    return type;
  }
  
  public long getValue() {
    return value;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
