//
// Copyright (C) 2013 United States Government as represented by the
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

import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.bytecode.ArrayElementInstruction;

/**
 * abstract class for operations that access elements of arrays
 */
public abstract class JVMArrayElementInstruction extends  ArrayElementInstruction {
  
  protected int arrayRef;
  protected int index; // the accessed element
  
  // we need this to be abstract because of the LongArrayStore insns
  abstract public int peekIndex (ThreadInfo ti);
  
  abstract protected int peekArrayRef (ThreadInfo ti);
  
  public boolean isReferenceArray() {
    return false;
  }
  
  @Override
  public ElementInfo getElementInfo (ThreadInfo ti){
    if (isCompleted(ti)){
      return ti.getElementInfo(arrayRef);
    } else {
      int ref = peekArrayRef(ti);
      return ti.getElementInfo(arrayRef);
    }
  }
  
  /**
   * only makes sense from an executeInstruction() or instructionExecuted() listener,
   * it is undefined outside of insn exec notifications
   */
  public int getArrayRef (ThreadInfo ti){
    if (ti.isPreExec()){
      return peekArrayRef(ti);
    } else {
      return arrayRef;
    }
  }

  public ElementInfo peekArrayElementInfo (ThreadInfo ti){
    int aref = getArrayRef(ti);
    return ti.getElementInfo(aref);
  }
  
  public int getIndex (ThreadInfo ti){
    if (!isCompleted(ti)){
      return peekIndex(ti);
    } else {
      return index;
    }
  }
  
  /**
   * return size of array elements in stack words (long,double: 2, all other: 1)
   * e.g. used to determine where the object reference is on the stack
   * 
   * should probably be abstract, but there are lots of subclasses and only LongArrayLoad/Store insns have different size
   */
  protected int getElementSize () {
    return 1;
  }
  
  public abstract boolean isRead();
  
}
