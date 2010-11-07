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
package gov.nasa.jpf.jvm;

import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.ObjVector;

import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Area defines a memory class. An area can be used for objects
 * in the DynamicArea (heap) or classes in the StaticArea (classinfo with
 * static fields).
 */
public abstract class Area<EI extends ElementInfo> implements Iterable<EI> {

  public interface Memento {
    void restore();
  }

  /**
   * saves area contents in a simple ElementInfo.Memento array for later restoration
   */
  class GenericSnapshotMemento implements Memento {
    ElementInfo.Memento<EI>[] e;

    protected GenericSnapshotMemento (ElementInfo.Memento<EI>[] e){
      int len = elements.length();
      for (int i=0; i<len; i++){
        EI ei = elements.get(i);
        if (ei != null){
          e[i] = ei.getMemento();
        }
      }

      this.e = e;
    }

    public void restore() {
      int len = e.length;

      nElements = len;
      elements.setSize(len);
      
      for (int i=0; i<len; i++){
        ElementInfo.Memento<EI> em = e[i];
        elements.set(i, em != null ? em.restore() : null);
      }

      hasChanged.clear();
    }
  }

  /**
   * Contains the information for each element.
   */
  protected ObjVector<EI> elements;

  /**
   * The number of elements. This is the number of non-null
   * refs in the array, which can differ from the size of
   * the array and can differ from lastElement+1.
   */
  protected int nElements;

  /**
   * Reference of the kernel state this dynamic area is in.
   */
  public final KernelState ks;

  /**
   * Set of bits used to see which elements have changed.
   */
  protected final BitSet hasChanged;


  /**
   * support for iterators that return all allocated objects, so that
   * clients don't have to know our concrete implementation
   *
   * sometimes it sucks not having variant generics
   */
  protected abstract class IteratorBase {
    int i, visited;

    public void remove() {
      throw new UnsupportedOperationException ("illegal operation, only GC can remove objects");
    }

    public boolean hasNext() {
      return (i < elements.size()) && (visited < nElements);
    }

  }

  protected class EIiterator extends IteratorBase implements Iterator<EI> {
    public EI next() {
      for (; i < elements.size(); i++) {
        EI ei = elements.get(i);
        if (ei != null) {
          i++; visited++;
          return ei;
        }
      }

      throw new NoSuchElementException();
    }
  }

  protected class ElementInfoIterator extends IteratorBase implements Iterator<ElementInfo>, Iterable<ElementInfo> {
    public ElementInfo next() {
      for (; i < elements.size(); i++) {
        ElementInfo ei = elements.get(i);
        if (ei != null) {
          i++; visited++;
          return ei;
        }
      }

      throw new NoSuchElementException();
    }

    public Iterator<ElementInfo> iterator() {
      return this;
    }
  }

  /**
   * extended iterator that just returns changed elements entries. Note that next()
   * can return 'null' elements, in which case we can to query the current ref value
   * with getLastRef()
   */
  public class ChangedIterator implements java.util.Iterator<EI> {
    int i = getNextChanged(0);
    int ref = -1;

    public void remove() {
      throw new UnsupportedOperationException ("illegal operation, only GC can remove objects");
    }

    public boolean hasNext() {
      return (i >= 0);
    }

    public EI next() {
      if (i >= 0){
        EI ei = elements.get(i);
        ref = i;
        i = getNextChanged(i+1);
        return ei;

      } else {
        throw new NoSuchElementException();
      }
    }

    /**
     * this returns the index of the last element returned by next()
     */
    public int getLastRef() {
      return ref;
    }
  }

  // its not a standard java.util.EIiterator because we don't want to box ints
  public class ChangedReferenceIterator implements gov.nasa.jpf.util.IntIterator {
    int i = getNextChanged(0);

    public void remove() {
      throw new UnsupportedOperationException ("illegal operation, only GC can remove objects");
    }

    public boolean hasNext() {
      return (i >= 0);
    }

    public int next() {
      if (i >= 0){
        int ref = i;
        i = getNextChanged(i+1);
        return ref;

      } else {
        throw new NoSuchElementException();
      }
    }
  }

  public Area (KernelState ks) {
    this.ks = ks;
    elements = new ObjVector<EI>(1024);
    nElements = 0;
    hasChanged = new BitSet();
  }

  public EIiterator iterator() {
    return new EIiterator();
  }

  public ChangedIterator changedIterator() {
    return new ChangedIterator();
  }

  public ChangedReferenceIterator changedReferenceIterator() {
    return new ChangedReferenceIterator();
  }

  public int numberOfChanged() {
    return hasChanged.cardinality();
  }

  public abstract Memento getMemento();

  /**
   * reset any information that has to be re-computed in a backtrack
   * (i.e. hasn't been stored explicitly)
   */
  void resetVolatiles () {
    // nothing yet
  }

  void restoreVolatiles () {
    // nothing to do
  }

  void cleanUpDanglingReferences () {
    Heap heap = JVM.getVM().getHeap();

    for (ElementInfo e : this) {
      if (e != null) {
        e.cleanUp(heap);
      }
    }
  }

  public int count () {
    return nElements;
  }

  public int getNextChanged (int startIdx) {
    return hasChanged.nextSetBit(startIdx);
  }

  public EI get (int index) {
    if (index < 0) {
      return null;
    } else {
      return elements.get(index);
    }
  }

  public EI ensureAndGet(int index) {
    EI ei = elements.get(index);
    if (ei == null) {      
      ei = createElementInfo();

      ei.resurrect(this, index);
      
      elements.set(index, ei);
      nElements++;
    }
    return ei;
  }

  public int getLength() {
    return elements.size();
  }

  public void hash (HashData hd) {
    int length = elements.size();

    for (int i = 0; i < length; i++) {
      EI ei = elements.get(i);
      if (ei != null) {
        ei.hash(hd);
      }
    }
  }

  public int hashCode () {
    HashData hd = new HashData();

    hash(hd);

    return hd.getValue();
  }

  public void removeAll() { removeAllFrom(0); }

  public void removeAllFrom (int idx) {
    int l = elements.size();

    for (int i = idx; i < l; i++) {
      remove(i,true);
    }
  }

  public String toString () {
    return getClass().getName() + "@" + super.hashCode();
  }

  // BUG! nElements is not consistent with the elements array length
  // somtimes it seems to be bigger
  // UPDATE: fixed? -pcd
  protected void add (int index, EI e) {
    e.setIndex(index);

    assert (elements.get(index) == null) :
      "trying to overwrite non-null object: " + elements.get(index) + " with: " + e;

    nElements++;
    elements.set(index,e);
    markChanged(index);
  }

  public void markChanged (int index) {
    hasChanged.set(index);
    ks.changed();
  }

  public void markUnchanged() {
    hasChanged.clear();
  }

  public boolean anyChanged() {
    return !hasChanged.isEmpty();
  }

  protected void remove (int index, boolean nullOk) {
    EI ei = elements.get(index);

    if (nullOk && ei == null) return;

    assert (ei != null) : "trying to remove null object at index: " + index;

    if (ei.recycle()) {
      elements.set(index, null);
      elements.squeeze();
      nElements--;
      markChanged(index);
    }
  }

  abstract EI createElementInfo ();
}
