//
// Copyright (C) 2012 United States Government as represented by the
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

import java.io.PrintStream;
import java.util.Iterator;

/**
 * immutable generic map type that maps integer keys to object values
 */
public abstract class PersistentIntMap<V> {
  
  /**
   * the abstract root class for all node types
   */
  protected abstract static class Node<V> implements Cloneable {
    
        
    abstract Object getElement( int levelIndex);
    
    /**
     *  this one is dangerous - only to be used on already cloned nodes!
     */
    abstract void setElement( int levelIndex, Object e);
    
    abstract Node<V> cloneWithReplacedElement( int idx, Object o);
    abstract Node<V> cloneWithAddedElement( int idx, Object o);
    abstract Node<V> cloneWithoutElement( int idx); // caller has to make sure it's there

    abstract Node<V> removeAllSatisfying(PersistentIntMap<V> map, Predicate<V> pred, Result<V> result);
    
    abstract void process (PersistentIntMap<V> map, Processor<V> proc);
    abstract V getNodeValue();
    
    void printIndentOn (PrintStream ps, int level) {
      for (int i=0; i<level; i++) {
        ps.print("    ");
      }
    }
    void printNodeInfoOn (PrintStream ps) {
      String clsName = getClass().getSimpleName();
      int idx = clsName.indexOf('$');
      if (idx > 0) {
        clsName = clsName.substring(idx+1);
      }
      ps.print(clsName);
    }
    abstract void printOn(PrintStream ps, int level);
  }
  
  /**
   * a node that has only one element and hence does not need an array.
   * This can also be the element at index 0, in which case this element
   * has to be a Node, or otherwise the value would be directly stored in the
   * parent elements.
   * If the element changes from a Node into a value, this OneNode gets
   * demoted into a parent value element
   * If a new Element is added, this OneNode gets promoted into a BitmapNode
   */
  protected final static class OneNode<V> extends Node<V> {
    final int idx;
    /*final*/ Object nodeOrValue;  // unfortunately we can't make it final because of setElement
    
    static int nNodes;
    
    OneNode (int idx, Object o){
nNodes++;
      this.idx = idx;
      this.nodeOrValue = o;
    }
            
    protected Object getElement (int levelIndex) {
      if (levelIndex == idx) {
        return nodeOrValue;
      } else {
        return null;
      }
    }
    
    /**
     * danger - only use on already cloned nodes!
     */
    protected void setElement (int levelIndex, Object o) {
      if (levelIndex == idx) {
        nodeOrValue = o;
      } else {
        throw new RuntimeException("trying to set value of non-member element");
      }
    }

    
    protected Node<V> cloneWithReplacedElement( int oidx, Object o){
      // assert oidx == idx
      return new OneNode<V>(idx, o);
    }
    
    protected final Node<V> cloneWithAddedElement(int oIdx, Object o){
      Object[] a = new Object[2];
      
      if (oIdx < idx){
        a[0] = o;
        a[1] = nodeOrValue;
      } else {
        a[0] = nodeOrValue;
        a[1] = o;
      }
      int bitmap = (1 << idx) | (1 << oIdx);
      
      return new BitmapNode<V>(bitmap, a);
    }
    
    protected final Node<V> cloneWithoutElement (int levelIdx){
      return null;
    }
        
    //--- Node interface
        
    V getNodeValue() {
      if (idx == 0) {
        Object o = nodeOrValue;
        if ((o instanceof Node)) {
          return ((Node<V>)o).getNodeValue();
        } else {
          return (V)o;
        }
      } else {
        return null;
      }
    }
    
    V getValue (int idx){
      if (this.idx == idx){
        Object o = nodeOrValue;
        if (o instanceof Node) {
          return ((Node<V>)o).getNodeValue();
        } else {
          return (V)o;
        }
      } else {
        return null;
      }
    }

    Node<V> removeAllSatisfying(PersistentIntMap<V> map, Predicate<V> pred, Result<V> result) {
      Object o = nodeOrValue;
      if (o instanceof Node){
        Node<V> node = (Node<V>)o;
        node = map.removeAllSatisfying(node, pred, result); // give map a chance to redirect
        if (node == null) { // nothing left
          return null;
        } else if (node == o) { // nothing changed
          return this;
        } else {
          return cloneWithReplacedElement(idx, node);
        }
        
      } else { // we had a value
        V v = (V)o;
        if (pred.isTrue(v)){
          result.removedValue( null, v); // bulk op -> no use to keep track of nodeBase 
          return null;
        } else {
          return this;
        }
      }
    }
    
