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

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.AnnotationInfo;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.InfoObject;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.verify.Contract;
import gov.nasa.jpf.verify.ContractAnd;
import gov.nasa.jpf.verify.ContractContext;
import gov.nasa.jpf.verify.ContractSpecLexer;
import gov.nasa.jpf.verify.ContractSpecParser;
import gov.nasa.jpf.verify.EmptyContract;
import gov.nasa.jpf.verify.VarLookup;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.logging.Logger;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

/**
 * listener tool to evaluate pre-, post-conditions and invariants given as
 * Java annotations
 */
public class ContractVerifier extends ListenerAdapter {

  protected static Logger log = JPF.getLogger("gov.nasa.jpf.test");

  static class PostCond {
    VarLookup lookupPolicy;
    Contract contract;
    StackFrame frame;

    PostCond (StackFrame f, Contract c, VarLookup p){
      frame = f;
      contract = c;
      lookupPolicy = p;
    }
  }

  // needs to be an IdentityHashMap since StackFrame overrides hashCode(), and
  // the return value is different when a single bit changes
  IdentityHashMap<StackFrame,PostCond> pending = new IdentityHashMap<StackFrame,PostCond>();

  HashMap<InfoObject,Contract> dictPre = new HashMap<InfoObject,Contract>();
  HashMap<InfoObject,Contract> dictPost = new HashMap<InfoObject,Contract>();
  HashMap<InfoObject,Contract> dictIInv = new HashMap<InfoObject,Contract>();

  ContractContext ctx = new ContractContext(log);

  public void classLoaded(JVM vm) {
    getInvariant(vm.getLastClassInfo());
  }

  ClassInfo getClassInfo (StackFrame frame) {
    MethodInfo mi = frame.getMethodInfo();

    if (mi.isStatic()) {
      return mi.getClassInfo();
    } else {
      int thisRef = frame.getThis();
      ElementInfo ei = DynamicArea.getHeap().get(thisRef);
      return ei.getClassInfo();
    }
  }

  public void executeInstruction (JVM vm){
    Instruction insn = vm.getLastInstruction();

    // we do eval preConds post-exec, so that we catch
    // threading related violations (field values might change
    // between pre- and post-exec of the invoke, due to synchronization)

    if (insn instanceof ReturnInstruction) { // eval pending postconditions
      // we need to do this pre-exec so that we still get the right stack snapshot
      ThreadInfo ti = vm.getLastThreadInfo();
      StackFrame frame = ti.getTopFrame();
      MethodInfo mi = frame.getMethodInfo();

      ctx.setContextInfo(mi, ti);

      if (!frame.isDirectCallFrame()) { // no contracts on synthetic methods

        //--- check postconditions
        PostCond pc = pending.get(frame);
        if (pc != null) {
          Contract postCond = pc.contract;
          VarLookup lookupPolicy = new VarLookup.PostCond(ti, (ReturnInstruction)insn, pc.lookupPolicy);

          if (!postCond.holdsAll(lookupPolicy)) {
            Instruction nextPc = ti.createAndThrowException("java.lang.AssertionError",
                                                              getErrorMessage(postCond, lookupPolicy, "postcondition", "AND"));
            ti.setNextPC(nextPc);
            pending.remove(frame);
            return;
          }

          // unfortunately we can't turn this into a WeakHashMap, since it
          // has to be an IdentityHashMap, so we need to clean up explicitly
          pending.remove(frame);
        }

        //--- check class invariants
        // <2do> - this does not yet handle value lookup and reporting correctly
        ClassInfo ci = getClassInfo(frame);
        if (ci != null) {

          if (mi.isStatic()) {
            // <2do> add static class invariants

          } else if (!mi.isCtor()){ // Hmm, do we really want to skip ctors?
            Contract inv = dictIInv.get(ci);
            if (inv.hasNonEmptyContracts() ) {
              VarLookup lookupPolicy = new VarLookup.Invariant(ti);

              if (!inv.holdsAll(lookupPolicy)) {
                Instruction nextPc = ti.createAndThrowException("java.lang.AssertionError",
                                                                  getErrorMessage(inv,lookupPolicy,"invariant", "AND"));
                ti.setNextPC(nextPc);
                return;
              }
            }
          }
        }
      }
    }
  }

