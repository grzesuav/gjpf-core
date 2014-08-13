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
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.SystemAttribute;
import gov.nasa.jpf.util.FieldSpecMatcher;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.MethodSpecMatcher;
import gov.nasa.jpf.util.TypeSpecMatcher;

/**
 * an abstract SharednessPolicy implementation that makes use of both
 * shared field access CGs and exposure CGs.
 * 
 * This class is highly configurable, both in terms of using exposure CGs and filters.
 * The *never_break filters should be used with care to avoid missing defects, especially
 * the (transitive) method filters.
 * NOTE - the default settings from jpf-core/jpf.properties include several
 * java.util.concurrent* and java.lang.* fields/methods that can in fact contribute to
 * concurrency defects, esp. in SUTs that explicitly use Thread/ThreadGroup objects, in
 * which case they should be removed.
 * 
 * The *always_break field filter should only be used for white box SUT analysis if JPF
 * fails to detect sharedness (e.g. because no exposure is used). This should only
 * go into application property files
 */
public abstract class GenericSharednessPolicy implements SharednessPolicy, Attributor {
  
  //--- auxiliary types to configure filters
  static class NeverBreakIn implements SystemAttribute {
    static NeverBreakIn singleton = new NeverBreakIn();
  } 
  static class NeverBreakOn implements SystemAttribute {
    static NeverBreakOn singleton = new NeverBreakOn();
  } 
  static class AlwaysBreakOn implements SystemAttribute {
    static AlwaysBreakOn singleton = new AlwaysBreakOn();
  } 
  
  protected static JPFLogger logger = JPF.getLogger("shared");
  
  
  //--- options used for concurrent field access detection
  
  /**
   * never break or expose if a matching method is on the stack
   */
  protected MethodSpecMatcher neverBreakInMethods;
  
  /**
   * never break on matching fields 
   */  
  protected FieldSpecMatcher neverBreakOnFields;
    
  /**
   * always break matching fields, no matter if object is already shared or not
   */  
  protected FieldSpecMatcher alwaysBreakOnFields;
  

  /**
   * do we break on final field access 
   */
  protected boolean skipFinals;
  protected boolean skipConstructedFinals;
  protected boolean skipStaticFinals;
  
  /**
   * do we break inside of constructors
   * (note that 'this' references could leak from ctors, but
   * this is rather unusual)
   */
  protected boolean skipInits;

  /**
   * do we add CGs for objects that could become shared, e.g. when storing
   * a reference to a non-shared object in a shared object field.
   * NOTE: this is a conservative measure since we don't know yet at the
   * point of exposure if the object will ever be shared, which means it
   * can cause state explosion.
   */
  protected boolean breakOnExposure;
  
  /**
   * options to filter out lock protected field access, which is not
   * supposed to cause shared CGs
   * (this has no effect on exposure though)
   */
  protected boolean useSyncDetection;
  protected int lockThreshold;  
  
  
  protected GenericSharednessPolicy (Config config){
    neverBreakInMethods = MethodSpecMatcher.create( config.getStringArray("vm.shared.never_break_methods"));
    neverBreakOnFields = FieldSpecMatcher.create( config.getStringArray("vm.shared.never_break_fields"));
    alwaysBreakOnFields = FieldSpecMatcher.create( config.getStringArray("vm.shared.always_break_fields"));
    
    skipFinals = config.getBoolean("vm.shared.skip_finals", true);
    skipConstructedFinals = config.getBoolean("vm.shared.skip_constructed_finals", false);
    skipStaticFinals = config.getBoolean("vm.shared.skip_static_finals", true);
    skipInits = config.getBoolean("vm.shared.skip_inits", true);
    
    breakOnExposure = config.getBoolean("vm.shared.break_on_exposure", true);
    
    useSyncDetection = config.getBoolean("vm.shared.sync_detection", true);
    lockThreshold = config.getInt("vm.shared.lockthreshold", 5);  
  }
  
