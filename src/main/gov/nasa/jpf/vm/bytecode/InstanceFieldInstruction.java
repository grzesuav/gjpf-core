//
// Copyright (C) 2014 United States Government as represented by the
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

package gov.nasa.jpf.vm.bytecode;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

/**
 * common machine independent type for all instance field access instructions
 */
public abstract class InstanceFieldInstruction extends FieldInstruction {

  protected int lastThis = MJIEnv.NULL;

  protected InstanceFieldInstruction (String fieldName, String classType, String fieldDescriptor){
    super(fieldName, classType, fieldDescriptor);
  }

  public abstract int getObjectSlot (StackFrame frame);
  
  @Override
  public ElementInfo getElementInfo (ThreadInfo ti){
    if (isCompleted(ti)){
      return ti.getElementInfo(lastThis);
    } else {
      return peekElementInfo(ti);
    }
  }
  
  @Override
  public String toPostExecString(){
    StringBuilder sb = new StringBuilder();
    sb.append(getMnemonic());
    sb.append(' ');
    sb.append( getLastElementInfo());
    sb.append('.');
    sb.append(fname);
    
    return sb.toString();
  }

  public FieldInfo getFieldInfo () {
    if (fi == null) {
      ClassInfo ci = ClassLoaderInfo.getCurrentResolvedClassInfo(className);
      if (ci != null) {
        fi = ci.getInstanceField(fname);
      }
    }
    return fi;
  }

  /**
   * NOTE - the return value is *only* valid in a instructionExecuted() context, since
   * the same instruction can be executed from different threads
   */
  public int getLastThis() {
    return lastThis;
  }

  /**
   * since this is based on getLastThis(), the same context restrictions apply
   */
  public ElementInfo getLastElementInfo () {
    if (lastThis != MJIEnv.NULL) {
      return VM.getVM().getHeap().get(lastThis); // <2do> remove - should be in clients
    }

    return null;
  }

  public String getFieldDescriptor () {
    ElementInfo ei = getLastElementInfo();
    FieldInfo fi = getFieldInfo();

    return ei.toString() + '.' + fi.getName();
  }

}
