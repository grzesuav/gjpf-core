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
package gov.nasa.jpf.vm;

import gov.nasa.jpf.Config;

/**
 * SharednessPolicy is a configured policy that is used to detect data races for objects that are accessible from concurrent
 * threads.
 *
 * Its major purpose is to detect operations that need CGs in order to expose races, and introduce such CGs in a way that
 * minimizes state space overhead. This was previously implemented in various different places (ThreadInfo, FieldInstruction etc.)
 * and controlled by a number of "vm.por.*" properties. Strictly speaking, the default implementation does not do classic partial
 * order reduction, it merely tries to reduce states associated with shared objects based on information that was collected by
 * previous execution. All configuration therefore is now done through "vm.shared.*" properties that are loaded in
 * SharednessPolicy and its subclasses.
 *
 * The interface of this class, which is used by field and array element accessors (GETx, PUTx, xASTORE, xALOAD, Field
 * reflection), revolves around two concepts:
 *
 * (1) OBJECT SHAREDNESS - this is supposed to be precise, i.e. a property associated with ElementInfo instances that is set if
 * such an instance is in fact used by two different threads. As such, it has to be updated from the respective accessors (e.g.
 * GETx, PUTx). Detecting sharedness is the responsibility of the SharednessPolicy, storing it is done by means of ElementInfo
 * flags (which could also be set explicitly by listeners or peers).
 *
 * Once an object is marked as shared, respective field and element access operations can lead to races and hence it has to be
 * determined if a CG should be registered to re-execute such instructions. In order to minimize superfluous states, the default
 * policies filter out a number of access specific conditions such as immutable objects, and access point state such as number of
 * runnable threads and especially LOCK PROTECTION (which should normally happen for well designed concurrent programs). Detecting
 * lock protection is field specific and is done by keeping track of (intersections of) lock sets held by the accessing thread.
 * The corresponding FieldLockInfo objects are stored in the respective ElementInfo. The default FieldLockInfo is semi-conservative
 * - even if there is a non-empty lock set it treats the access as unprotected for a certain number of times. If the set becomes
 * empty the object/field is permanently marked as unprotected. If the threshold is reached with a non-empty set, the object/field
 * is henceforth treated as protected (a warning is issued should the lock set subsequently become empty).  *
 * If the access operation passes all filters, the SharednessPolicy registers a CG, i.e. the respective operation is re-executed
 * after performing a scheduling choice. Semantic actions (pushing operand values, changing field values) is done in the bottom
 * half of such operations. However, this can lead to cases in which sharedness is not detected due to non-overlapping lifetimes
 * of accessing threads. Note this does not mean that there is only one live thread at a time, only that threads don't have a CG
 * within their overlapping lifetime that would expose the race. This can lead to missed paths/errors.
 *
 * (2) OBJECT EXPOSURE - is a conservative measure to avoid such missed paths. It is used to introduce additional CGs when a
 * reference to an unshared object gets stored in a shared object, i.e. it is conceptually related to the object whose reference
 * gets stored (i.e. the field value), not to the object that holds the field. Exposure CGs are conservative since at this point
 * JPF only knows that the exposed object /could/ become shared in the future, not that it will. There are various different
 * degrees of conservatism that can be employed (transitive, only first object etc.), but exposure CGs can be a major contributor
 * to state explosion and hence should be minimized. Exposure CGs should break /after/ the semantic action (e.g. field
 * assignment), so that it becomes visible to other threads. This leads to a tricky problem that the bottom half of related
 * accessors has to determine if the semantic action already took place (exposure CGs) or not (sharedness CG). The action is not
 * allowed to be re-executed in the bottom half since this could change the SUT program behavior. In order to determine execution
 * state, implementation mechanisms have to be aware of that there can be any number of transitions between the top and bottom
 * half, i.e. cannot rely on the current CG in the bottom half execution. Conceptually, the execution state is a StackFrame
 * attribute.
 *
 *
 * Concrete SharednessPolicy implementations fall within a spectrum that is marked by two extremes: SEARCH GLOBAL and PATH LOCAL
 * behavior. Search global policies are mostly for bug finding and keep sharedness, lock protection and exposure information from
 * previously executed paths. This has two implications: (a) the search policy / execution order matters (e.g. leading to
 * different results when randomizing choice selection), (b) path replay based on CG traces can lead to different results (e.g.
 * not showing errors found in a previous search).
 *
 * The opposite mode is path local behavior, i.e. no sharednes/protection/exposure information from previous execution paths is
 * used. This should yield the same results for the same path, no matter of execution history. This mode requires the use of
 * persistent data structures for sharedness, lock info and exposure, and hence can be significantly more expensive. However, it
 * is required to guarantee path reply that preserves outcome.
 *
 * Although the two standard policy implementations (GlobalSharednessPolicy and PathSharednessPolicy) correspond to static
 * incarnations of these extremes, it should be noted that the SharednessPolicy interface strives to accommodate mixed and dynamic
 * modes, especially controlling re-execution with adaptive behavior. A primary use for this could be to avoid exposure CGs until
 * additional information becomes available that indicates object sharedness (e.g. non-overlapping thread access), then backtrack
 * to a common ancestor state and re-execute with added exposure CGs for the respective object/field.
 *
 * The motivation for this flexibility and the related implementation complexity/cost is that race detection based on field/array
 * access is a major contributor to state explosion. In many cases, suitable optimizations make the difference between running
 * into search constraints or finishing the search.
 * 
 * <2do> note that field filtering is still done through the DefaultAttributor
 */
