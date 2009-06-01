//
// Copyright (C) 2008 United States Government as represented by the
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * A generic sparse reference array that assumes clusters, and more
 * frequent intra-cluster access.
 *
 * This is motivated by looking for a more suitable ADT to model a heap address
 * space, where reference values consist of segment/offset pairs, we have
 * reasonably dense and very dynamic population inside of well separated segment
 * clusters, and intra-cluster access is more likely than inter-cluster access.
 *
 * An especially important feature is to be able to iterate efficiently over
 * set/unset elements in index intervals (cluster sizes).
 *
 * The result should find a compromise between the fast element access & iteration
 * of a simple, dense array, and the efficient memory storage of a HashMap
 * (if only it could avoid box objects).
 *
 * <2do> so far, we are totally ignorant of population constraints
 */
public final class SparseClusterArray <E> implements Iterable<E> {

  static final int CHUNK_BITS = 11;
  static final int CHUNK_SIZE = 2048;
  static final int N_ELEM = 1 << CHUNK_BITS;     // 11 bits chunk index -> 21 bits segment key (3x7bits / 128 segs)
  static final int ELEM_MASK = 0x7ff;
  static final int BM_ENTRIES = N_ELEM / 64;     // number of bitmap long entries

  // 7 bits per segment -> 128 children
  static final int SEG_BITS = 7;
  static final int N_SEG = 1 << SEG_BITS;
  static final int SEG_MASK = 0x7f;
  static final int S1 = 32-SEG_BITS; // L1 shift
  static final int S2 = S1-SEG_BITS; // L2 shift
  static final int S3 = S2-SEG_BITS; // L3 shift
  static final int CHUNK_BASEMASK = ~SEG_MASK;

  Root root = new Root();
  Chunk lastChunk;
  Chunk head;   // linked list for traversal
  int   nSet; // number of set elements;

  boolean trackChanges = false;
  Entry changes; // on demand change (LIFO) queue

  //------------------------------------ public types
  public static class Snapshot<T,E> {
    Entry<E> first;
    Entry<E> last;

    void add (int index, E value) {
      Entry entry = new Entry(index, value);

      if (first == null) {
        first = last = entry;
      } else {
        last.next = entry;
        last = entry;
      }
    }

  }

  public static class Entry<E> {  // queued element
    int index;
    Object value;

    Entry<E> next;

    Entry (int index, Object value){
      this.index = index;
      this.value = value;
    }
  }

  //------------------------------------ internal types

  //--- how we keep our data - index based trie
  static class Root {
    Node[] seg = new Node[N_SEG];
  }

  static class Node  {
    ChunkNode[] seg = new ChunkNode[N_SEG];
  }

  static class ChunkNode  {
    Chunk[] seg  = new Chunk[N_SEG];
  }

  static class Chunk implements Cloneable { // with some extra info to optimize in-chunk access
    int base, top;
    Chunk next;
    Object[] elements;  // it's actually E[], but of course we can't create arrays of a generic type
    long[] bitmap;

    Chunk() {}

    Chunk(int base){
      this.base = base;
      this.top = base + N_ELEM;

      elements = new Object[N_ELEM];
      bitmap = new long[BM_ENTRIES];
    }

    public String toString() {
      return "Chunk [base=" + base + ",top=" + top + ']';
    }

    @SuppressWarnings("unchecked")
    public <E> Chunk deepCopy( Cloner<E> cloner) throws CloneNotSupportedException {
      Chunk nc = (Chunk) super.clone();

      E[] elem = (E[])elements;   // bad, but we have to cope with type erasure
      Object[] e = new Object[N_ELEM];

      for (int i=nextSetBit(0); i>=0; i=nextSetBit(i+1)) {
        e[i] = cloner.clone(elem[i]);
      }

      nc.elements = e;
      nc.bitmap = bitmap.clone();

      return nc;
    }