    public void process (PersistentIntMap<V> map, Processor<V> proc){
      if (nodeOrValue instanceof Node){
        map.processNode((Node<V>)nodeOrValue,proc);
      } else {
        proc.process((V)nodeOrValue);
      }
    }
    
    public void printOn (PrintStream ps, int level) {
      printIndentOn(ps, level);
      ps.printf("%2d: ", idx);

      Object o = nodeOrValue;
      if (o instanceof Node) {
        Node<V> n = (Node<V>) o;
        printNodeInfoOn(ps);
        ps.println();
        n.printOn(ps, level + 1);
      } else {
        ps.print("value=");
        ps.println(o);
      }
    }
  }


  /**
   * a node that holds between 2 and 31 elements.
   * We use bitmap based element compaction - the corresponding level part of the key
   * [0..31] is used as an index into the bitmap. The elements are stored in a dense
   * array at indices corresponding to the number of set bitmap bits to the right of the
   * respective index, e.g. for 
   *   key = 289 =  b..01001.00001, shift = 5, node already contains a key 97 =>
   *     idx = (key >>> shift) & 0x1f = b01001 = 9
   *     bitmap =  1000001000 (bit 3 from previous key 97)
   *     element index = 1 (one set bit to the right of bit 9)
   * If the bit count is 2 and an element is removed, this gets demoted into a OneNode.
   * If the bit count is 31 and an element is added, this gets promoted into a FullNode
   */
  protected final static class BitmapNode<V> extends Node<V> {
    
    final int bitmap; // key partition bit positions of non-null child nodes or values    
    final Object[] nodesOrValues; // dense child|value array indexed by bitmap bitcount of pos
    
    static int nNodes;

    BitmapNode (int bitmap, Object[] nodesOrValues){
nNodes++;
      this.bitmap = bitmap;
      this.nodesOrValues = nodesOrValues;
    }
        
    protected Object getElement (int levelIndex) {
      int bit = 1 << levelIndex;
      if ((bitmap & bit) != 0) {
        int idx = Integer.bitCount( bitmap & (bit-1));
        return nodesOrValues[idx];
      } else {
        return null;
      }
    }

    /**
     * danger - only use on already cloned nodes!
     */
    protected void setElement (int levelIndex, Object o) {
      int bit = 1 << levelIndex;
      if ((bitmap & bit) != 0) {
        int idx = Integer.bitCount( bitmap & (bit-1));
        nodesOrValues[idx] = o;
      } else {
        throw new RuntimeException("trying to set value of non-member element");
      }
    }
    
    private final Object[] cloneArrayWithReplaced (int idx, Object o){
      Object[] a = nodesOrValues.clone();
      a[idx] = o;
      return a;
    }
    
    private final Object[] cloneArrayWithAdded (int idx, Object o){
      int n = nodesOrValues.length;
      Object[] a = new Object[n+1];
            
      if (idx > 0){
        System.arraycopy( nodesOrValues, 0, a, 0, idx);
      }
      
      a[idx] = o;
      
      if (n > idx){
        System.arraycopy( nodesOrValues, idx, a, idx+1, (n-idx));
      }
      
      return a;
    }
    
    private final Object[] cloneArrayWithout (int idx){
      int n = nodesOrValues.length;
      Object[] a = new Object[n-1];

      if (idx > 0){
        System.arraycopy( nodesOrValues, 0, a, 0, idx);
      }
      
      n--;
      if (n > idx){
        System.arraycopy( nodesOrValues, idx+1, a, idx, (n-idx));
      }

      return a;
    }

    //--- these are here to support changing the concrete Node type based on population
    
    protected Node<V> cloneWithReplacedElement (int levelIdx, Object o){
      int idx = Integer.bitCount( bitmap & ((1<<levelIdx) -1));
      return new BitmapNode<V>( bitmap, cloneArrayWithReplaced(idx, o));
    }

    protected final Node<V> cloneWithAddedElement (int levelIdx, Object o){
      int bit = 1 << levelIdx;
      int idx = Integer.bitCount( bitmap & (bit -1));
      if (nodesOrValues.length == 31){
        return new FullNode<V>( nodesOrValues, idx, o); 
      } else {
        return new BitmapNode<V>( bitmap | bit, cloneArrayWithAdded(idx, o));
      }
    }
    
