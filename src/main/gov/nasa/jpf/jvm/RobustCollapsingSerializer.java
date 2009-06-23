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

import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.Misc;

import java.util.Iterator;

/**
 * FOR DEBUGGING PURPOSES ONLY.
 * 
 * This is a duplicate of CollapsingSerializer that removes lots of optimizations
 * to help find bugs.
 *   
 * @author peterd
 */
public class RobustCollapsingSerializer extends CachingSerializerDeserializer {
  protected final CollapsePools.AllIndexed pool = new CollapsePools.AllIndexed();

  
  // ************ SERIALIZATION STUFF ************ //

  protected void updateThreadCache(ThreadInfo ti, TCacheEntry entry) {
    IntVector cache = entry.cache;
    int   length = ti.stack.size();
    
    cache.set(0, pool.getThreadDataIndex(ti));
    cache.set(1, length);

    ti.threadData = ti.threadData.clone();
    
    final int cStart = 2;
    
    int firstChanged;
    firstChanged = 0;
    entry.ti = ti; // going to be valid for next time

    cache.setSize(cStart + firstChanged);
    
    for (int i = firstChanged; i < length; i++) {
      cache.add(pool.getStackFrameIndex(ti.stack.get(i).clone()));
    }
    
    ti.markUnchanged();
  }

  protected static final int   daDelta = 3;
  protected static final int[] daZeros = new int[daDelta];
  
  protected void updateDynamicAreaCache (DynamicArea a) {
    int length = a.getLength();

    daCache.set(0, length);
    
    int j = 1;
    for (int i = 0; i < length; i++, j += daDelta) {
      DynamicElementInfo ei = a.get(i);
      if (ei != null) {
        daCache.set(j+0,pool.getFieldsIndex(ei));
        ei.fields = ei.fields.clone();
        daCache.set(j+1,pool.getMonitorIndex(ei));
        ei.monitor = ei.monitor.clone();
        daCache.set(j+2,ei.getAttributes());
        ei.markUnchanged();
      } else {
        IntVector.copy(daZeros, 0, daCache, j, daDelta);
      }
    }

    daCache.setSize(j);  // trims if needed
    
    a.markUnchanged();
  }


  protected static final int   saDelta = 5;
  protected static final int[] saZeros = new int[saDelta];
  
  protected void updateStaticAreaCache (StaticArea a) {
    int length = a.getLength();

    saCache.set(0, length);
    
    int j = 1;
    for (int i = 0; i < length; i++, j += saDelta) {
      StaticElementInfo ei = a.get(i);
      if (ei != null) {
        saCache.set(j+0,pool.getFieldsIndex(ei));
        ei.fields = ei.fields.clone();
        saCache.set(j+1,pool.getMonitorIndex(ei));
        ei.monitor = ei.monitor.clone();
        saCache.set(j+2,ei.getAttributes());
        saCache.set(j+3,ei.getClassObjectRef());
        saCache.set(j+4,ei.getStatus());
        ei.markUnchanged();
      } else {
        IntVector.copy(saZeros, 0, saCache, j, saDelta);
      }
    }

    saCache.setSize(j);  // trims if needed
    
    a.markUnchanged();
  }

  
  
  // *********** DESERIALIZATION STUFF *********** //

  protected ThreadInfo restoreThreadInfo (final ArrayOffset storing, TCacheEntry entry) {
    int tdIdx = storing.get();
    ThreadData td = pool.getThreadDataAt(tdIdx).clone();
    int objRef = td.objref;
    ThreadInfo ti = ThreadInfo.threadInfos.get(objRef);

    ti.resetVolatiles();
      
    ti.restoreThreadData(td);

    final int length = storing.get();
    
    Iterator<StackFrame> iter = new Iterator<StackFrame>() {
      int i = 0;
      
      public boolean hasNext () {
        return i < length;
      }

      public StackFrame next () {
        i++;
        return pool.getStackFrameAt(storing.get()).clone();
      }

      public void remove () {}
    };
    
    ti.replaceStackFrames(Misc.iterableFromIterator(iter));
      
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

        ei.fields = pool.getFieldsAt(storing.get()).clone();
        ei.monitor = pool.getMonitorAt(storing.get()).clone();
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

        ei.fields = pool.getFieldsAt(storing.get()).clone();
        ei.monitor = pool.getMonitorAt(storing.get()).clone();
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
