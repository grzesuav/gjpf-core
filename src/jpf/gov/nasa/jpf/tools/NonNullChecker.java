//
// Copyright (C) 2008 United States Government as represented by the
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

import java.util.HashSet;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;
import gov.nasa.jpf.jvm.bytecode.PUTSTATIC;
import gov.nasa.jpf.jvm.bytecode.RETURN;

/**
 * listener that checks if @NonNull marked 
 * 
 *  - methods return null values
 *  - instance fields are or get nullified outside of ctors
 *  - static fields are or get nullified outside clinit
 * 
 * In case the property is violated, the listener throws an AssertionError
 */
public class NonNullChecker extends ListenerAdapter {

  // sets to cache field check relevant classes
  HashSet<ClassInfo> staticFieldCandidates = new HashSet<ClassInfo>();
  HashSet<ClassInfo> instanceFieldCandidates = new HashSet<ClassInfo>();
  
  public void classLoaded (JVM vm) {
    
    // check for @NonNull static fields w/o clinit
    ClassInfo ci = vm.getLastClassInfo();
    for (FieldInfo fi : ci.getDeclaredStaticFields()) {
      if (fi.getAnnotation("gov.nasa.jpf.NonNull") != null) {
        if (ci.getClinit() == null) {
          throwAssertionError( vm.getLastThreadInfo(),
                               "@NonNull static field without clinit: " + fi.getFullName());
          return;          
        }
        staticFieldCandidates.add(ci);
        break;
      }
    }
    
    // check if there are any @NonNull instance fields
    for (FieldInfo fi : ci.getDeclaredInstanceFields()) {
      if (fi.getAnnotation("gov.nasa.jpf.NonNull") != null) {
        if (!ci.hasCtors()) {
          throwAssertionError( vm.getLastThreadInfo(),
                               "@NonNull instance field without ctor: " + fi.getFullName());
          return;          
        }
        instanceFieldCandidates.add(ci);
        break;
      }
    }
  }
  
  public void instructionExecuted (JVM vm) {
    Instruction insn = vm.getLastInstruction();
    
    if (insn instanceof ARETURN) {  // check @NonNull method returns
      ARETURN areturn = (ARETURN)insn;
      MethodInfo mi = insn.getMethodInfo();
      if (areturn.getReturnValue() == MJIEnv.NULL) {
        // probably faster to directly check for annotation, and not bother with instanceCandidate lookup
        if (mi.getAnnotation("gov.nasa.jpf.NonNull") != null) {
          throwAssertionError( vm.getLastThreadInfo(),
                               "null return from @NonNull method: " + mi.getCompleteName()); 
          return;
        }
      }
      
    } else if (insn instanceof RETURN) {  // check @NonNull fields
      RETURN ret = (RETURN)insn;
      
      // rather than checking every GETFIELD, we only check at the end of
      // each ctor in the inheritance hierarchy if declared @NonNulls are initialized
      
      // NOTE - this is currently per-base, but we could defer the check until the
      // concrete type ctor returns
      
      MethodInfo mi = insn.getMethodInfo();
      
      if (mi.isCtor()) {   // instance field checks
        ClassInfo ci = mi.getClassInfo();
        if (instanceFieldCandidates.contains(ci)) {
          ThreadInfo ti = vm.getLastThreadInfo();
          ElementInfo ei = ti.getElementInfo(ret.getReturnFrame().getThis());

          FieldInfo fi = checkNonNullInstanceField(ci, ei);
          if (fi != null) {
            throwAssertionError(ti, "uninitialized @NonNull instance field: " + fi.getFullName()); 
            return;          
          }
        }
        
      } else if (mi.isClinit()) {  // static field checks
        ClassInfo ci = mi.getClassInfo();
        if (staticFieldCandidates.contains(ci)) {
          FieldInfo fi = checkNonNullStaticField( ci);
          if (fi != null) {
            throwAssertionError( vm.getLastThreadInfo(),
                                 "uninitialized @NonNull static field: " + fi.getFullName());
            return;          
          }
        }
      }
      
    } else if (insn instanceof PUTFIELD) {  // null instance field assignment
      MethodInfo mi = insn.getMethodInfo();
      if (!mi.isCtor()) {
        // probably faster to directly check for annotation, and not bother with instanceCandidate lookup
        PUTFIELD put = (PUTFIELD)insn;
        if (put.getLastValue() == MJIEnv.NULL) {
          FieldInfo fi = put.getFieldInfo();
          if (fi.getAnnotation("gov.nasa.jpf.NonNull") != null) {
            throwAssertionError( vm.getLastThreadInfo(),
                                 "null assignment to @NonNull instance field: " + fi.getFullName());
            return;                    
          }
        }
      }
            
    } else if (insn instanceof PUTSTATIC) {  // null static field assignment
      MethodInfo mi = insn.getMethodInfo();
      if (!mi.isClinit()) {
        // probably faster to directly check for annotation, and not bother with staticCandidate lookup
        PUTSTATIC put = (PUTSTATIC)insn;
        if (put.getLastValue() == MJIEnv.NULL) {
          FieldInfo fi = put.getFieldInfo();
          if (fi.getAnnotation("gov.nasa.jpf.NonNull") != null) {
            throwAssertionError( vm.getLastThreadInfo(),
                                 "null assignment to @NonNull static field: " + fi.getFullName());
            return;
          }
        }
      }
    }
  }
  
  
  
  //--- internal helper methods
  
  void throwAssertionError (ThreadInfo ti, String msg) {
    Instruction nextPc = ti.createAndThrowException("java.lang.AssertionError", msg);
    ti.setNextPC(nextPc);    
  }
  
  FieldInfo checkNonNullInstanceField (ClassInfo ci, ElementInfo ei) {
    
    for (FieldInfo fi : ci.getDeclaredInstanceFields()) {
      if (fi.getAnnotation("gov.nasa.jpf.NonNull") != null) {
        if (ei.getReferenceField(fi) == MJIEnv.NULL) {
          return fi;
        }
      }
    }
    
    return null;
  }
  
  FieldInfo checkNonNullStaticField (ClassInfo ci) {
    ElementInfo ei = ci.getStaticElementInfo();
    
    for (FieldInfo fi : ci.getDeclaredStaticFields()) {
      if (fi.getAnnotation("gov.nasa.jpf.NonNull") != null) {
        if (ei.getReferenceField(fi) == MJIEnv.NULL) {
          return fi;
        }
      }
    }
    
    return null;
  }
  
}
