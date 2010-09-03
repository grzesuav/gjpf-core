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
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.GETSTATIC;
import gov.nasa.jpf.jvm.bytecode.InstanceFieldInstruction;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.perturb.OperandPerturbator;
import gov.nasa.jpf.util.JPFLogger;
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
 *   perturb.fields = altitude,...
 *   perturb.altitude.field = x.y.MyClass.alt
 *   perturb.altitude.class = .perturb.IntOverUnder
 *   perturb.altitude.location = MyClass.java:42
 *   perturb.altitude.delta = 1
 *
 */

public class Perturbator extends ListenerAdapter {

  static JPFLogger log = JPF.getLogger("gov.nasa.jpf.Perturbator");

  static class Entry {
    String fieldName;
    SourceRef sref;    // location where field access should be perturbed
    Class<? extends ChoiceGenerator<?>> cgType; // needs to be compatible with field type
    OperandPerturbator perturbator;
  }

  HashMap<String,List<Entry>> fieldWatchList = new HashMap<String,List<Entry>>();
  HashMap<FieldInfo,Entry> perturbedFields = new HashMap<FieldInfo,Entry>();

  static Class<?>[] argTypes = { Config.class, String.class };

  public Perturbator (Config conf){

    // get the configured field perturbators
    String[] fieldIds = conf.getCompactTrimmedStringArray("perturb.fields");
    for (String id : fieldIds){
      addToFieldWatchList(conf, id);
    }

  }

  protected void addToFieldWatchList (Config conf, String id){
    String keyPrefix = "perturb." + id;

    String fieldId = conf.getString(keyPrefix + ".field");
    if (fieldId != null) {
      int idx = fieldId.lastIndexOf('.');
      if (idx > 0) {
        String clsName = fieldId.substring(0, idx);
        String fieldName = fieldId.substring(idx + 1);

        List<Entry> watchFields = fieldWatchList.get(clsName);
        if (watchFields == null) {
          watchFields = new ArrayList<Entry>();
          fieldWatchList.put(clsName, watchFields);
        }

        Object[] args = { conf, keyPrefix };
        OperandPerturbator perturbator = conf.getInstance(keyPrefix + ".class", OperandPerturbator.class, argTypes, args);
        if (perturbator != null){
          Entry e = new Entry();
          e.fieldName = fieldName;
          e.perturbator = perturbator;

          String loc = conf.getString(keyPrefix + ".location");
          if (loc != null){
            e.sref = new SourceRef(loc);
          }

          watchFields.add(e);

        } else {
          log.warning("invalid perturbator spec for ", id );
        }
      }
    } else {
      log.warning("missing field id specification for ", id);
    }
  }

  public void classLoaded (JVM jvm){
    ClassInfo ci = jvm.getLastClassInfo();
    String clsName = ci.getName();

    List<Entry> watchFields = fieldWatchList.get(clsName);
    if (watchFields != null){
      for (Entry e : watchFields){
        FieldInfo fi = ci.getDeclaredInstanceField(e.fieldName);
        if (fi == null){
          fi = ci.getDeclaredStaticField(e.fieldName);
          if (fi == null){
            log.warning("unknown perturbed field ", e.fieldName, "in ", clsName);
            return;
          }
        }

        perturbedFields.put(fi, e);

        // now that we know the field type, lets get the compatible CG type
        Class<? extends ChoiceGenerator<?>> cgType = fi.getChoiceGeneratorType();
        if (cgType == null) {
          log.warning("unknown choice generator for field type ", fi.getType());
          return;
        }
        e.cgType = cgType;
      }

      // we processed all requested fields for this class, so it's safe to remove
      fieldWatchList.remove(clsName);
    }
  }

  public void executeInstruction (JVM vm){
    Instruction insn = vm.getLastInstruction();
    ThreadInfo ti = vm.getLastThreadInfo();

    // save operand stack for relevant ops to allow re-execution
  }

  public void instructionExecuted(JVM vm) {
    Instruction insn = vm.getLastInstruction();
    ThreadInfo ti = vm.getLastThreadInfo();

    if (insn instanceof GETFIELD){
      FieldInfo fi = ((InstanceFieldInstruction)insn).getFieldInfo();
      Entry e = perturbedFields.get(fi);

      if (e != null){  // managed field
        if (e.sref == null || e.sref.equals(insn.getFilePos())){  // none or managed filePos
          StackFrame frame = ti.getTopFrame();
          SystemState ss = vm.getSystemState();

          if (ti.isFirstStepInsn()) { // retrieve value from CG and replace it on operand stack
            ChoiceGenerator<?> cg = ss.getInsnChoiceGeneratorOfType(e.cgType, insn, null);
            if (cg != null){
              e.perturbator.perturb(cg, frame, 0);
            } else {
              log.warning("wrong choice generator type ", cg);
            }
            
          } else {
            ChoiceGenerator<?> cg = e.perturbator.createChoiceGenerator(frame, 0);
            if (cg != null){
              ss.setNextChoiceGenerator(cg);

              // we have to restore stack contents
              int objref = ((InstanceFieldInstruction)insn).getLastThis();
              frame.setOperand(0, objref, true);

              ti.setNextPC(insn); // reexecute
            }
          }
        }
      }
    }
  }

}
