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
import gov.nasa.jpf.util.SparseObjVector;

/**
 * a SharedObjectPolicy that uses search global ThreadInfoSets and FieldLockInfos,
 * i.e. we remember thread access of all previously executed paths.
 * 
 * Use this policy for bug finding modes that don't have to create replay-able traces.
 * 
 * Note that this policy requires search global object ids (SGOID), i.e. only works
 * with Heap implementations providing SGOIDs
 */

public class GlobalSharednessPolicy extends GenericSharednessPolicy {
  // our global caches
  protected SparseObjVector<ThreadInfoSet> globalTisCache = new SparseObjVector<ThreadInfoSet>(1024);
  protected SparseObjVector<FieldLockInfo> globalFliCache = new SparseObjVector<FieldLockInfo>(1024);
  

  public GlobalSharednessPolicy (Config config){
    super(config);
  }
  
  protected ThreadInfoSet getRegisteredThreadInfoSet (int key, ThreadInfo allocThread) {
    ThreadInfoSet tis = globalTisCache.get(key);
    if (tis == null) {
      tis = new TidSet(allocThread);
      globalTisCache.set(key, tis);
    }
    
    return tis;    
  }
  
  protected FieldLockInfo getRegisteredFieldLockInfo (int key, ThreadInfo ti){
    FieldLockInfo fli = globalFliCache.get(key);
    
    if (fli == null){
      int[] lockRefs = ti.getLockedObjectReferences();
      if (lockRefs.length == 0) {
        fli = FieldLockInfo.getEmptyFieldLockInfo();
      } else if (lockRefs.length == 1){
        fli = new SingleLockThresholdFli(ti, lockRefs[0], lockThreshold);
      } else {
        fli = new PersistentLockSetThresholdFli(ti, lockRefs, lockThreshold);
      }
      
      globalFliCache.set(key, fli);
    }
    
    return fli;
  }
  
  @Override
  public void initializeSharedness (ThreadInfo allocThread, DynamicElementInfo ei) {
    ThreadInfoSet tis = getRegisteredThreadInfoSet(ei.getObjectRef(), allocThread);
    ei.setReferencingThreads( tis);
  }

  @Override
  public void initializeSharedness (ThreadInfo allocThread, StaticElementInfo ei) {
    ThreadInfoSet tis;
    int ref = ei.getClassObjectRef();
    if (ref == MJIEnv.NULL) { // startup class, we don't have a class object yet
      // note that we don't have to store this in our globalCache since we can never
      // backtrack to a point where the startup classes were not initialized yet.
      // <2do> is this true for MultiProcessVM ?
      tis = new TidSet(allocThread);
    } else {
      tis = getRegisteredThreadInfoSet(ref, allocThread);
    }
    
    ei.setReferencingThreads(tis);
    ei.setExposed(); // static fields are per se exposed
  }

  @Override
  protected FieldLockInfo createFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi) {
    int id;
    
    if (ei instanceof StaticElementInfo){
      id = ((StaticElementInfo)ei).getClassObjectRef();
      if (id == MJIEnv.NULL){
        return null;
      }
    } else {
      id = ei.getObjectRef();
    }
    
    return getRegisteredFieldLockInfo( id, ti);
  }
}