public abstract class SharednessPolicy {
  
  //--- options used for concurrent field access detection
  protected boolean skipFinals;
  protected boolean skipConstructedFinals;
  protected boolean skipStaticFinals;
  protected boolean skipInits;
  
  //--- exposure
  protected boolean breakOnExposure;
  
  //--- options to filter out lock protected field access
  protected boolean useSyncDetection;
  protected int lockThreshold;

    
  public SharednessPolicy (Config config){
    skipFinals = config.getBoolean("vm.shared.skip_finals", true);
    skipConstructedFinals = config.getBoolean("vm.shared.skip_constructed_finals", false);
    skipStaticFinals = config.getBoolean("vm.shared.skip_static_finals", false);
    skipInits = config.getBoolean("vm.shared.skip_inits", true);
    
    breakOnExposure = config.getBoolean("vm.shared.break_on_exposure", true);
    
    useSyncDetection = config.getBoolean("vm.shared.sync_detection", true);
    lockThreshold = config.getInt("vm.shared.lockthreshold", 5);  
  }
  

  
  //------------------------------------------------ object sharedness
  
  /**
   * factory method called during object creation 
   */
  public abstract void initializeSharedness (ThreadInfo allocThread, DynamicElementInfo ei);
  public abstract void initializeSharedness (ThreadInfo allocThread, StaticElementInfo ei);


  /**
   * those are the public interfaces towards the FieldInstructions, which have to be aware of
   * that the field owning ElementInfo (instance or static) will change if it becomes shared
   */
  public ElementInfo checkSharedInstanceFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    eiFieldOwner = updateSharedness(ti, eiFieldOwner);
    if (isRelevantInstanceFieldAccess(ti, insn, eiFieldOwner, fi)){
      eiFieldOwner = updateFieldLockInfo(ti,eiFieldOwner,fi);
      if (!eiFieldOwner.isLockProtected(fi)){
//System.out.println("@@@ shared CG for ifield " + fi);
        createAndSetSharedFieldAccessCG(ti, eiFieldOwner);
      }
    }
    