    // quite some redundancy for a little '~' operator, but we don't want to waste an
    // extra call for it either (this is about performance)
    final int nextClearBit (int iStart) {
      long[] bm = bitmap;
      if (iStart < CHUNK_SIZE){
        int j = (iStart >> 6);
        long l = ~bm[j] & (0xffffffffffffffffL << iStart); // fill with zeros to the right

        while (true) {
          if (l != 0){
            int n, prevHalf, lowHalf;

            lowHalf = (int)l;                 // look at low int
            if (lowHalf != 0) {               // has bits -> check low 32 bits
              n = 31;
              prevHalf = lowHalf;
            } else {                          // low all zero, check high 32 bits
              n = 63;
              prevHalf = (int)(l>>>32);
            }

            lowHalf = prevHalf << 16;         // upper 16 bits all zeros?
            if (lowHalf != 0) {
              n -= 16;
              prevHalf = lowHalf;
            }

            lowHalf = prevHalf << 8;          // upper 8 bits all zeros all zeros?
            if (lowHalf != 0) {
              n -= 8;
              prevHalf = lowHalf;
            }

            lowHalf = prevHalf << 4;          // upper 4 bits all zeros?
            if (lowHalf != 0) {
              n -= 4;
              prevHalf = lowHalf;
            }

            lowHalf = prevHalf << 2;          // upper 2 bits all zeros?
            if (lowHalf != 0) {
              n -= 2;
              prevHalf = lowHalf;
            }

            return (j<<6) + n - ((prevHalf << 1) >>> 31);
          }

          if (j<31) {
            l = ~bm[++j];
          } else {
            break;
          }
        }
      }

      return -1;
    }

    final int nextSetBit (int iStart) {
      long[] bm = bitmap;

      if (iStart < CHUNK_SIZE){
        int j = (iStart >> 6);
        long l = bm[j] & (0xffffffffffffffffL << iStart); // fill with zeros to the right

        while (true) {
          if (l != 0){
            int n, prevHalf, lowHalf;

            lowHalf = (int)l;                 // look at low int
            if (lowHalf != 0) {               // has bits -> check low 32 bits
              n = 31;
              prevHalf = lowHalf;
            } else {                          // low all zero, check high 32 bits
              n = 63;
              prevHalf = (int)(l>>>32);
            }

            lowHalf = prevHalf << 16;         // upper 16 bits all zeros?
            if (lowHalf != 0) {
              n -= 16;
              prevHalf = lowHalf;
            }

            lowHalf = prevHalf << 8;          // upper 8 bits all zeros all zeros?
            if (lowHalf != 0) {
              n -= 8;
              prevHalf = lowHalf;
            }

            lowHalf = prevHalf << 4;          // upper 4 bits all zeros?
            if (lowHalf != 0) {
              n -= 4;
              prevHalf = lowHalf;
            }

            lowHalf = prevHalf << 2;          // upper 2 bits all zeros?
            if (lowHalf != 0) {
              n -= 2;
              prevHalf = lowHalf;
            }

            return (j<<6) + n - ((prevHalf << 1) >>> 31);
          }

          if (j<31) {
            l = bm[++j];
          } else {
            break;
          }
        }
      }

      return -1;
    }

    public boolean isEmpty() {
      long[] bm = bitmap;

      if ((bm[0] | bm[1] | bm[2] | bm[3]) != 0) return false;
      if ((bm[4] | bm[5] | bm[6] | bm[7]) != 0) return false;
      if ((bm[8] | bm[9] | bm[10] | bm[11]) != 0) return false;
      if ((bm[12] | bm[13] | bm[14] | bm[15]) != 0) return false;
      if ((bm[16] | bm[17] | bm[18] | bm[19]) != 0) return false;
      if ((bm[20] | bm[21] | bm[22] | bm[23]) != 0) return false;
      if ((bm[24] | bm[25] | bm[26] | bm[27]) != 0) return false;
      if ((bm[28] | bm[29] | bm[30] | bm[31]) != 0) return false;

      return true;
    }
  }

  //--- iteration over set elements
  // <2do> so far, both iterators are totally ignorant of changes in the underlying ADT

  class ElementIterator implements Iterator<E> {

    int idx;
    Chunk cur;
    int nVisited;

    ElementIterator () {
      cur = head;
    }

    public boolean hasNext() {
      return (nVisited < nSet);
    }

    @SuppressWarnings("unchecked")
    public E next() {
      Chunk c = cur;
      int i = idx;

      while (c != null) {
        i = c.nextSetBit(i);

        if (i < 0) { // try next chunk
          c = c.next;
          i = 0;
        } else {
          cur = c;
          idx = i+1;
          nVisited++;
          return (E) c.elements[i];
        }
      }

      return null;
    }

