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

import java.util.HashMap;

/**
 *
 */
public class InternStringRepository {

  protected HashMap<String,InternStringEntry> internStrings = new HashMap<String,InternStringEntry>();
  
  // we are trying to save a backtracked HashMap here. The idea is that the string
  // value chars are constant and not attributed, so the Fields object of the
  // 'char[] value' should never change. The string Fields itself
  // can (e.g. if we set symbolic attributes on the value field), so we have
  // to go one level deeper. We can't just store the String ElementInfo in the
  // map because the new state branch might just have reused it for another string
  // (unlikely but possible)
  static class InternStringEntry {
    String str;
    int ref;
    Fields fValue;

    InternStringEntry (String str, int ref, Fields fValue){
      this.str = str;
      this.ref = ref;
      this.fValue = fValue;
    }
  }

  protected boolean checkInternStringEntry (Heap heap, InternStringEntry e) {
    ElementInfo ei = heap.get(e.ref);
    if (ei != null && ei.getClassInfo() == ClassInfo.stringClassInfo) {
      // check if it was the interned string
      int vref = ei.getReferenceField("value");
      ei = heap.get(vref);
      if (ei != null && ei.getFields() == e.fValue) {
        return true;
      }
    }

    return false;
  }


  public int newInternString (Heap heap, String str, ThreadInfo ti) {
    int ref = -1;

    InternStringEntry e = internStrings.get(str);
    if (e == null || !checkInternStringEntry(heap, e)) { // not seen or new state branch
      ref = heap.newString(str,ti);
      ElementInfo ei = heap.get(ref);
      ei.pinDown(true); // that's important, interns don't get recycled

      int vref = ei.getReferenceField("value");
      ei = heap.get(vref);
      internStrings.put(str, new InternStringEntry(str,ref,ei.getFields()));
      return ref;

    } else {
      return e.ref;
    }
  }

}
