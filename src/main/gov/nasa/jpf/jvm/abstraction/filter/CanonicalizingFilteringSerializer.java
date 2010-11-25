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

/**
 * a FilteringSerializer that performs on-the-fly heap canonicalization to
 * achieve heap symmetry. It does so by storing the order in which
 * objects are referenced, not the reference values themselves.
 *
 * Use this serializer if the Heap implementation does not provide
 * sufficient symmetry, i.e. reference values depend on the order of
 * thread scheduling.
 *
 * Ad hoc heap symmetry is hard to achieve in the heap because of static initialization.
 * Each time a thread loads a class all the static init (at least the class object and
 * its fields) are associated with this thread, hence thread reference
 * values depend on which classes are already loaded by other threads. Associating
 * all allocations from inside of clinits to one address range doesn't help either
 * because then this range will experience scheduling dependent orders. A hybrid
 * approach in which only this segment is canonicalized might work, but it is
 * questionable if the overhead is worth the effort.
 */
public class CanonicalizingFilteringSerializer extends FilteringSerializer {

  // we flip this on every serialization, which helps us to avoid passes
  // over the serialized objects to reset their sids. This work by resetting
  // the sid to 0 upon backtrack, and counting either upwards from 1 or downwards
  // from -1, but store the absolute value in the serialization stream
  boolean positiveSid;

  int sidCount;

  @Override
  protected void initReferenceQueue() {
    super.initReferenceQueue();

    if (positiveSid){
      positiveSid = false;
      sidCount = -1;
    } else {
      positiveSid = true;
      sidCount = 1;
    }
  }

  @Override
  protected void addObjRef(int objref) {
    if (objref < 0) {
      buf.add(-1);

    } else {
      ElementInfo ei = heap.get(objref);
      int sid = ei.getSid();

      if (positiveSid){ // count sid upwards from 1
        if (sid <= 0){  // not seen before in this serialization run
          sid = sidCount++;
          ei.setSid(sid);
          refQueue.add(ei);
        }
      } else { // count sid downwards from -1
        if (sid >= 0){ // not seen before in this serialization run
          sid = sidCount--;
          ei.setSid(sid);
          refQueue.add(ei);
        }
        sid = -sid;
      }

      // note that we always add the absolute sid value
      buf.add(sid);
    }
  }

  @Override
  protected void processReferenceQueue() {
    refQueue.process(this);
  }
}