    return eiFieldOwner;
  }
  
  public ElementInfo checkSharedStaticFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    eiFieldOwner = updateSharedness(ti, eiFieldOwner);
    if (isRelevantStaticFieldAccess(ti, insn, eiFieldOwner, fi)) {
      eiFieldOwner = updateFieldLockInfo(ti, eiFieldOwner, fi);
      if (!eiFieldOwner.isLockProtected(fi)) {
//System.out.println("@@@ shared CG for sfield " + fi);
        createAndSetSharedFieldAccessCG(ti, eiFieldOwner);
      }
    }
    
    return eiFieldOwner;
  }
  
  public ElementInfo checkSharedArrayAccess (ThreadInfo ti, Instruction insn, ElementInfo eiArray, int index){
    eiArray = updateSharedness(ti, eiArray);
    if (isRelevantArrayAccess(ti,insn,eiArray,index)){
      // <2do> we should check lock protection for the whole array here
//System.out.println("@@@ shared CG for array " + eiArray);
      createAndSetSharedArrayAccessCG(ti, eiArray);
    }
    
    return eiArray;
  }
  
  
  //--- internal policy methods that can be overridden by subclasses
  
  protected ElementInfo updateSharedness (ThreadInfo ti, ElementInfo ei){
    ThreadInfoSet tis = ei.getReferencingThreads();
    ThreadInfoSet newTis = tis.add(ti);
    
    if (tis != newTis){
      ei = ei.getModifiableInstance();
      ei.setReferencingThreads(newTis);
    }
      
    // we only change from non-shared to shared
    if (newTis.isShared(ti, ei) && !ei.isShared() && !ei.isSharednessFrozen()) {
      ei = ei.getModifiableInstance();
      ei.setShared(ti, true);
    }

    return ei;
  }

  protected boolean createAndSetSharedFieldAccessCG (ThreadInfo ti,  ElementInfo eiFieldOwner) {
    VM vm = ti.getVM();
    ChoiceGenerator<?> cg = vm.getSchedulerFactory().createSharedFieldAccessCG(eiFieldOwner, ti);
    if (cg != null) {
      if (vm.setNextChoiceGenerator(cg)){
        ti.skipInstructionLogging(); // <2do> Hmm, might be more confusing not to see it
        return true;
      }
    }

    return false;
  }
  
  protected boolean createAndSetSharedArrayAccessCG ( ThreadInfo ti, ElementInfo eiArray) {
    VM vm = ti.getVM();
    ChoiceGenerator<?> cg = vm.getSchedulerFactory().createSharedArrayAccessCG(eiArray, ti);
    if (vm.setNextChoiceGenerator(cg)){
      ti.skipInstructionLogging();
      return true;
    }
        
    return false;
  }
  
  /**
   * boolean relevance test based on static and dynamic attributes of executing
   * thread, field owning object and field.
   * This method does not have side effects, i.e. doesn't change the ElementInfo
   */ 
  protected boolean isRelevantInstanceFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo ei, FieldInfo fi){
    if (fi.neverBreak()) { // never break on this field, regardless of shared-ness
      return false;
    }
    
    if (!ei.isShared()){
      return false;
    }
    
    if (fi.breakShared()){ // always break on this field if object is shared
      return true;
    }
    
    if  (ei.isImmutable()){
      return false;
    }
    
    if (skipFinals && fi.isFinal()){
      return false;
    }
    
    if (!ti.hasOtherRunnables()){ // nothing to break for
      return false;
    }
    
    if (ti.isFirstStepInsn()){ // we already did break
      return false;
    }
    
    if (insn.isMonitorEnterPrologue()){
      return false;
    }
    if (fi.getName().startsWith("this$")) {
      // that one is an automatically created outer object reference in an inner class,
      // it can't be set. Unfortunately, we don't have an 'immutable' attribute for
      // fields, just objects, so we can't push it into class load time attributing.
      return false;
    }
    
    //--- mixed (dynamic) attributes
    if (skipConstructedFinals && fi.isFinal() && ei.isConstructed()){
      return false;
    }
    
    if (skipInits && insn.getMethodInfo().isInit()){
      return false;
    }

    // Ok, it's a candidate for a transition break
    return true;
  }
  
  
  protected boolean isRelevantStaticFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo ei, FieldInfo fi){
    if (fi.neverBreak()) { // never break on this field, regardless of shared-ness
      return false;
    }
    
    if (!ei.isShared()){
      return false;
    }
    
    if (fi.breakShared()){ // always break on this field if object is shared
      return true;
    }
    
    if ("$assertionsDisabled".equals(fi.getName())){
      return false;
    }
    
    if  (ei.isImmutable()){
      return false;
    }
    
    if (skipStaticFinals && fi.isFinal()){
      return false;
    }    
    
    if (!ti.hasOtherRunnables()){ // nothing to break for
      return false;
    }
    
    if (ti.isFirstStepInsn()){ // we already did break
      return false;
    }

    //--- instruction attributes
    if (insn.isMonitorEnterPrologue()){
      return false;
    }

    //--- mixed (dynamic) attributes
    MethodInfo mi = insn.getMethodInfo();
    if (mi.isClinit() && (fi.getClassInfo() == mi.getClassInfo())) {
      // clinits are all synchronized, so they are lock protected per se
      return false;
    }
    
    return true;
  }

  
  protected boolean isRelevantArrayAccess (ThreadInfo ti, Instruction insn, ElementInfo ei, int index){
    // <2do> this is too simplistic, we should support filters for array objects
    
    if (!ti.hasOtherRunnables()){
      return false;
    }
    
    if (!ei.isShared()){
      return false;
    }

    return true;
  }
  
  //------------------------------------------------ object exposure 

  /**
   * <2do> explain why not transitive
   * 
   * these are the public interfaces towards FieldInstructions. Callers have to be aware this will 
   * change the /referenced/ ElementInfo in case the respective object becomes exposed
   */
  public void checkInstanceFieldObjectExposure (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, int refValue){
    checkObjectExposure(ti,insn,eiFieldOwner,fi,refValue);
  }

  public void checkStaticFieldObjectExposure (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, int refValue){
    checkObjectExposure(ti,insn,eiFieldOwner,fi,refValue);
  }

  public void checkArrayObjectExposure (ThreadInfo ti, Instruction insn, ElementInfo eiArray, int idx, int refValue){
    if (breakOnExposure){
      if (refValue != MJIEnv.NULL && (refValue != eiArray.getReferenceElement(idx))) {
        ElementInfo eiRefValue = ti.getElementInfo(refValue);
        if (isFirstExposure(eiArray, eiRefValue)) {
          eiRefValue = eiRefValue.getExposedInstance(ti, eiArray);
//System.out.println("@@@ exposure CG for element " + eiArray);
          createAndSetObjectExposureCG(ti, eiRefValue);
        }
      }
    }    
  }

  
  //--- internal policy methods that can be overridden by subclasses
  
  protected void checkObjectExposure (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, int refValue){
    if (breakOnExposure){
      if (refValue != MJIEnv.NULL && (refValue != eiFieldOwner.getReferenceField(fi))) {
        ElementInfo eiRefValue = ti.getElementInfo(refValue);
        if (isFirstExposure(eiFieldOwner, eiRefValue)) {
          eiRefValue = eiRefValue.getExposedInstance(ti, eiFieldOwner);
//System.out.println("@@@ exposure CG for field " + fi);
          createAndSetObjectExposureCG(ti, eiRefValue);
        }
      }
    }
  }

  protected boolean isFirstExposure (ElementInfo eiFieldOwner, ElementInfo eiExposed){
    if (!eiExposed.isImmutable()){
      if (!eiExposed.isExposedOrShared()) {
         return (eiFieldOwner.isExposedOrShared());
      }
    }
    
    return false;  
  }
  
  protected boolean createAndSetObjectExposureCG (ThreadInfo ti, ElementInfo eiFieldValue) {
    VM vm = ti.getVM();
    ChoiceGenerator<?> cg = vm.getSchedulerFactory().createObjectExposureCG(eiFieldValue, ti);
    if (cg != null) {
      if (vm.setNextChoiceGenerator(cg)){
        ti.skipInstructionLogging(); // <2do> Hmm, might be more confusing not to see it
        return true;
      }
    }

    return false;
  }

  
  /**
   * factory method called during object creation 
   */
  protected abstract FieldLockInfo createFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi);

  
  /**
   * generic version of FieldLockInfo update, which relies on FieldLockInfo implementation to determine
   * if ElementInfo needs to be cloned
   */  
  protected ElementInfo updateFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    FieldLockInfo fli = ei.getFieldLockInfo(fi);
    if (fli == null){
      fli = createFieldLockInfo(ti, ei, fi);
      ei = ei.getModifiableInstance();
      ei.setFieldLockInfo(fi, fli);
      
    } else {
      FieldLockInfo newFli = fli.checkProtection(ti, ei, fi);
      if (newFli != fli) {
        ei = ei.getModifiableInstance();
        ei.setFieldLockInfo(fi,newFli);
      }
    }
    
    return ei;
  }  
  
  /**
   * give policy a chance to clean up referencing ThreadInfoSets upon
   * thread termination
   */
  public void cleanupThreadTermination(ThreadInfo ti) {
    // default action is to do nothing
  }
}
