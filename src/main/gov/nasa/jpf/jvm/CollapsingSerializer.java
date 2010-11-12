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
package gov.nasa.jpf.jvm;

import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.util.IntVector;
import java.util.Collections;

import java.util.List;

/**
 * CONSIDER THIS BROKEN
 *
 * <2do> the StackFrame cache order mismatch makes this less than optimal, but its
 * probably going to vanish anyways
 */

public class CollapsingSerializer extends CachingSerializerDeserializer {
  protected final CollapsePools.AllIndexed pool = new CollapsePools.AllIndexed();

  public CollapsingSerializer () {
    if (! (JVM.getVM().getHeap() instanceof DynamicArea)){
      throw new JPFConfigException("CollapsingSerializer requires DynamicArea heap");
    }
  }


  // ************ SERIALIZATION STUFF ************ //

  protected void updateThreadCache(ThreadInfo ti, TCacheEntry entry) {
    IntVector cache = entry.cache;
    int length = ti.getStackDepth();

    if (ti.hasDataChanged() || ti != entry.ti) { // cache not valid
      cache.set(0, pool.getThreadDataIndex(ti));
    }

    cache.set(1, length);

    final int cStart = 2;

    // this is in stack order (top first)
    List<StackFrame> changedFrames = ti.getChangedStackFrames();

    int firstChanged;
    if (ti == entry.ti) {
      if (changedFrames.isEmpty()) {
        firstChanged = length;
      } else {
        firstChanged = length - changedFrames.size();
      }
    } else {  // cache not valid
      firstChanged = 0;
      entry.ti = ti; // going to be valid for next time
    }

    if (firstChanged != length){
      cache.setSize(cStart + firstChanged);

      // <2do> this doesn't work - it doesn't replace the prev fields with the pooled frame instances
      Collections.reverse(changedFrames); // we need to add them in ascending order
      StackFrame last = firstChanged == 0 ? null : changedFrames.get(0).getPrevious(); // unchanged or null

      for (StackFrame frame : changedFrames) {
        int idx = pool.getStackFrameIndex(frame);
        StackFrame pooledFrame = pool.getStackFrameAt(idx);
        pooledFrame.setPrevious(last);
        last = pooledFrame;

        cache.add(idx);
      }
    }

    ti.markUnchanged();
  }

  protected static final int   daDelta = 3;
  protected static final int[] daZeros = new int[daDelta];

  protected void updateDynamicAreaCache (DynamicArea area) {
    if (area.anyChanged()) {
      int length = area.getLength();
      daCache.set(0, length);

      for (int i=0; (i=area.getNextChanged(i)) >= 0; i++) {
        int j = (i * daDelta) + 1;

        DynamicElementInfo ei = area.get(i);
        if (ei != null) {
          daCache.set(j+0,pool.getFieldsIndex(ei));
          daCache.set(j+1,pool.getMonitorIndex(ei));
          daCache.set(j+2,ei.getAttributes());
          ei.markUnchanged();
        } else {
          IntVector.copy(daZeros, 0, daCache, j, daDelta);
        }
      }

      daCache.setSize((length*daDelta) +1);  // trims if needed
      area.markUnchanged();
    }
  }


  protected static final int   saDelta = 5;
  protected static final int[] saZeros = new int[saDelta];

  protected void updateStaticAreaCache (StaticArea area) {
    if (area.anyChanged()) {
      int length = area.getLength();
      saCache.set(0, length);

      for (int i=0; (i=area.getNextChanged(i)) >= 0; i++) {
        int j =(i * saDelta) + 1;
        StaticElementInfo ei = area.get(i);
        if (ei != null) {
          saCache.set(j+0,pool.getFieldsIndex(ei));
          saCache.set(j+1,pool.getMonitorIndex(ei));
          saCache.set(j+2,ei.getAttributes());
          saCache.set(j+3,ei.getClassObjectRef());
          saCache.set(j+4,ei.getStatus());
          ei.markUnchanged();
        } else {
          IntVector.copy(saZeros, 0, saCache, j, saDelta);
        }
      }

      saCache.setSize((length*saDelta)+1);  // trims if needed
      area.markUnchanged();
    }
  }


  // *********** DESERIALIZATION STUFF *********** //

  protected ThreadInfo restoreThreadInfo (final ArrayOffset storing, TCacheEntry entry) {
    int tdIdx = storing.get();
    ThreadData td = pool.getThreadDataAt(tdIdx);
    int objRef = td.objref;
    ThreadInfo ti = ThreadInfo.threadInfos.get(objRef);

    ti.resetVolatiles();

    ti.restoreThreadData(td);

    int length = storing.get();

    // we only need to restore the top StackFrame
    storing.advance(length-1);
    StackFrame frame = pool.getStackFrameAt(storing.get());
    ti.setTopFrame(frame);

    ti.markUnchanged();

    // update cache
    int dataLen = length + 2;
    entry.ti = ti;
    entry.cache.clear();
    entry.cache.append(storing.data, storing.offset - dataLen, dataLen);
    // END update cache

    return ti;
  }

  protected void doRestore(DynamicArea a, ArrayOffset storing) {
    int length = storing.get();

    // restore cache
    daCache.clear();
    daCache.add(length);
    daCache.append(storing.data,storing.offset,length * daDelta);

    // restore state.....
    a.resetVolatiles();

    a.removeAllFrom(length);

    for (int i = 0; i < length; i++) {
      if (storing.peek() != 0) {
        DynamicElementInfo ei = a.ensureAndGet(i);

        ei.fields = pool.getFieldsAt(storing.get());
        ei.monitor = pool.getMonitorAt(storing.get());
        ei.attributes = storing.get();

        ei.markUnchanged();
        ei.updateLockingInfo(); // monitor needs to be set before we call this
      } else {
        a.remove(i, true);
        storing.advance(daDelta);
      }
    }

    a.restoreVolatiles();

    a.markUnchanged();
  }

  protected void doRestore(StaticArea a, ArrayOffset storing) {
    int length = storing.get();

    // restore cache
    saCache.clear();
    saCache.add(length);
    saCache.append(storing.data,storing.offset,length * saDelta);

    // restore state.....
    a.resetVolatiles();

    a.removeAllFrom(length);

    for (int i = 0; i < length; i++) {
      if (storing.peek() != 0) {
        StaticElementInfo ei = a.ensureAndGet(i);

        ei.fields = pool.getFieldsAt(storing.get());
        ei.monitor = pool.getMonitorAt(storing.get());
        ei.attributes = storing.get();
        ei.classObjectRef = storing.get();
        ei.status = storing.get();

        ei.markUnchanged();
        ei.updateLockingInfo(); // monitor needs to be set before we call this
      } else {
        a.remove(i, true);
        storing.advance(saDelta);
      }
    }

    a.restoreVolatiles();

    a.markUnchanged();
  }
}
