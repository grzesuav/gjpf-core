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

import gov.nasa.jpf.util.FixedBitSet;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;
import gov.nasa.jpf.util.SparseClusterArray;
import gov.nasa.jpf.util.Transformer;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * a MementoRestorer that provides memento classes for the standard KernelState components
 */
public class DefaultMementoRestorer extends MementoRestorer {


  static class KsMemento implements Memento<KernelState> {
    // note - order does matter: threads need to be restored before the heap
    Memento<ThreadList> threadsMemento;
    Memento<StaticArea> staticsMemento;
    Memento<Heap> heapMemento;

    KsMemento (MementoRestorer factory, KernelState ks){
      threadsMemento = ks.threads.getMemento(factory);
      staticsMemento = ks.statics.getMemento(factory);
      heapMemento = ks.heap.getMemento(factory);
    }

    public KernelState restore (KernelState ks) {
      // those are all in-situ objects, no need to set them in ks
      threadsMemento.restore(ks.threads);
      staticsMemento.restore(ks.statics);
      heapMemento.restore(ks.heap);

      return ks;
    }
  }

  // we keep the ThreadList identity
  static class TlistMemento implements Memento<ThreadList> {
    // note that ThreadInfo mementos are also identity preserving
    Memento<ThreadInfo>[] tiMementos;

    TlistMemento(MementoRestorer factory, ThreadList tl) {
      ThreadInfo[] threads = tl.threads;
      int len = threads.length;

      tiMementos = new Memento[len];
      for (int i=0; i<len; i++){
        ThreadInfo ti = threads[i];
        Memento<ThreadInfo> m = null;

        if (!ti.isNewOrChanged()){
          m = ti.memento;
        }
        if (m == null){
          m = ti.getMemento(factory);
          ti.memento = m;
        }
        tiMementos[i] = m;
      }
    }

    public ThreadList restore(ThreadList tl){
      int len = tiMementos.length;
      ThreadInfo[] threads = new ThreadInfo[len];
      for (int i=0; i<len; i++){
        TiMemento m = (TiMemento) tiMementos[i];
        ThreadInfo ti = m.restore(m.ti);
        ti.memento = m;
        threads[i] = ti;
      }
      tl.threads = threads;

      return tl;
    }
  }

  /**
   * note that ThreadInfo instances are invariant along the same path
   */
  static class TiMemento implements Memento<ThreadInfo> {
    // we have to preserve ThreadInfo identities.
    // If we ever want to avoid storing direct references, we would use
    // ThreadInfo.getThreadInfo(threadData.objref) to retrieve the ThreadInfo
    ThreadInfo ti;

    ThreadData threadData;
    StackFrame top;
    int stackDepth;

    TiMemento (MementoFactory factory, ThreadInfo ti){
      this.ti = ti;
      threadData = ti.threadData;  // no need to clone - it's copy on first write
      top = ti.top; // likewise
      stackDepth = ti.stackDepth; // we just copy this for efficiency reasons

      for (StackFrame frame = top; frame != null && frame.hasChanged(); frame = frame.getPrevious()){
        frame.setChanged(false);
      }
      ti.markUnchanged();
    }

    public ThreadInfo restore(ThreadInfo ti) {
      ti.resetVolatiles();

      ti.threadData = threadData;
      ti.top = top;
      ti.stackDepth = stackDepth;

      ti.markUnchanged();

      return ti;
    }
  }


  static abstract class AreaMemento<E extends ElementInfo, A extends Area<E>> {
    // note we need to store as EIMementos because we need to retrieve the
    // ElementInfo index *before* restoring the object, or otherwise we can't
    // do in-situ restoration
    EIMemento<E>[] liveEI;

    public AreaMemento (MementoFactory factory, A area){
      int len = area.size();
      EIMemento<E>[] a = new EIMemento[len];

      int i=0;
      // it actually makes sense to use the elements iterator at this point
      // since this happens after gc, i.e. there is a good chance that the
      // area got fragmented again
      for (E ei : area.elements()){
        Memento<ElementInfo> m = null;
        if (!ei.hasChanged()){
          m = ei.memento;
        }
        if (m == null){
          m = ei.getMemento(factory);
          ei.memento = m;
        }
        // the cast sucks, but we don't want to expose EIMemento in the MementoFactory interface
        a[i++] = (EIMemento<E>)m;
      }

      area.markUnchanged();
      liveEI = a;
    }

    public A restore(A area) {
      ObjVector<E> e = area.elements;
      EIMemento<E>[] a = liveEI;
      int len = a.length;

      area.resetVolatiles();

      int index = -1;
      int lastIndex = -1;
      for (int i=0; i<len; i++){
        EIMemento<E> m = a[i];
        index = m.ref;

        area.removeRange(lastIndex+1, index);
        lastIndex = index;

        //E ei = e.get(index);
        E ei = (E)m.restore(null);
        ei.memento = m;
        //e.set(index, ei);
        area.set(index,ei);
      }

      if (index >= 0){
        area.removeAllFrom(index+1);
      }

      area.nElements = len;
      area.restoreVolatiles();
      area.markUnchanged();

      return area;
    }
  }

  static class SAMemento extends AreaMemento<StaticElementInfo,StaticArea> implements Memento<StaticArea> {
    SAMemento (MementoFactory factory, StaticArea area){
      super(factory, area);
    }
  }

  static class DAMemento extends AreaMemento<DynamicElementInfo,DynamicArea> implements Memento<Heap> {
    DAMemento (MementoFactory factory, DynamicArea area){
      super(factory, area);
    }

    public Heap restore (Heap heap){
      // not very typesafe
      return super.restore((DynamicArea)heap);
    }
  }


