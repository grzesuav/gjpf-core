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

  int newArray (String elementType, int nElements, ThreadInfo ti);

  int newObject (ClassInfo ci, ThreadInfo ti);

  int newString (String str, ThreadInfo ti);

  int newInternString (String str, ThreadInfo ti);
  
  Iterable<ElementInfo> liveObjects();

  int size();

  //--- system internal interface

  // these methods are used by heap related classes such as StackFrames and
  // ElementInfos, to keep them as heap implementation independent as possible

  void analyzeHeap (boolean sweep);

  void updateReachability( boolean isSharedOwner, int oldRef, int newRef);

  void checkConsistency (boolean isStateStore);

  boolean markThreadRoot (int objref, int tid);

  void markStaticRoot (int objRef);

  void cleanUpDanglingReferences();

  void registerWeakReference (Fields f);

  // to be called from ElementInfo.markRecursive(), to avoid exposure of
  // mark implementation
  void queueMark (int objref, int refTid, int refAttr, int attrMask);

  // to be called from MarkQueue (if the heap implementation uses an external
  // data structure to keep track of marked references)
  void mark (int objref, int refTid, int refAttr, int attrMask);

  void markChanged(int objref);

  void hash(HashData hd);

  void resetVolatiles();

  void restoreVolatiles();

  Memento<Heap> getMemento(MementoFactory factory);
}
