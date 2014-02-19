//
// Copyright (C) 2009 United States Government as represented by the
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
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.bytecode.InstanceInvokeInstruction;

/**
 * base class for INVOKEVIRTUAL, INVOKESPECIAL and INVOLEINTERFACE
 */
public abstract class InstanceInvocation extends JVMInvokeInstruction implements InstanceInvokeInstruction {

  protected InstanceInvocation() {}

  protected InstanceInvocation (String clsDescriptor, String methodName, String signature){
    super(clsDescriptor, methodName, signature);
  }

  public int getArgSize () {
    if (argSize < 0) {
      argSize = Types.getArgumentsSize(signature) + 1; // 'this'
    }

    return argSize;
  }
  
  public int getCalleeThis (ThreadInfo ti) {
    if (!ti.isPostExec()){
      // we have to dig out the 'this' reference from the callers stack
      return ti.getCalleeThis( getArgSize());
    } else {
      // enter() cached it
      return lastObj;
    }
  }

  @Override
  public int getObjectSlot (StackFrame frame){
    int top = frame.getTopPos();
    int argSize = getArgSize();
    
    if (argSize == 1){ // object ref is on top
      return top;
      
    } else {
      return top - argSize -1;
    }
  }
  
  public ElementInfo getThisElementInfo (ThreadInfo ti) {
    int thisRef = getCalleeThis(ti);
    if (thisRef != MJIEnv.NULL) {
      return ti.getElementInfo(thisRef);
    } else {
      return null;
    }
  }
  
  public void accept(JVMInstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

}
