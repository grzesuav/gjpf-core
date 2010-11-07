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

/**
 * queue class used to recursively mark live heap objects.
 * We use an explicit queue to avoid recursive calls that can run out of
 * stack space when traversing long reference chains (e.g. linked lists)
 */
public class MarkQueue {

  static class MarkEntry {

    MarkEntry next; // single linked list
    int objref;  // reference value
    int refTid;  // referencing thread
    int refAttr;
    int attrMask;
  }
  MarkEntry markEnd;
  MarkEntry markHead;

  public void queue(int objref, int refTid, int refAttr, int attrMask) {
    MarkEntry e = new MarkEntry();
    e.objref = objref;
    e.refTid = refTid;
    e.refAttr = refAttr;
    e.attrMask = attrMask;

    if (markEnd != null) {
      markEnd.next = e;
    } else {
      markHead = e;
    }

    markEnd = e;
  }

  public void process(Heap heap) {
    for (MarkEntry e = markHead; e != null; e = e.next) {
      heap.mark( e.objref, e.refTid, e.refAttr, e.attrMask);
    }
    clear();
  }

  public void clear () {
    markHead = markEnd = null;
  }
}
