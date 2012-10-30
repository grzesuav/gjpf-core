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

/**
 * Statics implementation that uses a simple ObjVector as the underlying container.
 * 
 * The ids used to retrieve ElementInfos are dense and search global, computation is based 
 * on the assumption that each ClassLoader can only define one class per binary class name
 */
public class ObjVectorStatics implements Statics {

  static class OVMemento implements Memento<Statics> {
    ObjVector.Snapshot<StaticElementInfo> eiSnap;
    
    OVMemento (ObjVectorStatics statics){
      eiSnap = statics.elementInfos.getSnapshot();
    }
    
    @Override
    public Statics restore(Statics inSitu) {
      ObjVectorStatics statics = (ObjVectorStatics) inSitu;
      statics.elementInfos.restore(eiSnap);
      return statics;
    }
  }
  
  protected ObjVector<StaticElementInfo> elementInfos;
  
  // search global class ids (for this ClassLoader only)
  // NOTE this is per instance so that each one is as dense as possible, but since
  // it is search global it does NOT have to be restored and we can copy the reference when cloning
  protected int nextId;
  protected IntTable<String> ids;
  
  
  //--- construction
  
  public ObjVectorStatics (Config conf) {
    elementInfos = new ObjVector<StaticElementInfo>();
    
    nextId = 0;
    ids = new IntTable<String>();
  }
  
  protected int computeId (ClassInfo ci) {
    String clsName = ci.getName();
    IntTable.Entry<String> e = ids.get(clsName);
    if (e == null) {
      int id = nextId++;
      ids.put( clsName, id);
      return id;
      
    } else {
      return e.val;
    }
  }
  
  protected StaticElementInfo createStaticElementInfo (ClassInfo ci, ThreadInfo ti) {
    Fields   f = ci.createStaticFields();
    Monitor  m = new Monitor();

    StaticElementInfo ei = new StaticElementInfo( ci, f, m, ti, ci.getClassObjectRef());

    ci.initializeStaticData(ei, ti);

    return ei;
  }
  
  @Override
  public StaticElementInfo newClass(ClassInfo ci, ThreadInfo ti) {
    int id = computeId( ci);
    
    StaticElementInfo ei = createStaticElementInfo( ci, ti);
    ei.setObjectRef(id);
    elementInfos.set(id, ei);
    
    return ei;
  }

  
  //--- accessors
  
  @Override
  public StaticElementInfo get(int id) {
    return elementInfos.get(id);
  }

  @Override
  public StaticElementInfo getModifiable(int id) {
    StaticElementInfo ei = elementInfos.get(id);
    
    if (ei.isFrozen()) {
      ei = (StaticElementInfo)ei.deepClone();
      // freshly created ElementInfos are not frozen, so we don't have to defreeze
      elementInfos.set(id, ei);
    }
    
    return ei;
  }

  //--- housekeeping
  
  @Override
  public void cleanUpDanglingReferences (Heap heap) {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    int tid = ti.getId();
    boolean isThreadTermination = ti.isTerminated();
    
    for (ElementInfo e : elementInfos) {
      e.cleanUp( heap, isThreadTermination, tid);
    }
  }
  
  //--- state restoration
  
  @Override
  public Memento<Statics> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  @Override
  public Memento<Statics> getMemento() {
    return new OVMemento(this);
  }
  
  @Override
  public void markRoots(Heap heap) {
    for (StaticElementInfo ei : elementInfos.elements()){
      ei.markStaticRoot(heap);
    }
  }

  @Override
  public Iterable<StaticElementInfo> elementInfos() {
    return elementInfos.elements();
  }

  @Override
  public int size() {
    return elementInfos.length();
  }
}
