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
import gov.nasa.jpf.vm.bytecode.FieldInstruction;
import gov.nasa.jpf.vm.bytecode.InstanceFieldInstruction;
import gov.nasa.jpf.vm.bytecode.StaticFieldInstruction;

/**
 * abstract factory class that encapsulates the policies to detect and handle
 * shared object access.
 * 
 * This includes two components that should be consistent:
 * 
 *  (1) ThreadInfoSets - to detect shared objects
 *  (2) FieldLockInfos - to reduce field access transition breaks on shared objects
 * 
 * The main policy is if respective information is search global, i.e. has
 * carry-over between unrelated paths, or if it is path-local, which requires
 * the use of persistent data structures to keep related information in order
 * to avoid destructive update of already state stored objects.
 * 
 */
public abstract class SharednessPolicy {
  
  //--- options used for concurrent field access detection
  protected boolean skipFinals;
  protected boolean skipConstructedFinals;
  protected boolean skipStaticFinals;
  protected boolean skipInits;
  
  //--- options to filter out lock protected field access
  protected boolean useSyncDetection;
  protected int lockThreshold;

    
  public SharednessPolicy (Config config){
    skipFinals = config.getBoolean("vm.shared.skip_finals", true);
    skipConstructedFinals = config.getBoolean("vm.shared.skip_constructed_finals", false);
    skipStaticFinals = config.getBoolean("vm.shared.skip_static_finals", false);
    skipInits = config.getBoolean("vm.shared.skip_inits", true);
    
    useSyncDetection = config.getBoolean("vm.shared.sync_detection", true);
        
    lockThreshold = config.getInt("vm.shared.lockthreshold", 5);
  }
  
  /**
   * factory method called during object creation 
   */
  public abstract void initializeSharedness (ThreadInfo allocThread, DynamicElementInfo ei);
  public abstract void initializeSharedness (ThreadInfo allocThread, StaticElementInfo ei);


  /**
   * generic version of ElementInfo sharedness update that relies on ThreadInfoSet implementations
   * to determine if ElementInfo needs cloning
   * 
   * NOTE - this might return a new (cloned) ElementInfo in case the state stored/restored
   * flag has been changed and/or the SharedObjectPolicy in effect uses persistent
   * ThreadInfoSet objects (i.e. replaces them upon add).
   * Use only from system code that is aware of the potential ElementInfo
   * identity change (i.e. does not keep a reference to the old one) 
   */
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
    
  
  /**
   * factory method called during object creation 
   */
  protected abstract FieldLockInfo createFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi);

  
  /**
   * generic version of FieldLockInfo update, which relies on FieldLockInfo implementation to determine
   * if ElementInfo needs to be cloned
   */  
  public ElementInfo updateFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
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
   * boolean relevance test based on static and dynamic attributes of executing
   * thread, field owning object and field.
   * This method does not have side effects, i.e. doesn't change the ElementInfo
   */ 
  public boolean isRelevantInstanceFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo ei, FieldInfo fi){
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
  
  
  public boolean isRelevantStaticFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo ei, FieldInfo fi){
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

  /**
   * give policy a chance to clean up referencing ThreadInfoSets upon
   * thread termination
   */
  public void cleanupThreadTermination(ThreadInfo ti) {
    // default action is to do nothing
  }
}
