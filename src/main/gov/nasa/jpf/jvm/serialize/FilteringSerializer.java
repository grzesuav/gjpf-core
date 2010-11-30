//
// Copyright (C) 2006 United States Government as represented by the
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
package gov.nasa.jpf.jvm.serialize;


import gov.nasa.jpf.jvm.AbstractSerializer;
import gov.nasa.jpf.jvm.ArrayFields;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.Fields;
import gov.nasa.jpf.jvm.Heap;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.ReferenceProcessor;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.StaticArea;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.ThreadList;
import gov.nasa.jpf.util.BitArray;
import gov.nasa.jpf.util.FinalBitSet;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;
import gov.nasa.jpf.jvm.ReferenceQueue;


/**
 * Improvement on SimpleFilteringSerializer that performs heap
 * canonicalization/GC while serializing.  Despite the apparent extra
 * work, this one can be faster than its superclass because it does not
 * consider objects that become unreachable, such as class name strings
 * and other constants.
 *
 * @author peterd
 */
public class FilteringSerializer extends AbstractSerializer implements ReferenceProcessor {

  // indexed by method globalId
  final ObjVector<FramePolicy> methodCache    = new ObjVector<FramePolicy>();

  // the Fields slots reference masks, indexed by class uniqueId
  final ObjVector<FinalBitSet> instanceRefMasks = new ObjVector<FinalBitSet>();
  final ObjVector<FinalBitSet> staticRefMasks   = new ObjVector<FinalBitSet>();

  // the Fields slots filter masks, indexed by class uniqueid
  final ObjVector<FinalBitSet> instanceFilterMasks = new ObjVector<FinalBitSet>();
  final ObjVector<FinalBitSet> staticFilterMasks   = new ObjVector<FinalBitSet>();


  protected FilterConfiguration filter;

  protected transient IntVector buf = new IntVector(4096);

  Heap heap;


  @Override
  public void attach(JVM jvm) {
    super.attach(jvm);
    
    filter = jvm.getConfig().getInstance("filter.class", FilterConfiguration.class);
    if (filter == null) {
      filter = new DefaultFilterConfiguration();
    }
    filter.init(jvm.getConfig());
  }

  protected FramePolicy getFramePolicy(MethodInfo mi) {
    FramePolicy p = null;

    int mid = mi.getGlobalId();
    if (mid >= 0){
      p = methodCache.get(mid);
    if (p == null) {
      p = filter.getFramePolicy(mi);
      methodCache.set(mid, p);
    }
    } else {
      p = filter.getFramePolicy(mi);
    }

    return p;
  }

