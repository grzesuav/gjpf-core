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

import gov.nasa.jpf.util.HashData;

/**
 * this is our implementation independent model of the heap
 */
public interface Heap {

  //--- this is the common heap client API

  ElementInfo get (int objref);

  void gc();

  boolean isOutOfMemory();

  void setOutOfMemory(boolean isOutOfMemory);

  //--- the allocator primitives
  int newArray (String elementType, int nElements, ThreadInfo ti);
  int newObject (ClassInfo ci, ThreadInfo ti);
  
  int newSystemArray (String elementType, int nElements, ThreadInfo ti, int anchor);
  int newSystemObject (ClassInfo ci, ThreadInfo ti, int anchor);

  //--- convenience allocators that avoid constructor calls
  int newString (String str, ThreadInfo ti);
  int newSystemString (String str, ThreadInfo ti, int anchor);
  
  int newInternString (String str, ThreadInfo ti);
  
  int newSystemThrowable (ClassInfo ci, String details, int[] stackSnapshot, int causeRef,
                          ThreadInfo ti, int anchor);
  
  Iterable<ElementInfo> liveObjects();

  int size();

  //--- system internal interface


  //void updateReachability( boolean isSharedOwner, int oldRef, int newRef);

  void markThreadRoot (int objref, int tid);

  void markStaticRoot (int objRef);

  // these update per-object counters - object will be gc'ed if it goes to zero
  void registerPinDown (int objRef);
  void releasePinDown (int objRef);

  void unmarkAll();

  void cleanUpDanglingReferences();

  boolean isAlive (ElementInfo ei);

  void registerWeakReference (ElementInfo ei);

  // to be called from ElementInfo.markRecursive(), to avoid exposure of
  // mark implementation
  void queueMark (int objref);

  boolean hasChanged();
  
  void markUnchanged();

  void markChanged(int objref);

  void resetVolatiles();

  void restoreVolatiles();

  void checkConsistency (boolean isStateStore);


  Memento<Heap> getMemento(MementoFactory factory);
  Memento<Heap> getMemento();
}
