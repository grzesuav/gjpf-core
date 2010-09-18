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
package gov.nasa.jpf.jvm;

import java.io.PrintWriter;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.jvm.bytecode.Instruction;


/**
 * the class that encapsulates not only the current execution state of the VM
 * (the KernelState), but also the part of it's history that is required
 * by JVM to backtrack, plus some potential annotations that can be used to
 * control the search (i.e. forward/backtrack calls)
 */
public class SystemState {

  /**
   * instances of this class are used to store the SystemState parts which are
   * subject to backtracking/state resetting. At some point, we might have
   * stripped SystemState down enough to just store the SystemState itself
   * (so far, we don't change it's identity, there is only one)
   * the KernelState is still stored separately (which seems to be another
   * anachronism)
   *
   * NOTE: this gets stored at the end of a transition, i.e. if we need a value
   * to be restored to it's transition entry state (like atomicLevel), we have
   * to do that explicitly. Alternatively we could create the Memento before
   * we start to execute the step, but then we have to update the nextCg in the
   * snapshot, since it's only set at the transition end (required for
   * restore(), i.e.  HeuristicSearches)
   */
  static class Memento {
    ChoiceGenerator<?> curCg;  // the ChoiceGenerator for the current transition
    ChoiceGenerator<?> nextCg;
    int atomicLevel;
    ChoicePoint trace;
    ThreadInfo execThread;
    int id;              // the state id

    Memento (SystemState ss) {
      nextCg = ss.nextCg;
      curCg = ss.curCg;
      atomicLevel = ss.entryAtomicLevel; // store the value we had when we started the transition
      id = ss.id;
      execThread = ss.execThread;
    }

    /**
     * this one is used to restore to a state which will re-execute with the next choice
     * of the same CG, i.e. nextCG is reset
     */
    void backtrack (SystemState ss) {
      ss.nextCg = null; // this is important - the nextCG will be set by the next Transition
      ss.curCg = curCg;
      ss.atomicLevel = atomicLevel;
      ss.id = id;
      ss.execThread = execThread;
    }

    /**
     * this one is used if we restore and then advance, i.e. it might change the CG on
     * the next advance (if nextCg was set)
     */
    void restore (SystemState ss) {
      ss.nextCg = nextCg;
      ss.curCg = curCg;
      ss.atomicLevel = atomicLevel;
      ss.id = id;
      ss.execThread = execThread;
    }
  }

  int id;                   /** the state id */

  ChoiceGenerator<?> nextCg;   // the ChoiceGenerator for the next transition
  ChoiceGenerator<?>  curCg;   // the ChoiceGenerator used in the current transition
  ThreadInfo execThread;    // currently executing thread, reset by ThreadChoiceGenerators
  
  static enum RANDOMIZATION {random, path, def};

  /** current execution state of the VM (stored separately by VM) */
  public KernelState ks;

  public Transition trail;      /** trace information */

  //--- attributes that can be explicitly set for a state

  boolean retainAttributes; // as long as this is set, we don't reset attributes

  //--- ignored and isNewState are imperative
  boolean isIgnored; // treat this as a matched state, i.e. backtrack
  boolean isForced;  // treat this as a new state

  //--- those are hints (e.g. for HeuristicSearches)
  boolean isInteresting;
  boolean isBoring;

  boolean isBlockedInAtomicSection;

  /** uncaught exception in current transition */
  public UncaughtException uncaughtException;

  /** set to true if garbage collection is necessary */
  boolean GCNeeded = false;

  // this is an optimization - long transitions can cause a lot of short-living
  // garbage, which in turn can slow down the system considerably (heap size)
  // by setting 'nAllocGCThreshold', we can do sync. on-the-fly gc when the
  // number of new allocs within a single transition exceeds this value
  int maxAllocPerGC;
  int nAlloc;
  
  RANDOMIZATION randomization = RANDOMIZATION.def;

  /** NOTE: this has changed its meaning again. Now it once more is an
   * optimization that can be used by applications calling Verify.begin/endAtomic(),
   * but be aware of that it now reports a deadlock property violation in
   * case of a blocking op inside an atomic section
   * Data CGs however are now allowed to be inside atomic sections
   */
  int atomicLevel;
  int entryAtomicLevel;

  /** the policy object used to create scheduling related ChoiceGenerators */
  SchedulerFactory schedulerFactory;

