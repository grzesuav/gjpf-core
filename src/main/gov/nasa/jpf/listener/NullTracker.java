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
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;
import gov.nasa.jpf.jvm.bytecode.PUTSTATIC;
import gov.nasa.jpf.jvm.bytecode.RETURN;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
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
    
    protected NullSource cause;
    
    NullSource (ThreadInfo ti, Instruction insn, ElementInfo ei){
      this.ti = ti;
      this.insn = insn;
      this.ei = ei;
    }
    
    void setCause (NullSource cause){
      this.cause = cause;
    }
    
    abstract void printOn (PrintWriter pw);
    
    void printInsnOn (PrintWriter pw){
      pw.printf("    instruction: %s [%d]\n", insn.getMnemonic(), insn.getPosition());
    }
        
    void printThreadInfoOn (PrintWriter pw){
      pw.println("    executed by: " + ti.getName() + " (id=" + ti.getId() + ")");
    }
    
    void printMethodInfoOn (PrintWriter pw, String msg, Instruction instruction){
      MethodInfo mi = instruction.getMethodInfo();
      ClassInfo ci = mi.getClassInfo();
      pw.println( msg + ci.getName() + '.' + mi.getLongName() + " (" + instruction.getFilePos() + ')');
    }
    
    void printCauseOn (PrintWriter pw){
      if (cause != null){
        pw.println("set by: ");
        cause.printOn(pw);
      }
    }
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
      printInsnOn(pw);
      if (local != null){
        pw.println("     for local: " + local.getName());
      } else {
        pw.println("     for local: #" + ((ASTORE)insn).getLocalVariableIndex());
      }
      printMethodInfoOn(pw, "      in method: ", insn);
      printThreadInfoOn(pw);
      
      printCauseOn(pw);
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
            
      printInsnOn(pw);
      pw.println("      for field: " + fi.getFullName());
      printMethodInfoOn(pw, "      in method: ", insn);
      printThreadInfoOn(pw);
      
      printCauseOn(pw);
    }
  }

  static class MethodSource extends NullSource {
    InvokeInstruction call;
    
    public MethodSource (ThreadInfo ti, ReturnInstruction insn, InvokeInstruction call, ElementInfo ei){
      super(ti,insn,ei);
      this.call = call;
    }
    
    @Override
    void printOn (PrintWriter pw){            
      printInsnOn(pw);
      printMethodInfoOn(pw, "      of method: ", insn);
      
      if (ei != null){
        pw.println("     for object: " + ei);
      }
      printMethodInfoOn(pw, "      called by: ", call);
      printThreadInfoOn(pw);
      
      printCauseOn(pw);
    }    
  }
  
  static class CtorSource extends MethodSource {
    public CtorSource (ThreadInfo ti, RETURN insn, InvokeInstruction call, ElementInfo ei){
      super(ti,insn,call, ei);
    }
    
    @Override
    void printOn (PrintWriter pw){ 
      printMethodInfoOn(pw, "   missing init: ", insn);
      
      if (ei != null){
        pw.println("     for object: " + ei);
      }
      printMethodInfoOn(pw, "      called by: ", call);
      printThreadInfoOn(pw);
      
      printCauseOn(pw);
    }    
  }
    
  
  protected void checkAndSetPreCtorSources (ThreadInfo ti, RETURN insn){
    MethodInfo mi = insn.getMethodInfo();
    if (mi.isCtor()) {
      StackFrame callerFrame = null;
      InvokeInstruction call = null;
      ElementInfo ei = ti.getThisElementInfo();
      ClassInfo ci = ei.getClassInfo();
      int nInstance = ci.getNumberOfDeclaredInstanceFields();

      for (int i = 0; i < nInstance; i++) {
        FieldInfo fi = ci.getDeclaredInstanceField(i);
        if (fi.isReference()) {
          int ref = ei.getReferenceField(fi);
          if (ref == MJIEnv.NULL) {
            ei = ei.getModifiableInstance();  // why do we need this in a ctor?
            if (call == null) {
              callerFrame = ti.getCallerStackFrame();
              call = (InvokeInstruction) callerFrame.getPC();
            }
            NullSource attr = new CtorSource(ti, insn, call, ti.getThisElementInfo());
            ei.setFieldAttr(fi, attr);
          }
        }
      }
    }
  }
  
  protected void checkAndSetPrePutSources (ThreadInfo ti, FieldInstruction put){
    FieldInfo fi = put.getFieldInfo();
    if (fi.isReference()) {
      StackFrame frame = ti.getTopFrame();
      int ref = frame.peek();

      if (ref == MJIEnv.NULL) { // field set to null
        ElementInfo ei = put.peekElementInfo(ti);
        NullSource attr = new FieldSource(ti, put, ei);

        NullSource cause = frame.getOperandAttr(NullSource.class);
        if (cause != null) {
          attr.setCause(cause);
          frame.replaceOperandAttr(cause, attr);
        } else {
          frame.addOperandAttr(attr);
        }

      } else { // not null anynmore
        NullSource attr = frame.getOperandAttr(NullSource.class);
        if (attr != null) {
          frame.removeOperandAttr(attr);
        }
      }
    }    
  }
  
  protected void checkAndSetPreAreturnSources (ThreadInfo ti, ARETURN aret){
    StackFrame frame = ti.getTopFrame();
    int ref = frame.peek();
    if (ref == MJIEnv.NULL) {
      StackFrame callerFrame = ti.getCallerStackFrame();
      InvokeInstruction call = (InvokeInstruction) callerFrame.getPC();
      NullSource attr = new MethodSource(ti, aret, call, ti.getThisElementInfo());

      NullSource cause = frame.getOperandAttr(NullSource.class);
      if (cause != null) {
        attr.setCause(cause);
        frame.replaceOperandAttr(cause, attr);
      } else {
        frame.addOperandAttr(attr);
      }
    }
  }
  
  @Override
  public void executeInstruction (VM vm, ThreadInfo ti, Instruction insn) {
    
    if (insn instanceof ARETURN){
      checkAndSetPreAreturnSources( ti, (ARETURN)insn);
      
    } else if (insn instanceof PUTFIELD || insn instanceof PUTSTATIC){
      checkAndSetPrePutSources( ti, (FieldInstruction) insn);
      
    } else if (insn instanceof RETURN){
      checkAndSetPreCtorSources(ti, (RETURN) insn);
    }
  }

  protected void checkAndSetPostAstoreSources (ThreadInfo ti, ASTORE astore){
    int slotIdx = astore.getLocalVariableIndex();
    StackFrame frame = ti.getTopFrame();
    int ref = frame.getLocalVariable(slotIdx);
    if (ref == MJIEnv.NULL) {
      LocalVarInfo lv = astore.getLocalVarInfo();
      NullSource attr = new LocalSource(ti, astore, lv);

      NullSource cause = frame.getLocalAttr(slotIdx, NullSource.class);
      if (cause != null) {
        attr.setCause(cause);
        frame.replaceLocalAttr(slotIdx, cause, attr);
      } else {
        frame.addLocalAttr(slotIdx, attr);
      }
    }
  }
  
  @Override
  public void instructionExecuted (VM vm, ThreadInfo ti, Instruction nextInsn, Instruction insn){
    
    // we need to do ASTORE post exec since it did overwrite the attr if it had an immediate operand
    if (insn instanceof ASTORE) {
      checkAndSetPostAstoreSources( ti, (ASTORE)insn);
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
      publisher.publishTopicStart("NullTracker " + publisher.getLastErrorId());

      pw.println("null value set by: ");
      nullSource.printOn(pw);
    }
  }
}
