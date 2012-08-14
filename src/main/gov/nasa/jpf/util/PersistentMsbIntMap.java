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

/**
 * an immutable Vector that is implemented as a bitwise trie with 32-element nodes. Trie levels are
 * ordered in most significant bit order on top, so that consecutive key values are stored in the
 * same (terminal) nodes.
 */
public class PersistentMsbIntMap<V> extends PersistentIntMap<V> {
  
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

  //--- static ImmutableObjectTable methods
  
  static <V> Node<V> createNode (int shift, int finalShift, int key, V value, V nodeValue, Result<V> result){
    int idx = ((key>>>shift) & 0x01f);
    Node<V> node;
    
    if (shift == finalShift) {
      if (nodeValue != null) {
        Object[] nodesOrValues = new Object[2];
        nodesOrValues[0] = nodeValue;
        nodesOrValues[1] = value;
        int bitmap = (1 << idx) | 1;
        node = new BitmapNode<V>(bitmap, nodesOrValues);        
      } else {
        node = new OneNode<V>( idx, value);
      }
      result.addedValue(node, value);
      
    } else {
assert (shift-5 >= finalShift) : "shift=" + shift + ", fsh= " + finalShift + "key=" + key;
      node = createNode( shift-5, finalShift, key, value, nodeValue, result);
      node = new OneNode<V>(idx, node);
    }
    
    return node;
  }

  static <V> Node<V> createAndMergeNode (int shift, int finalShift, int key, V value, Node<V> mergeNode, Result<V> result){
    int k = (key >>> shift);
    int idx = (k & 0x01f);
    
    int bitmap = (1 << idx) | 1;
    Object[] nodesOrValues = new Object[2];
    nodesOrValues[0] = mergeNode;

    Node<V> node;
    if (shift == finalShift) {
      nodesOrValues[1] = value;
      node = new BitmapNode<V>(bitmap, nodesOrValues);
      result.addedValue( node, value);  
    } else {
      nodesOrValues[1] = createNode( shift-5, finalShift, key, value, null, result);
      node = new BitmapNode<V>(bitmap, nodesOrValues);
    }
    
    return node;
  }
  
  static <V> Node<V> propagateMergeNode (int shift, int nodeShift, Node<V> node){
    shift -= 5;
    while (shift > nodeShift){
      node = new OneNode<V>(0, node);
      shift -= 5;
    }
    
    return node;
  }
  
  /**
  static int getInitialShift (int key) {
    if ((key & 0xc0000000) != 0) return 30;
    if ((key & 0x3e000000) != 0) return 25;
    if ((key & 0x1f00000) != 0)  return 20;
    if ((key & 0xf8000) != 0)    return 15;
    if ((key & 0x7c00) != 0)     return 10;
    if ((key & 0x3e0) != 0)      return 5;
    return 0;
  }
  **/
  
  static final int LeadingMultiplyDeBruijnBitPosition[] = {
    0, 5, 0, 10, 10, 20, 0, 25, 10, 10, 15, 15, 20, 25, 0, 30,
    5, 10, 20, 25, 15, 15, 20, 5, 15, 25, 20, 5, 25, 5, 0, 30
  };
  static int getInitialShift (int v){
    v |= v >>> 1;
    v |= v >>> 2;
    v |= v >>> 4;
    v |= v >>> 8;
    v |= v >>> 16;

    return LeadingMultiplyDeBruijnBitPosition[(v * 0x07C4ACDD) >>> 27];
  }

  
  
  /**
  static int getFinalShift (int key) {
    if (key == 0 || (key & 0x1f) != 0)       return 0;
    if ((key & 0x3e0) != 0)      return 5;
    if ((key & 0x7c00) != 0)     return 10;
    if ((key & 0xf8000) != 0)    return 15;
    if ((key & 0x1f00000) != 0)  return 20;
    if ((key & 0x3e000000) != 0) return 25;
    return 30;
  }
  **/
  
  /**
  static final int Mod37Shifts[] = {
    0, 0, 0, 25, 0, 20, 25, 0, 0, 15, 20, 30, 25, 10, 0, 10, 0,
    5, 15, 0,  25,  20, 30, 15, 25, 10,  10, 5, 0, 20, 10, 5, 5,
    20, 5, 15, 15
  };
  static int getFinalShift (int v){
    return Mod37Shifts[ (-v & v) % 37];
  }
  **/

  static final int TrailingMultiplyDeBruijnBitPosition[] = {
      0, 0, 25, 0, 25, 10, 20, 0, 30, 20, 20, 15, 25, 15, 0, 5, 
      30, 25, 10, 20, 20, 15, 15, 5, 25, 10, 15, 5, 10, 5, 10, 5
  };
  static int getFinalShift (int v) {
    return TrailingMultiplyDeBruijnBitPosition[(((v & -v) * 0x077CB531)) >>> 27];
  }
  
  
  //--- invariant instance data
  final protected int size;
  final protected Node<V> root;
  
