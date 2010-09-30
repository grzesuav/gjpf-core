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
 * NOTE: this is not actively used during native method execution, it just
 * preserves call stack consistency
 */
public class NativeStackFrame extends StackFrame {

  static int[] EMPTY_ARRAY = new int[0];

  public NativeStackFrame (MethodInfo mi, StackFrame caller){
    this.mi = mi;

    if (!mi.isStatic()){
      thisRef = caller.getCalleeThis(mi);
    }

    // we don't have to copy the caller operands into locals since we
    // don't use any locals or operands

    pc = mi.getInstruction(0);
    top = -1;

    locals = EMPTY_ARRAY;
    operands = EMPTY_ARRAY;
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
