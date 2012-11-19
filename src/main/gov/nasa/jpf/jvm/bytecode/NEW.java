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

import gov.nasa.jpf.vm.*;


/**
 * Create new object
 * ... => ..., objectref
 */
public class NEW extends JVMInstruction implements AllocInstruction {
  protected String cname;
  protected int newObjRef = -1;

  public NEW (String clsDescriptor){
    cname = Types.getClassNameFromTypeName(clsDescriptor);
  }
  
  public String getClassName(){    // Needed for Java Race Finder
    return(cname);
  }

  @Override
  public Instruction execute (ThreadInfo ti) {
    Heap heap = ti.getHeap();
    ClassInfo ci;

    // resolve the referenced class
    ClassInfo cls = ti.getTopFrameMethodInfo().getClassInfo();
    try {
      ci = cls.resolveReferencedClass(cname);
    } catch(LoadOnJPFRequired lre) {
      return ti.getPC();
    }

    if (!ci.isRegistered()){
      ci.registerClass(ti);
    }

    // since this is a NEW, we also have to pushClinit
    if (!ci.isInitialized()) {
      if (ci.initializeClass(ti)) {
        return ti.getPC();  // reexecute this instruction once we return from the clinits
      }
    }

    if (heap.isOutOfMemory()) { // simulate OutOfMemoryError
      return ti.createAndThrowException("java.lang.OutOfMemoryError",
                                        "trying to allocate new " + cname);
    }

    ElementInfo ei = heap.newObject(ci, ti);
    int objRef = ei.getObjectRef();
    newObjRef = objRef;

    // pushes the return value onto the stack
    StackFrame frame = ti.getModifiableTopFrame();
    frame.pushRef( objRef);

    return getNext(ti);
  }

  public int getLength() {
    return 3; // opcode, index1, index2
  }

  public int getByteCode () {
    return 0xBB;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  public int getNewObjectRef() {
    return newObjRef;
  }

  public String toString() {
    if (newObjRef != -1){
      return "new " + cname + '@' + Integer.toHexString(newObjRef);

    } else {
      return "new " + cname;
    }
  }
}
