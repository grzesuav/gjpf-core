//
// Copyright (C) 2011 United States Government as represented by the
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
import java.util.BitSet;

/**
 * a ThreadList implementation that can remove terminated threads and 
 * reuse thread ids. This is required if the SUT uses lots of dynamically created,
 * short living threads that would grow the ThreadList (and potentially the ElementInfo
 * refTid reference sets).
 * 
 * Note that the reuse of thread ids requires an extra pass over all live objects when a
 * thread terminates, since ElementInfo.refTid fields have to be updated
 * 
 * Also note that ids are NOT index values anymore since we don't have holes
 * in our threads[] array
 */
public class ReorgThreadList extends ThreadList {
  
  BitSet ids = new BitSet();
  
  public ReorgThreadList (Config config, KernelState ks) {
    super(config, ks);
  }
  
  
  public int add (ThreadInfo ti) {
    int n = threads.length;

    ids.clear();
    
    // check if it's already there
    for (int i=0; i<n; i++) {
      ThreadInfo t = threads[i];
      if (t == ti) {
        return t.getId();
      }
      
      ids.set( t.getId());
    }

    // append it
    ThreadInfo[] newThreads = new ThreadInfo[n+1];
    System.arraycopy(threads, 0, newThreads, 0, n);
    newThreads[n] = ti;
    threads = newThreads;
    
    return ids.nextClearBit(0);
  }
  
  public boolean unregister (ThreadInfo ti){
    int n = threads.length;
    
    for (int i=0; i<n; i++) {
      if (ti == threads[i]){
        int n1 = n-1;
        ThreadInfo[] newThreads = new ThreadInfo[n1];
        if (i>0){
          System.arraycopy(threads, 0, newThreads, 0, i);
        }
        if (i<n1){
          System.arraycopy(threads, i+1, newThreads, i, (n1-i));
        }
        
        threads = newThreads;
        
        // since we are going to reuse thread ids, we have to clean up ElementInfos
        ti.cleanupReferencedObjects();
        
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * we don't have terminated threads in our list 
   */
  public int getLiveThreadCount () {
    return threads.length;
  }
}