  /**
   * this can be used to initialize per-application mechanisms such as ClassInfo attribution
   */
  @Override
  public void initialize (VM vm, ApplicationContext appCtx){
    SystemClassLoaderInfo sysCl = appCtx.getSystemClassLoader();
    sysCl.addAttributor(this);
  }
  
  protected void setTypeAttribute (TypeSpecMatcher matcher, ClassInfo ci, Object attr){
    if (matcher != null){
      if (matcher.matches(ci)){
        ci.addAttr(attr);
      }
    }    
  }
  
  protected void setFieldAttributes (FieldSpecMatcher neverMatcher, FieldSpecMatcher alwaysMatcher, ClassInfo ci){
    for (FieldInfo fi : ci.getDeclaredInstanceFields()) {
      // invisible fields (created by compiler)
      if (fi.getName().startsWith("this$")) {
        fi.addAttr( NeverBreakOn.singleton);
        continue;
      }        

      // configuration
      if (neverMatcher != null && neverMatcher.matches(fi)) {
        fi.addAttr( NeverBreakOn.singleton);
      }
      if (alwaysMatcher != null && alwaysMatcher.matches(fi)) {
        fi.addAttr( AlwaysBreakOn.singleton);
      }
      
      // annotation
      if (fi.hasAnnotation("gov.nasa.jpf.annotation.NeverBreak")){
        fi.addAttr( NeverBreakOn.singleton);        
      }
    }

    for (FieldInfo fi : ci.getDeclaredStaticFields()) {
      // invisible fields (created by compiler)
      if ("$assertionsDisabled".equals(fi.getName())) {
        fi.addAttr( NeverBreakOn.singleton);
        continue;
      }

      // configuration
      if (neverMatcher != null && neverMatcher.matches(fi)) {
        fi.addAttr( NeverBreakOn.singleton);
      }
      if (alwaysMatcher != null && alwaysMatcher.matches(fi)) {
        fi.addAttr( AlwaysBreakOn.singleton);
      }
      
      // annotation
      if (fi.hasAnnotation("gov.nasa.jpf.annotation.NeverBreak")){
        fi.addAttr( NeverBreakOn.singleton);        
      }
    }
  }
  
  @Override
  public void setAttributes (ClassInfo ci){    
    setFieldAttributes( neverBreakOnFields, alwaysBreakOnFields, ci);
    
    // this one is more expensive to iterate over and should be avoided
    if (neverBreakInMethods != null){
      for (MethodInfo mi : ci.getDeclaredMethods().values()){
        if (neverBreakInMethods.matches(mi)){
          mi.setAttr( NeverBreakIn.singleton);
        }
      }
    }
    
  }
  
  //------------------------------------------------ object sharedness
  
  
  protected Boolean isSharednessRelevant (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    //--- thread
    if (ti.hasAttr( NeverBreakIn.class)){
      return Boolean.FALSE;
    }
    
    //--- call site (method)
    for (StackFrame frame = ti.getTopFrame(); frame != null; frame=frame.getPrevious()){
      MethodInfo mi = frame.getMethodInfo();
      if (mi.hasAttr( NeverBreakIn.class)){
        return Boolean.FALSE;
      }
    }
    
    //--- field
    if (fi.hasAttr( AlwaysBreakOn.class)){
      return Boolean.TRUE;
    }
    if (fi.hasAttr( NeverBreakOn.class)){
      return Boolean.FALSE;
    }
    
    return null;    
  }
  
  /**
   * static attribute filters that determine if the check..Access() and check..Exposure() methods should be called.
   * This is only called once per instruction execution since it filters all cases that would set a CG.
   * Filter conditions have to apply to both field access and object exposure.
   */
  @Override
  public boolean isInstanceSharednessRelevant (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    Boolean ret = isSharednessRelevant( ti, insn, eiFieldOwner, fi);
    if (ret != null){
      return ret;
    }
    
    // more instance specific filters here
    
    return true;
  }
  
