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
package gov.nasa.jpf.tools;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.AnnotationInfo;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;
import gov.nasa.jpf.jvm.bytecode.PUTSTATIC;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.Trace;

/**
 * listener to check for @Const annotations. A method that is marked @Const
 * is not allowed to modify any of it's class or instance fields.
 * 
 * If the property is violated, this listener throws an AssertionError
 * 
 * <2do> so far, we only support 'deep' const, i.e. any PUTFIELD
 * between enter and exit from the @Const method is treated as a 
 * violation
 */
public class ConstChecker extends ListenerAdapter {
  
  // <2do> should really use pools for ConstObj, ConstContext 
  static class ConstObj {
    int ref;
    int count;
    
    ConstObj (int r, int c){
      ref = r;
      count = c; 
    }
    
    ConstObj inc() {
      return new ConstObj(ref, count+1);
    }
    
    ConstObj dec() {
      assert count > 0;
      return new ConstObj(ref, count-1);
    }
  }
  
  static class ConstContext {
    // probably not the best implementation - we assume for now that const
    // methods are rather small and short living. Otherwise this is neither
    // time nor performance optimal
    ConstObj[] consts;
    
    ConstContext() {
      consts = new ConstObj[0];
    }
    
    ConstContext (ConstObj[] c) {
      consts = c;
    }
    
    // handle statics via class object reference, i.e. two ConstObj entries per instance
    
    ConstContext incRef (int ref) {
      for (int i=0; i<consts.length; i++) {
        if (consts[i].ref == ref) { // replace ConstObj
          ConstObj[] newConsts = consts.clone();
          newConsts[i] = consts[i].inc();
          return new ConstContext(newConsts);
        }
      }
      
      // append a new ConstObj
      ConstObj[] newConsts = new ConstObj[consts.length+1];
      if (consts.length>0) {
        System.arraycopy(consts,0,newConsts,0,consts.length);
      }
      newConsts[consts.length] = new ConstObj(ref,1);
      return new ConstContext(newConsts);      
    }
    
    ConstContext decRef (int ref) {
      for (int i=0; i<consts.length; i++) {
        if (consts[i].ref == ref) {
          ConstObj[] newConsts;
          if (consts[i].count > 1) {
            newConsts = consts.clone();
            newConsts[i] = consts[i].dec();
          } else {
            int l = consts.length-1;
            if (l > 0) {
              newConsts = new ConstObj[l];
              if (i>0) {
                System.arraycopy(consts,0,newConsts,0,i);
              }
              if (i<l) {
                System.arraycopy(consts,i+1,newConsts,i,l-i);
              }
            } else {
              newConsts = new ConstObj[0];
            }
          }
          return new ConstContext(newConsts);
        }
      }
      
      return this;
    }

    ConstContext incRefs (int ref1, int ref2) {
      // <2do> fix this for efficiency
      ConstContext c = this;
      if (ref1 != -1) {
        c = c.incRef(ref1);
      }
      if (ref2 != -1) {
        c = c.incRef(ref2);
      }
      return c;
    }
    ConstContext decRefs (int ref1, int ref2) {
      // <2do> fix this for efficiency
      ConstContext c = this;
      if (ref1 != -1) {
        c = c.decRef(ref1);
      }
      if (ref2 != -1) {
        c = c.decRef(ref2);
      }
      return c;
    }
    
    
    boolean includes (int ref) {
      for (int i=consts.length-1; i>=0; i--) {
        if (consts[i].ref == ref) {
          return true;
        }
      }
      return false;
    }
  }
  
  // another peudo trace that really is just a backtrackable state extension
  Trace<ConstContext> constContext = new Trace<ConstContext>();
  
  public ConstChecker (Config conf) {
    // <2do> we should probably configure if const instance also includes const static
    constContext.addOp(new ConstContext());
  }
  
