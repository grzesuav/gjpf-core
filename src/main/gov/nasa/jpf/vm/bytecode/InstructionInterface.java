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

package gov.nasa.jpf.vm.bytecode;

import gov.nasa.jpf.util.Attributable;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * we use that to access Instruction methods from xInstruction interfaces
 * 
 * NOTE - this has to be kept in sync with Instruction
 */
public interface InstructionInterface extends Attributable {

  /**
   * this is for cases where we need the Instruction type. Try to use InstructionInterface in clients
   */
  Instruction asInstruction();
  
  int getByteCode();
  boolean isFirstInstruction();
  boolean isBackJump();
  boolean isExtendedInstruction();
  Instruction getNext();
  int getInstructionIndex();
  int getPosition();
  MethodInfo getMethodInfo();
  int getLength();
  Instruction getPrev();
  boolean isCompleted(ThreadInfo ti);
  String getSourceLine();
  String getSourceLocation();
  Instruction execute(ThreadInfo ti);
  String toPostExecString();
  String getMnemonic();
  int getLineNumber();
  String getFileLocation();
  String getFilePos();
  Instruction getNext (ThreadInfo ti);

  
  
  //.. and probably a lot still missing
}