  /** do we want CGs to randomize the order in which they return choices? */
  boolean randomizeChoices = false;

  /** do we want executed insns to be recorded */
  boolean recordSteps;

  /**
   * Creates a new system state.
   */
  public SystemState (Config config, JVM vm) {
    ks = new KernelState(config);
    id = StateSet.UNKNOWN_ID;

    Class<?>[] argTypes = { Config.class, JVM.class, SystemState.class };
    Object[] args = { config, vm, this };
    schedulerFactory = config.getEssentialInstance("vm.scheduler_factory.class",
                                                    SchedulerFactory.class,
                                                    argTypes, args);

    // we can't yet initialize the trail until we have the start thread

   
    randomization = config.getEnum("cg.randomize_choices", RANDOMIZATION.values(), 
    						RANDOMIZATION.def);
   
    if(randomization != RANDOMIZATION.def) {
    	randomizeChoices = true;
    }
    
    
    maxAllocPerGC = config.getInt("vm.max_alloc_gc", Integer.MAX_VALUE);

    // recordSteps is set later by VM, first we need a reporter (which requires the VM)
  }

  protected SystemState() {
    // just for unit test mockups
  }

  public void setStartThread (ThreadInfo ti) {
    execThread = ti;
    trail = new Transition(nextCg, execThread);
  }

  /**
   * return the stack of CGs of the current path
   */
  public ChoiceGenerator<?>[] getChoiceGenerators () {
    ChoiceGenerator<?> cg;
    int i, n;

    cg = curCg;
    for (n=0; cg != null; n++) {
      cg = cg.getPreviousChoiceGenerator();
    }

    ChoiceGenerator<?>[] list = new ChoiceGenerator[n];

    cg = curCg;
    for (i=list.length-1; cg != null; i--) {
      list[i] = cg;
      cg = cg.getPreviousChoiceGenerator();
    }

    return list;
  }

  public int getId () {
    return id;
  }

  public void setId (int newId) {
    id = newId;
    trail.setStateId(newId);
  }

  public void recordSteps (boolean cond) {
    recordSteps = cond;
  }

  /**
   * use those with extreme care, it overrides scheduling choices
   */
  public void incAtomic () {
    atomicLevel++;
  }

  public void decAtomic () {
    if (atomicLevel > 0) {
      atomicLevel--;
    }
  }
  public void clearAtomic() {
    atomicLevel = 0;
  }

  public boolean isAtomic () {
    return (atomicLevel > 0);
  }

  public void setBlockedInAtomicSection() {
    isBlockedInAtomicSection = true;
  }

  public Transition getTrail() {
    return trail;
  }

  public SchedulerFactory getSchedulerFactory () {
    return schedulerFactory;
  }

  /**
   * answer the ChoiceGenerator that was used in the current transition
   */
  public ChoiceGenerator<?> getChoiceGenerator () {
    return curCg;
  }

  public <T extends ChoiceGenerator<?>> T getLastChoiceGeneratorOfType (Class<T> cgType) {
    ChoiceGenerator<?> cg = curCg;
    while ((cg != null) && !(cgType.isAssignableFrom(cg.getClass()))) {
      cg = cg.getPreviousChoiceGenerator();
    }

    return (T)cg;
  }

  public <T extends ChoiceGenerator<?>> T getInsnChoiceGeneratorOfType (Class<T> cgType, Instruction insn, ChoiceGenerator<?> cgPrev){
    ChoiceGenerator<?> cg = cgPrev != null ? cgPrev.getPreviousChoiceGenerator() : curCg;

    if (cg != null && cg.getInsn() == insn && cgType.isAssignableFrom(cg.getClass())){
      return (T)cg;
    }

    return null;
  }

  public ChoiceGenerator<?> getNextChoiceGenerator () {
    return nextCg;
  }

  /**
   * set the ChoiceGenerator to be used in the next transition
   */
  public void setNextChoiceGenerator (ChoiceGenerator<?> cg) {
    // first, check if we have to randomize it (might create a new one)
    if (randomizeChoices){
      cg = cg.randomize();
    }

    // set its context (thread and insn)
    cg.setContext(execThread);

    // do we already have a nextCG, which means this one is a cascadet CG
    if (nextCg != null){
      cg.setPreviousChoiceGenerator( nextCg);
      nextCg.setCascaded(); // note the last registered CG is NOT set cascaded

    } else {
      cg.setPreviousChoiceGenerator(curCg);
    }

    nextCg = cg;
  }

  
  public Object getBacktrackData () {
    return new Memento(this);
  }