    protected final Node<V> cloneWithoutElement (int levelIdx){
      int bit = (1<<levelIdx);
      int idx = Integer.bitCount( bitmap & (bit-1));
      
      if (nodesOrValues.length == 2){
        Object o = (idx == 0) ? nodesOrValues[1] : nodesOrValues[0];
        int i = Integer.numberOfTrailingZeros(bitmap ^ bit);
        return new OneNode<V>( i, o);
      } else {
        return new BitmapNode<V>(bitmap ^ bit, cloneArrayWithout(idx));
      }
    }
        
    //--- the abstract Node methods
    
    public V getNodeValue() {
      if ((bitmap & 1) != 0) {
        Object o = nodesOrValues[0];
        if ((o instanceof Node)) {
          return ((Node<V>)o).getNodeValue();
        } else {
          return (V)o;
        }
      } else {
        return null;
      }
    }
    
    V getValue (int index){
      int bit = 1 << index;
      if ((bitmap & bit) != 0){
        int idx = Integer.bitCount( bitmap & (bit -1));
        Object o = nodesOrValues[idx];
        if (o instanceof Node) {
          return ((Node<V>) o).getNodeValue();
        } else {
          return (V) o;
        }
      } else {
        return null;
      }
    }
        
    public Node<V> removeAllSatisfying (PersistentIntMap<V> map, Predicate<V> pred, Result<V> result){
      Object[] nv = nodesOrValues;
      Object[] a = null; // deferred initialized
      int newBitmap = bitmap;
            
      //--- check which nodesOrValues are affected and update bitmap
      int bit=1;
      for (int i=0; i<nv.length; i++) {
        while ((newBitmap & bit) == 0){
          bit <<= 1;
        }
        
        Object o = nv[i];
        if (o instanceof Node){ // we have a node at this index
          Node<V> node = (Node<V>)o;
          node = map.removeAllSatisfying( node, pred, result);
          if (node != o) {
            if (a == null){
              a = nv.clone();
            }
            a[i] = node;
            if (node == null){
              newBitmap ^= bit;
            }            
          }
          
        } else { // we have a value at this index
          V v = (V) o;
          
          if (pred.isTrue(v)){ // value got removed
            if (a == null){
              a = nv.clone();
            }
            result.removedValue( this, v);
            a[i] = null;
            newBitmap ^= bit;
          }
        }
        bit <<= 1;
      }
      
      //--- now figure out what we have to return
      if (a == null){ // no nodesOrValues got changed
        return this;
        
      } else { // nodesOrValues got changed, we need to compact and update the bitmap
        int newLen= Integer.bitCount(newBitmap);

        if (newLen == 0){ // nothing left of this node
          return null;
          
        } else if (newLen == 1){ // reduce node
          int idx = Integer.bitCount( bitmap & (newBitmap -1));
          Object o=a[idx];
          return new OneNode<V>( idx, o);
          
        } else { // still a BitmapNode
          Object[] newNodesOrValues = new Object[newLen];
          int j = 0;
          for (int i = 0; i < a.length; i++) {
            Object o = a[i];
            if (o != null) {
              newNodesOrValues[j++] = o;
            }
          }
          
          return new BitmapNode<V>( newBitmap, newNodesOrValues);
        }
      }
    }
    
    public void process (PersistentIntMap<V> map, Processor<V> proc){
      for (int i=0; i<nodesOrValues.length; i++){
        Object o = nodesOrValues[i];
        if (o instanceof Node){
          map.processNode((Node<V>)o,proc);
        } else {
          proc.process((V)o);
        }
      }
    }
    
    public void printOn (PrintStream ps, int level) {
      int j=0;
      for (int i=0; i<32; i++) {
        if ((bitmap & (1<<i)) != 0) {
          printIndentOn(ps, level);
          ps.printf("%2d: ", i);
          
          Object o = nodesOrValues[j++];
          if (o instanceof Node) {
            Node<V> n = (Node<V>)o;
            n.printNodeInfoOn(ps);
            ps.println();
            n.printOn(ps, level+1);
          } else {
            ps.print("value=");
            ps.println(o);
          }
        }
      }
    }
  }


  /**
   * a node with 32 elements, for which we don't need a bitmap.
   * No element can be added since this means we just promote an existing element
   * If an element is removed, this FullNode gets demoted int a BitmapNode
   */
  protected final static class FullNode<V> extends Node<V> {
    final Object[] nodesOrValues;
          
    static int nNodes;

