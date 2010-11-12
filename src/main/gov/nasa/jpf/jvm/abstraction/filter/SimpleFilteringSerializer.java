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
package gov.nasa.jpf.jvm.abstraction.filter;

import gov.nasa.jpf.jvm.AbstractSerializer;
import gov.nasa.jpf.jvm.ArrayFields;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.Fields;
import gov.nasa.jpf.jvm.Heap;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.StaticArea;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.ThreadList;
import gov.nasa.jpf.util.BitArray;
import gov.nasa.jpf.util.FinalBitSet;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;

/**
 * Uses filter configuration to serialize state.  Does not do heap
 * canonicalization/GC, so objects that become unreachable after filtering
 * are still taken into account.
 *
 * @author peterd
 */
public class SimpleFilteringSerializer extends AbstractSerializer {
  protected FilterConfiguration filter;

  // indexed by method globalId
  final ObjVector<FramePolicy> methodCache    = new ObjVector<FramePolicy>();
  // indexed by class uniqueId
  final ObjVector<FinalBitSet> instanceCache = new ObjVector<FinalBitSet>();
  // indexed by class uniqueId
  final ObjVector<FinalBitSet> staticCache   = new ObjVector<FinalBitSet>();

  @Override
  public void attach(JVM jvm) {
    super.attach(jvm);
    filter = jvm.getConfig().getInstance("filter.class", FilterConfiguration.class);
    if (filter == null) filter = new DefaultFilterConfiguration();
    filter.init(jvm.getConfig());
  }

  // not implemented
  /*
  FinalBitSet getLocalFilter(MethodInfo mi) {
    int mid = mi.getGlobalId();
    FinalBitSet v = localCache.get(mid);
    if (v == null) {
      BitArray a = filter.getFrameLocalInclusion(mi);
      a.invert(); // included => filtered
      v = FinalBitSet.create(a);
      if (v == null) throw new IllegalStateException("Null BitSet returned.");
      localCache.set(mid, v);
    }
    return v;
  }
  */

  FramePolicy getFramePolicy(MethodInfo mi) {
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

  FinalBitSet getIFields(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = instanceCache.get(cid);
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
      instanceCache.set(cid, v);
    }
    return v;
  }

  FinalBitSet getSFields(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = staticCache.get(cid);
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
      staticCache.set(cid, v);
    }
    return v;
  }

  protected transient IntVector buf = new IntVector(300);

  protected int[] computeStoringData() {

    ThreadList threads = ks.getThreadList();
    buf.clear();
    buf.add(threads.length());

    for (ThreadInfo t : threads.getThreads()) {
      buf.add2(t.getThreadObjectRef(),t.getState().ordinal());

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
        buf.add2(mi.getGlobalId(), pc);

        int lenIdx = buf.size();
        buf.add(0); // place holder
        int len = 0;

        if (policy.includeLocals) {
          int lcount = f.getLocalVariableCount();
          len += lcount;
          for (int i = 0; i < lcount; i++) {
            buf.add(f.getLocalVariable(i));
          }
        }
        if (policy.includeOps) {
          int ocount = f.getTopPos() + 1;
          len += ocount;
          for (int i = 0; i < ocount; i++) {
            buf.add(f.getAbsOperand(i));
          }
        }

        buf.set(lenIdx, len);
        if (!policy.recurse) break;
      }

      buf.set(frameCountPos, frameCount);
    }

    Heap heap = ks.getHeap();
    buf.add( heap.size());
    for (ElementInfo d : heap.liveObjects()) {
      if (d == null) {
        buf.add(-1);
      } else {
        Fields fields = d.getFields();
        ClassInfo ci = fields.getClassInfo();
        buf.add(ci.getUniqueId());
        if (fields instanceof ArrayFields) {
          int[] values = fields.dumpRawValues();
          buf.add(values.length);
          buf.append(values);
        } else {
          FinalBitSet filtered = getIFields(ci);
          int max = ci.getInstanceDataSize();
          if (filtered == FinalBitSet.empty) {
            buf.append(fields.dumpRawValues());
          } else {
            for (int i = 0; i < max; i++) {
              if (! filtered.get(i)) {
                buf.add(fields.getIntValue(i));
              }
            }
          }
        }
      }
    }

    StaticArea statics = ks.getStaticArea();
    //[not really needed, but to be safe:
    buf.add(statics.getLength());
    //]
    for (StaticElementInfo s : statics) {
      if (s == null) {
        buf.add(-1);
      } else {
        buf.add(s.getStatus());

        Fields fields = s.getFields();
        ClassInfo ci = fields.getClassInfo();
        FinalBitSet filtered = getSFields(ci);
        int max = ci.getStaticDataSize();
        if (filtered == FinalBitSet.empty) {
          buf.append(fields.dumpRawValues());
        } else {
          for (int i = 0; i < max; i++) {
            if (! filtered.get(i)) {
              buf.add(fields.getIntValue(i));
            }
          }
        }
      }
    }

    return buf.toArray();
  }

}
