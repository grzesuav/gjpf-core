//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.GETSTATIC;
import gov.nasa.jpf.jvm.bytecode.InstanceFieldInstruction;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.perturb.OperandPerturbator;
import gov.nasa.jpf.util.FieldSpec;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.MethodSpec;
import gov.nasa.jpf.util.SourceRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * listener that perturbs GETFIELD/GETSTATIC and InvokeInstruction results
 *
 * NOTE - this listener initializes in two steps: (1) during listener construction
 * it builds a list of classes it has to monitor, and (2) during class load
 * time it further analyzes classes from this list to get the actual target
 * objects (FieldInfos and MethodInfos) so that instruction monitoring is
 * efficient enough.
 *
 * This means the listener always has to be instantiated BEFORE the respective
 * target classes get loaded.
 *
 * configuration example:
 *
 *   # field getter example
 *   perturb.fields = altitude,...
 *   perturb.altitude.field = x.y.MyClass.alt
 *   perturb.altitude.class = .perturb.IntOverUnder
 *   perturb.altitude.location = MyClass.java:42
 *   perturb.altitude.delta = 1
 *
 *   # method return value example
 *   perturb.returns = velocity,...
 *   perturb.velocity.method = x.y.MyClass.computeVelocity()
 *   perturb.velocity.class = .perturb.IntOverUnder
 *   perturb.velocity.delta = 50
 *
 */

public class Perturbator extends ListenerAdapter {

  static JPFLogger log = JPF.getLogger("gov.nasa.jpf.Perturbator");

  static class Perturbation {
    SourceRef sref;    // location where field access should be perturbed
    Class<? extends ChoiceGenerator<?>> cgType; // needs to be compatible with field type
    OperandPerturbator perturbator;

    Perturbation (OperandPerturbator perturbator, String loc){
      this.perturbator = perturbator;

      if (loc != null){
        sref = new SourceRef(loc);
      }
    }
  }

  static class FieldPerturbation extends Perturbation {
    FieldSpec fieldSpec;

    FieldPerturbation (FieldSpec fieldSpec, OperandPerturbator perturbator, String loc){
      super(perturbator, loc);

      this.fieldSpec = fieldSpec;
    }
  }

  static class ReturnPerturbation extends Perturbation {
    MethodSpec mthSpec;

    ReturnPerturbation (MethodSpec mthSpec, OperandPerturbator perturbator, String loc){
      super(perturbator, loc);

      this.mthSpec = mthSpec;
    }
  }

  static Class<?>[] argTypes = { Config.class, String.class };


  List<FieldPerturbation> fieldWatchList = new ArrayList<FieldPerturbation>();
  HashMap<FieldInfo,FieldPerturbation> perturbedFields = new HashMap<FieldInfo,FieldPerturbation>();

  List<ReturnPerturbation> returnWatchList = new ArrayList<ReturnPerturbation>();
  HashMap<MethodInfo,ReturnPerturbation> perturbedReturns = new HashMap<MethodInfo,ReturnPerturbation>();

  StackFrame savedFrame;

  public Perturbator (Config conf){

    // in the ctor we only find out which classname patterns we have to watch
    // for, and store them in a list (together with their partially initialized
    // Perturbation instances) that is to be checked upon classLoaded notifications

    // get the configured field perturbators
    String[] fieldIds = conf.getCompactTrimmedStringArray("perturb.fields");
    for (String id : fieldIds){
      addToFieldWatchList(conf, id);
    }

    String[] returnIds = conf.getCompactTrimmedStringArray("perturb.returns");
    for (String id : returnIds){
      addToReturnWatchList(conf, id);
    }
  }