  // for msb first mode, we keep track of the initial shift to save cloning empty top nodes
  final protected int rootShift;
  
  
  public PersistentMsbIntMap(){
    size = 0;
    root = null;
    rootShift = 0;
  }

  protected PersistentMsbIntMap (int size, Node<V> root, int initialShift){
    this.size = size;
    this.root = root;
    this.rootShift = initialShift;
  }
  
  //--- set() related methods
  protected Node<V> assocNodeValue (Node<V> node, V value, Result<V> result){
    Object o = node.getElement(0);

    if (o != null){                        // we got something for index 0
      if (o instanceof Node){           // we've got a node
        Node<V> newNodeElement = assocNodeValue( (Node<V>)o, value, result);
        if (newNodeElement == o){
          return node;
        } else {
          return node.cloneWithReplacedElement( 0, newNodeElement);
        }

      } else {                             // we've got a value
        if (o == value){
          return node;
        } else {
          node = node.cloneWithReplacedElement( 0, value);
          result.replacedValue( node, (V)o);
          return node;
        }
      }

    } else {                               // we didn't have anything for this index
      node = node.cloneWithAddedElement( 0, value);
      result.addedValue( node, value);
      return node;
    }
  }
  
  protected Node<V> assoc (int shift, int finalShift, int key, V value, Node<V> node, Result<V> result){
    int k = key >>> shift;
    int levelIdx = k & 0x1f;
    Object o = node.getElement(levelIdx);

    if (o != null){                       // we already have a node or value for this index

      if (o instanceof Node){             // we had a node for this index
        Node<V> replacedNode;
        if (shift == finalShift){         // at value level
          replacedNode = assocNodeValue( node, value, result);
        } else {                          // not at value level yet, recurse
          replacedNode = assoc( shift-5, finalShift, key, value, (Node<V>)o, result);
        }
        if (replacedNode == o){           // nothing changed
          return node;
        } else {                          // element got replaced, clone the node
          return node.cloneWithReplacedElement( levelIdx, replacedNode);
        }        

      } else { // we had a value for this index
        if (shift == finalShift){         // at value level
          if (value == o){                // nothing changed
            return node;
          } else {                        // clone with updated value
            node = node.cloneWithReplacedElement( levelIdx, value);
            result.replacedValue( node, (V)o);
            return node;
          }

        } else {                          // not at value level yet
          Node<V> addedNode = createNode( shift-5, finalShift, key, value, (V)o, result);
          return node.cloneWithReplacedElement( levelIdx, addedNode);
        }
      }

    } else { // nothing for this index yet
      if (shift == finalShift){           // at value level
        node = node.cloneWithAddedElement( levelIdx, value);
        result.addedValue( node, value);

      } else {                            // not at value level yet
        Node<V> addedNode = createNode( shift-5, finalShift, key, value, null, result);
        node = node.cloneWithAddedElement( levelIdx, addedNode);
      }
      return node;
    }
  }
  
  public PersistentMsbIntMap<V> set (int key, V value, Result<V> result){
    int ish = getInitialShift(key);
    int fsh = getFinalShift(key);
    result.clear();
    
    if (root == null){
      Node<V> newRoot = createNode( ish, fsh, key, value, null, result);
      return new PersistentMsbIntMap<V>( 1, newRoot, ish);

    } else {            
      Node<V> newRoot;
      int newRootShift;
      
      if (ish <= rootShift) { // new key can be added to current root (key smaller than previous ones)
        newRoot = assoc( rootShift, fsh, key, value, root, result);
        newRootShift = rootShift;

      } else { // current root has to be merged into new one
        Node<V> mergeNode = propagateMergeNode( ish, rootShift, root);
        newRoot = createAndMergeNode( ish, fsh, key, value, mergeNode, result);
        newRootShift = ish;
      }
      
      if (root == newRoot){ // key and value were already there
        return this;
      } else { // could have been a replaced value that didn't change the size
        int newSize = size + result.getSizeChange();
        return new PersistentMsbIntMap<V>( newSize, newRoot, newRootShift);
      }
    }
  }

  public PersistentMsbIntMap<V> set (int key, V value){
    Result<V> result = Result.getNewResult();
    return set( key, value, result);
  }  

