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

import gov.nasa.jpf.Config.Exception;
import gov.nasa.jpf.jvm.ArrayFields;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DynamicElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.Fields;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.util.BitArray;
import gov.nasa.jpf.util.FinalBitSet;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.ObjVector;


/**
 * Improvement on SimpleFilteringSerializer that performs heap
 * canonicalization/GC while serializing.  Despite the apparent extra
 * work, this one can be faster than its superclass because it does not
 * consider objects that become unreachable, such as class name strings
 * and other constants.
 *
 * @author peterd
 */
public class FilteringSerializer extends SimpleFilteringSerializer {
  // indexed by class uniqueId
  final ObjVector<FinalBitSet> instanceRefCache = new ObjVector<FinalBitSet>();
  // indexed by class uniqueId
  final ObjVector<FinalBitSet> staticRefCache   = new ObjVector<FinalBitSet>();


  @Override
  public void attach(JVM jvm) throws Exception {
    super.attach(jvm);
    // more config?
  }


  FinalBitSet getIFieldsAreRefs(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = instanceRefCache.get(cid);
    if (v == null) {
      BitArray b = new BitArray(ci.getInstanceDataSize());
      for (FieldInfo fi : filter.getMatchedInstanceFields(ci)) {
        if (fi.isReference()) {
          b.set(fi.getStorageOffset());
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      instanceRefCache.set(cid, v);
    }
    return v;
  }

  FinalBitSet getSFieldsAreRefs(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = staticRefCache.get(cid);
    if (v == null) {
      BitArray b = new BitArray(ci.getStaticDataSize());
      for (FieldInfo fi : filter.getMatchedStaticFields(ci)) {
        if (fi.isReference()) {
          b.set(fi.getStorageOffset());
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      staticRefCache.set(cid, v);
    }
    return v;
  }


  //inherited:
  //protected transient IntVector buf = new IntVector(300);

  // 0 == unmapped
  // 1 is first mapped, etc.
  protected transient IntVector heapMap    = new IntVector(200);
  protected transient IntVector invHeapMap = new IntVector(200);

  protected void addObjRef(int objref) {
    if (objref < 0) {
      buf.add(-1);
    } else {
      int idx = heapMap.get(objref);
      if (idx == 0) {
        DynamicElementInfo d = ks.da.get(objref);

        if (d == null) { // some weird cases
          idx = -1;
        } else {
          idx = invHeapMap.size();
          invHeapMap.add(objref);
        }
        heapMap.set(objref, idx);
      }
      buf.add(idx);
    }
  }

  @Override
  protected int[] computeStoringData() {
    buf.clear();
    heapMap.clear();
    invHeapMap.clear();

    buf.add(ks.tl.length());
    for (ThreadInfo t : ks.tl.getThreads()) {
      addObjRef(t.getThreadObjectRef());
      buf.add(t.getStatus());
      StackFrame[] frames = t.dumpStack();

      int frameCountPos = buf.size();
      buf.add(0); // placeholder
      int frameCount = 0;

      for (StackFrame f : frames) {
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
            int v = f.getLocalVariable(i);
            if (f.isLocalVariableRef(i)) {
              addObjRef(v);
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
              addObjRef(v);
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

    buf.add(ks.sa.getLength());
    for (StaticElementInfo s : ks.sa) {
      if (s == null) {
        buf.add(-1);
      } else {
        buf.add(s.getStatus());

        Fields fields = s.getFields();
        ClassInfo ci = fields.getClassInfo();
        FinalBitSet filtered = getSFields(ci);
        FinalBitSet refs = getSFieldsAreRefs(ci);
        int max = ci.getStaticDataSize();
        for (int i = 0; i < max; i++) {
          if (! filtered.get(i)) {
            int v = fields.getIntValue(i);
            if (refs.get(i)) {
              addObjRef(v);
            } else {
              buf.add(v);
            }
          }
        }
      }
    }

    for (int newRef = 0; newRef < invHeapMap.size(); newRef++) {
      DynamicElementInfo d = ks.da.get(invHeapMap.get(newRef));

      Fields fields = d.getFields();
      ClassInfo ci = fields.getClassInfo();
      buf.add(ci.getUniqueId());
      if (fields instanceof ArrayFields) {
        int[] values = fields.dumpRawValues();
        buf.add(values.length);
        if (ci.isReferenceArray()) {
          for (int i = 0; i < values.length; i++) {
            addObjRef(values[i]);
          }
        } else {
          buf.append(values);
        }
      } else {
        FinalBitSet filtered = getIFields(ci);
        FinalBitSet refs = getIFieldsAreRefs(ci);
        int max = ci.getInstanceDataSize();
        for (int i = 0; i < max; i++) {
          if (! filtered.get(i)) {
            int v = fields.getIntValue(i);
            if (refs.get(i)) {
              addObjRef(v);
            } else {
              buf.add(v);
            }
          }
        }
      }
    }

    return buf.toArray();
  }

}