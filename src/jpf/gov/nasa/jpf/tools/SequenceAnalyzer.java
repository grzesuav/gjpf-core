package gov.nasa.jpf.tools;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.jvm.AnnotationInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.bytecode.ASTORE;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;
import gov.nasa.jpf.jvm.bytecode.PUTSTATIC;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.verify.SequenceOp;
import gov.nasa.jpf.verify.SequenceProcessor;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.util.Trace;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class SequenceAnalyzer extends ListenerAdapter {

  public static class Exception extends RuntimeException {
    public Exception (String msg) {
      super(msg);
    }
  }
  
  // our default processor writes out sequences in a Java-like format
  // that is compatible to Alex Moffat's SEQUENCE diagram editor
  class DefaultProcessor extends SequenceProcessor {
    int level;
    int nSequences = 0;
    String fname;
    PrintWriter pw;
    
    public void processSequence (String id, Trace<SequenceOp> trace) {
      level=0;
      fname = config.getString("sequence.out", (config.getTargetArg() + '-' + nSequences + ".seq"));
      
      try {
        pw = new PrintWriter(fname);
      } catch (IOException iox) {
        pw = new PrintWriter(System.out);
      }
      
      super.processSequence(id, trace);

      pw.close();
      
      nSequences++;
      sequenceProcessed("generated sequence file: " + fname);
    }
        
    public void visit (SequenceOp.Enter s) {
      String res = s.getResult();
      
      if (res == null || res.length() == 0) {
        res = "' '";  // SEQUENCE peculiarity
      }
      
      indent();
      pw.println( s.getTgt() + '.' + s.getScope() + " -> " + res + " {");
      level++;
    }
    
    public void visit (SequenceOp.Exit s) {
      level--;
      indent();
      pw.println("}");
    }
    
    void indent() {
      for (int i=0; i<level; i++) {
        pw.print("  ");
      }
    }
  }
  
  // <2do> we should really use pools for SequenceInfo and SequenceContext
  
  static class SequenceInfo implements Cloneable {
    String id;
    boolean isActive;
    
    String[] objects;   // our symbolic participant names
    int[] objRefs;      // reference values for participants
    String[] localVars; // (optional) local var/arg names that identify participants

    void parseObjectSpecs (String[] objSpecs) {
      ArrayList<String[]> newEntries = new ArrayList<String[]>();
      
      for (int i=0; i<objSpecs.length; i++) {
        String os = objSpecs[i];
        String name, var = null;
        
        String[] e = os.split(" *[:=] *");
        if (e.length == 1) { // just an object name
          name = e[0];
        } else if (e.length == 2) { // "<object>=<var>" pair
          name = e[0];
          var = e[1];
        } else {
          throw new Exception("illegal object spec: " + os);
        }
        
        if (objects != null) { // check if this object is already defined
          for (int j=0; j<objects.length; j++) {
            if (objects[j].equals(name)) {
              if (var != null) {
                throw new Exception("object variable already defined: " + os);
              } else {
                continue; // have it
              }
            }
          }
        }
        
        newEntries.add(e);        
      }
      
      if (!newEntries.isEmpty()) { // append the new entries
        String[] newObjects = new String[objSpecs.length];
        int[] newRefs = new int[objSpecs.length];
        String[] newLocalVars = new String[newObjects.length];
        
        int j=0;
        if (objects != null) {
          for (j=0; j<objects.length; j++) {
            newObjects[j] = objects[j];
            newRefs[j] = objRefs[j];
          }
        }
        
        for (String[] e : newEntries) {
          newObjects[j] = e[0];
          if (e.length > 1){
            newLocalVars[j] = e[1];
          }
          j++;
        }
        
        objects = newObjects;
        objRefs = newRefs;
        localVars = newLocalVars;
      }
    }
    
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException x) {
        // no dice
        return null;
      }
    }
    
    void resolveReferences (InvokeInstruction call, ThreadInfo ti) {
      if (localVars != null) {
        for (int i=0; i<localVars.length; i++) {
          String vn = localVars[i];
          if (vn != null) {
            Object v = call.getFieldOrArgumentValue(vn, ti);
            if (v != null) {
              if (v instanceof ElementInfo) {
                objRefs[i] = ((ElementInfo)v).getIndex();
              } else {
                throw new Exception("field or argument not a reference: " + vn);
              }
            }
          }
        }
      }
    }
    
    // for method scope sequences
    SequenceInfo (String id, String[] objSpecs, InvokeInstruction call, ThreadInfo ti){
      this.id = id;
      init(objSpecs, call, ti);
    }
        
    public SequenceInfo(String id, String name, int ref) {
      this.id = id;
      
      isActive = false;
      
      objects = new String[1];
      objects[0] = name;
      
      objRefs = new int[1];
      objRefs[0] = ref;
    }

    String getId() {
      return id;
    }
    
    boolean isActive() {
      return isActive;
    }
    
    void init (String[] objSpecs, InvokeInstruction call, ThreadInfo ti) {
      isActive = true;
      
      parseObjectSpecs(objSpecs);
      resolveReferences(call, ti);      
    }
    
    String getObjectName (int ref) {
      if (objRefs != null) {
        for (int i=0; i<objRefs.length; i++) {
          if (objRefs[i] == ref) {
            return objects[i];
          }
        }
      }
      return null;
    }
    
    SequenceInfo setActive(boolean b) {
      if (b != isActive) {
        SequenceInfo si = (SequenceInfo)clone();
        si.isActive = b;
        return si;
      } else {
        return this;
      }
    }
    
    SequenceInfo activate (String[] objSpecs, InvokeInstruction call, ThreadInfo ti) {
      SequenceInfo si = (SequenceInfo)clone();
      init(objSpecs, call, ti);
      return si;
    }
    
    SequenceInfo addRef (String name, int ref) {
      String[] vn = null;
      int[] or = null;
      
      if (objRefs != null){
        for (int i=0; i<objRefs.length; i++) {
          if (objRefs[i] == ref) {
            return this;
          }
        }
        
        or = new int[objRefs.length+1];
        System.arraycopy(objRefs, 0, or, 0, objRefs.length);
        or[objRefs.length] = ref;
        
        vn = new String[or.length];
        System.arraycopy(objects,0,vn,0,objects.length);
        vn[objects.length] = name;        
        
      } else { // this is the first one
        or = new int[1];
        or[0] = ref;
        vn = new String[1];
        vn[0] = name;
      }
      
      SequenceInfo si = (SequenceInfo)clone();
      si.objects = vn;
      si.objRefs = or;
      return si;
    }

    SequenceInfo setLocalVarRef (String name, int ref) {
      if (localVars != null) {
        for (int i=0; i<localVars.length; i++) {
          if (localVars[i].equals(name)) {
            SequenceInfo si = (SequenceInfo)clone();
            si.objRefs = objRefs.clone();
            si.objRefs[i] = ref;
            return si;
          }
        }
      }
      
      throw new Exception("trying to set reference for unknown object: " + name);
    }
    
    
    boolean containsRef (int ref) {
      if (objRefs != null) {
        for (int i=0; i<objRefs.length; i++) {
          if (objRefs[i] == ref) {
            return true;
          }
        }
      }
      
      return false;
    }
    
    boolean containsVar (String varName) {
      if (localVars != null) {
        for (int i=0; i<localVars.length; i++) {
          if (localVars[i].equals(varName)) {
            return true;
          }
        }
      }
      return false;
    }

  }
  
  
  static class SequenceContext {
    SequenceInfo[] list;

    SequenceContext (SequenceInfo[] list){
      this.list = list;
    }
    
    int getIdx (String id) {
      if (list != null) {
        for (int i=0; i<list.length; i++) {
          if (list[i].getId().equals(id)) {
            return i;
          }
        }
      }
      return -1;
    }

    SequenceInfo get (int idx) {
      return list[idx];
    }
    
    SequenceInfo get (String id) {
      int idx = getIdx(id);
      if (idx >=0) {
        return list[idx];
      } else {
        return null;
      }
    }
    
    SequenceContext addSequence (SequenceInfo seq) {
      if (list == null) {
        SequenceInfo[] l = new SequenceInfo[1];
        l[0] = seq;
        return new SequenceContext(l);
        
      } else {
        if (getIdx(seq.getId()) < 0) {
          SequenceInfo[] l = new SequenceInfo[list.length+1];
          System.arraycopy(list,0, l, 0, list.length);
          l[list.length] = seq;
          return new SequenceContext(l);
        } else {
          return this;
        }
      }
    }
    
    SequenceContext removeSequence (SequenceInfo seq) {
      if (list != null) {
        int i = getIdx(seq.getId());
        if (i >=0) {
          SequenceInfo[] l = new SequenceInfo[list.length-1];
          System.arraycopy(list, 0, l, 0, i);
          int i1 = i+1;
          System.arraycopy(list, i1, l, i1, list.length-i1);
          return new SequenceContext(l);
        }
      }
      
      return this;
    }
        
    SequenceContext replaceSequence (int idx, SequenceInfo seq) {
      if (list != null) {
        if (idx >=0 && idx < list.length) {
          SequenceInfo[] l = list.clone();
          l[idx] = seq;
          return new SequenceContext(l);
        }
      }
      
      return this;
    }
        
    boolean containsRef (String id, int ref) {
      if (list != null) {
        int i = getIdx(id);
        if (i >=0) {
          return list[i].containsRef(ref);
        }
      }
      return false;      
    }
  }
  
    
  //----- this is where we store the traces
  // (we could keep the traces separate, but that looses the order info and
  // prohibits reasoning about combinations of sequences)
  Trace<SequenceOp> sequenceOps = new Trace<SequenceOp>();
  Trace<SequenceContext> sequenceContext = new Trace<SequenceContext>();
  
  // for optimization
  HashMap<MethodInfo,AnnotationInfo> annotations = new HashMap<MethodInfo,AnnotationInfo>();
 
  Config config;
  ArrayList<String> processedSequences;
  SequenceProcessor processor;
  
  public SequenceAnalyzer (Config conf, JPF jpf) {
    
    config = conf;
    processedSequences = new ArrayList<String>();
    
    try {
      processor = conf.getInstance("sequence.processor.class", SequenceProcessor.class);
    } catch (Config.Exception x) {
      throw new SequenceAnalyzer.Exception("cannot instantiate sequence processor");
    }
    if (processor == null) {
      processor = new DefaultProcessor();
    }
    
    jpf.addPublisherExtension(ConsolePublisher.class, this);
  }
  
  boolean isActiveSequence (String id) {
    SequenceContext sc = sequenceContext.getLastOp();
    if (sc != null) {
      return (sc.get(id) != null);
    } else {
      return false;
    }
  }
  
  boolean isSequenceObject (String id, int ref) {
    SequenceContext sc = sequenceContext.getLastOp();
    if (sc != null) {
      return sc.containsRef(id,ref);
    } else {
      return false;
    }
  }

  
  AnnotationInfo getAnnotation(MethodInfo mi, String aiName) {
    for (MethodInfo m=mi; m != null; m = m.getOverriddenMethodInfo()) {
      AnnotationInfo ai = m.getAnnotation(aiName);
      if (ai != null) {
        return ai;
      }
    }
    return null;
  }
   
  AnnotationInfo getAnnotation(FieldInfo fi, String aiName) {
    return fi.getAnnotation(aiName);
  }
  
  
  String getEventName (SequenceInfo si, MethodInfo mi, String annotationName) {
    
    return mi.getName();
    
    /*** BCEL bug workaround
    if (annotationName.equals("<method>")) {
      return mi.getName();
    } else {
      return annotationName;
    }
    ****/
  }
  
  protected void processSequence (String id) {
    processor.processSequence(id, sequenceOps);
  }
  
  protected void processAllOpenSequences() {
    // <2do>
  }
  
  public void sequenceProcessed (String msg) {
    processedSequences.add(msg);
  }
  
  //----- update squenceContext and store sequenceOps
  
  public void instructionExecuted (JVM vm) {
    Instruction insn = vm.getLastInstruction();
    ThreadInfo ti = vm.getLastThreadInfo();
    
    if (insn instanceof InvokeInstruction) {
      processInvokeInstruction(ti, (InvokeInstruction)insn);
    } else if (insn instanceof ASTORE) {
      processASTORE(ti, (ASTORE)insn);
    } else if (insn instanceof PUTFIELD) {
      processPUTFIELD(ti, (PUTFIELD)insn);
    } else if (insn instanceof PUTSTATIC) {
      // <2do>
    } else if (insn instanceof ReturnInstruction) {
      processReturnInstruction(ti, (ReturnInstruction)insn);
    }
  }

  void processInvokeInstruction (ThreadInfo ti, InvokeInstruction insn) {
    if (insn.isCompleted(ti) && !ti.isInstructionSkipped()) {
      MethodInfo mi = insn.getInvokedMethod();
      
      //--- a sequence scope
      AnnotationInfo ai = getAnnotation(mi, "gov.nasa.jpf.Sequence");
      if (ai != null) {
        updateSequenceScope(ai,ti,mi,insn);
      }
      
      // a sequence event
      ai = getAnnotation(mi, "gov.nasa.jpf.SequenceMethod");
      if (ai != null) {
        emitSequenceOp(ai,ti,mi,insn);
      }
    }    
  }

  void updateSequenceScope (AnnotationInfo ai, ThreadInfo ti, MethodInfo mi, InvokeInstruction insn) {
    String id = ai.getValueAsString("id");
    String[] objects = ai.getValueAsStringArray("objects");
    
    // update the sequenceContext
    SequenceContext sc = sequenceContext.getLastOp();
    SequenceInfo si = null;
    
    int idx = sc.getIdx(id);
    if (idx >= 0) {
      // add the varNames, and set it active
      si = sc.get(idx);
      si = si.activate(objects, insn, ti);
      sequenceContext.addOp(sc.replaceSequence(idx,si));
    } else {
      // create the SequenceInfo
      si = new SequenceInfo(id,objects, insn, ti);
      sequenceContext.addOp(sc.addSequence(si));
    }
    
    // update the sequenceOps
    SequenceOp.Start ss = new SequenceOp.Start(id);
    sequenceOps.addOp(ss);    
  }
  
  int getThis (ThreadInfo ti, Instruction insn) {
    // that's clumsy
    if (insn instanceof ReturnInstruction) {
      return ((ReturnInstruction)insn).getReturnFrame().getThis();
    } else {
      return ti.getTopFrame().getThis();
    }
  }
  
  void emitSequenceOp (AnnotationInfo ai, ThreadInfo ti, MethodInfo mi, Instruction insn) {
    String id = ai.getValueAsString("id");
    
    SequenceContext sc = sequenceContext.getLastOp();
    SequenceInfo si = sc.get(id);
    if (si != null) {
      if (si.isActive()) {
        // is this a sequence object
        if (!mi.isStatic()) {
          int refTgt = getThis(ti,insn);
          if (sc.containsRef(id, refTgt)) {
            int refSrc = getEventSource(ti, si);

            String e = getEventName(si, mi, ai.getValueAsString("name"));
            String src = si.getObjectName(refSrc);
            String tgt = si.getObjectName(refTgt);
            String ret = ai.getValueAsString("result");

            SequenceOp so = null;
            if (insn instanceof InvokeInstruction) {
              so = new SequenceOp.Enter(id,e,src,tgt, ret);
            } else if (insn instanceof ReturnInstruction){
              so = new SequenceOp.Exit(id,e,src,tgt, ret);              
            }
            sequenceOps.addOp(so);
          }
        }
      }
    }    
  }
  
  void processASTORE (ThreadInfo ti, ASTORE insn) {
    MethodInfo mi = insn.getMethodInfo();
    AnnotationInfo ai = getAnnotation(mi, "gov.nasa.jpf.Sequence");
    if (ai != null) {
      String id = ai.getValueAsString("id");

      SequenceContext sc = sequenceContext.getLastOp();
      int idx = sc.getIdx(id);
      if (idx >= 0) {
        String vn = insn.getLocalVariableName();
        
        SequenceInfo si = sc.get(idx);
        if (si.containsVar(vn)) {
          int r = ti.getLocalVariable(insn.getLocalVariableIndex()); // this is post exec
          si = si.setLocalVarRef(vn,r);
          sequenceContext.addOp(sc.replaceSequence(idx, si));
        }
      }        
    }    
  }
  
  void processPUTFIELD (ThreadInfo ti, PUTFIELD insn) {
    FieldInfo fi = insn.getFieldInfo();
    
    if (fi.isReference()) {
      AnnotationInfo ai = getAnnotation(fi, "gov.nasa.jpf.SequenceObject");
      if (ai != null){
        String id = ai.getValueAsString("id");
        String name = ai.getValueAsString("object");
        if (name.equals("<field>")) {
          name = fi.getName();
        }
        int ref = (int)insn.getLastValue();
        
        SequenceContext sc = sequenceContext.getLastOp();
        int idx = sc.getIdx(id);
        if (idx >= 0) { // we already have an 'id' sequence record
          SequenceInfo si = sc.get(idx);
          si = si.addRef(name, ref);
          sequenceContext.addOp(sc.replaceSequence(idx,si));

        } else { // add new sequence record
          SequenceInfo si = new SequenceInfo(id, name, ref);
          sequenceContext.addOp(sc.addSequence(si));
        }
      }
    }
  }
  
  void processReturnInstruction (ThreadInfo ti, ReturnInstruction insn) {
    MethodInfo mi = insn.getMethodInfo();
    
    AnnotationInfo ai = getAnnotation(mi, "gov.nasa.jpf.Sequence");
    if (ai != null) {
      String id = ai.getValueAsString("id");

      // update the sequenceContext
      SequenceContext sc = sequenceContext.getLastOp();
      int idx = sc.getIdx(id);
      if (idx >= 0) {
        // add the varNames, and set it active
        SequenceInfo si = sc.get(idx);
        si = si.setActive(false);
        sequenceContext.addOp(sc.replaceSequence(idx, si));
        
        // update the sequenceOps
        SequenceOp.End se = new SequenceOp.End(id);
        sequenceOps.addOp(se);
        
        processSequence(id);
      }
    }
    
    ai = getAnnotation(mi, "gov.nasa.jpf.SequenceMethod");
    if (ai != null) {
      emitSequenceOp(ai,ti,mi,insn);
    }    
  }
  
  int getEventSource (ThreadInfo ti,  SequenceInfo si) {
    // walk through the stackframes until we find a frame executing another sequence object
    int n = ti.getStackDepth();
    
    for (int i=n-2; i>=0; i--) {
      StackFrame frame = ti.getStackFrame(i);
      MethodInfo mi = frame.getMethodInfo();
      if (!mi.isStatic()) {
        int thisRef = frame.getThis();
        if (si.containsRef(thisRef)) {
          return thisRef;
        }
      }
    }
    
    return -1;
  }
  
  //----- set up
  public void searchStarted (Search search) {
    sequenceContext.addOp(new SequenceContext(null));
  }
  
  public void searchFinished (Search search) {
    // post process all stored sequences
  }
  
  //----- update the trace
  
  public void stateAdvanced (Search search) {
    sequenceOps.stateAdvanced(search);
    sequenceContext.stateAdvanced(search);
    
    // check if this is an end-state, which means we want to process all open sequences
    if (search.isEndState()) {
      processAllOpenSequences();
    }
  }
  
  public void stateBacktracked (Search search) {
    sequenceOps.stateBacktracked(search);
    sequenceContext.stateBacktracked(search);
  }

  public void stateStored (Search search) {
    sequenceOps.stateStored(search);
    sequenceContext.stateBacktracked(search);
  }

  public void stateRestored (Search search) {
    sequenceOps.stateRestored(search);
    sequenceContext.stateBacktracked(search);
  }

  //----- report what you did
  
  public void publishFinished (Publisher publisher) {
    PrintWriter out = publisher.getOut();
    publisher.publishTopicStart("sequence analyzer");
    
    out.println("number of sequences processed: " + processedSequences.size());
    for (String msg : processedSequences) {
      out.print('\t');
      out.println(msg);
    }
  }

}
