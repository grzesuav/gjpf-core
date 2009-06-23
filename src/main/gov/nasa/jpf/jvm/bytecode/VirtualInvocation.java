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

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * a base class for virtual call instructions
 */
public abstract class VirtualInvocation extends InvokeInstruction {

  ClassInfo lastCalleeCi; // cached for performance

  protected VirtualInvocation () {}

  public MethodInfo getInvokedMethod(ThreadInfo ti){
    int objRef;

    if (ti.getNextPC() == null){ // this is pre-exec
      objRef = ti.getCalleeThis(getArgSize());
    } else {                     // this is post-exec
      objRef = lastObj;
    }

    return getInvokedMethod(ti, objRef);
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int objRef = ti.getCalleeThis( getArgSize());

    if (objRef == -1) { // NPE
      return ti.createAndThrowException("java.lang.NullPointerException",
                                        "calling '" + mname + "' on null object");
    }

    MethodInfo mi = getInvokedMethod(ti, objRef);
    if (mi == null) {
      return ti.createAndThrowException("java.lang.NoSuchMethodException",
                                        ti.getClassInfo(objRef).getName() + "." + mname);
    }

    if (mi.isSynchronized()) {
      ChoiceGenerator<?> cg = getSyncCG(objRef, mi, ss, ks, ti);
      if (cg != null) {
        ss.setNextChoiceGenerator(cg);
        return this;   // repeat exec, keep insn on stack
      }
    }

    // this will lock the object if necessary
    return mi.execute(ti);
  }

  /**
   * return the 'this' reference for this call
   */
  public int getThis (ThreadInfo ti) {
    if (!ti.isPostExec()){
      // we have to dig out the 'this' reference from the callers stack
      return ti.getCalleeThis( getArgSize());
    } else {
      // execute() cached it
      return lastObj;
    }
  }

  public ElementInfo getThisElementInfo (ThreadInfo ti) {
    int thisRef = getThis(ti);
    if (thisRef != -1) {
      return ti.getElementInfo(thisRef);
    } else {
      return null;
    }
  }

  public MethodInfo getInvokedMethod (ThreadInfo ti, int objRef) {

    if (objRef != -1) {
      lastObj = objRef;

      ClassInfo cci = ti.getClassInfo(objRef);

      if (lastCalleeCi != cci) { // callee ClassInfo has changed
        lastCalleeCi = cci;
        invokedMethod = cci.getMethod(mname, true);

        // here we could catch the NoSuchMethodError
        if (invokedMethod == null) {
          lastObj = -1;
        }
      }

    } else {
      lastObj = -1;
      invokedMethod = null;
    }

    return invokedMethod;
  }

  public Object getFieldValue (String id, ThreadInfo ti){
    int objRef = getThis(ti);
    ElementInfo ei = ti.getVM().getDynamicArea().get(objRef);

    Object v = ei.getFieldValueObject(id);

    if (v == null){ // try a static field
      v = ei.getClassInfo().getStaticFieldValueObject(id);
    }

    return v;
  }
}