  AnnotationInfo getAnnotation (MethodInfo mi) {
    // <2do> inherited annotations? we might want to cache
    return mi.getAnnotation("gov.nasa.jpf.Const");
  }
  
  //--- update the constContext

  public void instructionExecuted (JVM vm) {
    ThreadInfo ti = vm.getLastThreadInfo();
    Instruction insn = vm.getLastInstruction();
  
    if (!insn.isCompleted(ti)) {
      return;
    }
    
    if (insn instanceof InvokeInstruction) { // check for new const context
      InvokeInstruction call = (InvokeInstruction)insn;
      MethodInfo mi = call.getInvokedMethod();
      if (!mi.isDirectCallStub()) {
        AnnotationInfo ai = getAnnotation(mi);
        if (ai != null) {
          ConstContext cc = constContext.getLastOp();

          if (!mi.isStatic()) {
            int objRef = ti.getThis(); // we are already on the stack
            int clsRef = vm.getDynamicArea().get(objRef).getClassInfo().getClassObjectRef();
            constContext.addOp(cc.incRefs(objRef, clsRef));
          } else {
            int clsRef = mi.getClassInfo().getClassObjectRef(); 
            constContext.addOp(cc.incRef(clsRef));
          }
        }
      }
      
    } else if (insn instanceof ReturnInstruction) { // check for const context update
      ReturnInstruction ret = (ReturnInstruction)insn;
      MethodInfo mi = insn.getMethodInfo();
      if (!mi.isDirectCallStub()) {
        AnnotationInfo ai = getAnnotation(mi);
        if (ai != null) {
          ConstContext cc = constContext.getLastOp();

          if (cc != null) {
            if (!mi.isStatic()) {
              int objRef = ret.getReturnFrame().getThis(); // it's already off the stack
              int clsRef = vm.getDynamicArea().get(objRef).getClassInfo().getClassObjectRef();
              constContext.addOp(cc.decRefs(objRef, clsRef));
            } else {
              int clsRef = mi.getClassInfo().getClassObjectRef(); 
              constContext.addOp(cc.decRef(clsRef));          
            }
          }
        }
      }
      
    } else if (insn instanceof PUTFIELD) { // check for illegal instance/static field writes
      PUTFIELD put = (PUTFIELD)insn;
      int objRef = put.getLastThis();
      ConstContext cc = constContext.getLastOp();
      
      if (cc != null) {
        // check the instance
        if (cc.includes(objRef)) {
          Instruction nextPc = ti.createAndThrowException("java.lang.AssertionError",
                                                          "instance field write within const context: " + put.getFieldInfo()); 
          ti.setNextPC(nextPc);
          return;
        }

        // now check the class
        int clsRef = vm.getDynamicArea().get(objRef).getClassInfo().getClassObjectRef();
        if (cc.includes(clsRef)) {
          Instruction nextPc = ti.createAndThrowException("java.lang.AssertionError",
                                                          "static field write within const context: " + put.getFieldInfo()); 
          ti.setNextPC(nextPc);
          return;
        }
      }      
      
    } else if (insn instanceof PUTSTATIC) { // check for illegal static field writes
      PUTSTATIC put = (PUTSTATIC)insn;
      int clsRef = put.getFieldInfo().getClassInfo().getClassObjectRef();
      ConstContext cc = constContext.getLastOp();
      
      if (cc != null) {
        if (cc.includes(clsRef)) {
          Instruction nextPc = ti.createAndThrowException("java.lang.AssertionError",
                                                          "static field write within const context: " + put.getFieldInfo()); 
          ti.setNextPC(nextPc);
          return;
        }
      }
    }
  }
  
  public void stateAdvanced (Search search) {
    constContext.stateAdvanced(search);
  }
  
  public void stateBacktracked (Search search) {
    constContext.stateBacktracked(search);
  }

  public void stateStored (Search search) {
    constContext.stateStored(search);
  }

  public void stateRestored (Search search) {
    constContext.stateRestored(search);
  }

}
