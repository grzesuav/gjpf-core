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


import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

class FastEqSet<T> extends FastTable<T,FastEqSet.Entry<T>> implements EqSet<T> {
  public FastEqSet(int pow) {
    super(pow);
  }
  
  public FastEqSet() {
    super();
  }
  
  
  Entry<T> create(T t) {
    return new Entry<T>(t);
  }
  
  static class Entry<V> extends FastTable.Entry<V, Entry<V>> {
    Entry(V v) {
      super(v);
    }
  }
  
  /* ================ Public Stuff ================ */
  public boolean add (T o) {
    int oldSize = size;
    ensureAdded(o);
    return size > oldSize;
  }

  public boolean addAll (SymEqCollection<? extends T> c) {
    return addAll((Collection<? extends T>) c);
  }
  
  public boolean addAll (Collection<? extends T> c) {
    int oldSize = size;
    for (T o : c) {
      ensureAdded(o);
    }
    return oldSize > size;
  }

  public boolean contains (Object o) {
    return lookup(o) != null;
  }

  public boolean containsAll (SymEqCollection<?> c) {
    return containsAll((Collection<?>)c);
  }
  
  public boolean containsAll (Collection<?> c) {
    for (Object e : c) {
      if (!contains(e)) {
        return false;
      }
    }
    // else
    return true;
  }

  public boolean remove (Object o) {
    return delete(o) != null;
  }

  public boolean removeAll (SymEqCollection<?> c) {
    return removeAll((Collection<?>)c);
  }
  
  public boolean removeAll (Collection<?> c) {
    boolean modified = false;
    for (Object th : c) {
      modified |= remove(th);
    }
    return modified;
  }

  public boolean retainAll (SymEqCollection<?> c) {
    return retainAll((Collection<?>)c);
  }
  
  public boolean retainAll (Collection<?> c) {
    boolean modified = false;
    for (Object e : this) {
      if (!c.contains(e)) {
        modified |= remove(e);
      }
    }
    return modified;
  }

  public Object[] toArray () {
    Object[] arr = new Object[size()];
    return toArray(arr);
  }

  @SuppressWarnings("unchecked")
  public <V> V[] toArray (V[] a) {
    if (a.length >= size()) {
      int pos = 0;
      for (T o : this) {
        a[pos++] = (V) o; // unchecked
      }
      return a;
    } else {
      return (V[]) toArray();
    }
  }
  
  public FastEqSet<T> clone() {
    FastEqSet<T> that = new FastEqSet<T>(tblPow);
    for (T o : this) {
      that.strictAdd(o);
    }
    return that;
  }
  
  public Iterator<T> iterator () {
    return new TblIterator();
  }

  protected class TblIterator implements Iterator<T> {
    int idx;
    Entry<T> cur;

    public TblIterator() {
      idx = -1; cur = null;
      advance();
    }
    
    void advance() {
      if (cur != null) {
        cur = cur.next;
      }
      int len = table.length();
      while (idx < len && cur == null) {
        idx++;
        if (idx < len) {
          cur = table.get(idx);
        }
      }
    }
    
    public boolean hasNext () {
      return idx < table.length();
    }

    public T next () {
      Entry<T> e = cur;
      if (e == null) throw new NoSuchElementException();
      advance();
      return e.val;
    }

    public void remove () { throw new UnsupportedOperationException(); }
    
  }
}
