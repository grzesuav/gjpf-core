//
// Copyright (C) 2006 United States Government as represented by the
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

import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.ThreadInfo;

import org.apache.bcel.classfile.ConstantPool;


/**
 * Return void from method
 *   ...  [empty]
 */
public class RETURN extends ReturnInstruction {
  public RETURN () {}

  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
  }

  public Object getReturnAttr (ThreadInfo ti){
    return null; // no return value
  }

  protected void storeReturnValue (ThreadInfo th) {
    // we don't have any
  }

  protected void pushReturnValue (ThreadInfo th) {
    // nothing to do
  }

  public Object getReturnValue(ThreadInfo ti) {
    //return Void.class; // Hmm, not sure if this is right, but we have to distinguish from ARETURN <null>
    return null;
  }

  public String toString() {
    return "return  " + mi.getFullName();
  }

  public int getByteCode () {
    return 0xB1;
  }
}
