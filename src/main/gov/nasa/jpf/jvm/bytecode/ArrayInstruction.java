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

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;

import org.apache.bcel.classfile.ConstantPool;


/**
 * abstraction for all array instructions
 */
public abstract class ArrayInstruction extends Instruction {

  public ArrayInstruction () {
  }

  /* define this here since none of the array instructions need to set a
   * peer. */
  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
  }


  /**
   * return size of array elements in stack words (long,double: 2, all other: 1)
   * e.g. used to determine where the object reference is on the stack
   */
  protected int getElementSize () {
    return 1;
  }
  
  boolean createAndSetArrayCG ( SystemState ss, ElementInfo ei, ThreadInfo ti) {
    // unfortunately we can't do the field filtering here because
    // there is no field info for array instructions - the reference might
    // have been on the operand-stack for a while, and the preceeding
    // GET_FIELD already was a scheduling point (i.e. we can't cache it)
    
    ChoiceGenerator cg = ss.getSchedulerFactory().createSharedArrayAccessCG(ei, ti);
    if (cg != null) {
      ss.setNextChoiceGenerator(cg);
      ti.skipInstructionLogging();
      return true;
    }
        
    return false;
  }
  
  /**
   * <2do> it is not really clear if that is going to buy us something. The
   * thinking is that there always is a preceeding GET_STATIC/FIELD for the
   * array itself, and at the point of the xxALOAD/STORE we don't know
   * the field name anymore, hence we cannot filter efficiently.
   */
  boolean isNewPorBoundary (ElementInfo ei, ThreadInfo ti) {
    return false;
    //return (!ti.checkPorFieldBoundary() && ei.isSchedulingRelevant());
  }
}