  public void instructionExecuted (JVM vm) {
    Instruction insn = vm.getLastInstruction();

    // check for pending post conditions - we need to do this *after* execution
    // because we need the new StackFrame
    if (insn instanceof InvokeInstruction){
      InvokeInstruction call = (InvokeInstruction)insn;
      MethodInfo mi = call.getInvokedMethod();
      ThreadInfo ti = vm.getLastThreadInfo();

      ctx.setContextInfo(mi, ti);

      // check preconditions, but not before we actually enter the method
      // (which might be synchronized, and the preconds might be violated
      // at the time of execution, or when we first see the call)
      Contract preCond = getPreCondition(mi);
      if (!preCond.isEmpty()){ // we can evaluate right away
        //printPreCondition(preCond);
        VarLookup lookup = new VarLookup.PreCond(ti, call);

        if (!preCond.holdsAny(lookup)){
          Instruction nextPc = ti.createAndThrowException("java.lang.AssertionError",
                                                          getErrorMessage(preCond,lookup,"precondition","OR"));
          ti.setNextPC(nextPc);
        }
      }

      // remember postcond 'old' - values
      Contract postCond = getPostCondition(mi);
      if (postCond.hasNonEmptyContracts()) { // store the postcond, together with 'old' values
        // <2do> native methods ??

        VarLookup lookup = new VarLookup.PostCondPreExec(ti);
        postCond.saveOldValues(lookup);
        lookup.purgeVars(); // so that we don't use cached values for fields and locals

        StackFrame frame = ti.getTopFrame();
        PostCond pc = new PostCond(frame,postCond,lookup);
        pending.put(frame, pc);
      }
    }
  }

  String getErrorMessage (Contract c, VarLookup policy, String type, String combinator) {
    String msg = type;

    msg += " violated: ";
    msg += c.getErrorMessage(policy,combinator);

    HashMap<Object,Object> values = policy.getCache();
    if (!values.isEmpty()) {
      msg += ", values=";
      msg += values;
    }

    return msg;
  }

  Contract loadContract (String annotation, InfoObject iobj, HashMap<InfoObject,Contract> dict) {
    Contract contract = null;

    AnnotationInfo ai = iobj.getAnnotation(annotation);
    if (ai != null){  // Ok, we have an unparsed contract spec
      Object v = ai.value();

      if (v instanceof String) {
        contract = parseContractSpec((String)v);

      } else if (v instanceof Object[]) {
        for (Object s : (Object[])v) {
          Contract c = parseContractSpec(s.toString());
          if (contract == null) {
            contract = c;
          } else {
            contract = new ContractAnd(contract, c);
          }
        }
      }

    } else {                // remember that we've checked this
      contract = new EmptyContract();
    }

    dict.put(iobj, contract);

    return contract;
  }

  Contract getContract (String annotation, MethodInfo mi, HashMap<InfoObject,Contract> dict) {
    Contract contract = dict.get(mi);

    if (contract == null){
      contract = loadContract(annotation, mi, dict);
    }

    // now check for super contracts
    MethodInfo smi = mi.getOverriddenMethodInfo();
    if (smi != null){
      contract.setSuperContract( getContract(annotation,smi,dict));
    }

    return contract;
  }

  Contract getContract (String annotation, ClassInfo ci, HashMap<InfoObject,Contract> dict) {
    Contract contract = dict.get(ci);

    if (contract == null){
      contract = loadContract(annotation, ci, dict);
    }

    ClassInfo sci = ci.getSuperClass();
    if (sci != null) {
      contract.setSuperContract( getContract(annotation,sci,dict));
    }

    return contract;
  }

  Contract getPreCondition (MethodInfo mi){
    return getContract("gov.nasa.jpf.Requires", mi, dictPre);
  }

  Contract getPostCondition (MethodInfo mi) {
    return getContract("gov.nasa.jpf.Ensures", mi, dictPost);
  }

  Contract getInvariant (ClassInfo ci) {
    return getContract("gov.nasa.jpf.Invariant", ci, dictIInv);
  }

  Contract parseContractSpec (String spec){
    ANTLRStringStream input = new ANTLRStringStream(spec);
    ContractSpecLexer lexer = new ContractSpecLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    ContractSpecParser parser = new ContractSpecParser(tokens, ctx);

    try {
      Contract contract = parser.contract();
      return contract;

    } catch (RecognitionException rx){
      error("spec did not parse: " + rx);
      return null;
    }
  }

  void error (String msg){
    System.err.println(msg);
  }
}