  protected FinalBitSet getInstanceRefMask(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = instanceRefMasks.get(cid);
    if (v == null) {
      BitArray b = new BitArray(ci.getInstanceDataSize());
      for (FieldInfo fi : filter.getMatchedInstanceFields(ci)) {
        if (fi.isReference()) {
          b.set(fi.getStorageOffset());
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      instanceRefMasks.set(cid, v);
    }
    return v;
  }

  protected FinalBitSet getStaticRefMask(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = staticRefMasks.get(cid);
    if (v == null) {
      BitArray b = new BitArray(ci.getStaticDataSize());
      for (FieldInfo fi : filter.getMatchedStaticFields(ci)) {
        if (fi.isReference()) {
          b.set(fi.getStorageOffset());
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      staticRefMasks.set(cid, v);
    }
    return v;
  }

  protected FinalBitSet getInstanceFilterMask(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = instanceFilterMasks.get(cid);
    if (v == null) {
      BitArray b = new BitArray(ci.getInstanceDataSize());
      b.setAll();
      for (FieldInfo fi : filter.getMatchedInstanceFields(ci)) {
        int start = fi.getStorageOffset();
        int end = start + fi.getStorageSize();
        for (int i = start; i < end; i++) {
          b.clear(i);
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      instanceFilterMasks.set(cid, v);
    }
    return v;
  }

  protected FinalBitSet getStaticFilterMask(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = staticFilterMasks.get(cid);
    if (v == null) {
      BitArray b = new BitArray(ci.getStaticDataSize());
      b.setAll();
      for (FieldInfo fi : filter.getMatchedStaticFields(ci)) {
        int start = fi.getStorageOffset();
        int end = start + fi.getStorageSize();
        for (int i = start; i < end; i++) {
          b.clear(i);
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      staticFilterMasks.set(cid, v);
    }
    return v;
  }


  //--- the methods that implement the heap traversal

  protected ReferenceQueue refQueue;

  protected void initReferenceQueue() {
    // note - this assumes all heap objects are in an unmarked state, but this
    // is true if we execute outside the gc

    if (refQueue == null){
      refQueue = new ReferenceQueue();
    } else {
      refQueue.clear();
    }
  }

  protected void addObjRef(int objref) {
    if (objref >= 0) {
      ElementInfo ei = heap.get(objref);
      if (!ei.isMarked()) { // only add objects once
        ei.setMarked();
        refQueue.add(ei);
      }
    }

    buf.add(objref);
  }

  public void processReference(ElementInfo ei) {

    Fields fields = ei.getFields();
    ClassInfo ci = ei.getClassInfo();
    buf.add(ci.getUniqueId());

    if (fields instanceof ArrayFields) { // not filtered
      ArrayFields afields = (ArrayFields) fields;
      buf.add(afields.arrayLength());

      if (afields.isReferenceArray()){
        int[] values = afields.asReferenceArray();
        for (int i = 0; i < values.length; i++) {
          addObjRef(values[i]);
        }
      } else {
        afields.appendTo(buf);
      }

    } else { // named fields, filtered
      FinalBitSet filtered = getInstanceFilterMask(ci);
      FinalBitSet refs = getInstanceRefMask(ci);

      int[] values = fields.asFieldSlots();
      for (int i = 0; i < values.length; i++) {
        if (!filtered.get(i)) {
          int v = values[i];
          if (refs.get(i)) {
            addObjRef(v);
          } else {
            buf.add(v);
          }
        }
      }
    }
  }

  protected void processReferenceQueue () {
    refQueue.process(this);
    
    // this sucks, but we can't do the 'isMarkedOrLive' trick used in gc here
    // because gc depends on live bit integrity, and we only mark non-filtered live
    // objects here, i.e. we can't just set the Heap liveBitValue subsequently.
    heap.unmarkAll();
  }


  //--- our main purpose in life

  @Override
  protected int[] computeStoringData() {
    buf.clear();

    initReferenceQueue();

    ThreadList tl = ks.getThreadList();
    int tlen = tl.length();
    heap = ks.getHeap();

    //--- serialize the threads
    for (int j=0; j<tlen; j++){
      ThreadInfo t = tl.get(j);
      if (!t.isAlive()) {
        continue;
      }

      addObjRef( t.getThreadObjectRef());
      buf.add(t.getState().ordinal());

      int frameCountPos = buf.size();
      buf.add(0); // placeholder
      int frameCount = 0;

      for (StackFrame f : t) {
        frameCount++;
        MethodInfo mi = f.getMethodInfo();
        FramePolicy policy = getFramePolicy(mi);
        int pc;
        if (policy.includePC) {
          pc = f.getPC().getOffset();
        } else {
          pc = -1;
        }
        buf.add(mi.getGlobalId(), pc);

        int lenIdx = buf.size();
        buf.add(0); // place holder
        int len = 0;

        if (policy.includeLocals) {
          int lcount = f.getLocalVariableCount();
          len += lcount;
          for (int i = 0; i < lcount; i++) {
            int v = f.getLocalVariable(i);
            if (f.isLocalVariableRef(i)) {
              addObjRef( v);
            } else {
              buf.add(v);
            }
          }
        }
        if (policy.includeOps) {
          int ocount = f.getTopPos() + 1;
          len += ocount;
          for (int i = 0; i < ocount; i++) {
            int v = f.getAbsOperand(i);
            if (f.isAbsOperandRef(i)) {
              addObjRef( v);
            } else {
              buf.add(v);
            }
          }
        }

        buf.set(lenIdx, len);
        if (!policy.recurse) break;
      }

      buf.set(frameCountPos, frameCount);
    }

    //--- the static fields
    StaticArea statics = ks.getStaticArea();
    buf.add(statics.getLength());
    for (StaticElementInfo s : statics) {
      if (s == null) {
        buf.add(-1);
      } else {
        buf.add(s.getStatus());

        Fields fields = s.getFields();
        ClassInfo ci = s.getClassInfo();
        FinalBitSet filtered = getStaticFilterMask(ci);
        FinalBitSet refs = getStaticRefMask(ci);
        int max = ci.getStaticDataSize();
        for (int i = 0; i < max; i++) {
          if (! filtered.get(i)) {
            int v = fields.getIntValue(i);
            if (refs.get(i)) {
              addObjRef( v);
            } else {
              buf.add(v);
            }
          }
        }
      }
    }

    processReferenceQueue();

//int[] data = buf.toArray();
//long hash = JenkinsStateSet.longLookup3Hash(data);

//System.out.println("@@@@ buf-size: " + buf.size() + ", hash= " + hash);
    return buf.toArray();
  }


}