    public void remove() {
      throw new UnsupportedOperationException("can't remove elements from SparseClusterArray iterator");
    }

  }

  class ElementIndexIterator implements IndexIterator {
    int idx, processed;
    Chunk cur;

    ElementIndexIterator (int startIdx){
      // locate the start chunk (they are sorted)
      Chunk c;
      int i = startIdx & ELEM_MASK;
      idx = i;
      for (c=head; c!= null; c=c.next) {
        if (c.top > i) {
          cur = c;
        }
      }
    }

    public int next () {
      Chunk c = cur;
      int i = idx;

      if (processed < nSet) {
        while (c != null) {
          i = c.nextSetBit(i);

          if (i < 0) { // try next chunk
            c = c.next;
            i = 0;
          } else {
            cur = c;
            idx = i+1;
            processed++;
            return i;
          }
        }
      }

      cur = null;
      return -1;
    }

  }


  //------------------------------------ internal methods

  void sortInChunk (Chunk newChunk) {
    if (head == null) {
      head = newChunk;
    } else {
      int base = newChunk.base;
      if (base < head.base) {
        newChunk.next = head;
        head = newChunk;
      } else {
        Chunk cprev, c;
        for (cprev=head, c=cprev.next; c != null; cprev=c, c=c.next) {
          if (base < c.base) {
            newChunk.next = c;
            break;
          }
        }
        cprev.next = newChunk;
      }
    }
  }

  //------------------------------------ public API

  public SparseClusterArray (){
  }

  @SuppressWarnings("unchecked")
  public E get (int i) {
    Node l1;
    ChunkNode l2;
    Chunk l3 = lastChunk;

    if (l3 != null && (l3.base == (i & CHUNK_BASEMASK))) {  // cache optimization for in-cluster access
      return (E) l3.elements[i & ELEM_MASK];
    }

    int  j = i >>>  S1;
    if ((l1 = root.seg[j]) != null) {           // L1
      j = (i >>> S2) & SEG_MASK;
      if ((l2 = l1.seg[j]) != null) {           // L2
        j = (i >>> S3) & SEG_MASK;
        if ((l3 = l2.seg[j]) != null) {         // L3
          // too bad we can't get rid of this cast
          lastChunk = l3;
          return  (E) l3.elements[i & ELEM_MASK];
        }
      }
    }

    lastChunk = null;
    return null;
  }


  public void set (int i, E e) {
    Node l1;
    ChunkNode l2;
    Chunk l3 = lastChunk;
    int j;

    if (l3 == null || (l3.base != (i & CHUNK_BASEMASK))) { // cache optimization for in-cluster access
      j = i >>>  S1;
      if ((l1 = root.seg[j]) == null) {         // new L1 -> new L2,L3
        l1 = new Node();
        root.seg[j] = l1;

        j = (i >>> S2) & SEG_MASK;
        l2 = new ChunkNode();
        l1.seg[j] = l2;

        j = (i >>> S3) & SEG_MASK;
        l3 = new Chunk(i & ~ELEM_MASK);
        sortInChunk(l3);
        l2.seg[j] = l3;

      } else {                                  // had L1
        j = (i >>> S2) & SEG_MASK;
        if ((l2 = l1.seg[j]) == null) {         // new L2 -> new L3
          l2 = new ChunkNode();
          l1.seg[j] = l2;

          j = (i >>> S3) & SEG_MASK;
          l3 = new Chunk(i & ~ELEM_MASK);
          sortInChunk(l3);
          l2.seg[j] = l3;

        } else {                                // had L2
          j = (i >>> S3) & SEG_MASK;
          if ((l3 = l2.seg[j]) == null) {       // new L3
            l3 = new Chunk(i & ~ELEM_MASK);
            sortInChunk(l3);
            l2.seg[j] = l3;
          }
        }
      }

      lastChunk = l3;
    }

    j = i & ELEM_MASK;

    long[/*32*/] bm = l3.bitmap;
    int u = (j >> 6);
    int v = (i & 0x3f);
    boolean isSet = ((bm[u] >> v) & 0x1) > 0;

    if (trackChanges) {
      Entry entry = new Entry(i,l3.elements[j]);
      entry.next = changes;
      changes = entry;
    }

    l3.elements[j] = e;

    if (e != null) {
      if (!isSet) {
        bm[u] |= (1L<<v);
        nSet++;
      }
    } else {
      if (isSet) {
        bm[u] &= ~(1L<<v);
        nSet--;
      }

      // <2do> discard upwards if chunk is empty ? (maybe as an option)
    }
  }

