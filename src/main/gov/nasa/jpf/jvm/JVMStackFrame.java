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

package gov.nasa.jpf.jvm;

import gov.nasa.jpf.util.FixedBitSet;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * a stackframe that is used for executing Java bytecode, supporting both
 * locals and an operand stack. This is essentially the JVm stack machine
 * implementation
 */
public class JVMStackFrame extends StackFrame {

  public JVMStackFrame (MethodInfo callee){
    super( callee);
  }
  
  /**
   * this sets up arguments in a bytecode callee 
   */
  protected void setCallArguments (ThreadInfo ti){
    StackFrame caller = ti.getTopFrame();
    MethodInfo miCallee = mi;
    int nArgSlots = miCallee.getArgumentsSize();
    
    if (nArgSlots > 0){
      int[] calleeSlots = slots;
      FixedBitSet calleeRefs = isRef;
      int[] callerSlots = caller.getSlots();
      FixedBitSet callerRefs = caller.getReferenceMap();

      for (int i = 0, j = caller.getTopPos() - nArgSlots + 1; i < nArgSlots; i++, j++) {
        calleeSlots[i] = callerSlots[j];
        if (callerRefs.get(j)) {
          calleeRefs.set(i);
        }
        Object a = caller.getSlotAttr(j);
        if (a != null) {
          setSlotAttr(i, a);
        }
      }

      if (!miCallee.isStatic()) {
        thisRef = calleeSlots[0];
      }
    }
  }

}
