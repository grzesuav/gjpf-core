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
package gov.nasa.jpf.jvm;

import gov.nasa.jpf.jvm.bytecode.Instruction;


/**
 * DynamicMapIndex instances are used to compute a deterministic object
 * reference (i.e. index into DynamicArea elements[]) to achieve heap
 * symmetry, so that the order of concurrent allocations does not result
 * in different heap states (i.e. objects at the end have the same 'locations'
 * in the heap).
 */
public class DynamicMapIndex {
  private Instruction pc;
  private int         threadref;
  private int         occurrence;

  public DynamicMapIndex (Instruction p, int t, int o) {
    pc = p;
    threadref = t;
    occurrence = o;
  }

  public DynamicMapIndex clone () {
    return new DynamicMapIndex(pc, threadref, occurrence);
  }

  public boolean equals (Object obj) {
    if (obj instanceof DynamicMapIndex) {
      DynamicMapIndex dmi = (DynamicMapIndex) obj;

      return ((pc == dmi.pc) && (threadref == dmi.threadref) && 
             (occurrence == dmi.occurrence));
    }

    return false;
  }

  public int hashCode () {
    // <2do> pcm - that's a bit simplistic
    return (((pc == null) ? 0 : pc.getPosition()) + threadref + occurrence);
  }

  public void next () {
    occurrence++;
  }
}
