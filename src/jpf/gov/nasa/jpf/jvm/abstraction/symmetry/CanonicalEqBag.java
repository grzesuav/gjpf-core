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

import gov.nasa.jpf.util.Misc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;


class CanonicalEqBag<T> implements EqBag<T> {
  // AbstractingSerializer has hard-coded dependence on the structure of this class
  Object[] data = Misc.emptyObjectArray;
  
  public boolean add (T o) {
    int len = data.length;
    int hash = System.identityHashCode(o);
    Object[] newData = new Object[len + 1];
    int i = 0;
    while (i < len && hash > System.identityHashCode(data[i])) {
      newData[i] = data[i];
      i++;
    }
    newData[i] = o;
    System.arraycopy(data, i, newData, i+1, len - i);
    data = newData;
    return true;
  }

  public boolean addAll (SymEqCollection<? extends T> c) {
    return addAll((Collection<? extends T>) c);
  }
  
  public boolean addAll (Collection<? extends T> c) {
    boolean modified = false;
    for (T e : c) {
      modified |= add(e);
    }
    return modified;
  }

  public void clear () {
    data = new Object[] {};
  }

  public boolean contains (Object o) {
    // TODO: could be binary search
    for (int i = 0; i < data.length; i++) {
      if (data[i] == o) {
        return true;
      }
    }
    // else
    return false;
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

  public boolean isEmpty () {
    return data.length == 0;
  }

  @SuppressWarnings("unchecked")
  public Iterator<T> iterator () {
    return (Iterator<T>) Arrays.asList(data).iterator();
  }

  public boolean remove (Object o) {
    int len = data.length;
    for (int i = 0; i < len; i++) {
      if (data[i] == o) {
        Object[] newData = new Object[len - 1];
        if (i > 0) {
          System.arraycopy(data, 0, newData, 0, i);
        }
        if (len - i - 1 > 0) {
          System.arraycopy(data, i + 1, newData, i, len - i - 1);
        }
        data = newData;
        return true;
      }
    }
    // else
    return false;
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

  public int size () {
    return data.length;
  }

  public Object[] toArray () {
    return data.clone();
  }

  @SuppressWarnings("unchecked")
  public <V> V[] toArray (V[] a) {
    if (a.length >= data.length) {
      System.arraycopy(data, 0, a, 0, data.length);
      return a;
    } else {
      return (V[]) toArray();
    }
  }
  
  public CanonicalEqBag<T> clone() {
    CanonicalEqBag<T> that = new CanonicalEqBag<T>();
    that.data = this.data; // used immutably
    return that;
  }
}
