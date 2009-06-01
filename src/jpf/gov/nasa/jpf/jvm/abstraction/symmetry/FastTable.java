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
package gov.nasa.jpf.jvm.abstraction.symmetry;

import gov.nasa.jpf.util.ObjArray;

abstract class FastTable<V,EntryType extends FastTable.Entry<V,EntryType>> {
  static final int INIT_TBL_POW = 7;
  static final double MAX_LOAD = 0.80;

  protected ObjArray<EntryType> table;
  protected int tblPow;       // = log_2(table.length)
  protected int mask;         // = table.length - 1
  protected int nextRehash;   // = ceil(MAX_LOAD * table.length);
  protected int size;         // number of Entry<E> objects reachable from table

  FastTable() {
    this(INIT_TBL_POW);
  }
  
  FastTable(int pow) {
    newTable(pow);
    size = 0;
  }
  
  void newTable(int pow) {
    tblPow = pow;
    table = new ObjArray<EntryType>(1 << tblPow);
    mask = table.length() - 1;
    nextRehash = (int) Math.ceil(MAX_LOAD * table.length());
  }
  
  int getIndex(Object o) {
    int hc = System.identityHashCode(o);
    return ((hc >> 3) + (hc << 3)) & mask;
  }
  
  boolean maybeRehash() {
    if (size < nextRehash) return false;
    ObjArray<EntryType> old = table;
    newTable(tblPow + 1);
    int len = old.length();
    for (int i = 0; i < len; i++) {
      addPostOrder(old.get(i));
    }
    return true;
  }
  
  private void addPostOrder(EntryType e) {
    if (e != null) {
      addPostOrder(e.next);
      doAdd(e, getIndex(e));
    }
  }

  // helper for adding
  private void doAdd(EntryType e, int idx) {
    e.next = table.get(idx);
    table.set(idx, e);
  }
  
  // helper for searching
  EntryType getHelper(Object val, int idx) {
    EntryType cur = table.get(idx);
    while (cur != null) {
      if (cur.val == val) {
        return cur;
      }
      cur = cur.next;
    }
    return null; // not found
  }

  abstract EntryType create(V v);
  
  // only if known to not be there!
  EntryType strictAdd(V v) {
    int idx = getIndex(v); 
    EntryType e = create(v);
    maybeRehash();
    doAdd(e,idx);
    size++;
    return e;
  }
  
  // add only if not there; always returns non-null
  EntryType ensureAdded(V v) {
    int idx = getIndex(v); 
    EntryType e = getHelper(v, idx);
    if (e == null) {
      e = create(v);
      maybeRehash();
      doAdd(e,idx);
      size++;
    }
    return e;  
  }
  
  // maybe return null
  EntryType lookup(Object v) {
    int idx = getIndex(v); 
    return getHelper(v, idx);
  }
  
  EntryType delete(Object v) {
    int idx = getIndex(v); 
    EntryType cur = table.get(idx);
    EntryType prev = null;
    while (cur != null) {
      if (cur.val == v) {
        if (prev != null) {
          prev.next = cur.next;
        } else {
          table.set(idx, cur.next);
        }
        cur.next = null;
        return cur;
      }
      prev = cur;
      cur = cur.next;
    }
    return null; // not found
  }
  
  static abstract class Entry<V,This> {
    V val;
    This next;

    Entry(V v) { val = v; next = null; }
  }

  int getPow() { return tblPow; }

  
  /* =============== Public Stuff!! ============== */
  
  public int size() { return size; }
  
  public boolean isEmpty() { return size == 0; }
  
  public void clear() {
    table.nullify();
    size = 0;
  }
  
}
