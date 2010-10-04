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

package gov.nasa.jpf.jvm;

/**
 * a stack frame for MJI methods
 *
 * NOTE: operands and locals can be, but are not automatically used during
 * native method execution.
 *
 * This differs from a DirectCallStackFrame in that it (a) can cause re-execution
 * of the native method (e.g. for iterative round trips from a native method), and
 * it does return values into its caller frame
 */
public class NativeStackFrame extends DynamicStackFrame {

  static int[] EMPTY_ARRAY = new int[0];

  // if this is set, the native method invocation is repeated once this
  // stack frame is on top again
  boolean repeatInvocation;

  public NativeStackFrame (MethodInfo mi, StackFrame caller){
    super(mi,caller);

    if (!mi.isStatic()){
      thisRef = caller.getCalleeThis(mi);
    }

    // we don't have to copy the caller operands into locals since we
    // don't use any locals or operands

    pc = mi.getInstruction(0);
    top = -1;

    // we start out with no operands and locals
    locals = EMPTY_ARRAY;
    operands = EMPTY_ARRAY;
  }

  public void repeatInvocation(boolean cond){
    repeatInvocation = cond;
  }

  public boolean isRepeatedInvocation() {
    return repeatInvocation;
  }

  public boolean isNative() {
    return true;
  }

  public boolean modifiesState() {
    return false;
  }

  public boolean hasAnyRef() {
    return false;
  }
  
}
