//
// Copyright (C) 2014 United States Government as represented by the
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
import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.ASTORE;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;
import gov.nasa.jpf.jvm.bytecode.PUTSTATIC;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import java.io.PrintWriter;

/**
 * trace where nulls come from - which is either a GETFIELD/STATIC or an
 * InvokeInstruction. Record which one in an attribute and use that for
 * providing more info when reporting NPEs
 */
public class NullTracker extends ListenerAdapter {

  static abstract class NullSource {
    protected Instruction insn;
    protected ThreadInfo ti;
    protected ElementInfo ei;
    
    NullSource (ThreadInfo ti, Instruction insn, ElementInfo ei){
      this.ti = ti;
      this.insn = insn;
      this.ei = ei;
    }
    
    abstract void printOn (PrintWriter pw);
  }
  
  protected NullSource nullSource;
  
  public NullTracker (Config config, JPF jpf){
    jpf.addPublisherExtension(ConsolePublisher.class, this);
  }
  
  static class LocalSource extends NullSource {
    protected LocalVarInfo local;
    
    public LocalSource (ThreadInfo ti, ASTORE insn, LocalVarInfo local){
      super(ti, insn, null);
      this.local = local;
    }

    @Override
    void printOn (PrintWriter pw){
      MethodInfo mi = insn.getMethodInfo();
      ClassInfo ci = mi.getClassInfo();
            
      pw.println("source of null assignment by " + insn.getMnemonic() + " instruction");
      if (local != null){
        pw.println("   for local:   " + local.getName());
      } else {
        pw.println("   for local:  #" + ((ASTORE)insn).getLocalVariableIndex());
      }
      pw.println("   in method:   " + ci.getName() + '.' + mi.getLongName() + " (" + insn.getFilePos() + ')');
      pw.println("   executed by: " + ti.getName() + " (id=" + ti.getId() + ")");
    }
  }
  
  static class FieldSource extends NullSource {
    public FieldSource (ThreadInfo ti, FieldInstruction insn, ElementInfo ei){
      super(ti,insn,ei);
    }
    
    @Override
    void printOn (PrintWriter pw){
      FieldInfo fi = ((FieldInstruction)insn).getFieldInfo();
      MethodInfo mi = insn.getMethodInfo();
      ClassInfo ci = mi.getClassInfo();
            
      pw.println("source of null reference is " + insn.getMnemonic() + " instruction");
      pw.println("   for field:   " + fi.getFullName());
      pw.println("   in method:   " + ci.getName() + '.' + mi.getLongName() + " (" + insn.getFilePos() + ')');
      pw.println("   executed by: " + ti.getName() + " (id=" + ti.getId() + ")");
    }
  }

  static class MethodSource extends NullSource {
    InvokeInstruction call;
    
    public MethodSource (ThreadInfo ti, ARETURN insn, InvokeInstruction call, ElementInfo ei){
      super(ti,insn,ei);
      this.call = call;
    }
    
    @Override
    void printOn (PrintWriter pw){
      MethodInfo mi = insn.getMethodInfo();
      ClassInfo ci = mi.getClassInfo();
            
      pw.println("source of null reference is return value ");
      pw.println("   from method: " + ci.getName() + '.' + mi.getLongName());
      if (ei != null){
        pw.println("   of object:   " + ei);
      }
      pw.println("   called from: " + call.getFilePos());
      pw.println("   executed by: " + ti.getName() + " (id=" + ti.getId() + ")");
    }    
  }
    
  @Override
  public void executeInstruction (VM vm, ThreadInfo ti, Instruction insn) {
    
    if (insn instanceof ARETURN){
      ARETURN aret = (ARETURN)insn;
      StackFrame frame = ti.getTopFrame();
      int ref = frame.peek();
      if (ref == MJIEnv.NULL){
        StackFrame callerFrame = ti.getCallerStackFrame();
        InvokeInstruction call = (InvokeInstruction)callerFrame.getPC();
        NullSource attr = new MethodSource( ti, aret, call, ti.getThisElementInfo());
        aret.setReturnAttr(ti, attr);
      }
      
    } else if (insn instanceof PUTFIELD || insn instanceof PUTSTATIC){
      FieldInstruction put = (FieldInstruction)insn;
      FieldInfo fi = put.getFieldInfo();
      if (fi.isReference()){
        StackFrame frame = ti.getTopFrame();
        int ref = frame.peek();
        if (ref == MJIEnv.NULL){
          ElementInfo ei = put.peekElementInfo(ti);
          NullSource attr = new FieldSource( ti, put, ei);
          frame.addOperandAttr( attr);
        }
      } 
    }
  }
  
  @Override
  public void instructionExecuted (VM vm, ThreadInfo ti, Instruction nextInsn, Instruction insn){
    
    // we need to do ASTORE post exec since it did overwrite the attr if it had an immediate operand
    if (insn instanceof ASTORE) {
      ASTORE astore = (ASTORE)insn;
      int slotIdx = astore.getLocalVariableIndex();
      StackFrame frame = ti.getTopFrame();
      int ref = frame.getLocalVariable(slotIdx);
      if (ref == MJIEnv.NULL) {
        if (!frame.hasLocalAttr(slotIdx, NullSource.class)) { // otherwise we want to preserve it
          LocalVarInfo lv = astore.getLocalVarInfo();
          NullSource attr = new LocalSource(ti, astore, lv);
          frame.addLocalAttr(slotIdx, attr);
        }
      }
    }
  }
    
  @Override
  public void exceptionThrown(VM vm, ThreadInfo ti, ElementInfo thrownException) {
    if (thrownException.instanceOf("Ljava/lang/NullPointerException;")){
      StackFrame frame = ti.getTopFrame();
      Instruction insn = ti.getPC();
      
      if (insn instanceof FieldInstruction){  // field access on null object
        NullSource attr = frame.getOperandAttr(NullSource.class);
        if (attr != null) {
          nullSource = attr;
        }
        
      } else if (insn instanceof InvokeInstruction) { // call on a null object
        InvokeInstruction call = (InvokeInstruction)insn;
        int argSize = call.getArgSize();
        NullSource attr = frame.getOperandAttr( argSize-1, NullSource.class);
        if (attr != null) {
          nullSource = attr;
        }
      }
    }
  }

  
  @Override
  public void publishPropertyViolation (Publisher publisher) {    
    if (nullSource != null){ // otherwise we don't have anything to report
      PrintWriter pw = publisher.getOut();
      publisher.publishTopicStart("nullTracker " + publisher.getLastErrorId());

      nullSource.printOn(pw);
    }
  }
}