  /**
   * find first null element within given range [i, i+length[
   * @return -1 if there is none
   */
  public int firstNullIndex (int i, int length) {
    Node l1;
    ChunkNode l2;
    Chunk l3 = lastChunk;
    int j;
    int iMax = i + length;

    if (l3 == null || (l3.base != (i & CHUNK_BASEMASK))) { // cache optimization for in-cluster access
      j = i >>>  S1;
      if ((l1 = root.seg[j]) != null) {         // new L1 -> new L2,L3
        j = (i >>> S2) & SEG_MASK;
        if ((l2 = l1.seg[j]) != null) {         // new L2 -> new L3
          j = (i >>> S3) & SEG_MASK;
          l3 = l2.seg[j];
        }
      }
    }

    int k = i & CHUNK_BASEMASK;
    while (l3 != null) {
      k = l3.nextClearBit(k);

      if (k >= 0) {             // Ok, got one in the chunk
        lastChunk = l3;
        i = l3.base + k;
        return (i < iMax) ? i : -1;

      } else {                  // chunk full
        Chunk l3Next = l3.next;
        int nextBase = l3.base + CHUNK_SIZE;
        if ((l3Next != null) && (l3Next.base == nextBase)) {
          if (nextBase < iMax) {
            l3 = l3Next;
            k=0;
          } else {
            return -1;
          }
        } else {
          lastChunk = null;
          return (nextBase < iMax) ? nextBase : -1;
        }
      }
    }

    // no allocated chunk for 'i'
    lastChunk = null;
    return i;
  }

  /**
   * deep copy
   * we need to do this depth first, right-to-left, to maintain the
   * Chunk list ordering. We also compact during cloning, i.e. remove
   * empty chunks and ChunkNodes/Nodes w/o descendants
   */
  public SparseClusterArray<E> deepCopy (Cloner<E> elementCloner) {
    SparseClusterArray<E> a = new SparseClusterArray<E>();
    a.nSet = nSet;

    Node[] newNodeList = a.root.seg;

    Node newNode = null;
    ChunkNode newChunkNode = null;
    Chunk newChunk = null, lastChunk = null;

    Node[] nList = root.seg;

    try {
      for (int i=0, i1=0; i<nList.length; i++) {
        Node n = nList[i];
        if (n != null) {
          ChunkNode[] cnList = n.seg;

          for (int j=0, j1=0; j<cnList.length; j++) {
            ChunkNode cn = cnList[j];
            if (cn != null) {
              Chunk[] cList = cn.seg;

              for (int k=0, k1=0; k<cList.length; k++) {
                Chunk c = cList[k];

                if (c != null && !c.isEmpty()) {
                  newChunk = c.deepCopy(elementCloner);
                  if (lastChunk == null) {
                    a.head = lastChunk = newChunk;
                  } else {
                    lastChunk.next = newChunk;
                    lastChunk = newChunk;
                  }

                  // create the required ChunkNode/Node instances
                  if (newNode == null) {
                    newNode = new Node();
                    j1 = k1 = 0;
                    newNodeList[i1++] = newNode;
                  }

                  if (newChunkNode == null) {
                    newChunkNode = new ChunkNode();
                    newNode.seg[j1++] = newChunkNode;
                  }

                  newChunkNode.seg[k1++] = newChunk;
                }
              }
            }
            newChunkNode = null;
          }
        }
        newNode = null;
      }
    } catch (CloneNotSupportedException cnsx) {
      return null; // maybe we should re-raise
    }

    return a;
  }

  /**
   * create a snapshot that can be used to restore a certain state of our array
   * This is more suitable than cloning in case the array is very sparse, or
   * the elements contain a lot of transient data we don't want to store
   */
  @SuppressWarnings("unchecked")
  public <T> Snapshot<E,T> getSnapshot (Transformer<E,T> transformer){
    int n = nSet;

    Snapshot snap = new Snapshot();
    int j=0;

    for (Chunk c = head; c != null; c = c.next) {
      int base = c.base;
      int i=-1;
      while ((i=c.nextSetBit(i+1)) >= 0) {
        T val = transformer.transform((E)c.elements[i]);
        snap.add((base + i), val);
        if (++j >= n) {
          break;
        }
      }
    }

    return (Snapshot<E,T>)snap;
  }

