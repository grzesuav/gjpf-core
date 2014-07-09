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
 * SharednessPolicy is a configured policy that is used to detect data races for objects that are accessible from concurrent
 * threads.
 *
 * Its major purpose is to detect operations that need CGs in order to expose races, and introduce such CGs in a way that
 * minimizes state space overhead. This was previously implemented in various different places (ThreadInfo, FieldInstruction etc.)
 * and controlled by a number of "vm.por.*" properties. Strictly speaking, the default implementation does not do classic partial
 * order reduction, it merely tries to reduce states associated with shared objects based on information that was collected by
 * previous execution. All configuration therefore is now done through "vm.shared.*" properties that are loaded in
 * SharednessPolicy implementations.
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
 * Note that exposure CGs are not mandatory. Concrete SharednessPolicy implementations can either ignore them in bug finding mode,
 * or can replace them by means of re-execution.
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
 */
public interface SharednessPolicy {
  
  /**
   * per application / SystemClassLoaderInfo specific initialization of this policy 
   */
  void initialize (VM vm, ApplicationContext appCtx);
  
  /**
   * initialize object specific sharedness data 
   */
  void initializeSharedness (ThreadInfo allocThread, DynamicElementInfo ei);
  
  /**
   * initialize class specific sharedness data 
   */  
  void initializeSharedness (ThreadInfo allocThread, StaticElementInfo ei);
  
  /**
   * static attribute filters that determine if the check..Access() and check..Exposure() methods should be called.
   * This is only called once per instruction execution since it filters all cases that would set a CG.
   * Filter conditions have to apply to both field access and object exposure.
   */
  boolean isInstanceSharednessRelevant (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi);
  boolean isStaticSharednessRelevant (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi);
  boolean isArraySharednessRelevant (ThreadInfo ti, Instruction insn, ElementInfo eiArray);
  
  
  /**
   * update sharedness status of the related object and break if shared access is detected. Note this
   * has to use return values in case the ElementInfo had to be cloned
   */
  ElementInfo checkSharedInstanceFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi);
  ElementInfo checkSharedStaticFieldAccess (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi);
  ElementInfo checkSharedArrayAccess (ThreadInfo ti, Instruction insn, ElementInfo eiArray, int index);
  
  /**
   * this can be used from locations which might update sharedness status but not break, i.e. it is called
   * from outside of field or array access operations (e.g. invokestatic)
   */
  ElementInfo updateSharedness (ThreadInfo ti, ElementInfo ei);
  
  /**
   * check if a un-shared object reference is stored in a shared object/class, i.e. the exposed object
   * could become shared in the future. Break if exposure CGs are configured.
   * 
   * NOTE - exposure CGs are conservative and have to be minimized in order to avoid state explosion
   * 
   * <2do> explain why not transitive
   */
  void checkInstanceFieldObjectExposure (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, int refValue);
  void checkStaticFieldObjectExposure (ThreadInfo ti, Instruction insn, ElementInfo eiFieldOwner, FieldInfo fi, int refValue);
  void checkArrayObjectExposure (ThreadInfo ti, Instruction insn, ElementInfo eiArray, int idx, int refValue);
  
  
  /**
   * give policy a chance to clean up referencing ThreadInfoSets upon
   * thread termination
   */
  void cleanupThreadTermination(ThreadInfo ti);
}