  /**
   * retrieve value for specified key
   * @return null if this key is not in the map
   */
  public V get (int key){
    if (root != null){
      int shift = rootShift;
      int finalShift = getFinalShift(key);      
      Node<V> node = root;

      for (;;){
        int levelIdx = (key>>>shift) & 0x1f;
        Object o = node.getElement(levelIdx);
        if (o != null){                      // do we have something for this index
          if (o instanceof Node){            // we have a node
            node = (Node<V>)o;
            if (shift == finalShift){        // at value level
              return node.getNodeValue();
            } else {                         // shift to next level (tail recursion)
              shift -= 5;
              continue;
            }
            
          } else {                           // we have a value
            if (shift == finalShift){        // at value level
              return (V)o;
            } else {
              return null;                   // can't go down, so it's not there
            }
          }
          
        } else {                             // nothing for this index
          return null;
        }
      }
      
    } else {                                 // no root
      return null;
    }
  }

  //--- remove() related methods
  
  protected Node<V> removeNodeValue (Node<V> node, Result<V> result){
    Object o = node.getElement(0);

    if (o != null){                        // we got something for index 0
      if (o instanceof Node){              // we've got a node
        Node<V> newNodeElement = removeNodeValue((Node<V>)o, result);
        if (newNodeElement == null){
          return node.cloneWithoutElement( 0);
        } else if (newNodeElement == o){
          return node;
        } else {
          return node.cloneWithReplacedElement( 0, newNodeElement);
        }

      } else {                             // we've got a value
        node = node.cloneWithoutElement( 0);
        result.removedValue( node, (V)o);
        return node;
      }

    } else {                               // we didn't have anything for this index
      return node;
    }
  }
  
  protected Node<V> remove (int shift, int finalShift, int key, Node<V> node, Result<V> result){
    int k = (key >>> shift);
    int levelIdx = k & 0x1f;
    Object o = node.getElement(levelIdx);

    if (o != null){                        // we got something for this index
      if (o instanceof Node){              // we've got a node
        Node<V> newNodeElement;
        if (shift == finalShift){          // at value level
          newNodeElement = removeNodeValue( (Node<V>)o, result);
        } else {                           // not yet at value level, recurse downwards
          newNodeElement = remove(shift-5, finalShift, key, (Node<V>)o, result);
        }
        if (newNodeElement == null){       // nothing left
          return node.cloneWithoutElement( levelIdx);
        } else if (newNodeElement == o){   // nothing changed
          return node;
        } else {
          return node.cloneWithReplacedElement( levelIdx, newNodeElement);
        }

      } else {                             // we've got a value
        if (shift == finalShift){          // at value level
          node = node.cloneWithoutElement( levelIdx);
          result.removedValue( node, (V)o);
          return node;
        } else {                           // not at value level, key isn't in the map
          return node;
        }
      }
    } else {                               // we didn't have anything for this index
      return node;
    }
  }
   
  /**
   * the public remove() method
   */
  public PersistentMsbIntMap<V> remove(int key, Result<V> result){
    if (root == null){
      return this;
      
    } else {
      result.clear();
      
      int fsh = getFinalShift(key);
      //Node<V> newRoot = root.remove( rootShift, fsh, key, result);
      Node<V> newRoot = remove( rootShift, fsh, key, root, result);

      if (root == newRoot){ // nothing removed
        return this;
      } else {
        // <2do> we should check if we can increase the initialShift
        return new PersistentMsbIntMap<V>( size-1, newRoot, rootShift);
      }
    }
  }

  public PersistentMsbIntMap<V> remove(int key){
    Result<V> result = Result.getNewResult();
    return remove( key, result);
  } 
  
  //--- bulk remove
  
  protected Node<V> removeAllSatisfying (Node<V> node, Predicate<V> predicate, Result<V> result) {
    return node.removeAllSatisfying( this, predicate, result);
  }
  
  public PersistentMsbIntMap<V> removeAllSatisfying (Predicate<V> predicate){
    Result<V> result = Result.getNewResult();
    return removeAllSatisfying(predicate, result);
  }
    
  public PersistentMsbIntMap<V> removeAllSatisfying (Predicate<V> predicate, Result<V> result){
    if (root != null){
      result.clear();
      Node<V> newRoot = (Node<V>) root.removeAllSatisfying( this, predicate, result);
      
      // <2do> we should check if we can increase the initialShift

      return new PersistentMsbIntMap<V>( size+result.getSizeChange(), newRoot, rootShift);
      
    } else {
      return this;
    }
  }
  
  
  //--- statistics
  
  public int size(){
    return size;
  }
  
  public boolean isEmpty(){
    return size==0;
  }
  
  //--- iterator-less generic value processing
  
  protected void processNode (Node<V> node, Processor<V> processor) {
    node.process(this, processor);
  }
  
  public void process (Processor<V> processor){
    if (root != null){
      root.process(this, processor);
    }
  }
  
  public void processInKeyOrder (Processor<V> processor){
    process( processor);
  }
  
  
  //--- debugging
  
  public void printOn (PrintStream ps) {
    if (root != null) {
      root.printNodeInfoOn(ps);
      ps.println();
      root.printOn(ps, 1);
    }
  }
}
