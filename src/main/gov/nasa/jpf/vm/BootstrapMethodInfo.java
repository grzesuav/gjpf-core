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
package gov.nasa.jpf.vm;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 *
 * For now, this is only used to capture boostrap methods for lambda expression,
 * which link the method representing the lambda body to a single abstract method 
 * (SAM) declared in a functional interface. References to bootstrap methods are
 * provided by the invokedynamic bytecode instruction. 
 */
public class BootstrapMethodInfo {
  
  int lambdaRefKind;
  
  // method capturing lambda body to be linked to the function method of function object
  MethodInfo lambdaBody;
  
  // class containing lamabda expression
  ClassInfo enclosingClass;
  
  // descriptor of a SAM declared within the functional interface   
  String samDescriptor;
  
  public BootstrapMethodInfo(int lambdaRefKind, ClassInfo enclosingClass, MethodInfo lambdaBody, String samDescriptor) {
    this.lambdaRefKind = lambdaRefKind;
    this.enclosingClass = enclosingClass;
    this.lambdaBody = lambdaBody;
    this.samDescriptor = samDescriptor;
  }
  
  public String toString() {
    return "BootstrapMethodInfo[" + enclosingClass.getName() + "." + lambdaBody.getName() + 
        "[SAM descriptor:" + samDescriptor + "]]";
  }
  
  public MethodInfo getLambdaBody() {
    return lambdaBody;
  }
  
  public String getSamDescriptor() {
    return samDescriptor;
  }
  
  public int getLambdaRefKind () {
    return lambdaRefKind;
  }
}
