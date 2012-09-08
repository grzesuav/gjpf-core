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
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.IntTable;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.SparseClusterArray;
import gov.nasa.jpf.util.Transformer;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * a Heap implementation that is based on the SparseClusterArray
 */
public class SparseClusterArrayHeap extends GenericHeapImpl {

  public static final int MAX_THREADS = SparseClusterArray.MAX_CLUSTERS; // 256
  public static final int MAX_THREAD_ALLOC = SparseClusterArray.MAX_CLUSTER_ENTRIES;  // 16,777,215


  SparseClusterArray<ElementInfo> elementInfos;

  // our default memento implementation
  static class SCAMemento extends GenericHeapImplMemento {
    SparseClusterArray.Snapshot<ElementInfo, Memento<ElementInfo>> eiSnap;

    SCAMemento(SparseClusterArrayHeap heap) {
      super(heap);
      eiSnap = heap.elementInfos.getSnapshot(ei2mei);
    }

    public Heap restore(Heap inSitu) {
      SparseClusterArrayHeap heap = (SparseClusterArrayHeap)inSitu;
      
      super.restore(heap);
      heap.elementInfos.restore(eiSnap, mei2ei);
      return heap;
    }
  }


  public SparseClusterArrayHeap (Config config, KernelState ks){
    super(config, ks);
    
    elementInfos = new SparseClusterArray<ElementInfo>();
  }

  protected int getFirstFreeIndex (int tid) {
    return elementInfos.firstNullIndex(tid << SparseClusterArray.S1, SparseClusterArray.MAX_CLUSTER_ENTRIES);
  }
  
  //--- Heap interface

  public Iterator<ElementInfo> iterator(){
    return elementInfos.iterator();
  }

  //--- the allocator primitives

  @Override
  protected int getNewElementInfoIndex (ClassInfo ci, ThreadInfo ti, String allocLocation) {
    int tid = (ti != null) ? ti.getId() : 0;
    int index = getFirstFreeIndex(tid);
    if (index < 0){
      throw new JPFException("per-thread heap limit exceeded");
    }
    
    return index;
  }
  
  @Override
  protected void set (int index, ElementInfo ei) {
    elementInfos.set(index, ei);
  }
  

  public Iterable<ElementInfo> liveObjects() {
    return elementInfos;
  }

  public int size() {
    return elementInfos.numberOfElements();
  }


  public void resetVolatiles() {
    // we don't have any
  }

  public void restoreVolatiles() {
    // we don't have any
  }

  public Memento<Heap> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<Heap> getMemento(){
    return new SCAMemento(this);
  }

  public void checkConsistency(boolean isStateStore) {
    for (ElementInfo ei : this){
      ei.checkConsistency();
    }
  }

  /**
   */
  public ElementInfo get(int ref) {
    if (ref<0) {
      return null;
    } else {
      return elementInfos.get(ref);
    }
  }

  protected void remove (int ref) {
    elementInfos.set( ref, null);
  }
}
