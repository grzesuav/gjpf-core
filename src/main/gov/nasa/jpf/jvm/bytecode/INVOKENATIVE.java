//
// Copyright (C) 2010 United States Government as represented by the
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
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.NativeMethodInfo;
import gov.nasa.jpf.jvm.NativeStackFrame;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;

/**
 * this is a synthetic instruction to (re-)execute native methods
 *
 * Note that StackFrame and lock handling has to occur from within
 * the corresponding NativeMethodInfo
 */
public class INVOKENATIVE extends InvokeInstruction {

  public boolean isExtendedInstruction() {
    return true;
  }

  public static final int OPCODE = 258;

  public int getByteCode () {
    return OPCODE;
  }

  public void setInvokedMethod (NativeMethodInfo mi){
    setInvokedMethod(mi.getClassName(), mi.getBaseName(), mi.getSignature());

    invokedMethod = mi;
  }

  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  public Instruction execute(SystemState ss, KernelState ks, ThreadInfo ti) {

    // we don't have to enter/leave or push/pop a frame, that's all done
    // in NativeMethodInfo.execute
    return invokedMethod.execute(ti);
  }

  public MethodInfo getInvokedMethod(ThreadInfo ti) {
    return invokedMethod;
  }

  public Object getFieldValue(String id, ThreadInfo ti) {
    return null;
  }

}
