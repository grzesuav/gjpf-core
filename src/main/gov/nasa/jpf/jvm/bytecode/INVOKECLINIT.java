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

import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * this is an artificial bytecode that we use to deal with the particularities of 
 * <clinit> calls, which are never in the loaded bytecode but always directly called by
 * the VM. The most obvious difference is that <clinit> execution does not trigger
 * class initialization.
 * A more subtle difference is that we save a wait() - if a class
 * is concurrently initialized, both execute INVOKECLINIT (i.e. compete and sync for/on
 * the class object lock), but once the second thread gets resumed and detects that the
 * class is now initialized (by the first thread), it skips the method execution and
 * returns right away (after deregistering as a lock contender). That's kind of hackish,
 * but we have no method to do the wait in, unless we significantly complicate the
 * direct call stubs, which would obfuscate observability (debugging dynamically
 * generated code isn't very appealing). 
 */
public class INVOKECLINIT extends INVOKESTATIC {

  public INVOKECLINIT (ClassInfo ci){
    super(ci.getSignature(), "<clinit>", "()V");
  }

  public Instruction execute (ThreadInfo ti) {    
    MethodInfo callee = getInvokedMethod(ti);
    ClassInfo ci = callee.getClassInfo();
    ElementInfo ei = ci.getModifiableClassObject();

    if (!ti.isFirstStepInsn()) {
      // if we can't acquire the lock, it means somebody else is initializing concurrently
      if (!ei.canLock(ti)) {
        //ei = ei.getInstanceWithUpdatedSharedness(ti);
        ei.block(ti);
        
        VM vm = ti.getVM();
        ChoiceGenerator<?> cg = vm.getSchedulerFactory().createSyncMethodEnterCG(ei, ti);
        if (vm.setNextChoiceGenerator(cg)){ 
          return this;   // repeat exec, keep insn on stack
        }        
      }
      
    } else { // re-execution after being blocked
      // if we got here, we can execute, and have the lock but there still might have been
      // another thread that passed us with the clinit
      if (!ci.needsInitialization()) {
        return getNext();
      }
    }
    
    // enter the method body, return its first insn
    // (this would take the lock, reset the lockRef etc., so make sure all these
    // side effects are dealt with if we bail out)
    return ti.execute(callee);
  }

  public boolean isExtendedInstruction() {
    return true;
  }

  public static final int OPCODE = 256;

  public int getByteCode () {
    return OPCODE;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