    FullNode( Object[] a32){
nNodes++;
      nodesOrValues = a32;
    }
    
    FullNode( Object[] a31, int idx, Object o){      
      Object[] nv = new Object[32];
      
      if (idx > 0){
        System.arraycopy( a31, 0, nv, 0, idx);
      }
      if (idx < 31){
        System.arraycopy( a31, idx, nv, idx+1, 31-idx);
      }
      nv[idx] = o;
      
      nodesOrValues = nv;
    }
    
    protected Object getElement (int levelIndex) {
      return nodesOrValues[levelIndex];
    }
    
    /**
     * danger - only use on already cloned nodes!
     */
    protected void setElement (int levelIndex, Object o) {
      nodesOrValues[levelIndex] = o;
    }

    
    protected Node<V> cloneWithReplacedElement(int idx, Object o){
      Object[] a = nodesOrValues.clone();
      a[idx] = o;
      return new FullNode<V>(a);
    }
    
    protected Node<V> cloneWithAddedElement (int idx, Object o){
      throw new RuntimeException("can't add elements to FullNode");
    }
    
    protected final Node<V> cloneWithoutElement(int idx){
      Object[] a = new Object[31];
      int bitmap = 0xffffffff ^ (1 << idx);
      
      if (idx > 0){
        System.arraycopy(nodesOrValues, 0, a, 0, idx);
      }
      if (idx < 31){
        System.arraycopy(nodesOrValues, idx+1, a, idx, 31-idx);
      }
      
      return new BitmapNode<V>( bitmap, a);
    }
    
    public V getNodeValue() {
      Object o = nodesOrValues[0];
      if ((o instanceof Node)) {
        return ((Node<V>) o).getNodeValue();
      } else {
        return (V) o;
      }
    }
    
    V getValue (int idx){
      Object o = nodesOrValues[idx];
      if (o instanceof Node) {
        return ((Node<V>) o).getNodeValue();
      } else {
        return (V) o;
      }
    }

    public void process (PersistentIntMap<V> map, Processor<V> proc) {      
      for (int i=0; i<32; i++){
        Object o=nodesOrValues[i];
        if (o instanceof Node){
          map.processNode((Node<V>)o,proc);
        } else {
          proc.process( (V)o);
        }
      }
    }
    
    public Node<V> removeAllSatisfying (PersistentIntMap<V> map, Predicate<V> pred, Result<V> result){
      Object[] nv = nodesOrValues;
      Object[] a = null; // deferred initialized
            
      //--- check which nodesOrValues are affected and create bitmap
      int newBitmap = 0;
      int bit = 1;
      for (int i=0; i<nv.length; i++) {
        Object o = nv[i];
        if (o instanceof Node){ // a node
          Node<V> node = (Node<V>)o;
          node = map.removeAllSatisfying( node, pred, result); // give map a chance to redirect
          if (node != o){ // node got removed
            if (a == null){
              a = nv.clone();
            }
            a[i] = node;
          }
          if (node != null){
            newBitmap |= bit;
          }
        } else { // a value
          V v = (V)o;
          if (pred.isTrue(v)){ // value got removed
            if (a == null){
              a = nv.clone();
            }
            result.removedValue( this, v);
            a[i] = null;
          } else {
            newBitmap |= bit;
          }
        }
        bit <<= 1;
      }
      
      //--- now figure out what we have to return
      if (a == null){ // no nodesOrValues got changed
        return this;
        
      } else { // nodesOrValues got changed, we need to compact
        int newLen= Integer.bitCount(newBitmap);

        if (newLen == 0){ // nothing left of this node
          return null;
          
        } else if (newLen == 1){ // reduce node
          // since this was a FullNode, a started at index 0
          int idx = Integer.numberOfTrailingZeros(newBitmap);
          Object o=a[idx];
          return new OneNode<V>( idx, o);
          
        } else { // a BitmapNode
          Object[] newNodesOrValues = new Object[newLen];
          int j = 0;
          for (int i = 0; i < a.length; i++) {
            Object o = a[i];
            if (o != null) {
              newNodesOrValues[j++] = o;
            }
          }
          
          return new BitmapNode<V>( newBitmap, newNodesOrValues);
        }
      }
    }
    