  protected void addToFieldWatchList (Config conf, String id){
    String keyPrefix = "perturb." + id;

    String fs = conf.getString(keyPrefix + ".field");
    if (fs != null) {
      FieldSpec fieldSpec = FieldSpec.createFieldSpec(fs);
      if (fieldSpec != null){
        Object[] args = {conf, keyPrefix};
        OperandPerturbator perturbator = conf.getInstance(keyPrefix + ".class", OperandPerturbator.class, argTypes, args);
        if (perturbator != null) {
          String loc = conf.getString(keyPrefix + ".location");
          FieldPerturbation p = new FieldPerturbation(fieldSpec, perturbator, loc);
          fieldWatchList.add(p);
        } else {
          log.warning("invalid perturbator spec for ", keyPrefix);
        }
      } else {
        log.warning("malformed field specification for ", keyPrefix);
      }

    } else {
      log.warning("missing field specification for ", keyPrefix);
    }
  }

  protected void addToReturnWatchList (Config conf, String id){
    String keyPrefix = "perturb." + id;

    String ms = conf.getString(keyPrefix + ".method");
    if (ms != null) {
      MethodSpec mthSpec = MethodSpec.createMethodSpec(ms);
      if (mthSpec != null) {
        Object[] args = {conf, keyPrefix};
        OperandPerturbator perturbator = conf.getInstance(keyPrefix + ".class", OperandPerturbator.class, argTypes, args);
        if (perturbator != null) {
          String loc = conf.getString(keyPrefix + ".location");
          ReturnPerturbation p = new ReturnPerturbation(mthSpec, perturbator, loc);
          returnWatchList.add(p);
        } else {
          log.warning("invalid perturbator spec for ", keyPrefix);
        }

      } else {
        log.warning("malformed method specification for ", keyPrefix);
      }

    } else {
      log.warning("missing method specification for ", keyPrefix);
    }
  }


  public void classLoaded (JVM jvm){
    // this one takes the watchlists, finds out if the loaded class matches
    // any of the watch entries, and in case it does fully initializes
    // the corresponding Perturbation object with the target construct
    // (MethodInfo, FieldInfo) we use to identify relevant ops during
    // instruction execution notifications

    ClassInfo ci = jvm.getLastClassInfo();
    String clsName = ci.getName();

    for (FieldPerturbation p : fieldWatchList){
      FieldSpec fs = p.fieldSpec;
      if (fs.isMatchingType(ci)){
        addFieldPerturbations( p, ci, ci.getDeclaredInstanceFields());
        addFieldPerturbations( p, ci, ci.getDeclaredStaticFields());
      }
    }

    for (ReturnPerturbation p : returnWatchList){
      MethodSpec ms = p.mthSpec;
      if (ms.isMatchingType(ci)){
        for (MethodInfo mi : ci.getDeclaredMethodInfos()){
          if (ms.matches(mi)){
            Class<? extends ChoiceGenerator<?>> returnCGType = mi.getReturnChoiceGeneratorType();
            Class<? extends ChoiceGenerator<?>> perturbatorCGType = p.perturbator.getChoiceGeneratorType();
            if (returnCGType.isAssignableFrom(perturbatorCGType)){
              p.cgType = returnCGType;
              perturbedReturns.put(mi, p);
            } else {
              log.warning("method " + mi + " not compatible with perturbator choice type " + perturbatorCGType.getName());
            }
          }
        }
      }
    }
  }

  protected void addFieldPerturbations (FieldPerturbation p, ClassInfo ci, FieldInfo[] fieldInfos){
    for (FieldInfo fi : ci.getDeclaredInstanceFields()) {
      if (p.fieldSpec.matches(fi)) {
        Class<? extends ChoiceGenerator<?>> fieldCGType = fi.getChoiceGeneratorType();
        Class<? extends ChoiceGenerator<?>> perturbatorCGType = p.perturbator.getChoiceGeneratorType();
        if (fieldCGType.isAssignableFrom(perturbatorCGType)) {
          p.cgType = fieldCGType;
          perturbedFields.put(fi, p);
        } else {
          log.warning("field " + fi + " not compatible with perturbator choice type " + perturbatorCGType.getName());
        }
      }
    }
  }

