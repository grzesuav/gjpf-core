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
 * class abstracting instructions that access local variables, to keep
 * track of slot/varname mapping
 */
public abstract class LocalVariableInstruction extends Instruction
  implements VariableAccessor {

  protected int index;
  protected String varId;

  protected LocalVariableInstruction(){} // this is going away

  protected LocalVariableInstruction(int index){
    this.index = index;
  }

  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
    index = ((org.apache.bcel.generic.LocalVariableInstruction) i).getIndex();
  }
  
  public int getLocalVariableIndex() {
    return index;
  }
  
  public String getLocalVariableName () {
    LocalVarInfo[] localVars = mi.getLocalVars();
    
    if (localVars != null && index < localVars.length) {
      return localVars[index].getName();
    } else {
      return "?";
    }
  }
  
  public String getLocalVariableType () {
    LocalVarInfo[] localVars = mi.getLocalVars();
    
    if (localVars != null && index < localVars.length) {
      return localVars[index].getType();
    } else {
      return "?";
    }
  }
  
  /**
   * just an on-demand set fully qualified class/method/var name
   */
  public String getVariableId () {
    if (varId == null) {
      varId = mi.getClassInfo().getName() + '.' + mi.getUniqueName() + '.' + getLocalVariableName();
    }
    
    return varId;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}


