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

import gov.nasa.jpf.jvm.ArrayIndexOutOfBoundsExecutiveException;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * abstraction for all array store instructions
 *
 *  ... array, index, <value> => ...
 */
public abstract class ArrayStoreInstruction extends ArrayInstruction
  implements StoreInstruction
{

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int aref = peekArrayRef(ti); // need to be poly, could be LongArrayStore
    if (aref == -1) {
      return ti.createAndThrowException("java.lang.NullPointerException");
    }

    ElementInfo e = ti.getElementInfo(aref);

    if (isNewPorBoundary(e, ti)) {
      if (createAndSetArrayCG(ss,e,ti, aref, peekIndex(ti), false)) {
        return this;
      }
    }

    int esize = getElementSize();
    Object attr = esize == 1 ? ti.getOperandAttr() : ti.getLongOperandAttr();

    long value = getValue(ti);
    index = ti.pop();
    // don't set 'arrayRef' before we do the CG check (would kill loop optimization)
    arrayRef = ti.pop();

    // check type compatibility for reference arrays - patch from Tihomir Gvero and Milos Gligoric
    // (could be in AASTORE, but would also require duplicated code)
    ClassInfo c = e.getClassInfo();
    if (c.isReferenceArray() && (value != -1)) { // no checks for storing 'null'
      ClassInfo elementCi = ti.getClassInfo((int) value);
      ClassInfo arrayElementCi = c.getComponentClassInfo();
      if (!elementCi.isInstanceOf(arrayElementCi)) {
        String exception = "java.lang.ArrayStoreException";
        String exceptionDescription = elementCi.getName();
        return ti.createAndThrowException(exception, exceptionDescription);
      }
    }

    try {
      setField(e, index, value);
      e.setElementAttrNoClone(index,attr); // <2do> what if the value is the same but not the attr?
      return getNext(ti);

    } catch (ArrayIndexOutOfBoundsExecutiveException ex) { // at this point, the AIOBX is already processed
      return ex.getInstruction();
    }
  }

  /**
   * this is for pre-exec use
   */
  protected int peekArrayRef(ThreadInfo ti) {
    return ti.peek(2);
  }

  protected int peekIndex(ThreadInfo ti){
    return ti.peek(1);
  }

  protected void setField (ElementInfo e, int index, long value)
                    throws ArrayIndexOutOfBoundsExecutiveException {
    e.checkArrayBounds(index);
    e.setElement(index, (int) value);
  }

  protected long getValue (ThreadInfo th) {
    return /*(long)*/ th.pop();
  }

  public boolean isRead() {
    return false;
  }

}
