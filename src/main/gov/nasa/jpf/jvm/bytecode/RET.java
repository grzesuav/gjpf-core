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

import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;

import org.apache.bcel.classfile.ConstantPool;


/**
 * Return from subroutine
 *   No change
 */
public class RET extends Instruction {
  private int index;

  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
    index = ((org.apache.bcel.generic.RET) i).getIndex();
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo th) {
    return th.getMethod().getInstructionAt(th.getLocalVariable(index));
  }

  public int getLength() {
    return 2; // opcode, index
  }
  
  public int getByteCode () {
    return 0xA9; // ?? wide
  }
}
