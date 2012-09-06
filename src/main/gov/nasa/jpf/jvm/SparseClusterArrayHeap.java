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

  public static class Snapshot<T> {
    int attributes;
    IntVector pinDownList;
    IntTable<String> internStrings;
    SparseClusterArray.Snapshot<ElementInfo, T> scaSnap;
  }

  static Transformer<ElementInfo,Memento<ElementInfo>> ei2mei = new Transformer<ElementInfo,Memento<ElementInfo>>(){
    public Memento<ElementInfo> transform (ElementInfo ei){
      Memento<ElementInfo> m = null;
      if (!ei.hasChanged()) {
        m = ei.cachedMemento;
      }
      if (m == null) {
        m = ei.getMemento();
        ei.cachedMemento = m;
      }
      return m;
    }
  };

  static Transformer<Memento<ElementInfo>,ElementInfo> mei2ei = new Transformer<Memento<ElementInfo>,ElementInfo>(){
    public ElementInfo transform(Memento<ElementInfo> m) {
      ElementInfo ei = m.restore(null);
      ei.cachedMemento = m;
      return ei;
    }
  };

  // our default memento implementation
  static class SCAMemento implements Memento<Heap> {
    Snapshot<Memento<ElementInfo>> snap;

    SCAMemento(SparseClusterArrayHeap scah) {
      snap = scah.getSnapshot(ei2mei);
      scah.markUnchanged();
    }

    public Heap restore(Heap inSitu) {
      SparseClusterArrayHeap scah = (SparseClusterArrayHeap)inSitu;
      scah.restoreSnapshot(snap, mei2ei);
      return scah;
    }

  }


  public SparseClusterArrayHeap (Config config, KernelState ks){
    super(config, ks);
    
    elementInfos = new SparseClusterArray<ElementInfo>();
  }

  // internal stuff


  public <T> Snapshot<T> getSnapshot (Transformer<ElementInfo,T> transformer){
    Snapshot<T> snap = new Snapshot<T>();
    
    snap.scaSnap = elementInfos.getSnapshot(transformer);
    
    // these are copy-on-first-write, so we don't have to clone
    snap.pinDownList = pinDownList;
    snap.internStrings = internStrings;
    snap.attributes = attributes & ATTR_STORE_MASK;

    return snap;
  }

  public <T> void restoreSnapshot (Snapshot<T> snap, Transformer<T,ElementInfo> transformer){    
    elementInfos.restoreSnapshot(snap.scaSnap, transformer);
    
    pinDownList = snap.pinDownList;
    internStrings = snap.internStrings;
    attributes = snap.attributes;

    liveBitValue = false; // always start with false after a restore
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
  
  
  private int newString(String str, ThreadInfo ti, boolean isIntern) {
    if (str != null) {      
      int length = str.length();
      int index = newObject(ClassInfo.stringClassInfo, ti, "Heap.newString");
      int vref = newArray("C", length, ti, "Heap.newString.value");
      
      ElementInfo e = get(index);
      // <2do> pcm - this is BAD, we shouldn't depend on private impl of
      // external classes - replace with our own java.lang.String !
      e.setReferenceField("value", vref);
      e.setIntField("offset", 0);
      e.setIntField("count", length);

      ElementInfo eVal = get(vref);
      CharArrayFields cf = (CharArrayFields)eVal.getFields();
      cf.setCharValues(str.toCharArray());

      if (isIntern){
        // we know it's not in the pinDown list yet, this is a new object
        e.incPinDown();
        addToPinDownList(index);
      }

      return index;

    } else {
      return -1;
    }
  }

  public int newString(String str, ThreadInfo ti){
    return newString(str,ti,false);
  }

  public int newInternString (String str, ThreadInfo ti) {
    IntTable.Entry<String> e = internStrings.get(str);
    if (e == null){
      int objref = newString(str,ti,true);

      if ((attributes & ATTR_INTERN_CHANGED) == 0){
        internStrings = internStrings.clone();
        attributes |= ATTR_INTERN_CHANGED;
      }
      internStrings.add(str, objref);

      return objref;

    } else {
      return e.val;
    }
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
