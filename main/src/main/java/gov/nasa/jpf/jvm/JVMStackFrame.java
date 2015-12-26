/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

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
public class JVMStackFrame
    extends StackFrame {

  public JVMStackFrame(MethodInfo callee){
    super(callee);
  }

  /**
   * creates callerSlots dummy Stackframe for testing of operand/local operations
   * NOTE - TESTING ONLY! this does not have callerSlots MethodInfo
   */
  JVMStackFrame(int nLocals, int nOperands){
    super(nLocals, nOperands);
  }

  /*
   * to be used to initialize locals of a stackframe (only required for explicit construction without a caller,
   * otherwise the Stackframe ctor/invoke insn will take care of copying the values from its caller)
   */
  @Override
  public void setArgumentLocal(int index, int value, Object attributes){
    setLocalVariable(index, value);
    if (attributes != null) {
      setLocalAttr(index, attributes);
    }
  }

  @Override
  public void setExceptionReference(int exceptionReference){
    clearOperandStack();
    pushRef(exceptionReference);
  }

  //--- these are for setting up arguments from a VM / listener caller

  @Override
  public void setLongArgumentLocal(int index, long value, Object attributes){
    setLongLocalVariable(index, value);
    if (attributes != null) {
      setLocalAttr(index, attributes);
    }
  }

  @Override
  public void setReferenceArgumentLocal(int index, int reference, Object attributes){
    setLocalReferenceVariable(index, reference);
    if (attributes != null) {
      setLocalAttr(index, attributes);
    }
  }

  /**
   * this sets up arguments from a bytecode caller
   */
  protected void setCallArguments(ThreadInfo threadInfo){
    StackFrame caller = threadInfo.getTopFrame();
    MethodInfo miCallee = methodInfo;
    int argumentsSize = miCallee.getArgumentsSize();
    if (argumentsSize > 0) {
      int[] calleeSlots = slots;
      FixedBitSet calleeRefs = isReferenced;
      int[] callerSlots = caller.getSlots();
      FixedBitSet callerRefs = caller.getReferenceMap();
      for (int i = 0, j = caller.getTopPos() - argumentsSize + 1; i < argumentsSize; i++, j++) {
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