  @SuppressWarnings("unchecked")
  public <T> void restoreSnapshot (Snapshot<E,T> snap, Transformer<E,T> transformer) {
    // <2do> - there are more efficient ways to restore small changes,
    // but since snapshot elements are ordered it should be reasonably fast
    clear();

    for (Entry e=snap.first; e != null; e = e.next) {
      E obj = transformer.restore((T)e.value);
      set(e.index,obj);
    }
  }

  public void clear() {
    lastChunk = null;
    head = null;
    root = new Root();
    nSet = 0;

    changes = null;
  }

  public void trackChanges () {
    trackChanges = true;
  }

  public void stopTrackingChanges() {
    trackChanges = false;
  }

  public boolean isTrackingChanges() {
    return trackChanges;
  }

  public Entry<E> getChanges() {
    return changes;
  }

  public void resetChanges() {
    changes = null;
  }

  public void revertChanges (Entry<E> changes) {
    for (Entry<E> e = changes; e != null; e = e.next) {
      set(e.index, (E)e.value);
    }
  }

  public String toString() {
    return "SparseClusterArray [nSet=" + nSet + ']';
  }

  public int numberOfChunks() {
    // that's only for debugging purposes, we should probably cache
    int n = 0;
    for (Chunk c = head; c != null; c = c.next) {
      n++;
    }
    return n;
  }

  //--- iteration over set elements

  public IndexIterator getElementIndexIterator (int startIdx) {
    return new ElementIndexIterator(startIdx);
  }

  public Iterator<E> iterator() {
    return new ElementIterator();
  }

  public int cardinality () {
    return nSet;
  }


  //------------------------------------ test driver & benchmarks
  final static int MAX_ROUNDS = 100;
  final static int MAX_N = 10000;
  final static int MAX_T = 6;

  public static void main (String[] args) {
    //testBasic();
    //testNextNull();

    //testClone();
    //testSnapshot();
    testChanges();

    //testSparseClusterArray();
    //testHashMap();
  }

  static void testBasic() {
    SparseClusterArray<Object> arr = new SparseClusterArray<Object>();
    int ref;

    ref = (1 << S1) | 42;
    arr.set(ref, new Integer(ref));

    Object o = arr.get(ref);
    System.out.println(o);

    ref = (2 << S1);
    arr.set(ref, new Integer(ref));

    System.out.println("n = " + arr.cardinality());
    for (Object e : arr) {
      System.out.println(e);
    }
  }

  static void testNextNull () {
    Object e = new Integer(42);
    SparseClusterArray<Object> arr = new SparseClusterArray<Object>();
    int k;
    int limit = 10000000;

    arr.set(0, e);
    k = arr.firstNullIndex(0, limit);
    System.out.println("k=" + k);  // 1

    arr.set(0,null);
    k = arr.firstNullIndex(0, limit);
    System.out.println("k=" + k);  // 0

    int i=0;
    for (;i<512; i++) {
      arr.set(i, e);
    }
    long t1 = System.currentTimeMillis();
    for (int j=0; j<100000; j++) {
      k = arr.firstNullIndex(0, limit);
    }
    long t2 = System.currentTimeMillis();
    System.out.println("k=" + k + ", 100000 lookups in: " + (t2 - t1)); // 512

    for (;i<2048;i++) {
      arr.set(i, e);
    }
    k = arr.firstNullIndex(0, limit);
    System.out.println("k=" + k);  // 2048 (no chunk)

    k = arr.firstNullIndex(0, 2048);
    System.out.println("k=" + k); // -1

    arr.set(2048, e);
    arr.set(2048,null);
    k = arr.firstNullIndex(0, limit);
    System.out.println("k=" + k);  // 2048 (new chunk)

    for (; i<2500; i++) {
      arr.set(i, e);
    }
    k = arr.firstNullIndex(0, limit);
    System.out.println("k=" + k);  // 2500

  }

