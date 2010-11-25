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

package gov.nasa.jpf.jvm.abstraction.filter;

import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.SparseIntVector;

/**
 * a FilteringSerializer that performs on-the-fly heap canonicalization to
 * achieve heap symmetry. It does so by storing the order in which
 * objects are referenced, not the reference values themselves.
 *
 * Use this serializer if the Heap implementation does not provide
 * sufficient symmetry, i.e. reference values depend on the order of
 * thread scheduling.
 *
 * Ad hoc heap symmetry is hard to achieve because of static initialization. Each
 * time a thread loads a class all the static init (at least the class object and
 * its fields) are associated with this thread, hence thread reference
 * values depend on which classes are already loaded by other threads. Associating
 * all allocations from inside of clinits to one address range doesn't help either
 * because then this range will experience scheduling dependent orders. A hybrid
 * approach in which only this segment is canonicalized might work, but it is
 * questionable if the overhead is worth the effort.
 */
public class CanonicalizingFilteringSerializer extends FilteringSerializer {

  // this stores the number in which object references are traversed, not
  // the reference value itself, which provides some additional heap symmetry.
  // NOTE - the problem is that it assumes we can allocate an array/map that is
  // large enough to hold all possible reference values. This is true in the
  // case of DynamicArea (which also stores elements in a single vector), but
  // not for the SparseClusterArrayHeap. Using a SparseIntVector can help,
  // but only if the number of serialized objects stays low enough.
  protected transient SparseIntVector heapMap = new SparseIntVector(14,0);
  //protected transient IntVector heapMap = new IntVector(4096);

  // invHeapMap is a dense array of all encountered live and non-filtered objects
  protected transient IntVector invHeapMap = new IntVector(4096);

  @Override
  protected void initReferenceQueue() {
    heapMap.clear();
    invHeapMap.clear();
  }

  @Override
  protected void addObjRef(int objref) {
    if (objref < 0) {
      buf.add(-1);

    } else {
      int idx = heapMap.get(objref);
      if (idx == 0) {
        ElementInfo ei = heap.get(objref);

        if (ei == null) { // some weird cases
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
  protected void processReferenceQueue() {
    int len = invHeapMap.size();
    for (int i=0; i<len; i++){
      int objref = invHeapMap.get(i);
      ElementInfo ei = heap.get(objref);
      processReference(ei);
    }
  }
}
