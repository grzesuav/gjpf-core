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
import gov.nasa.jpf.util.ObjVector;

public abstract class CachingSerializerDeserializer
extends AbstractSerializerDeserializer
implements IncrementalChangeTracker {
  // **************** CACHES *************** //
  
  protected final IntVector daCache = new IntVector();
  protected final IntVector saCache = new IntVector();
  
  static final class TCacheEntry {
    final IntVector cache = new IntVector();
    ThreadInfo ti;
  }
  protected final ObjVector<TCacheEntry> threadCaches = new ObjVector<TCacheEntry>();
  
  
  // ************* MISC HELPERS ************* //
  protected TCacheEntry ensureAndGetThreadEntry(int i) {
    TCacheEntry entry = threadCaches.get(i);
    if (entry == null) {
      entry = new TCacheEntry();
      threadCaches.set(i, entry);
    }
    return entry;
  }


  // ************ SERIALIZATION STUFF ************ //

  protected int[] computeStoringData () {
    // update caches
    updateThreadListCache(ks.tl);
    updateStaticAreaCache(ks.sa);
    updateDynamicAreaCache(ks.da);

    // compute required length
    int totalLen = 1;
    int nThreads = threadCaches.size();
    for (int i = 0; i < nThreads; i++) {
      totalLen += threadCaches.get(i).cache.size();
    }
    totalLen += saCache.size() + daCache.size();
    
    // create & dump to the array
    int[] data = new int[totalLen];
    data[0] = nThreads;
    int pos = 1;
    for (int i = 0; i < nThreads; i++) {
      pos = threadCaches.get(i).cache.dumpTo(data,pos);
    }
    pos = saCache.dumpTo(data, pos);
    pos = daCache.dumpTo(data, pos);
    
    assert pos == totalLen;
    
    return data;
  }

  protected void updateThreadListCache (ThreadList tl) {
    int     length = tl.length();

    for (int i = 0; i < length; i++) {
      ThreadInfo ti = tl.get(i);
      TCacheEntry cache = ensureAndGetThreadEntry(i);
      updateThreadCache(ti, cache);
    }
    threadCaches.setSize(length);
  }

  protected abstract void updateThreadCache(ThreadInfo ti, TCacheEntry entry);
  protected abstract void updateDynamicAreaCache (DynamicArea a);
  protected abstract void updateStaticAreaCache (StaticArea a);

  
  
  // *********** DESERIALIZATION STUFF *********** //

  protected void doRestore (int[] data) {
    ArrayOffset storing = new ArrayOffset(data);
    
    // we need to restore the Thread list first, since objects (ElementInfos)
    // might refer to it (e.g. when re-computing volatiles)
    doRestore(ks.tl, storing);
    doRestore(ks.sa, storing);
    doRestore(ks.da, storing);
  }
  
  protected void doRestore (ThreadList tl, ArrayOffset storing) {
    int newLength = storing.get();
    
    ThreadInfo[]  threads = new ThreadInfo[newLength];
    
    for (int i=0; i<newLength; i++) {
      threads[i] = restoreThreadInfo(storing, ensureAndGetThreadEntry(i));
    }
    
    tl.setAll(threads);
  }
  
  
  protected abstract ThreadInfo restoreThreadInfo (final ArrayOffset storing, TCacheEntry entry);
  protected abstract void doRestore(DynamicArea a, ArrayOffset storing);
  protected abstract void doRestore(StaticArea a, ArrayOffset storing);
}
