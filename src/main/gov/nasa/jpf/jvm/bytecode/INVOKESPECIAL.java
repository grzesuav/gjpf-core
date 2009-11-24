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
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * Invoke instance method; special handling for superclass, private,
 * and instance initialization method invocations
 * ..., objectref, [arg1, [arg2 ...]] => ...
 *
 * invokedMethod is supposed to be constant (ClassInfo can't change)
 */
public class INVOKESPECIAL extends InvokeInstruction {
  public INVOKESPECIAL () {}

  public int getByteCode () {
    return 0xB7;
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int objRef = ti.getCalleeThis( getArgSize());
    lastObj = objRef;

    // we don't have to check for NULL objects since this is either a ctor or
    // a private method

    MethodInfo mi = getInvokedMethod(ti);
    if (mi == null) {
      return ti.createAndThrowException("java.lang.NoSuchMethodException",
                                   "calling " + cname + "." + mname);
    }

    if (mi.isSynchronized()) {
      ChoiceGenerator<?> cg = getSyncCG(objRef, mi, ss, ks, ti);
      if (cg != null) {
        ss.setNextChoiceGenerator(cg);
        return this;   // repeat exec, keep insn on stack
      }
    }

    return mi.execute(ti);
  }

  /**
   * return the reference value of the callee object
   */
  public int getCalleeThis (ThreadInfo ti) {
    if (!ti.isPostExec()){
      // we have to dig out the 'this' reference from the callers stack
      return ti.getCalleeThis( getArgSize());
    } else {
      // execute() cached it
      return lastObj;
    }
  }


  /**
   * we can do some more caching here - the MethodInfo should be const
   */
  public MethodInfo getInvokedMethod (ThreadInfo th) {

    // since INVOKESPECIAL is only used for private methods and ctors,
    // we don't have to deal with null object calls

    if (invokedMethod == null) {
      ClassInfo ci = ClassInfo.getResolvedClassInfo(cname);
      invokedMethod = ci.getMethod(mname, true);
    }

    return invokedMethod; // we can store internally
  }

  public String toString() {
    MethodInfo callee = getInvokedMethod();

    return "invokespecial " + ((callee != null) ? callee.getFullName() : "?");
  }

  @Override
  public Object getFieldValue (String id, ThreadInfo ti) {
    int objRef = getCalleeThis(ti);
    ElementInfo ei = ti.getVM().getDynamicArea().get(objRef);

    Object v = ei.getFieldValueObject(id);

    if (v == null){ // try a static field
      v = ei.getClassInfo().getStaticFieldValueObject(id);
    }

    return v;
  }

}
