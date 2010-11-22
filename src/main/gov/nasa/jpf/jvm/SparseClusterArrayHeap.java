//
// Copyright (C) 2010 United States Government as represented by the
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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.SparseClusterArray;
import java.util.ArrayList;

/**
 *
 */
public class SparseClusterArrayHeap extends SparseClusterArray<DynamicElementInfo> implements Heap, ReferenceProcessor {

  public static final int MAX_THREADS = MAX_CLUSTERS; // 256
  public static final int MAX_THREAD_ALLOC = MAX_CLUSTER_ENTRIES;  // 16,777,215

  protected JVM vm;

  protected InternStringRepository internStrings = new InternStringRepository();

  // list of pinned down references (this is only efficient for a small number of objects)
  protected IntVector pinDownList;

  protected boolean runFinalizer;
  protected boolean sweep;

  protected boolean outOfMemory; // can be used by listeners to simulate outOfMemory conditions

  //--- these objects are only used during gc

  // used to keep track of marked WeakRefs that might have to be updated
  protected ArrayList<ElementInfo> weakRefs;

  protected ReferenceQueue markQueue = new ReferenceQueue();



  public SparseClusterArrayHeap (Config config, KernelState ks){
    vm = JVM.getVM();
    
    runFinalizer = config.getBoolean("vm.finalize", true);
    sweep = config.getBoolean("vm.sweep",true);
  }

  // internal stuff

  protected DynamicElementInfo createElementInfo (Fields f, Monitor m, ThreadInfo ti){
    int tid = ti == null ? 0 : ti.getIndex();
    return new DynamicElementInfo(f,m,tid);
  }

  //--- Heap interface

  public boolean isOutOfMemory() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void setOutOfMemory(boolean isOutOfMemory) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public int newArray(String elementType, int nElements, ThreadInfo ti) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public int newObject(ClassInfo ci, ThreadInfo ti) {
    int index = -1;

    // create the thing itself
    Fields             f = ci.createInstanceFields();
    Monitor            m = new Monitor();
    DynamicElementInfo dei = createElementInfo(f, m, ti);

    // get next free index into thread cluster
    int tid = (ti != null) ? ti.getIndex() : 0;
    index = firstNullIndex(tid << S1, MAX_CLUSTER_ENTRIES);

    if (index >= 0){
      dei.setIndex(index);
      set(index, dei);

      // and do the default (const) field initialization
      ci.initializeInstanceData(dei);

      //if (ti != null) { // maybe we should report them all, and put the burden on the listener
        vm.notifyObjectCreated(ti, dei);
      //}
    }

    // note that we don't return -1 if 'outOfMemory' (which is handled in
    // the NEWxx bytecode) because our allocs are used from within the
    // exception handling of the resulting OutOfMemoryError (and we would
    // have to override it, since the VM should guarantee proper exceptions)

    return index;
  }

  public int newString(String str, ThreadInfo ti) {
    if (str != null) {
      int length = str.length();
      int index = newObject(ClassInfo.stringClassInfo, ti);
      int value = newArray("C", length, ti);

      ElementInfo e = get(index);
      // <2do> pcm - this is BAD, we shouldn't depend on private impl of
      // external classes - replace with our own java.lang.String !
      e.setReferenceField("value", value);
      e.setIntField("offset", 0);
      e.setIntField("count", length);

      e = get(value);
      for (int i = 0; i < length; i++) {
        // that's bad too, we should store chars as chars
        e.setElement(i, str.charAt(i));
      }

      return index;
    } else {
      return -1;
    }
  }

  public int newInternString (String str, ThreadInfo ti) {
    return internStrings.newInternString(this, str, ti);
  }

  public Iterable<ElementInfo> liveObjects() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public Iterable<ElementInfo> markedObjects() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public int size() {
    return nSet;
  }


  public void checkConsistency(boolean isStateStore) {
    // <2do>
  }

  public void unmarkAll(){
    // <2do>
  }

  public void cleanUpDanglingReferences() {
    // <2do>
  }

  public void pinDown (int objRef) {
    if (pinDownList == null){
      pinDownList = new IntVector(16);
    }
    pinDownList.addIfAbsent(objRef);

    ElementInfo ei = get(objRef);
    ei.pinDown(true);
  }

  public void registerWeakReference (ElementInfo ei) {
    if (weakRefs == null) {
      weakRefs = new ArrayList<ElementInfo>();
    }

    weakRefs.add(ei);
  }

  /**
   * reset all weak references that now point to collected objects to 'null'
   * NOTE: this implementation requires our own Reference/WeakReference implementation, to
   * make sure the 'ref' field is the first one
   */
  protected void cleanupWeakRefs () {
    if (weakRefs != null) {
      for (ElementInfo ei : weakRefs) {
        Fields f = ei.getFields();
        int    ref = f.getIntValue(0); // watch out, the 0 only works with our own WeakReference impl
        if (ref != -1) {
          ElementInfo refEi = get(ref);
          if ((refEi == null) || (refEi.isNull())) {
            // we need to make sure the Fields are properly state managed
            ei.setReferenceField(ei.getFieldInfo(0), -1);
          }
        }
      }

      weakRefs = null;
    }
  }
  
  public void gc() {
    vm.notifyGCBegin();

    markQueue.clear();

    markPinDownList();
    vm.getThreadList().markRoots(this); // mark thread stacks
    vm.getStaticArea().markRoots(this); // mark objects referenced from StaticArea ElementInfos

    // add pinned down objects

    // at this point, all roots should be in the markQueue, but not traced yet

    markQueue.process(this); // trace all entries - this gets recursive

    // now go over all objects, purge the ones that are not live and reset attrs for rest
    for (ElementInfo ei : this){
      if (ei.isMarked()){
        ei.setUnmarked(); // so that we are ready for the next cycle

      } else {
        // <2do> still have to processReference finalizers here, which might make the object live again

        // check if this was a weak referenced, in which case the WeakReference ref field has to be nulled

        vm.notifyObjectReleased(ei);
        set(ei.getIndex(), null);   // <2do> - do we need a separate remove?
      }
    }

    vm.notifyGCEnd();
  }

  /**
   * called during non-recursive phase1 marking of all objects reachable
   * from Thread roots
   */
  public void markThreadRoot (int objref, int tid) {
    // the shared check will happen in mark()
    markQueue.add(objref);
  }

  /**
   * called during non-recursive phase1 marking of all objects reachable
   * from static fields
   */
  public void markStaticRoot (int objref) {
    // statics are globals and treated as shared
    markQueue.add(objref);
  }

  void markPinDownList (){
    if (pinDownList != null){
      int len = pinDownList.size();
      for (int i=0; i<len; i++){
        int objref = pinDownList.get(i);
        markQueue.add(objref);
      }
    }
  }


  public void queueMark (int objref){
    markQueue.add(objref);
  }

  // to be called from ReferenceQueue when processing it
  public void processReference (int objref) {
    if (objref == -1) {
      return;
    }

    ElementInfo ei = get(objref);
    if (ei != null && !ei.isMarked()){
      // first time around, mark used, record referencing thread, set attributes, and recurse
      ei.setMarked();
      ei.markRecursive( this);
    }
  }

  public void markChanged(int objref) {
    
  }

  public void hash(HashData hd) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void resetVolatiles() {
    
  }

  public void restoreVolatiles() {
    
  }

  public Memento<Heap> getMemento(MementoFactory factory) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

}
