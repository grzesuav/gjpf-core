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
 */
public abstract class ArrayStoreInstruction extends ArrayInstruction
  implements StoreInstruction
{

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    long value;
    int  index;
    int  arrayRef = peekArrayRef(ti);

    if (arrayRef == -1) {
      return ti.createAndThrowException("java.lang.NullPointerException");
    }

    DynamicArea da = DynamicArea.getHeap();
    ElementInfo e = da.get(arrayRef);

    if (isNewPorBoundary(e, ti)) {
      if (createAndSetArrayCG(ss,e,ti)) {
        return this;
      }
    }

    int esize = getElementSize();
    Object attr = esize == 1 ? ti.getOperandAttr() : ti.getLongOperandAttr();

    value = getValue(ti);
    index = ti.pop();
    ti.pop(); // we already got arrayRef

    // check type compatibility for reference arrays - patch from Tihomir Gvero and Milos Gligoric
    // (could be in AASTORE, but would also require duplicated code)
    ClassInfo c = e.getClassInfo();
    if (c.isReferenceArray() && (value != -1)) { // no checks for storing 'null'
      ClassInfo elementCi = da.get((int) value).getClassInfo();
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

  int peekArrayRef(ThreadInfo ti) {
    return ti.peek(2);
  }

  protected void setField (ElementInfo e, int index, long value)
                    throws ArrayIndexOutOfBoundsExecutiveException {
    e.checkArrayBounds(index);
    e.setElement(index, (int) value);
  }

  protected long getValue (ThreadInfo th) {
    return /*(long)*/ th.pop();
  }

}
