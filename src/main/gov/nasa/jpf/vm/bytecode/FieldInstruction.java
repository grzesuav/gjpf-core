//
// Copyright (C) 2014 United States Government as represented by the
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

package gov.nasa.jpf.vm.bytecode;

import gov.nasa.jpf.SystemAttribute;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.Types;

/**
 * abstract base for all field access instructions
 */
public abstract class FieldInstruction extends Instruction implements ReadOrWriteInstruction {

  protected String fname;
  protected String className;
  protected String varId;

  protected FieldInfo fi; // lazy eval, hence not public

  protected int    size;  // is it a word or a double word field
  protected boolean isReferenceField;

  protected long lastValue;

  protected FieldInstruction (String name, String clsName, String fieldDescriptor){
    fname = name;
    className = Types.getClassNameFromTypeName(clsName);
    isReferenceField = Types.isReferenceSignature(fieldDescriptor);
    size = Types.getTypeSize(fieldDescriptor);
  }

  /**
   * for explicit construction
   */
  public void setField (String fname, String fclsName) {
    this.fname = fname;
    this.className = fclsName;
    if (fclsName.equals("long") || fclsName.equals("double")) {
      this.size = 2;
      this.isReferenceField = false;
    } else {
      this.size = 1;
      if (fclsName.equals("boolean") || fclsName.equals("byte") || fclsName.equals("char") || fclsName.equals("short") || fclsName.equals("int")) {
        this.isReferenceField = false;
      } else {
        this.isReferenceField = true;
      }
    }
  }
  
  public abstract FieldInfo getFieldInfo();
  public abstract boolean isRead();
  
  // for use in instructionExecuted() implementations
  public abstract ElementInfo getLastElementInfo();
  
  // for use in executeInstruction implementations
  public abstract ElementInfo peekElementInfo (ThreadInfo ti);
  
  public String getClassName(){
     return className;
  }

  public String getFieldName(){
	  return fname;
  }

  public int getFieldSize() {
    return size;
  }
 
  public boolean isReferenceField () {
    return isReferenceField;
  }
  
  /**
   * only defined in instructionExecuted() notification context
   */
  public long getLastValue() {
    return lastValue;
  }

  public String getVariableId () {
    if (varId == null) {
      varId = className + '.' + fname;
    }
    return varId;
  }

  public String getId (ElementInfo ei) {
    // <2do> - OUTCH, should be optimized (so far, it's only called during reporting)
    if (ei != null){
      return (ei.toString() + '.' + fname);
    } else {
      return ("?." + fname);
    }
  }
  
  public String toString() {
    return getMnemonic() + " " + className + '.' + fname;
  }
  
  
  public boolean isMonitorEnterPrologue(){
    // override if this insn can be part of a monitorenter code pattern
    return false;
  }
  
}