  @Override
  public boolean isStaticSharednessRelevant (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    Boolean ret = isSharednessRelevant( ti, insn, eiFieldOwner, fi);
    if (ret != null){
      return ret;
    }

    // more static specific filters here
    
    return true;
  }
  
  @Override
  public boolean isArraySharednessRelevant (ThreadInfo ti, Instruction insn, ElementInfo eiArray){
    if (ti.hasAttr( NeverBreakIn.class)){
      return false;
    }
    
    //--- call site (method)
    for (StackFrame frame = ti.getTopFrame(); frame != null; frame=frame.getPrevious()){
      MethodInfo mi = frame.getMethodInfo();
      if (mi.hasAttr( NeverBreakIn.class)){
        return false;
      }
    }
    
    return true;
  }
  
  
  /**
   * those are the public interfaces towards the FieldInstructions, which have to be aware of
   * that the field owning ElementInfo (instance or static) will change if it becomes shared
   */
  @Override
  public ElementInfo checkSharedInstanceFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    eiFieldOwner = updateSharedness(ti, eiFieldOwner);
    if (isRelevantInstanceFieldAccess(ti, insn, eiFieldOwner, fi)){
      eiFieldOwner = updateFieldLockInfo(ti,eiFieldOwner,fi);
      if (!eiFieldOwner.isLockProtected(fi)){
        logger.info("shared CG accessing instance field ", fi);
        createAndSetSharedFieldAccessCG(ti, eiFieldOwner);
      }
    }
    
    return eiFieldOwner;
  }
  
  @Override
  public ElementInfo checkSharedStaticFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi){
    eiFieldOwner = updateSharedness(ti, eiFieldOwner);
    if (isRelevantStaticFieldAccess(ti, insn, eiFieldOwner, fi)) {
      eiFieldOwner = updateFieldLockInfo(ti, eiFieldOwner, fi);
      if (!eiFieldOwner.isLockProtected(fi)) {
        logger.info("shared CG accessing static field ", fi);
        createAndSetSharedFieldAccessCG(ti, eiFieldOwner);
      }
    }
    
    return eiFieldOwner;
  }
  
  @Override
  public ElementInfo checkSharedArrayAccess (ThreadInfo ti, Instruction insn, ElementInfo eiArray, int index){
    eiArray = updateSharedness(ti, eiArray);
    if (isRelevantArrayAccess(ti,insn,eiArray,index)){
      // <2do> we should check lock protection for the whole array here
      logger.info("shared CG accessing array ", eiArray);
      createAndSetSharedArrayAccessCG(ti, eiArray);
    }
    
    return eiArray;
  }
  
  
  //--- internal policy methods that can be overridden by subclasses
  
  @Override
  public ElementInfo updateSharedness (ThreadInfo ti, ElementInfo ei){
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
    if (!ei.isShared()){
      return false;
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
    if (!ei.isShared()){
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

    // call site. This could be transitive, in which case it has to be dynamic and can't be moved to isRelevant..()
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
    
    if (ti.isFirstStepInsn()){ // we already did break
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
  @Override
  public void checkInstanceFieldObjectExposure (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, int refValue){
    checkObjectExposure(ti,insn,eiFieldOwner,fi,refValue);
  }

  @Override
  public void checkStaticFieldObjectExposure (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, int refValue){
    checkObjectExposure(ti,insn,eiFieldOwner,fi,refValue);
  }

  @Override
  public void checkArrayObjectExposure (ThreadInfo ti, Instruction insn, ElementInfo eiArray, int idx, int refValue){
    if (breakOnExposure){
      if (refValue != MJIEnv.NULL && (refValue != eiArray.getReferenceElement(idx))) {
        ElementInfo eiRefValue = ti.getElementInfo(refValue);
        if (isFirstExposure(eiArray, eiRefValue)) {
          eiRefValue = eiRefValue.getExposedInstance(ti, eiArray);
          logger.info("exposure CG setting element in array ", eiArray, " to ", eiRefValue);
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
          logger.info("exposure CG setting field ", fi, " to ", eiRefValue);
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