    public void printOn (PrintStream ps, int level) {    
      for (int i=0; i<32; i++) {
        printIndentOn(ps, level);
        ps.printf("%2d: ", i);

        Object o = nodesOrValues[i];
        if (o instanceof Node) {
          Node<V> n = (Node<V>) o;
          printNodeInfoOn(ps);
          ps.println();
          n.printOn(ps, level + 1);
        } else {
          ps.print("value=");
          ps.println(o);
        }
      }
    }
  }

  
  /**
   * object that holds the results of operations performed on a PersistentMap
   */
  public static class Result<V>{
    private static Result cachedResult = new Result();
    
    public static synchronized <V> Result<V> getNewResult(){
      if (cachedResult != null){
        Result<V> r = (Result<V>)cachedResult;
        cachedResult = null;
        return r;
        
      } else {
        return new Result<V>();
      }
    }
    
    /**
     * the number of changed values (>0: added, <0: removed). If 0, no
     * change occurred (set value was already there or remove value wasn't in the map)
     */
    protected int changeCount;
    
    /**
     * internal use only - the node that takes the last added value
     */
    protected Node<V> valueNode;
    protected boolean merged;
    
    protected void finalize(){
      synchronized (Result.class){
        cachedResult = this;
      }
    }

    public void clear(){
      changeCount = 0;
      valueNode = null;
      merged = false;
    }
    
    protected void replacedValue(Node<V> node, V oldValue) {
      valueNode = node;
    }
    
    protected void addedValue(Node<V> node, V newValue) {
      valueNode = node;
      changeCount++;
    }
    
    protected void removedValue(Node<V> node, V oldValue) {
      valueNode = node;
      changeCount--;
    }
        
    public int getSizeChange(){
      return changeCount;
    }
    
    public boolean hasChangedSize(){
      return changeCount != 0;
    }
  }
  
  // in case we don't care
  public static final Result NO_RESULT = new Result();

  public static class RecordingResult<V> extends Result<V>{
    private static RecordingResult cachedResult = new RecordingResult();
    
    public static synchronized <V> Result<V> getNewResult(){
      if (cachedResult != null){
        Result<V> r = (Result<V>)cachedResult;
        cachedResult = null;
        return r;
        
      } else {
        return new Result<V>();
      }
    }
    
    private Object changedValue;

    protected void finalize(){
      synchronized (RecordingResult.class){
        changeCount = 0;
        changedValue = null;
        
        cachedResult = this;
      }
    }

    @Override
    public void clear(){
      super.clear();
      changedValue = null;
    }
        
    @Override
    protected void replacedValue(Node<V> node, V oldValue) {
      super.replacedValue(node, oldValue);
      changedValue = oldValue;
    }
    
    @Override
    protected void addedValue(Node<V> node, V newValue) {
      super.addedValue(node, newValue);
      changedValue = ObjectList.add(changedValue, newValue);
    }
    
    @Override
    protected void removedValue(Node<V> node, V oldValue) {
      super.removedValue(node, oldValue);
      changedValue = ObjectList.add( changedValue, oldValue);
    }
    
    public V getChanged(){
      return (V) ObjectList.getFirst(changedValue);
    }
    
    public Iterator<V> getAllChanged (){
      return (Iterator<V>)ObjectList.iterator(changedValue);
    }    
  }
  
  //--- the instance fields
  final protected int size;
  final protected Node<V> root;

  
  //--- instance methods 
  
  protected PersistentIntMap() {
    size = 0;
    root = null;
  }
  
  protected PersistentIntMap (int size, Node<V> root){
    this.size = size;
    this.root = root;
  }

  public int size(){
    return size;
  }
  
  public boolean isEmpty(){
    return size==0;
  }
  
  //--- abstract methods
  public abstract V get (int key);
  
  public abstract PersistentIntMap<V> set (int key, V value);
  public abstract PersistentIntMap<V> set (int key, V value, Result<V> result);  
  
  public abstract PersistentIntMap<V> remove (int key);
  public abstract PersistentIntMap<V> remove (int key, Result<V> result);

  protected abstract Node<V> removeAllSatisfying (Node<V> node, Predicate<V> predicate, Result<V> result); // node redirection
  public abstract PersistentIntMap<V> removeAllSatisfying (Predicate<V> predicate);
  public abstract PersistentIntMap<V> removeAllSatisfying (Predicate<V> predicate, Result<V> result);

  protected abstract void processNode (Node<V> node, Processor<V> processor); // node redirection
  public abstract void process (Processor<V> processor);
  public abstract void processInKeyOrder (Processor<V> processor);

  public abstract void printOn (PrintStream ps);
}
