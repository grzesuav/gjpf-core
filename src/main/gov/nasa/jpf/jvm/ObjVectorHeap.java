//
// Copyright (C) 2012 United States Government as represented by the
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
import gov.nasa.jpf.util.IntTable;
import gov.nasa.jpf.util.ObjVector;

import java.util.Iterator;

/**
 * a heap that implements search global object ids (SGOIDs) and uses
 * a simple ObjVector to store ElementInfos. This is only efficient
 * for small heaps with low fragmentation
 * 
 * SGOID computation uses HashedAllocationContext, which means there
 * is a chance of collisions, in which case a different heap type
 * has to be used (we don't try to resolve collisions here)
 */
public class ObjVectorHeap extends GenericHeapImpl {

  static class OVMemento extends GenericHeapImplMemento {
    IntTable.Snapshot<AllocationContext> ctxSnap;
    ObjVector.MutatingSnapshot<ElementInfo,Memento<ElementInfo>> eiSnap;

    OVMemento(ObjVectorHeap heap) {
      super(heap);
      
      ctxSnap = heap.allocCounts.getSnapshot();
      eiSnap = heap.elementInfos.getSnapshot(ei2mei);
    }

    public Heap restore(Heap inSitu) {
      ObjVectorHeap heap = (ObjVectorHeap)inSitu;
      
      super.restore( heap);
            
      heap.allocCounts.restore(ctxSnap);
      heap.elementInfos.restore(eiSnap, mei2ei);
      
      return heap;
    }
  }
  
  static int nextSgoid;
  static IntTable<Allocation> sgoids;
  
  Allocation alloc; // used as a key cache
  IntTable<AllocationContext> allocCounts;
  
  ObjVector<ElementInfo> elementInfos;
  int size;  // non-null elements - we need to maintain this ourselves since ObjVector size is different
  
  
  //--- constructors
  
  public ObjVectorHeap (Config config, KernelState ks){
    super(config, ks);
    
    // static inits
    initAllocationContext(config);
    sgoids = new IntTable<Allocation>();
    nextSgoid = 0;
    
    alloc = new Allocation();
    allocCounts = new IntTable<AllocationContext>();
    elementInfos = new ObjVector<ElementInfo>();
  }

  //--- to be overridden by subclasses that use different AllocationContext implementations
  
  protected void initAllocationContext(Config config) {
    HashedAllocationContext.init(config);    
  }
  
  protected AllocationContext getAllocationContext (ClassInfo ci, ThreadInfo ti, String loc) {
    return HashedAllocationContext.getAllocationContext(ci, ti, loc);
  }
  
  protected AllocationContext getSystemAllocationContext (ClassInfo ci, int anchor, String loc) {
    return HashedAllocationContext.getSystemAllocationContext(ci, anchor, loc);
  }
  
  //--- heap interface
  
  /**
   * return number of non-null elements
   */
  @Override
  public int size() {
    return size;
  }
  
  //--- the allocator primitives (called from the newXX() template methods)
  
  protected int getIndex (AllocationContext ctx) {
    int idx;
    int cnt;
    
    IntTable.Entry<AllocationContext> cntEntry = allocCounts.get(ctx);
    if (cntEntry == null) {
      allocCounts.put(ctx, 1);
      cnt = 1;
    } else {
      cnt = ++cntEntry.val;
    }
    
    alloc.init(ctx, cnt);
    IntTable.Entry<Allocation> sgoidEntry = sgoids.get(alloc);
    if (sgoidEntry != null) { // we already had this one
      idx = sgoidEntry.val;
      
    } else { // new entry
      idx = nextSgoid++;
      sgoids.put(alloc, idx);
      alloc = new Allocation(); // !! create new one so that we don't modify a stored key
    }
    
    // we do this here since we know how elements are stored
    assert elementInfos.get(idx) == null;
    
    return idx;
  }
  
  @Override
  protected int getNewElementInfoIndex (ClassInfo ci, ThreadInfo ti, String loc) {
    AllocationContext ctx = getAllocationContext(ci, ti, loc);
    return getIndex(ctx);
  }
  
  @Override
  protected int getNewSystemElementInfoIndex (ClassInfo ci, int anchor, String loc) {
    AllocationContext ctx = getSystemAllocationContext(ci, anchor, loc);
    return getIndex(ctx);    
  }
  
  //--- the container interface
  
  @Override
  protected void set (int index, ElementInfo ei) {
    elementInfos.set(index, ei);
  }

  /**
   * we treat ref < 0 as NULL reference instead of throwing an exception
   */
  @Override
  public ElementInfo get(int ref) {
    if (ref < 0) {
      return null;
    } else {
      return elementInfos.get(ref);
    }
  }

  @Override
  protected void remove(int ref) {
    if (elementInfos.remove(ref) != null) {
      size--;
    }
  }

  @Override
  public Iterator<ElementInfo> iterator() {
    return elementInfos.nonNullIterator();
  }

  @Override
  public Iterable<ElementInfo> liveObjects() {
    return elementInfos.elements();
  }

  @Override
  public void resetVolatiles() {
    // we don't have any
  }

  @Override
  public void restoreVolatiles() {
    // we don't have any
  }

  public Memento<Heap> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<Heap> getMemento(){
    return new OVMemento(this);
  }

}