  public void backtrackTo (Object backtrackData) {
    ((Memento) backtrackData).backtrack( this);
  }

  public void restoreTo (Object backtrackData) {
    ((Memento) backtrackData).restore( this);
  }

  public void retainAttributes (boolean b){
    retainAttributes = b;
  }

  public boolean getRetainAttributes() {
    return retainAttributes;
  }

  /**
   * this can be called anywhere from within a transition, to revert it and
   * go on with the next choice. This is mostly used explicitly in the app
   * via Verify.ignoreIf(..)
   */
  public void setIgnored (boolean b) {

    if (nextCg != null) {
      // Umm, that's kinky - can only happen if somebody first explicitly sets
      // a CG from a listener, only to decide afterwards to dump this whole
      // transition alltogether. Gives us problems because ignored transitions
      // are not handed back to the search, i.e. are not normally backtracked
      // causes a ClassCastException in nextSuccessor (D&C's bug)
      nextCg = null;
    }
    isIgnored = b;

    if (b){
      isForced = false; // mutually exclusive
    }
  }

  public boolean isIgnored () {
    return isIgnored;
  }

  public void setForced (boolean b){
    isForced = b;

    if (b){
      isIgnored = false; // mutually exclusive
    }
  }

  public boolean isForced () {
    return isForced;
  }

  public void setInteresting (boolean b) {
    isInteresting = b;

    if (b){
      isBoring = false;
    }
  }

  public boolean isInteresting () {
    return isInteresting;
  }

  public void setBoring (boolean b) {
    isBoring = b;

    if (b){
      isInteresting = false;
    }
  }

  public boolean isBoring () {
    return isBoring;
  }

  public boolean isInitState () {
    return (id == StateSet.UNKNOWN_ID);
  }


  public int getNonDaemonThreadCount () {
    return ks.tl.getNonDaemonThreadCount();
  }

  public ElementInfo getObject (int reference) {
    return ks.da.get(reference);
  }

  @Deprecated
  public ThreadInfo getThread (int index) {
    return ks.tl.get(index);
  }

  @Deprecated
  public ThreadInfo getThread (ElementInfo reference) {
    return getThread(reference.getIndex());
  }

  public int getThreadCount () {
    return ks.tl.length();
  }

  public int getRunnableThreadCount () {
    return ks.tl.getRunnableThreadCount();
  }

  public int getLiveThreadCount () {
    return ks.tl.getLiveThreadCount();
  }

  public ThreadInfo getThreadInfo (int idx) {
    return ks.tl.get(idx);
  }

  public boolean isDeadlocked () {
    if (isBlockedInAtomicSection) {
      return true; // blocked in atomic section
    }

    return ks.isDeadlocked();
  }

  public UncaughtException getUncaughtException () {
    return uncaughtException;
  }

  public void activateGC () {
    GCNeeded = true;
  }

  public void gcIfNeeded () {
    if (GCNeeded) {
      ks.gc();
      GCNeeded = false;
    }

    nAlloc = 0;
  }

  /**
   * check if number of allocations since last GC exceed the maxAllocPerGC
   * threshold, perform on-the-fly GC if yes. This is aimed at avoiding a lot
   * of short-living garbage in long transitions, which slows down the heap
   * exponentially
   */
  public void checkGC () {
    if (nAlloc++ > maxAllocPerGC){
      gcIfNeeded();
    }
  }

  public void hash (HashData hd) {
    ks.hash(hd);
  }


  void dumpThreadCG (ThreadChoiceGenerator cg) {
    PrintWriter pw = new PrintWriter(System.out, true);
    cg.printOn(pw);
    pw.flush();
  }


