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
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * common machine independent type for static field accessors
 */
public abstract class StaticFieldInstruction extends FieldInstruction {

  protected StaticFieldInstruction(String fieldName, String clsDescriptor, String fieldDescriptor){
    super(fieldName, clsDescriptor, fieldDescriptor);
  }

  /**
   * on-demand initialize the ClassInfo and FieldInfo fields. Note that
   * classinfo might not correspond with the static className, but can be one of
   * the super classes. Rather than checking for this on each subsequent access,
   * we get the right one that declares the field here
   */
  protected void initialize() {
    ClassInfo ciRef = mi.getClassInfo().resolveReferencedClass(className);
    
    FieldInfo f = ciRef.getStaticField(fname);
    ClassInfo ciField = f.getClassInfo();
    if (!ciField.isRegistered()){
      // classLoaded listeners might change/remove this field
      ciField.registerClass(ThreadInfo.getCurrentThread());
      f = ciField.getStaticField(fname);
    }
    
    fi = f;
  }

  /**
   * who owns the field?
   * NOTE: this should only be used from a executeInstruction()/instructionExecuted() context
   */
  @Override
  public ElementInfo getElementInfo(ThreadInfo ti){
    return getFieldInfo().getClassInfo().getStaticElementInfo();
  }
  
  @Override
  public String toPostExecString(){
    StringBuilder sb = new StringBuilder();
    sb.append(getMnemonic());
    sb.append(' ');
    sb.append( fi.getFullName());
    
    return sb.toString();
  }

  public ClassInfo getClassInfo() {
    if (fi == null) {
      initialize();
    }
    return fi.getClassInfo();
  }

  public FieldInfo getFieldInfo() {
    if (fi == null) {
      initialize();
    }
    return fi;
  }

  /**
   *  that's invariant, as opposed to InstanceFieldInstruction, so it's
   *  not really a peek
   */
  public ElementInfo peekElementInfo (ThreadInfo ti) {
    return getLastElementInfo();
  }

  public StaticElementInfo getLastElementInfo() {
    return getFieldInfo().getClassInfo().getStaticElementInfo();
  }

  // this can be different than ciField - the field might be in one of its
  // superclasses
  public ClassInfo getLastClassInfo(){
    return getFieldInfo().getClassInfo();
  }

  public String getLastClassName() {
    return getLastClassInfo().getName();
  }

}
