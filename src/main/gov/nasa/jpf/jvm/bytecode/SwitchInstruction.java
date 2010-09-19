//
// Copyright (C) 2007 United States Government as represented by the
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

import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.InstructionHandle;

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.IntChoiceGenerator;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;

/**
 * common root class for LOOKUPSWITCH and TABLESWITCH insns
 */
public abstract class SwitchInstruction extends Instruction {

  public static final int DEFAULT = -1; 
  
  protected int   target;   // default branch
  protected int[] targets;  // explicit value branches
  protected int[] matches;  // branch consts

  protected int lastIdx;
  
  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
    target = ((org.apache.bcel.generic.Select) i).getTarget()
                                                     .getPosition();
    matches = ((org.apache.bcel.generic.Select) i).getMatchs();

    int length = matches.length;
    targets = new int[length];

    InstructionHandle[] ih = ((org.apache.bcel.generic.Select) i).getTargets();

    for (int j = 0; j < length; j++) {
      targets[j] = ih[j].getPosition();
    }
  }

  
  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int value = ti.pop();

    lastIdx = DEFAULT;
    
    for (int i = 0, l = matches.length; i < l; i++) {
      if (value == matches[i]) {
        lastIdx = i;
        return ti.getMethod().getInstructionAt(targets[i]);
      }
    }

    return ti.getMethod().getInstructionAt(target);
  }

  /** useful for symbolic execution modes */
  public Instruction executeAllBranches (SystemState ss, KernelState ks, ThreadInfo ti) {
    if (!ti.isFirstStepInsn()) {
      IntIntervalGenerator cg = new IntIntervalGenerator("switchAll", 0,matches.length);
      ss.setNextChoiceGenerator(cg);
      return this;
      
    } else {
      IntIntervalGenerator cg = ss.getCurrentChoiceGenerator("switchAll", IntIntervalGenerator.class);
      assert (cg != null) : "no IntIntervalGenerator";
      
      int idx = ti.pop(); // but we are not using it
      idx = cg.getNextChoice();
      
      if (idx == matches.length){ // default branch
        lastIdx = DEFAULT;
        return ti.getMethod().getInstructionAt(target);        
      } else {
        lastIdx = idx;
        return ti.getMethod().getInstructionAt(targets[idx]);        
      }
    }
  }

  //--- a little inspection, but only post exec yet
  
  public int getLastTargetIndex () {
    return lastIdx;
  }
  
  public int getNumberOfTargets () {
    return matches.length;
  }
  
  public int getMatchConst (int idx){
    return matches[idx];
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