  static void testClone() {
    SparseClusterArray<Integer> arr = new SparseClusterArray<Integer>();

    arr.set(0, new Integer(0));
    arr.set(42, new Integer(42));
    arr.set(6762, new Integer(6762));
    arr.set(6762, null);

    Cloner<Integer> cloner = new Cloner<Integer>() {
      public Integer clone (Integer other) {
        return new Integer(other);
      }
    };
    SparseClusterArray<Integer> newArr = arr.deepCopy(cloner);
    for (Integer i : newArr) {
      System.out.println(i);
    }
  }

  static void testSnapshot() {
    SparseClusterArray<Integer> arr = new SparseClusterArray<Integer>();

    arr.set(0, new Integer(0));
    arr.set(42, new Integer(42));
    arr.set(4095, new Integer(4095));
    arr.set(4096, new Integer(4096));
    arr.set(7777, new Integer(7777));
    arr.set(67620, new Integer(67620));
    arr.set(67620, null);
    arr.set(7162827, new Integer(7162827));

    Transformer<Integer,String> transformer = new Transformer<Integer,String>() {
      public String transform (Integer n) {
        return n.toString();
      }
      public Integer restore (String s) {
        return new Integer( Integer.parseInt(s));
      }
    };

    Snapshot<Integer,String> snap = arr.getSnapshot(transformer);
    for (Entry<String> e = snap.first; e != null; e = e.next) {
      System.out.println("a[" + e.index + "] = " + e.value);
    }

    arr.set(42,null);
    arr.set(87, new Integer(87));
    arr.set(7162827, new Integer(-1));

    arr.restoreSnapshot(snap, transformer);
    for (Integer i : arr) {
      System.out.println(i);
    }
  }

  static void testChanges() {
    SparseClusterArray<Integer> arr = new SparseClusterArray<Integer>();

    arr.set(42, new Integer(42));
    arr.set(6276, new Integer(6276));

    arr.trackChanges();

    arr.set(0, new Integer(0));
    arr.set(42, new Integer(-1));
    arr.set(4095, new Integer(4095));
    arr.set(4096, new Integer(4096));
    arr.set(7777, new Integer(7777));
    arr.set(7162827, new Integer(7162827));

    Entry<Integer> changes = arr.getChanges();
    arr.revertChanges(changes);

    for (Integer i : arr) {
      System.out.println(i);
    }
  }

  static void testSparseClusterArray() {
    Random r = new Random(0);
    Object elem = new Object();
    long t1, t2;
    int n = 0;

    t1 = System.currentTimeMillis();
    SparseClusterArray<Object> arr = new SparseClusterArray<Object>();

    for (int i=0; i<MAX_ROUNDS; i++) {
      int ref = r.nextInt(MAX_T) << S1;
      for (int j=0; j<MAX_N; j++) {
        ref |= r.nextInt(MAX_N);
        //ref |= j;
        arr.set(ref, elem);
        if (arr.get(ref) == null) throw new RuntimeException("element not set: " + i);
      }
    }
    t2 = System.currentTimeMillis();
    System.out.println("SparseArray random write/read of " + arr.cardinality() + " elements: "+ (t2 - t1));

    n=0;
    t1 = System.currentTimeMillis();
    for (Object e : arr) {
      n++;
    }
    t2 = System.currentTimeMillis();
    System.out.println("SparseArray iteration over " + n + " elements: " + (t2 - t1));

  }

  static void testHashMap() {
    Random r = new Random(0);
    Object elem = new Object();
    long t1, t2;

    t1 = System.currentTimeMillis();
    HashMap<Integer,Object> arr = new HashMap<Integer,Object>();

    for (int i=0; i<MAX_ROUNDS; i++) {
      int ref = r.nextInt(MAX_T) << S1;
      for (int j=0; j<MAX_N; j++) {
        ref |= r.nextInt(MAX_N);
        //ref |= j;
        arr.put(ref, elem);
        if (arr.get(ref) == null) throw new RuntimeException("element not set: " + i);
      }
    }
    t2 = System.currentTimeMillis();
    System.out.println("HashMap random write/read of " + arr.size() + " elements: " + (t2 - t1));

    int n=0;
    t1 = System.currentTimeMillis();
    for (Object e : arr.values()) {
      n++;
    }
    t2 = System.currentTimeMillis();
    System.out.println("HashMap iteration over " + n + " elements: " + (t2 - t1));

  }
}