  protected boolean isRelevantCallLocation (ThreadInfo ti, Perturbation p){
    if (p.sref == null){
      // no caller location specified -> all calls relevant
      return true;
    } else {
      StackFrame caller = ti.getCallerStackFrame();
      if (caller != null) {
        Instruction invokeInsn = caller.getPC();
        return p.sref.equals(invokeInsn.getFilePos());
      } else {
        return false;
      }
    }
  }

  public void executeInstruction (JVM vm){
    Instruction insn = vm.getLastInstruction();
    ThreadInfo ti = vm.getLastThreadInfo();

    if (insn instanceof GETFIELD){
      FieldInfo fi = ((InstanceFieldInstruction)insn).getFieldInfo();
      FieldPerturbation e = perturbedFields.get(fi);

      if (e != null) {  // managed field
        if (isMatchingInstructionLocation(e,insn)) {
          if (!ti.isFirstStepInsn()){
            // save the current stackframe so that we can restore it before
            // we re-execute
            savedFrame = ti.getTopFrame().clone();
          }
        }
      }

    } else if (insn instanceof ReturnInstruction){
      MethodInfo mi = insn.getMethodInfo();
      ReturnPerturbation e = perturbedReturns.get(mi);

      if (e != null && isRelevantCallLocation(ti, e)){
        SystemState ss = vm.getSystemState();

        if (!ti.isFirstStepInsn()){
          // first time, create & set CG but DO NOT execute the insn since it would
          // pop the callee stackframe and modify the caller stackframe
          // note that we don't need to execute in order to get the perturbation base
          // value because its already on the operand stack
          ChoiceGenerator<?> cg = e.perturbator.createChoiceGenerator(ti.getTopFrame(), 0);
          if (cg != null) {
            ss.setNextChoiceGenerator(cg);
            ti.setNextPC(insn); // reexecute
            ti.skipInstruction();
          }

        } else {
          // re-executing, modify the operand stack top and execute
          ChoiceGenerator<?> cg = ss.getInsnChoiceGeneratorOfType(e.cgType, insn, null);
          if (cg != null) {
            e.perturbator.perturb(cg, ti.getTopFrame(), 0);
          }
        }
      }

    }
  }

  public void instructionExecuted(JVM vm) {
    Instruction insn = vm.getLastInstruction();
    ThreadInfo ti = vm.getLastThreadInfo();

    if (insn instanceof GETFIELD){
      FieldInfo fi = ((InstanceFieldInstruction)insn).getFieldInfo();
      FieldPerturbation p = perturbedFields.get(fi);
      if (p != null){
        if (isMatchingInstructionLocation(p, insn)) {  // none or managed filePos
          StackFrame frame = ti.getTopFrame();
          SystemState ss = vm.getSystemState();

          if (ti.isFirstStepInsn()) { // retrieve value from CG and replace it on operand stack
            ChoiceGenerator<?> cg = ss.getInsnChoiceGeneratorOfType(p.cgType, insn, null);
            if (cg != null) {
              p.perturbator.perturb(cg, frame, 0);
            } else {
              log.warning("wrong choice generator type ", cg);
            }

          } else { // first time around, create&set the CG and reexecute
            ChoiceGenerator<?> cg = p.perturbator.createChoiceGenerator(frame, 0);
            if (cg != null) {
              assert savedFrame != null;

              ss.setNextChoiceGenerator(cg);
              // we could more efficiently restore the stackframe
              // to pre-exec state from last 'this' or classobject ref, but then
              // we have to deal with different field value sizes
              ti.swapTopFrame(savedFrame);
              ti.setNextPC(insn); // reexecute

              savedFrame = null;
            }
          }
        }
      }
    }
  }


  protected boolean isMatchingInstructionLocation (Perturbation p, Instruction insn){
    return p.sref == null || p.sref.equals(insn.getFilePos());
  }

}