  /**
   * Compute next program state
   *
   * return 'true' if we actually executed instructions, 'false' if this
   * state was already completely processed
   *
   * This is one of the key methods of the JPF execution
   * engine (together with VM.forward() and ThreadInfo.executeStep(),executeInstruction()
   *
   */
  public boolean nextSuccessor (JVM vm) throws JPFException {

    if (!retainAttributes){
      isIgnored = false;
      isForced = false;
      isInteresting = false;
      isBoring = false;
    }

    // 'nextCg' got set at the end of the previous transition (or a preceding
    // choiceGeneratorSet() notification).
    // Be aware of that 'nextCg' is only the *last* CG that was registered, i.e.
    // there can be any number of CGs between the previous 'curCg' and 'nextCg'
    // that were registered for the same insn.
    while (nextCg != null) {
      curCg = nextCg;
      nextCg = null;

      // Hmm, that's a bit late (could be in setNextCG), but we keep it here
      // for the sake of locality, and it's more consistent if it just refers
      // to curCg, i.e. the CG that is actually going to be used
      notifyChoiceGeneratorSet(vm, curCg);
    }

    assert (curCg != null) : "transition without choice generator";

    if (!advanceCurCg( vm)){
      return false;
    }

    // do we have a thread context switch
    setExecThread( vm, curCg);

    assert execThread.isRunnable() : "current thread not runnable: " + execThread.getStateDescription();


    trail = new Transition(curCg, execThread);
    entryAtomicLevel = atomicLevel; // store before we start to execute

    execThread.executeStep(this);

    return true;
  }

  // the number of advanced choice generators in this step
  protected int nAdvancedCGs;

  protected void advance( JVM vm, ChoiceGenerator<?> cg){
    while (true) {
      if (cg.hasMoreChoices()){
        cg.advance();

        isIgnored = false;
        vm.notifyChoiceGeneratorAdvanced(cg);
        if (!isIgnored){
          nAdvancedCGs++;
          break;
        }
        
      } else {
        vm.notifyChoiceGeneratorProcessed(cg);
        break;
      }
    }
  }

  protected void advanceAllCascadedParents( JVM vm, ChoiceGenerator<?> cg){
    ChoiceGenerator<?> parent = cg.getCascadedParent();
    if (parent != null){
      advanceAllCascadedParents(vm, parent);
    }
    advance(vm, cg);
  }

  protected boolean advanceCascadedParent (JVM vm, ChoiceGenerator<?> cg){
    if (cg.hasMoreChoices()){
      advance(vm,cg);
      return true;

    } else {
      ChoiceGenerator<?> parent = cg.getCascadedParent();
      if (parent != null){
        if (advanceCascadedParent(vm,parent)){
          cg.reset();
          advance(vm,cg);
          return true;
        }
      }
      return false;
    }
  }

  protected boolean advanceCurCg (JVM vm){
    nAdvancedCGs = 0;

    ChoiceGenerator<?> cg = curCg;
    ChoiceGenerator<?> parent = cg.getCascadedParent();

    if (cg.hasMoreChoices()){
      // check if this is the first time, when we also have to advance our parents
      if (parent != null && parent.getProcessedNumberOfChoices() == 0){
        advanceAllCascadedParents(vm,parent);
      }
      advance(vm, cg);

    } else { // this one is done, but how about our parents
      if (parent != null){
        if (advanceCascadedParent(vm,parent)){
          cg.reset();
          advance(vm,cg);
        }
      }
    }

    return (nAdvancedCGs > 0);
  }



  protected void notifyChoiceGeneratorSet (JVM vm, ChoiceGenerator<?> cg){
    ChoiceGenerator<?> parent = cg.getCascadedParent();
    if (parent != null) {
      notifyChoiceGeneratorSet(vm, parent);
    }
    vm.notifyChoiceGeneratorSet(cg); // notify top down
  }

  protected void setExecThread( JVM vm, ChoiceGenerator<?> cg){
    if (cg instanceof ThreadChoiceGenerator){
      ThreadChoiceGenerator tcg = (ThreadChoiceGenerator)cg;
      if (tcg.isSchedulingPoint()){
        ThreadInfo tiNext = tcg.getNextChoice();
        if (tiNext != execThread){
          vm.notifyThreadScheduled(tiNext);
          execThread = tiNext;
          return;
        }
      }
    }

    ChoiceGenerator<?> parent = cg.getCascadedParent();
    if (parent != null){
      setExecThread( vm, parent);
    }
  }


  // this is called on every executeInstruction from the running thread
  public boolean breakTransition () {
    return ((nextCg != null) || isIgnored);
  }

  void recordExecutionStep (Instruction pc) {
    // this can require a lot of memory, so we should only store
    // executed insns if we have to
    if (recordSteps) {
      Step step = new Step(pc);
      trail.addStep( step);
    } else {
      trail.incStepCount();
    }
  }

  public boolean isEndState () {
    return ks.isTerminated();
  }

  // the three primitive ops used from within JVM.forward()


}

