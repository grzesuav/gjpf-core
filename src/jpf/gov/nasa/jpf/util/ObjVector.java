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
package gov.nasa.jpf.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * more customizable alternative to java.util.Vector.  also, set(x,v) automatically
 * grows the structure as needed.
 * @author pcd
 */
public final class ObjVector<E> implements ReadOnlyObjList<E>, Cloneable {
  public static final int defaultInitCap = 40;

  
  /** <i>size</i> as in a java.util.Vector. */
  protected int size;
  
  /** the backing array. */
  protected Object[] data;
  
  /** growth strategy. */
  protected Growth growth;
  
  
  /*======================== CONSTRUCTORS ======================*/
  public ObjVector(Growth initGrowth, int initCap) {
    growth = initGrowth;
    data = new Object[initCap];
    size = 0;
  }
  
  public ObjVector(Growth initGrowth) {
    this(initGrowth,defaultInitCap);
  }
  
  public ObjVector(int initCap) { 
    this(Growth.defaultGrowth, initCap);
  }
  
  public ObjVector() {
    this(Growth.defaultGrowth,defaultInitCap);
  }
  
  public <F extends E> ObjVector(F[] init) {
    this(init.length);
    append(init);
  }
  
  public <F extends E> ObjVector(ObjVector<F> from) {
    this.data = new Object[from.data.length];
    this.size = from.size;
    this.growth = from.growth;
    System.arraycopy(from.data, 0, this.data, 0, size);
  }
  
  
  /*========================= PUBLIC METHODS =======================*/
  public void add(E x) {
    if (size >= data.length) {
      ensureCapacity(size+1);
    }
    data[size] = x;
    size++;
  }
  
  public void addNulls (int length) {
    int newSize = size + length;
    if (newSize > data.length) {
      ensureCapacity(size + length);
    }
    for (int i = size; i < newSize; i++) {
      data[i] = null;
    }
    size = newSize;
  }

  public <F extends E> void append(F[] x) {
    if (size + x.length > data.length) {
      ensureCapacity(size + x.length);
    }
    System.arraycopy(x, 0, data, size, x.length);
    size += x.length;
  }

  public <F extends E> void append(F[] x, int pos, int len) {
    if (size + len > data.length) {
      ensureCapacity(size + len);
    }
    System.arraycopy(x, pos, data, size, len);
    size += len;
  }
  
  public <F extends E> void append(ObjVector<F> x) {
    if (size + x.size > data.length) {
      ensureCapacity(size + x.size);
    }
    System.arraycopy(x.data, 0, data, size, x.size);
    size += x.size;
  }
  
  @SuppressWarnings("unchecked")
  public <F extends E> void append(ObjArray<F> x) {
    append((F[])(x.data));
  }
  
  public <F extends E> void addAll(Iterable<F> x) {
    if (x instanceof ObjVector) {
      append((ObjVector<F>) x);
      return;
    }
    // else
    if (x instanceof ObjArray) {
      append((ObjArray<F>) x);
      return;
    }
    // else
    if (x == null) return;
    // else
    for (F e : x) {
      add(e);
    }
  }
  
  @SuppressWarnings("unchecked")
  public E get(int idx) {
    if (idx >= size) {
      return null;
    } else {
      return (E) data[idx];
    }
  }
  
  public void set(int idx, E v) {
    ensureSize(idx+1);
    data[idx] = v;
  }

  public <F> F[] toArray (F[] dst) {
    System.arraycopy(data,0,dst,0,size);
    return dst;
  }

  public ObjArray<E> toObjArray () {
    ObjArray<E> dst = new ObjArray<E>(size);
    System.arraycopy(data,0,dst.data,0,size);
    return dst;
  }

  public int dumpTo (Object[] dst, int pos) {
    System.arraycopy(data,0,dst,pos,size);
    return pos + size;
  }

  public ObjVector<E> clone() {
    return new ObjVector<E>(this);
  }
  
  public void squeeze() {
    while (size > 0 && data[size - 1] == null) size--;
  }
  
  public void setSize(int sz) {
    if (sz > size) {
      ensureCapacity(sz);
      size = sz;
    } else {
      while (size > sz) {
        size--;
        data[size] = null;
      }
    }
  }

  public void clear() { setSize(0); }
  
  public int size() { return size; }
  
  public int length() { return size; }
  
  public void ensureSize(int sz) {
    if (size < sz) {
      ensureCapacity(sz);
      size = sz;
    }
  }
  
  public void ensureCapacity(int desiredCap) {
    if (data.length < desiredCap) {
      Object[] newData = new Object[growth.grow(data.length, desiredCap)];
      System.arraycopy(data, 0, newData, 0, size);
      data = newData;
    }
  }
  
  @SuppressWarnings("unchecked")
  public void sort(Comparator<? super E> comp) {
    Arrays.sort(data, 0, size, (Comparator<Object>) comp);
  }
  
  public static <E> void copy(ObjVector<? extends E> src, int srcPos,
                              ObjVector<E> dst, int dstPos, int len) {
    src.ensureCapacity(srcPos + len);
    dst.ensureSize(dstPos+len);
    System.arraycopy(src.data, srcPos, dst.data, dstPos, len);
  }
  
  public static <E> void copy(ObjVector<? extends E> src, int srcPos,
      E[] dst, int dstPos, int len) {
    src.ensureCapacity(srcPos + len);
    //dst.ensureSize(dstPos+len);
    System.arraycopy(src.data, srcPos, dst, dstPos, len);
  }

  public Iterator<E> iterator () {
    return new OVIterator();
  }
  
  class OVIterator implements Iterator<E> {
    int idx = 0;
    
    public boolean hasNext () {
      return idx < size;
    }

    @SuppressWarnings("unchecked")
    public E next () {
      if (idx >= data.length) throw new NoSuchElementException();
      E e = (E) data[idx];
      idx++;
      return e;
    }

    public void remove () {
      throw new UnsupportedOperationException();
    }
    
  }
}