  static class EIMemento<EI extends ElementInfo> extends SoftReference<EI> implements Memento<ElementInfo> {
    int ref;
    Fields fields;
    Monitor monitor;
    FixedBitSet refTid;
    int attributes;


    EIMemento (MementoFactory factory, EI ei){
      super(ei);

      ei.markUnchanged(); // we don't want any of the change flags

      this.ref = ei.index;
      this.attributes = ei.attributes;
      this.fields = ei.fields;
      this.monitor = ei.monitor;
      this.refTid = ei.refTid;

      ei.markUnchanged();
    }

    public ElementInfo restore (ElementInfo ei){
      ei.index = ref;
      ei.attributes = attributes;
      ei.fields = fields;
      ei.monitor = monitor;
      ei.refTid = refTid;

      ei.sid = 0;
      ei.updateLockingInfo();
      ei.markUnchanged();

      return ei;
    }

    /** for debugging purposes
    public boolean equals(Object o){
      if (o instanceof EIMemento){
        EIMemento other = (EIMemento)o;
        if (ref != other.ref) return false;
        if (fields != other.fields) return false;
        if (monitor != other.monitor) return false;
        if (refTid != other.refTid) return false;
        if (attributes != other.attributes) return false;
        return true;
      }
      return false;
    }
    public String toString() {
     return "EIMemento {ref="+ref+",attributes="+Integer.toHexString(attributes)+
             ",fields="+fields+",monitor="+monitor+",refTid="+refTid+"}";
    }
    **/
  }

  static class DEIMemento extends EIMemento<DynamicElementInfo> implements Memento<ElementInfo> {
    DEIMemento (MementoFactory factory, DynamicElementInfo ei) {
      super(factory, ei);
    }

    @Override
    public ElementInfo restore (ElementInfo ei){
      ei = get();
      if (ei == null){
        ei = new DynamicElementInfo();
      }

      super.restore(ei);
      return ei;
    }
  }

  static class SEIMemento extends EIMemento<StaticElementInfo>  implements Memento<ElementInfo> {
    int classObjectRef;
    int status;

    SEIMemento (MementoFactory factory, StaticElementInfo ei) {
      super(factory, ei);

      this.classObjectRef = ei.classObjectRef;
      this.status = ei.status;
    }

    @Override
    public ElementInfo restore (ElementInfo ei){
/**
      StaticElementInfo sei;
      if (ei == null){
        sei = new StaticElementInfo();
      } else {
        sei = (StaticElementInfo)ei;
      }
**/
      StaticElementInfo sei = get();
      if (sei == null){
        sei = new StaticElementInfo();
      }

      super.restore(sei);

      sei.status = status;
      sei.classObjectRef = classObjectRef;

      return sei;
    }

    /** for debugging purposes
    public boolean equals(Object o){
      if (o instanceof SEIMemento){
        SEIMemento other = (SEIMemento)o;
        if (!super.equals(o)) return false;
        if (classObjectRef != other.classObjectRef) return false;
        if (status != other.status) return false;
        return true;
      }
      return false;
    }
    public String toString() {
      return "SEIMemento {{" +super.toString() + "},classObjRef="+classObjectRef+",status="+status+"}";
    }
    **/
  }


  //-- the MementoFactory interface

  public Memento<KernelState> getMemento(KernelState ks) {
    return new KsMemento(this, ks);
  }

  public Memento<ThreadList> getMemento(ThreadList tlist) {
    return new TlistMemento(this,tlist);
  }

  public Memento<ThreadInfo> getMemento(ThreadInfo ti) {
    return new TiMemento(this,ti);
  }

  public Memento<Heap> getMemento(DynamicArea da) {
    return new DAMemento(this,da);
  }

  public Memento<StaticArea> getMemento(StaticArea sa) {
    return new SAMemento(this,sa);
  }

  public Memento<ElementInfo> getMemento(DynamicElementInfo ei) {
    return new DEIMemento(this,ei);
  }

  public Memento<ElementInfo> getMemento(StaticElementInfo ei) {
    return new SEIMemento(this,ei);
  }


  //--- new heap support
  static class ElementInfoTransformer implements Transformer<ElementInfo, Memento<ElementInfo>> {
    MementoFactory factory;

    ElementInfoTransformer(MementoFactory factory) {
      this.factory = factory;
    }

    public Memento<ElementInfo> transform(ElementInfo ei) {
      Memento<ElementInfo> m = null;
      if (!ei.hasChanged()) {
        m = ei.memento;
      }
      if (m == null) {
        m = ei.getMemento(factory);
        ei.memento = m;
      }
      return m;
    }

    public ElementInfo restore(Memento<ElementInfo> m) {
      ElementInfo ei = m.restore(null);
      ei.memento = m;
      return ei;
    }
  }
  ElementInfoTransformer transformer; // initialized upon demand


  static class SCAMemento implements Memento<Heap> {
    ElementInfoTransformer transformer;
    SparseClusterArrayHeap.Snapshot<Memento<ElementInfo>> snap;

    SCAMemento(MementoFactory factory, SparseClusterArrayHeap sca, ElementInfoTransformer transformer) {
      this.transformer = transformer;
      snap = sca.getSnapshot(transformer);
      sca.markUnchanged();
    }

    public Heap restore(Heap inSitu) {
      SparseClusterArrayHeap sca = (SparseClusterArrayHeap)inSitu;
      sca.restoreSnapshot(snap, transformer);
      return sca;
    }

  }

  public Memento<Heap> getMemento(SparseClusterArrayHeap sca){
    if (transformer == null){
      transformer = new ElementInfoTransformer(this);
    }
    return new SCAMemento(this,sca,transformer);
  }
}
