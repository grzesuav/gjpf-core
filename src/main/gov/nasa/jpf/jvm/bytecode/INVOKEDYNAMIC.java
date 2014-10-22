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
package gov.nasa.jpf.jvm.bytecode;

import gov.nasa.jpf.vm.BootstrapMethodInfo;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.FunctionObjectFactory;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.VM;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 * 
 * Invoke dynamic method. It allows dynamic linkage between a call site and a method implementation.
 *
 * ..., [arg1, [arg2 ...]]  => ...
 */
public class INVOKEDYNAMIC extends Instruction {
  
  // index of a bootstrap method (index to the array bootstrapMethods[] declared in ClassInfo
  // containing this bytecode instruction)
  int bootstrapMethodIndex;
  
  // Free variables are those that are not defined within the lamabda body and 
  // are captured from the lexical scope. Note that for instance lambda methods 
  // the first captured variable always represents "this"
  String[] freeVariableTypeNames;
  byte[] freeVariableTypes;
  
  String functionalInterfaceName;
  
  String samMethodName;
  
  int funcObjRef = MJIEnv.NULL;
  
  public INVOKEDYNAMIC () {}

  protected INVOKEDYNAMIC (int bmIndex, String methodName, String descriptor){
    bootstrapMethodIndex = bmIndex;
    samMethodName = methodName;
    freeVariableTypeNames = Types.getArgumentTypeNames(descriptor);
    freeVariableTypes = Types.getArgumentTypes(descriptor);
    functionalInterfaceName = Types.getReturnTypeSignature(descriptor);
  }

  public int getByteCode () {
    return 0xBA;
  }
  
  public String toString() {
    String args = "";
    for(String type: freeVariableTypeNames) {
      if(args.length()>0) {
        type += ','+ type;
      }
      args += type;
    }
    return "invokedynamic " + bootstrapMethodIndex + " " + 
    samMethodName + '(' + args +"):" + functionalInterfaceName;
  }

  /**
   * For now, INVOKEDYNAMIC works only in the context of lambda expressions.
   * Executing this returns an object that implements the functional interface 
   * and contains a method which captures the behavior of the lambda expression.
   */
  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame frame = ti.getModifiableTopFrame();
    
    if(funcObjRef == MJIEnv.NULL) {
      ClassInfo fiClassInfo;

      // First, resolve the functional interface
      try {
        fiClassInfo = ti.resolveReferencedClass(functionalInterfaceName);
      } catch(LoadOnJPFRequired lre) {
        return ti.getPC();
      }

      if (!fiClassInfo.isRegistered()){
        fiClassInfo.registerClass(ti);
      }

      if (fiClassInfo.initializeClass(ti)) {
        return ti.getPC();
      }
      
      ClassInfo enclosingClass = this.getMethodInfo().getClassInfo();
      
      BootstrapMethodInfo bmi = enclosingClass.getBootstrapMethodInfo(bootstrapMethodIndex);
      
      VM vm = VM.getVM();
      FunctionObjectFactory funcObjFactory = vm.getFunctionObjectFacotry();
      
      String samUniqueName = samMethodName + bmi.getSamDescriptor();
      Object[] freeVariableValues = frame.getArgumentsValues(ti, freeVariableTypes);
      
      funcObjRef = funcObjFactory.getFunctionObject(ti, fiClassInfo, samUniqueName, bmi, freeVariableTypeNames, freeVariableValues);
    }
    
    frame.pop(freeVariableTypes.length);
    frame.pushRef(funcObjRef);
    
    return getNext(ti);
  }
}
