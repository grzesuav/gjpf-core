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

package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.bytecode.InstanceFieldInstruction;
import gov.nasa.jpf.jvm.bytecode.VirtualInvocation;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.util.IntSet;
import gov.nasa.jpf.util.SortedArrayIntSet;
import gov.nasa.jpf.util.StateExtensionClient;
import gov.nasa.jpf.util.StateExtensionListener;
import gov.nasa.jpf.util.StringSetMatcher;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.ThreadInfo;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * listener that keeps track of all operations on objects that are specified by
 * reference value or types
 */
public class ObjectTracker extends ListenerAdapter implements StateExtensionClient {
  
  static class Attr {
    // nothing here, just a tag
  }
  
  static final Attr ATTR = new Attr(); // we need only one
  
  enum OpType { NEW, CALL, FIELD, RECYCLE };
  
  static class LogRecord {
    ElementInfo ei;
    ThreadInfo ti;
    Instruction insn;
    OpType opType;
    
    LogRecord prev;
    
    LogRecord (OpType opType, ElementInfo ei, ThreadInfo ti, Instruction insn, LogRecord prev){
      this.opType = opType;
      this.ei = ei;
      this.ti = ti;
      this.insn = insn;
      this.prev = prev;
    }
    
    void printOn (PrintWriter pw){
      if (prev == null || ti != prev.ti){
        pw.printf("-------------------------------------- %s  id=%d\n", ti.getName(), ti.getId());
      }
      
      pw.printf( "%-8s %s ", opType.toString(), ei.toString());
      
      if (insn != null){
        pw.print(insn);
        
        if (insn instanceof FieldInstruction){
          FieldInstruction finsn = (FieldInstruction)insn;
          String fname = finsn.getFieldName();
          pw.print(' ');
          pw.print(fname);
        }
      }
      
      pw.println();
    }
  }
  
  protected LogRecord log; // needs to be state restored
  
  //--- log options  
  protected StringSetMatcher includeClasses, excludeClasses; // type name patterns
  protected IntSet trackedRefs;
  
  protected boolean logFieldAccess;
  protected boolean logCalls;

    
  
  //--- internal stuff
  
  public ObjectTracker (Config conf, JPF jpf) {
    includeClasses = StringSetMatcher.getNonEmpty(conf.getStringArray("ot.include"));
    excludeClasses = StringSetMatcher.getNonEmpty(conf.getStringArray("ot.exclude", new String[] { "*" }));

    trackedRefs = new SortedArrayIntSet();
    
    int[] refs = conf.getIntArray("ot.refs");
    if (refs != null){
      for (int i=0; i<refs.length; i++){
        trackedRefs.add(refs[i]);
      }
    }
    
    logCalls = conf.getBoolean("ot.log_calls", true);
    logFieldAccess = conf.getBoolean("ot.log_fields", true);
    
    jpf.addPublisherExtension(ConsolePublisher.class, this);
  }
    
  protected void log (OpType opType, ElementInfo ei, ThreadInfo ti, Instruction insn){
    log = new LogRecord( opType, ei, ti, insn,  log);
  }
  
  
  //--- Listener interface
  
  @Override
  public void classLoaded (VM vm, ClassInfo ci){
    if (StringSetMatcher.isMatch(ci.getName(), includeClasses, excludeClasses)){
      ci.addAttr(ATTR);
    }
  }
  
  @Override
  public void objectCreated (VM vm, ThreadInfo ti, ElementInfo ei) {
    ClassInfo ci = ei.getClassInfo();
    int ref = ei.getObjectRef();
    
    if (ci.hasAttr(Attr.class) || trackedRefs.contains(ref)){
      // it's new, we don't need to call getModifiable
      ei.addObjectAttr(ATTR);
      log( OpType.NEW, ei, ti, ti.getPC());
    }
  }
  
  @Override
  public void objectReleased (VM vm, ThreadInfo ti, ElementInfo ei) {
    if (ei.hasObjectAttr(Attr.class)){
      log( OpType.RECYCLE, ei, ti, ti.getPC());      
    }
  }

  @Override
  public void instructionExecuted (VM vm, ThreadInfo ti, Instruction nextInsn, Instruction executedInsn){
    
    if (logCalls && executedInsn instanceof VirtualInvocation){      
      if (nextInsn != executedInsn){ // otherwise we didn't enter
        VirtualInvocation call = (VirtualInvocation)executedInsn;
        int ref = call.getCalleeThis(ti);
        ElementInfo ei = ti.getElementInfo(ref);
        
        if (ei.hasObjectAttr(Attr.class)) {
          log( OpType.CALL, ei, ti, executedInsn);
        }
      }
      
    } else if (logFieldAccess && executedInsn instanceof InstanceFieldInstruction){
      if (nextInsn != executedInsn){ // otherwise we didn't enter
        InstanceFieldInstruction finsn = (InstanceFieldInstruction) executedInsn;
        ElementInfo ei = finsn.getLastElementInfo();
        
        if (ei.hasObjectAttr(Attr.class)) {
          log( OpType.FIELD, ei, ti, executedInsn);
        }
      }
    }
  }

  //--- state store/restore
  
  public Object getStateExtension () {
    return log;
  }

  public void restore (Object stateExtension) {
    log = (LogRecord)stateExtension;
  }

  public void registerListener (JPF jpf) {
    StateExtensionListener<Number> sel = new StateExtensionListener(this);
    jpf.addSearchListener(sel);
  }

  
  //--- reporting
  
  @Override
  public void publishPropertyViolation (Publisher publisher) {    
    if (log != null){ // otherwise we don't have anything to report
      PrintWriter pw = publisher.getOut();
      publisher.publishTopicStart("ObjectTracker " + publisher.getLastErrorId());
      printLogOn(pw);
    }
  }

  protected void printLogOn (PrintWriter pw){
    // the log can be quite long so we can't use recursion (Java does not optimize tail recursion)
    List<LogRecord> logRecs = new ArrayList<LogRecord>();
    for (LogRecord lr = log; lr != null; lr = lr.prev){
      logRecs.add(lr);
    }
    
    Collections.reverse(logRecs);
    
    for (LogRecord lr : logRecs){
      lr.printOn(pw);
    }
  }
}
