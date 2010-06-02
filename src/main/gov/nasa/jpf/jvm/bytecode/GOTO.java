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
 * Branch always
 * No change
 */
public class GOTO extends Instruction {
  protected int targetPosition;
  Instruction target;

  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
    targetPosition = ((org.apache.bcel.generic.GOTO) i).getTarget().getPosition();
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo th) {
    return getTarget();
  }

  public boolean isBackJump () {
    return (targetPosition <= position);
  }
  
  public Instruction getTarget() {
    if (target == null) {
      target = mi.getInstructionAt(targetPosition);
    }
    return target;
  }

  public int getLength() {
    return 3; // opcode, bb1, bb2
  }
  
  public int getByteCode () {
    return 0xA7;
  }
  
  public String toString () {
    if (asString == null) {
      asString = getMnemonic() + " " + getTarget().getOffset();
    }
    return asString;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
