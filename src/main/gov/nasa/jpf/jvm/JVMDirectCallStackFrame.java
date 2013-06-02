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

import gov.nasa.jpf.vm.DirectCallStackFrame;
import gov.nasa.jpf.vm.MethodInfo;

/**
 * a direct call stackframe that supports JVM calling conventions
 */
public class JVMDirectCallStackFrame extends DirectCallStackFrame {
  
  JVMDirectCallStackFrame (MethodInfo miDirectCall, MethodInfo callee){
    super( miDirectCall, callee);
  }

  //--- return value handling
  
  @Override
  public int getResult(){
    return pop();
  }
  
  @Override
  public int getReferenceResult(){
    return pop();
  }
  
  @Override
  public long getLongResult(){
    return popLong();
  }

  @Override
  public Object getResultAttr(){
    return getOperandAttr();
  }
  
  @Override
  public Object getLongResultAttr(){
    return getLongOperandAttr();
  }

  
  //--- direct call argument initialization
  // NOTE - we don't support out-of-order arguments yet, i.e. the argIdx is ignored
  
  @Override
  public void setArgument (int argIdx, int v, Object attr){
    push(v);
    if (attr != null){
      setOperandAttr(attr);
    }
  }
  @Override
  public void setReferenceArgument (int argIdx, int ref, Object attr){
    pushRef(ref);
    if (attr != null){
      setOperandAttr(attr);
    }
  }
  @Override
  public void setLongArgument (int argIdx, long v, Object attr){
    pushLong(v);
    if (attr != null){
      setLongOperandAttr(attr);
    }    
  }
  
  //--- DirectCallStackFrame methods don't have arguments
  
  @Override
  public void setArgumentLocal (int argIdx, int v, Object attr){
    throw new UnsupportedOperationException("direct call methods don't have arguments");
  }
  @Override
  public void setReferenceArgumentLocal (int argIdx, int v, Object attr){
    throw new UnsupportedOperationException("direct call methods don't have arguments");
  }
  @Override
  public void setLongArgumentLocal (int argIdx, long v, Object attr){
    throw new UnsupportedOperationException("direct call methods don't have arguments");
  }
}
