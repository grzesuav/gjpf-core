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
import gov.nasa.jpf.util.SparseClusterArray;

/**
 *
 */
public class SparseClusterArrayHeap extends SparseClusterArray<DynamicElementInfo> implements Heap {

  public static final int MAX_THREADS = MAX_CLUSTERS; // 256
  public static final int MAX_THREAD_ALLOC = MAX_CLUSTER_ENTRIES;  // 16,777,215

  protected JVM vm;

  protected InternStringRepository internStrings = new InternStringRepository();

  protected MarkQueue markQueue = new MarkQueue();

  protected boolean runFinalizer;
  protected boolean sweep;

  protected boolean outOfMemory; // can be used by listeners to simulate outOfMemory conditions


  public SparseClusterArrayHeap (Config config, KernelState ks){
    vm = JVM.getVM();
    
    runFinalizer = config.getBoolean("vm.finalize", true);
    sweep = config.getBoolean("vm.sweep",true);
  }

  // internal stuff

  protected DynamicElementInfo createElementInfo (Fields f, Monitor m){
    return new DynamicElementInfo(f,m);
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
    DynamicElementInfo dei = createElementInfo(f, m);

    // get next free index into thread cluster
    int tid = (ti != null) ? ti.getIndex() : 0;
    index = firstNullIndex(tid << S1, MAX_CLUSTER_ENTRIES);

    if (index >= 0){
      dei.setIndex(index);
      set(index, dei);

      // and do the default (const) field initialization
      ci.initializeInstanceData(dei);

      if (ti != null) { // maybe we should report them all, and put the burden on the listener
        vm.notifyObjectCreated(ti, dei);
      }
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

  public int size() {
    return nSet;
  }

  public void updateReachability(boolean isSharedOwner, int oldRef, int newRef) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void checkConsistency(boolean isStateStore) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void cleanUpDanglingReferences() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void registerWeakReference(Fields f) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void gc() {
    analyzeHeap(sweep);
  }

  public void analyzeHeap(boolean sweep) {
    vm.notifyGCBegin();

    markQueue.clear();

    vm.getThreadList().markRoots(this); // mark thread stacks
    vm.getStaticArea().markRoots(this); // mark objects referenced from StaticArea ElementInfos

    // queue pinned down objects

    // at this point, all roots should be in the markQueue, but not traced yet

    markQueue.process(this); // trace all entries - this gets recursive

    // now go over all objects, purge the ones that are not live and reset attrs for rest
    for (ElementInfo ei : this){
      if (ei.isLive()){
        ei.resetMarkAttrs(); // so that we are ready for the next cycle

      } else {
        // <2do> still have to process finalizers here, which might make the object live again

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
    markQueue.queue(objref, tid, 0, ElementInfo.ATTR_PROP_MASK);
  }

  /**
   * called during non-recursive phase1 marking of all objects reachable
   * from static fields
   */
  public void markStaticRoot (int objref) {
    // statics are globals and treated as shared
    markQueue.queue(objref, 0, ElementInfo.ATTR_TSHARED, ElementInfo.ATTR_PROP_MASK);
  }

  void markPinnedDown (int objref){
    // statics are globals and treated as shared
    markQueue.queue(objref, 0, 0, ElementInfo.ATTR_PROP_MASK);
  }


  public void queueMark (int objref, int refTid, int refAttr, int attrMask){
    markQueue.queue(objref, refTid, refAttr, attrMask);
  }

  // to be called from MarkQueue when processing it
  public void mark (int objref, int refTid, int refAttr, int attrMask) {
    if (objref == -1) {
      return;
    }
    ElementInfo ei = get(objref);

    // this is a bit tricky - (1) we have to recursively descend, and (2) we
    // have to make sure we do this only where needed (or we might get an infinite recursion
    // or at least get slow)

    if (ei.isLive()){

      // we have seen this before, and have to check for a change in attributes that
      // might require a re-recurse. That change could either be introduced at this
      // level (we hit a non-shared object referenced from another thread), or it could
      // be refAttr inflicted (i.e. passed in from a re-recurse). But in any way, we
      // have to check for these changes being masked out (attrMask)

      int attrs = ei.getAttributes();

      // Ok, gotcha - but be aware sharedness might be masked out (the ThreadGroup thing)
      if (!ei.isShared() && (refTid != (objref>>>S1))) {
        ei.setShared(attrMask);
      }

      // even if we didn't change sharedness here, we have to propagate attributes
      // (we might get here from the recursion of another object detected to be shared)
      ei.propagateAttributes(refAttr, attrMask);


      // only if the attributes have changed, we have to recurse
      if (ei.getAttributes() != attrs) {

        ei.markRecursive( this, refTid, attrMask);

      } else {
        // if attributes haven't changed, we still have to traverse this if it is a root object
      }

    } else { // its not live yet
      // first time around, mark used, record referencing thread, set attributes, and recurse
      ei.setLive();

      ei.propagateAttributes(refAttr, attrMask);
      ei.markRecursive( this, refTid, attrMask);
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
