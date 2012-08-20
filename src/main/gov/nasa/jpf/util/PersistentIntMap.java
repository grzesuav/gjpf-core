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
 * Persistent (immutable) associative array that maps integer keys to generic objects.
 * This is an abstract base for a number of concrete subclasses that are all implemented
 * as bitwise search tries.
 * <p>
 * The 32bit key values are broken up into 5bit blocks that represent the trie
 * levels, with each 5bit block (0..31) being the index for the respective child level node or value.
 * The concrete subclasses differ in terms of how the blocks are processed: left-to-right
 * (Msb - most significant bit first), or right-to-left (Lsb - least significant bit first).
 * For instance, PersistentMsbIntMap stores value 'x' for key 12345
 * 
 * <blockquote><pre>
 *   level:     1    2     3     4     5     6     7
 *              00.00000.00000.00000.01100.00001.11001  = 12345
 *   block-val:  0     0     0     0    12     1    25
 * 
 *       Node0
 *         ... 
 *         12 -> Node1
 *                 ...
 *                 1 -> Node2
 *                        ...
 *                        25 -> 'x'
 *</pre></blockquote>
 * <p>
 * The internal trie representation uses a protected Node type, which uses the bit block values (0..31)
 * as index into an array that stores either child node references (in case this is not a
 * terminal block), or value objects (if this is the terminal block). There are three Node
 * subtypes that get promoted upon population in the following order:
 * 
 * <ul>
 *  <li>OneNode - store only a single value/child element. Every node starts as a OneNode
 *  <li>BitmapNode - stores up to 31 elements (compressed)
 *  <li>FullNode - stores 32 elements
 * </ul>
 * 
 * It is essential that all Node instances remain internal to guarantee invariant data.
 * <p>
 * Apart from the Node hierarchy, PersistentIntMaps differ from other tries by using
 * heterogenous containers to store child nodes and values. Values that are associated
 * with keys that have one or more trailing zero blocks (such as 0x18000) are called
 * 'NodeValues', and are directly stored as elements of the node that corresponds
 * to the lowest set bit block, unless this element already holds a reference to a child
 * node. In this case NodeValues are stored in the first 0-indexed child node that does not
 * have a node as its 0-indexed element.     
 * 
 * The five major public operations for PersistentIntMaps are
 * 
 * <ol>
 *  <li>set(int key, V value) -> PersistentIntMap : return a new map with an additional value 
 *  <li>get(int key) -> V : retrieve value
 *  <li>remove(int key) -> PersistentIntMap : return a new map without the specified key/value
 *  <li>removeAllSatisfying(Predicate<V> predicate) -> PersistentIntMap : return a new map
 *                             without all values satisfying the specified predicate
 *  <li>process(Processor<V> processor) : iterate over all values with specified processor
 * </ol>
 *  
 * Being a persistent data structure, the main property of PersistentIntMaps is that all
 * add/remove operations (set,remove,removeAllSatisfying) have to return new PersistenIntMap
 * instances, no destructive update is allowed. No PersistentIntMap instance can ever change
 * its keys or value identities. Normal usage patterns therefore looks like this:
 * 
 * <blockquote><pre>
 *   PersistentIntMap<String> map = PersistentMsbIntMap<String>();
 *   ..
 *   map = map.set(42, "fortytwo"); // returns a new map
 *   ..
 *   map = map.remove(42); // returns a new map
 *   ..
 *   map = map.removeAllSatisfying( new Predicate<String>(){ // returns a new map
 *     public boolean isTrue (String val){ 
 *       return val.endsWith("two");
 *     });
 *     
 *   map.process( new Processor<String>(){
 *     public void process (String val){
 *       System.out.println(val);
 *     });
 * </pre></blockquote>
 * 
 * <p>
 * If clients need to obtain more detailed information about add/remove operations (added/removed
 * values), they have to provide a Result<V> object that is created by the respective map instance.
 *  
 * Result objects also double up as containers for variant (operation dependent) state. The
 * general principle is that map state is invariant, and all temporary state that is required
 * during add/remove operations is contained in Result objects. This resembles the
 * intrinsic/external state separation of the Flyweight design pattern (but for different
 * reasons). To ensure invariance of map data, it is imperative that Result objects only
 * expose change counts and values to clients, but do not leak internal trie information such
 * as nodes.
 * 
 * <p>
 * The main implementation challenge is to minimize the number of nodes that have to be
 * cloned when adding/removing values. In general, the new map has to replace all nodes from
 * the one that holds the value up to the root node in order to separate data that is
 * shared with the old map from modified data of the new map. The required path copy upon
 * modification means that the trie height should be kept at a minimum, which comes down to
 * ignoring heading zero blocks and to roll up trailing zero blocks by directly storing
 * values in parent nodes. The latter one requires recursive propagation of such values into
 * NodeValues of child nodes in case there is a conflict between a value and a child node.
 * This gets further complicated by promotion/demotion of node types due to population.
 * 
 * <i>Comment: While add operations of concrete PersistentMap classes are considered
 * to be reasonably complete with respect to trie height minimization, the same is not yet
 * true for remove operations</i>
 *  
 */
public abstract class PersistentIntMap<V> {
  
  /**
   * Abstract root class for all node types. This type needs to be internal, no instances
   * are allowed to be visible outside the PersistentIntMap class hierarchy in order to guarantee
   * invariant data.
   */
  protected abstract static class Node<V> implements Cloneable {
    
    //--- element setters/getters used by PersistentStagingMsbIntMap
    /**
     * obtain Node or value for the provided bit block value
     * @param idx element index (bit block value of range [0..31])
     * @return Node or value stored under levelIndex, or null if not present
     * @see gov.nasa.jpf.util.PersistentStagingMsbIntMap
     */
    protected abstract Object getElement( int idx);
    
    /**
     * store a new node or value for the provided index
     *  THIS IS DANGEROUS - only to be used on already cloned nodes (by PersistentStagingMsbIntMap)
     * @param idx element index (bit block value of range [0..31])
     * @param e (non-null) node or value to set
     * @see gov.nasa.jpf.util.PersistentStagingMsbIntMap
     */
    protected abstract void setElement( int idx, Object e);
    
    //--- node promotion/demotion
    /**
     * create new Node of same type with replaced element
     * @param idx element index (bit block value of range [0..31])
     * @param e (non-null) node or value to set
     * @return new Node (non-null)
     */
    protected abstract Node<V> cloneWithReplacedElement( int idx, Object e);
    
    /**
     * create new Node with additional element. Node type can change based on population.
     * @param idx index for new element (bit block value of range [0..31])
     * @param e (non-null) node or value to add
     * @return new Node (non-null)
     */    
    protected abstract Node<V> cloneWithAddedElement( int idx, Object e);
    
    /**
     * create new Node without specified element. Node type can change based on population.
     * @param idx index of element to remove (bit block value of range [0..31])
     * @return new Node or null if no element left
     */    
    protected abstract Node<V> cloneWithoutElement( int idx); // caller has to make sure it's there

    /**
     * create new Node without all values that satisfy provided Predicate. This needs the containing
     * map as an explicit argument to allow specialized PersistentIntMaps to filter/forward cached nodes 
     * 
     * @param map PersistentIntMap containing this node
     * @param pred Predicate that identifies values to remove (if it evaluates to true for a given value)
     * @param result Result object with updated changeCount upon return
     * @return new Node or null if no element left
     * 
     * @see renmoveAllSatisfying(Node<V>, Predicate<V> pred, Result<V> result)
     */
    protected abstract Node<V> removeAllSatisfying(PersistentIntMap<V> map, Predicate<V> pred, Result<V> result);
    
    
    protected abstract void process (PersistentIntMap<V> map, Processor<V> proc);
    
    protected abstract V getNodeValue();
    
    //--- debug support (printing nodes)
    
    protected void printIndentOn (PrintStream ps, int level) {
      for (int i=0; i<level; i++) {
        ps.print("    ");
      }
    }
    
    protected void printNodeInfoOn (PrintStream ps) {
      String clsName = getClass().getSimpleName();
      int idx = clsName.indexOf('$');
      if (idx > 0) {
        clsName = clsName.substring(idx+1);
      }
      ps.print(clsName);
    }
    
    protected abstract void printOn(PrintStream ps, int level);
  }
  
  /**
   * Node that has only one element and hence does not need an array.
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
      nNodes++; // <2do> just for statistics, to be disabled in production code
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
        
    protected V getNodeValue() {
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

    protected Node<V> removeAllSatisfying(PersistentIntMap<V> map, Predicate<V> pred, Result<V> result) {
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
   * A node that holds between 2 and 31 elements.
   * 
   * We use bitmap based element array compaction - the corresponding bit block of the key
   * [0..31] is used as an index into a bitmap. The elements are stored in a dense
   * array at indices corresponding to the number of set bitmap bits to the right of the
   * respective index in the bitmap, e.g. for
   * 
   * <blockquote><pre> 
   *   key = 289 =  b...01001.00001, shift = 5, node already contains a key 97 =>
   *     idx = (key >>> shift) & 0x1f = b01001 = 9
   *     bitmap =  1000001000 (bit 3 from key 97)
   *     element index = 1 (one set bit to the right of bit 9)
   * </pre></blockquote>
   * <p>
   * If the bit count of a BitmapNode is 2 and an element is removed, this gets demoted into a OneNode.
   * If the bit count of a BitmapNode is 31 and an element is added, this gets promoted into a FullNode
   */
  protected final static class BitmapNode<V> extends Node<V> {
    
    final int bitmap; // key partition bit positions of non-null child nodes or values    
    final Object[] nodesOrValues; // dense child|value array indexed by bitmap bitcount of pos
    
    static int nNodes;

    BitmapNode (int bitmap, Object[] nodesOrValues){
      nNodes++; // <2do> just for statistics, to be disabled in production code
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
      nNodes++; // <2do> just for statistics, to be disabled in production code
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
    
    /**
     * the number of changed values (>0: added, <0: removed). If 0, no
     * change occurred (set value was already there or remove value wasn't in the map)
     */
    protected int changeCount;
    
    /**
     * internal use only - the node that takes the last added value
     * <2do> these should be in PersistentStagingMsbIntMap, not here
     */
    protected Node<V> valueNode;
    protected int valueNodeLevel;
    protected boolean merged;
    
    public void clear(){
      changeCount = 0;
      valueNode = null;
      valueNodeLevel = -1;
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


  public static class RecordingResult<V> extends Result<V>{
    
    private Object changedValue;

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
  
  //--- static utilities
  
  /**
  static int getMsbShift (int key) {
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
  
  /**
   * get the shift count for the highest bit index (bit block). This is essentially counting the number of leading zero bits,
   * which we can derive from http://graphics.stanford.edu/~seander/bithacks.html#IntegerLogLookup
   */
  static int getMsbShift (int v){
    v |= v >>> 1;
    v |= v >>> 2;
    v |= v >>> 4;
    v |= v >>> 8;
    v |= v >>> 16;

    return LeadingMultiplyDeBruijnBitPosition[(v * 0x07C4ACDD) >>> 27];
  }
  
  /**
  static int getLsbShift (int key) {
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
  static int getLsbShift (int v){
    return Mod37Shifts[ (-v & v) % 37];
  }
  **/

  static final int TrailingMultiplyDeBruijnBitPosition[] = {
      0, 0, 25, 0, 25, 10, 20, 0, 30, 20, 20, 15, 25, 15, 0, 5, 
      30, 25, 10, 20, 20, 15, 15, 5, 25, 10, 15, 5, 10, 5, 10, 5
  };
  
  /**
   * get the shift count for the lowest bit index (bit block). This is essentially counting the number of trailing
   * zero bits, which we can derive from http://graphics.stanford.edu/~seander/bithacks.html#ZerosOnRightMultLookup
   */
  static int getLsbShift (int v) {
    return TrailingMultiplyDeBruijnBitPosition[(((v & -v) * 0x077CB531)) >>> 27];
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
  
  protected Node<V> assocNodeValue (Node<V> node, V value, Result<V> result){
    result.valueNodeLevel++;
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
  
  protected Node<V> removeNodeValue (Node<V> node, Result<V> result){
    result.valueNodeLevel++;
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
  
  //--- abstract methods
  public abstract V get (int key);
  
  public PersistentIntMap<V> set (int key, V value) {
    return set( key, value, createResult());
  }
  public abstract PersistentIntMap<V> set (int key, V value, Result<V> result);  
  
  public PersistentIntMap<V> remove (int key){
    return remove( key, createResult());
  }
  public abstract PersistentIntMap<V> remove (int key, Result<V> result);

  protected abstract Node<V> removeAllSatisfying (Node<V> node, Predicate<V> predicate, Result<V> result); // node redirection
  public abstract PersistentIntMap<V> removeAllSatisfying (Predicate<V> predicate, Result<V> result);
  
  public PersistentIntMap<V> removeAllSatisfying (Predicate<V> predicate) {
    return removeAllSatisfying( predicate, createResult());
  }

  
  protected void processNode (Node<V> node, Processor<V> processor) {
    node.process(this, processor);
  }
  
  public void process (Processor<V> processor){
    if (root != null){
      root.process(this, processor);
    }
  }
    
  /**
   * Obtain a Result object that allows inspection of size changes of the map, but not changed values.
   * Can be overridden to allow concrete PersistentIntMap classes to use specialized Result objects with varying
   * internal information but the same public interface
   * @return new Result<V> object
   */
  public Result<V> createResult(){
    return new Result<V>();
  }
  
  /**
   * Obtain a Result object that allows inspection of size and value changes of the map.
   * Can be overridden to allow concrete PersistentIntMap classes to use specialized Result objects with varying
   * internal information but the same public interface
   * @return new Result<V> object
   */
  public Result<V> createRecordingResult(){
    return new RecordingResult<V>();
  }
  
  public void printOn (PrintStream ps) {
    if (root != null) {
      root.printNodeInfoOn(ps);
      ps.println();
      root.printOn(ps, 1);
    }
  }
}
