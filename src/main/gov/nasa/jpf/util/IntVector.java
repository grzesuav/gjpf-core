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


/**
 * (more efficient?) alternative to Vector<Integer>
 * @author pcd
 */
public final class IntVector implements Comparable<IntVector> {
  public static final int defaultInitCap = 40;

  
  /** <i>size</i> as in a java.util.Vector. */
  protected int size;
  
  /** the backing array. */
  protected int[] data;
  
  /** growth strategy. */
  protected Growth growth;
  
  
  public IntVector(Growth initGrowth, int initCap) {
    growth = initGrowth;
    data = new int[initCap];
    size = 0;
  }
  
  public IntVector(int[] init) {
    this(Growth.defaultGrowth, init.length);
    size = init.length;
    System.arraycopy(init, 0, data, 0, size);
  }
  
  public IntVector(Growth initGrowth) { this(initGrowth,defaultInitCap); }
  
  public IntVector(int initCap) { this(Growth.defaultGrowth, initCap); }
  
  public IntVector() { this(Growth.defaultGrowth,defaultInitCap); }
  
  public void add(int x) {
    if (size+1 > data.length) {
      ensureCapacity(size+1);
    }
    data[size] = x;
    size++;
  }

  public void add2(int x1, int x2) {
    if (size+2 > data.length) {
      ensureCapacity(size+2);
    }
    data[size] = x1;
    size++;
    data[size] = x2;
    size++;
  }
  
  public void add3(int x1, int x2, int x3) {
    if (size+3 > data.length) {
      ensureCapacity(size+3);
    }
    data[size] = x1;
    size++;
    data[size] = x2;
    size++;
    data[size] = x3;
    size++;
  }
  
  public void addZeros (int length) {
    int newSize = size + length;
    if (newSize > data.length) {
      ensureCapacity(size + length);
    }
    for (int i = size; i < newSize; i++) {
      data[i] = 0;
    }
    size = newSize;
  }

  public void append(int[] x) {
    if (size + x.length > data.length) {
      ensureCapacity(size + x.length);
    }
    System.arraycopy(x, 0, data, size, x.length);
    size += x.length;
  }
  
  public void append(int[] x, int pos, int len) {
    if (size + len > data.length) {
      ensureCapacity(size + len);
    }
    System.arraycopy(x, pos, data, size, len);
    size += len;
  }
  
  public void append(IntVector x) {
    if (x == null) return;
    if (size + x.size > data.length) {
      ensureCapacity(size + x.size);
    }
    System.arraycopy(x.data, 0, data, size, x.size);
    size += x.size;
  }
  
  public int get(int idx) {
    if (idx >= size) {
      return 0;
    } else {
      return data[idx];
    }
  }
  
  public void set(int idx, int x) {
    ensureSize(idx+1);
    data[idx] = x;
  }
  
  public int[] toArray (int[] dst) {
    System.arraycopy(data,0,dst,0,size);
    return dst;
  }

  public int dumpTo (int[] dst, int pos) {
    System.arraycopy(data,0,dst,pos,size);
    return pos + size;
  }

  public void squeeze() {
    while (size > 0 && data[size - 1] == 0) size--;
  }
  
  public void setSize(int sz) {
    if (sz > size) {
      ensureCapacity(sz);
      size = sz;
    } else {
      while (size > sz) {
        size--;
        data[size] = 0;
      }
    }
  }

  public void clear() { setSize(0); }
  
  public int size() { return size; }
  
  public int[] toArray() {
    int[] out = new int[size];
    System.arraycopy(data, 0, out, 0, size);
    return out;
  }

  public void ensureSize(int sz) {
    if (size < sz) {
      ensureCapacity(sz);
      size = sz;
    }
  }
  
  public void ensureCapacity(int desiredCap) {
    if (data.length < desiredCap) {
      int[] newData = new int[growth.grow(data.length, desiredCap)];
      System.arraycopy(data, 0, newData, 0, size);
      data = newData;
    }
  }
  
  public static void copy(IntVector src, int srcPos, IntVector dst, int dstPos, int len) {
    if (len == 0) return;
    src.ensureCapacity(srcPos + len);
    dst.ensureSize(dstPos+len);
    System.arraycopy(src.data, srcPos, dst.data, dstPos, len);
  }

  public static void copy(int[] src, int srcPos, IntVector dst, int dstPos, int len) {
    if (len == 0) return;
    dst.ensureSize(dstPos+len);
    System.arraycopy(src, srcPos, dst.data, dstPos, len);
  }

  public static void copy(IntVector src, int srcPos, int[] dst, int dstPos, int len) {
    if (len == 0) return;
    src.ensureCapacity(srcPos + len);
    System.arraycopy(src.data, srcPos, dst, dstPos, len);
  }

  /** dictionary/lexicographic ordering */
  public int compareTo (IntVector that) {
    if (that == null) return this.size; // consider null and 0-length the same
    
    int comp;
    int smaller = Math.min(this.size, that.size);
    for (int i = 0; i < smaller; i++) {
      comp = this.data[i] - that.data[i];
      if (comp != 0) return comp;
    }
    // same up to shorter length => compare sizes
    return this.size - that.size;
  }
}
