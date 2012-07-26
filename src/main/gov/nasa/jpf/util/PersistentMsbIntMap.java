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
public class PersistentMsbIntMap<V> implements PersistentIntMap<V> {

  static final int SHIFT_INC = -5; // msb first
  
  /**
   * the abstract root class for all node types
   */
  protected abstract static class Node<V> implements Cloneable {
    
    abstract Node<V> assocNodeValue( V value, Result<V> result);
    abstract Node<V> removeNodeValue (Result<V> result);
    
    abstract Node<V> assoc (int shift, int finalShift, int key, V value, Result<V> result);
    abstract V find (int shift, int finalShift, int key);
    abstract V getValue (int idx);
    abstract Node<V> setValue (int idx, V value, Result<V> result);
    abstract Node<V> remove (int shift, int finalShift, int key, Result<V> result);
    abstract Node<V> removeAllSatisfying(Predicate<V> pred, Result<V> result);
    abstract void process (Processor<V> proc);
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
  protected static class OneNode<V> extends Node<V> {
    final int idx;
    final Object nodeOrValue;
    
    OneNode (int idx, Object o){
      this.idx = idx;
      this.nodeOrValue = o;
    }
        
    private final Node<V> cloneWithReplacedNodeOrValue( Object o){
      return new OneNode<V>(idx, o);
    }
    
    private final Node<V> cloneWithAddedNodeOrValue(int oIdx, Object o){
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
    
    Node<V> setValue (int idx, V value, Result<V> result) {
      if (this.idx == idx) {
        Object o = nodeOrValue;
        if (o instanceof Node) {
          Node<V> node = (Node<V>) o;
          node = node.assocNodeValue(value, result);
          if (node != o) {
            return new OneNode<V>(idx, node);
          } else {
            return this;
          }
        } else { // replace value
          V v = (V)o;
          if (v == value) {
            return this;
          } else {
            result.replacedValue(v);
            return new OneNode<V>(idx, value);
          }
        }
      } else {
        result.addedValue(value);
        return cloneWithAddedNodeOrValue( idx, value);
      }
    }
    
    public Node<V> assocNodeValue (V value, Result<V> result){
      if (idx == 0) {
        Object o = nodeOrValue;
        if (o instanceof Node) {
          Node<V> node = (Node<V>)o;
          node = node.assocNodeValue(value, result);
          if (node == o) {
            return this;
          } else {
            return cloneWithReplacedNodeOrValue(node);
          }
          
        } else {
          if (o == value) {
            return this;
          } else {
            result.replacedValue((V)o);
            return cloneWithReplacedNodeOrValue(value);
          }
        }
        
      } else { // we don't have anything for index 0
        result.addedValue(value);
        return cloneWithAddedNodeOrValue(0, value);
      }
    }
    
    public Node<V> assoc (int shift, int finalShift, int key, V value, Result<V> result){
      int idx = (key>>>shift) & 0x1f;
      
      if (idx == this.idx) { // do we already have something for this index?
        Object o = nodeOrValue;
        if (o instanceof Node) { // we already have a node for this index
          Node<V> node = (Node<V>) o;
          node = (shift == finalShift) ? node.assocNodeValue(value, result) :
                              node.assoc(shift+SHIFT_INC, finalShift, key, value, result);
          if (node == o) { // no change
            return this;
          } else {
            return cloneWithReplacedNodeOrValue(node);
          }
        } else { // we had a value
          if (shift == finalShift) {
            if (o == value) { // no change
              return this;
            } else {
              result.replacedValue((V)o);
              return cloneWithReplacedNodeOrValue(value);
            }
          } else { // not at value level yet
            result.addedValue(value);
            Node<V> node = createNode(shift+SHIFT_INC, finalShift, key, value, (V)o);
            return cloneWithReplacedNodeOrValue(node);
          }
        }
        
      } else { // nothing for this index yet
        Object o = (shift == finalShift) ? value : 
                             createNode(shift+SHIFT_INC, finalShift, key, value, null);
        result.addedValue(value);
        return cloneWithAddedNodeOrValue(idx, o);
      }
    }
        
    public V find (int shift, int finalShift, int key){
      int idx = ((key>>>shift) & 0x01f);

      if (idx == this.idx) { // we  have a node or value for this index
        Object o = nodeOrValue;
        
        if (o instanceof Node){ // recurse down
          Node<V> node = (Node<V>)o;
          
          if (shift == finalShift){ // at value level
            return node.getNodeValue();
          } else {
            return node.find( shift + SHIFT_INC, finalShift, key);
          }
          
        } else { // we have a value for this index
          if (shift == finalShift){
            return (V)o;
          } else {
            return null;
          }
        }
        
      } else {
        return null;
      }
    }
    
    public Node<V> removeNodeValue (Result<V> result){
      if (idx == 0) {
        Object o = nodeOrValue;
        if (o instanceof Node) {
          Node<V> node = (Node<V>)o;
          node = node.removeNodeValue(result);
          if (node == null) {
            return null;
          } else if (node == o) {
            return this;
          } else {
            return cloneWithReplacedNodeOrValue(node);
          }
          
        } else { // we had a value
          result.removedValue((V)o);
          return null;
        }
      } else {
        return this;
      }
    }
    
    public Node<V> remove (int shift, int finalShift, int key, Result<V> result){
      int idx = ((key>>>shift) & 0x01f);
      
      if (idx == this.idx) { // we have a node or value for this index
        Object o = nodeOrValue;
        
        if (o instanceof Node){
          Node<V> node = (Node<V>)o;
          node = (shift == finalShift) ?  node.removeNodeValue(result) :
                             node.remove(shift +SHIFT_INC, finalShift, key, result);
          if (node == null) { // nothing left
            return null;
          } else if (node == o) { // nothing changed
            return this;
          } else {
            return cloneWithReplacedNodeOrValue(node);
          }            
          
        } else { // we have a value for this index
          if (shift == finalShift){
            result.removedValue((V)o);
            return null; // no child left
          } else {
            return this; // the key isn't in this map
          }
        }
        
      } else { // nothing stored for this key
        return this;
      }
    }

    Node<V> removeAllSatisfying(Predicate<V> pred, Result<V> result) {
      Object o = nodeOrValue;
      if (o instanceof Node){
        Node<V> node = (Node<V>)o;
        node = node.removeAllSatisfying(pred, result);
        if (node == null) { // nothing left
          return null;
        } else if (node == o) { // nothing changed
          return this;
        } else {
          return cloneWithReplacedNodeOrValue(node);
        }
        
      } else { // we had a value
        V v = (V)o;
        if (pred.isTrue(v)){
          result.removedValue(v);
          return null;
        } else {
          return this;
        }
      }
    }
    
    public void process (Processor<V> proc){
      if (nodeOrValue instanceof Node){
        ((Node<V>)nodeOrValue).process(proc);
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
    
    BitmapNode (int bitmap, Object[] nodesOrValues){
      this.bitmap = bitmap;
      this.nodesOrValues = nodesOrValues;
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
    
    private final Node<V> cloneWithReplacedNodeOrValue (int idx, Object o){
      return new BitmapNode<V>( bitmap, cloneArrayWithReplaced(idx, o));
    }

    private final Node<V> cloneWithAddedNodeOrValue (int bit, int idx, Object o){
      if (nodesOrValues.length == 31){
        return new FullNode<V>( nodesOrValues, idx, o); 
      } else {
        return new BitmapNode<V>( bitmap | bit, cloneArrayWithAdded(idx, o));
      }
    }
    
    private final Node<V> cloneWithoutNodeOrValue (int bit, int idx){
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
    
    public Node<V> assocNodeValue (V value, Result<V> result){
      if ((bitmap & 1) != 0) {
        Object o = nodesOrValues[0];
        if (o instanceof Node) {
          Node<V> node = (Node<V>)o;
          node = node.assocNodeValue(value, result);
          if (node == o) {
            return this;
          } else {
            return cloneWithReplacedNodeOrValue(0, node);
          }
          
        } else {
          if (o == value) {
            return this;
          } else {
            result.replacedValue((V)o);
            return cloneWithReplacedNodeOrValue(0, value);
          }
        }
        
      } else { // we don't have anything for index 0
        result.addedValue(value);
        return cloneWithAddedNodeOrValue(1, 0, value);
      }
    }

            
    public Node<V> assoc (int shift, int finalShift, int key, V value, Result<V> result){
      int bit = 1 << ((key>>>shift) & 0x1f); // bitpos = index into bitmap
      
      int idx = Integer.bitCount( bitmap & (bit -1));

      if ((bitmap & bit) != 0) { // we already have an entry for this index
        Object o = nodesOrValues[idx];
        
        if (o instanceof Node){ // we already have a node
          Node<V> node = (Node<V>)o;
          node = (shift == finalShift) ?  node.assocNodeValue(value, result) :
                        node.assoc( shift + SHIFT_INC, finalShift, key, value, result);
          if (node == o) { // nothing changed
            return this;
          } else {
            return cloneWithReplacedNodeOrValue(idx, node);
          }

        } else { // we already have a value
          V v = (V)o;
          if (shift == finalShift){ // value level, check value
            if (value == v){ // it's already there
              return this;
            } else { // replace value
              result.replacedValue(v);
              return cloneWithReplacedNodeOrValue( idx, value);
            }
            
          } else { // not at value level, we need to replace the value with a Node
            result.addedValue(value);
            Node<V> node = createNode( shift + SHIFT_INC, finalShift, key, value, v);
            return cloneWithReplacedNodeOrValue( idx, node);            
          }
        }

      } else { // neither child node nor value for this bit yet
        result.addedValue(value);
        
        if (shift == finalShift){ // we can directly store it as a value
          return cloneWithAddedNodeOrValue( bit, idx, value);
          
        } else { // needs to be a node
          Node<V> node = createNode( shift + SHIFT_INC, finalShift, key, value, null);
          return cloneWithAddedNodeOrValue( bit, idx, node);          
        }
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
    
    Node<V> setValue (int index, V value, Result<V> result) {
      int bit = 1 << index;
      if ((bitmap & bit) != 0){
        int idx = Integer.bitCount( bitmap & (bit -1));
        Object o = nodesOrValues[idx];
        if (o instanceof Node) {
          Node<V> node = (Node<V>) o;
          node = node.assocNodeValue(value, result);
          if (node == o) {
            return this;
          } else {
            return cloneWithReplacedNodeOrValue(idx, node);
          }
          
        } else { // replace value
          V v = (V) o;
          if (v == value) {
            return this;
          } else {
            result.replacedValue(v);
            return cloneWithReplacedNodeOrValue(idx, value);
          }
        }
      } else {
        int idx = Integer.bitCount((bitmap | bit) & (bit -1));
        result.addedValue(value);
        return cloneWithAddedNodeOrValue(bit, idx, value);
      }
    }
    
    public V find (int shift, int finalShift, int key){
      int bit = 1 << ((key>>>shift) & 0x1f); // bitpos = index into bitmap
      
      if ((bitmap & bit) != 0) { // we  have a node or value for this index
        int idx = Integer.bitCount( bitmap & (bit -1));
        Object o = nodesOrValues[idx];
        
        if (o instanceof Node){ // recurse down
          Node<V> node = (Node<V>)o;
          
          if (shift == finalShift){ // at leaf level
            return node.getNodeValue();
          } else {
            return node.find( shift+SHIFT_INC, finalShift, key);
          }
          
        } else {
          if (shift == finalShift){
            return (V)o;
          } else {
            return null;
          }
        }
        
      } else {
        return null;
      }
    }
    
    public Node<V> removeNodeValue (Result<V> result){
      if ((bitmap & 1) != 0) {
        Object o = nodesOrValues[0];
        if (o instanceof Node) {
          Node<V> node = (Node<V>)o;
          node = node.removeNodeValue(result);
          if (node == null) {
            return cloneWithoutNodeOrValue(1, 0);
          } else if (node != o) {
            return cloneWithReplacedNodeOrValue(0,node);
          } else {
            return this;
          }
          
        } else { // we had a value at index 0
          result.removedValue((V)o);
          return cloneWithoutNodeOrValue(1, 0);
        }
      } else {
        return this;
      }
    }
    
    public Node<V> remove (int shift, int finalShift, int key, Result<V> result){
      int bit = 1 << ((key>>>shift) & 0x1f); // bitpos = index into bitmap
      
      if ((bitmap & bit) != 0) { // we have a node or value for this index
        int idx = Integer.bitCount( bitmap & (bit -1));
        Object o = nodesOrValues[idx];
        
        if (o instanceof Node){
          Node<V> node = (Node<V>)o;
          node = (shift == finalShift) ? node.removeNodeValue(result) :
                            node.remove(shift +SHIFT_INC, finalShift, key, result);
          if (node == null) {
            return cloneWithoutNodeOrValue( bit, idx);
          } else if (node == o) {
            return this;
          } else {
            return cloneWithReplacedNodeOrValue(idx, node);            
          }
          
        } else { // we have a value for this index
          if (shift == finalShift) {
            result.removedValue((V)o);
            return cloneWithoutNodeOrValue( bit, idx);
          } else {
            return this; // key is not in this map
          }
        }
        
      } else { // nothing stored for this key
        return this;
      }
    }
    
    public Node<V> removeAllSatisfying (Predicate<V> pred, Result<V> result){
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
          node = node.removeAllSatisfying(pred, result);
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
            result.removedValue(v);
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
    
    
    public void process (Processor<V> proc){
      for (int i=0; i<nodesOrValues.length; i++){
        Object o = nodesOrValues[i];
        if (o instanceof Node){
          ((Node<V>)o).process(proc);
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
  protected static class FullNode<V> extends Node<V> {
    final Object[] nodesOrValues;
          
    FullNode( Object[] a32){
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
    
    
    private final Node<V> cloneWithReplacedNodeOrValue(int idx, Object o){
      Object[] a = nodesOrValues.clone();
      a[idx] = o;
      return new FullNode<V>(a);
    }
    
    private final Node<V> cloneWithoutNodeOrValue(int idx){
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
    
    public Node<V> assocNodeValue (V value, Result<V> result){
      Object o = nodesOrValues[0];
      if (o instanceof Node) {
        Node<V> node = (Node<V>) o;
        node = node.assocNodeValue(value, result);
        if (node == o) {
          return this;
        } else {
          return cloneWithReplacedNodeOrValue(0, node);
        }

      } else {
        if (o == value) {
          return this;
        } else {
          result.replacedValue((V) o);
          return cloneWithReplacedNodeOrValue(0, value);
        }
      }
    }
    
    public Node<V> assoc(int shift, int finalShift, int key, V value, Result<V> result) {
      int idx = (key>>>shift) & 0x01f;
      
      // this is a full node, so we don't have to check if we have a node or value
      Object o = nodesOrValues[idx];
      
      if (o instanceof Node){
        Node<V> node = (Node<V>)o;
        node = (shift == finalShift) ?  node.assocNodeValue(value, result) :
                      node.assoc( shift + SHIFT_INC, finalShift, key, value, result);
        if (node == o) { // nothing changed
          return this;
        } else {
          return cloneWithReplacedNodeOrValue(idx, node);
        }

      } else { // a value at this index
        V v = (V)o;
        if (shift == finalShift){ // leaf level, check value
          if (value == v){ // it's already there
            return this;
          } else { // replace value
            result.replacedValue(v);
            return cloneWithReplacedNodeOrValue( idx, value);
          }
          
        } else { // not at value level, we need to replace the value with a Node
          result.addedValue(v);
          Node<V> node = createNode( shift + SHIFT_INC, finalShift, key, value, v);
          return cloneWithReplacedNodeOrValue( idx, node);            
        }
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
    
    Node<V> setValue (int idx, V value, Result<V> result) {
      Object o = nodesOrValues[idx];
      if (o instanceof Node) {
        Node<V> node = (Node<V>) o;
        node = node.assocNodeValue(value, result);
        if (node == o) {
          return this;
        } else {
          return cloneWithReplacedNodeOrValue(idx, node);
        }
        
      } else { // replace value
        V v = (V) o;
        if (v == value) {
          return this;
        } else {
          result.replacedValue(v);
          return cloneWithReplacedNodeOrValue(idx, value);
        }
      }
    }
    
    public V find (int shift, int finalShift, int key) {
      int idx = (key>>>shift) & 0x01f;      
      Object o = nodesOrValues[idx];
      
      if (o instanceof Node){ // recurse down
        Node<V> node = (Node<V>)o;
        
        if (shift == finalShift){ // at leaf level
          return node.getNodeValue();
        } else {
          return node.find( shift+SHIFT_INC, finalShift, key);
        }
        
      } else {
        if (shift == finalShift){
          return (V)o;
        } else {
          return null;
        }
      }
    }

    public void process (Processor<V> proc) {      
      for (int i=0; i<32; i++){
        Object o=nodesOrValues[i];
        if (o instanceof Node){
          ((Node<V>)o).process(proc);
        } else {
          proc.process( (V)o);
        }
      }
    }
     
    public Node<V> removeNodeValue (Result<V> result){
      Object o = nodesOrValues[0];
      if (o instanceof Node) {
        Node<V> node = (Node<V>)o;
        node = node.removeNodeValue(result);
        if (node == null) {
          return cloneWithoutNodeOrValue( 0);
        } else if (node != o) {
          return cloneWithReplacedNodeOrValue(0,node);
        } else {
          return this;
        }
        
      } else { // we had a value at index 0
        result.removedValue((V)o);
        return cloneWithoutNodeOrValue( 0);
      }
    }
    
    public Node<V> remove (int shift, int finalShift, int key, Result<V> result) {
      int idx = (key>>>shift) & 0x01f;
      
      Object o = nodesOrValues[idx];
      
      if (o instanceof Node){
        Node<V> node = (Node<V>)o;
        node = (shift == finalShift) ? node.removeNodeValue(result) :
                          node.remove(shift +SHIFT_INC, finalShift, key, result);
        if (node == null) {
          return cloneWithoutNodeOrValue( idx);
        } else if (node == o) {
          return this;
        } else {
          return cloneWithReplacedNodeOrValue(idx, node);            
        }
        
      } else { // we have a value for this index
        if (shift == finalShift) {
          result.removedValue((V)o);
          return cloneWithoutNodeOrValue(idx);
        } else {
          return this; // key is not in this map
        }
      }
    }
    
    public Node<V> removeAllSatisfying (Predicate<V> pred, Result<V> result){
      Object[] nv = nodesOrValues;
      Object[] a = null; // deferred initialized
            
      //--- check which nodesOrValues are affected and create bitmap
      int newBitmap = 0;
      int bit = 1;
      for (int i=0; i<nv.length; i++) {
        Object o = nv[i];
        if (o instanceof Node){ // a node
          Node<V> node = (Node<V>)o;
          node = node.removeAllSatisfying(pred, result);
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
            result.removedValue(v);
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
  
  static <V> Node<V> createNode (int shift, int finalShift, int key, V value, V nodeValue){
    int idx = ((key>>>shift) & 0x01f);
    
    Object o;
    if (shift == finalShift){
      o = value;
    } else {
      o = createNode( shift + SHIFT_INC, finalShift, key, value, 
                      ((idx == 0) ? nodeValue : null));
    }
    
    if (nodeValue != null) {
      Object[] nodesOrValues = new Object[2];
      nodesOrValues[0] = nodeValue;
      nodesOrValues[1] = o;
      int bitmap = (1 << idx) | 1;
      
      return new BitmapNode<V>(bitmap, nodesOrValues);
      
    } else {
      return new OneNode<V>( idx, o);
    }
  }

  static <V> Node<V> createAndMergeNode (int shift, int finalShift, int key, V value, Node<V> mergeNode){
    int idx = ((key>>>shift) & 0x01f);
    
    Object o;    
    if (shift == finalShift){
      o = value;
    } else {
      o = createNode( shift + SHIFT_INC, finalShift, key, value, null);
    }
      
    Object[] nodesOrValues = new Object[2];
    nodesOrValues[0] = mergeNode;
    nodesOrValues[1] = o;
    int bitmap = (1 << idx) | 1;
    
    return new BitmapNode<V>(bitmap, nodesOrValues);
  }
  
  static <V> Node<V> propagateMergeNode (int shift, int nodeShift, Node<V> node){
    shift += SHIFT_INC;
    while (shift > nodeShift){
      node = new OneNode<V>(0, node);
      shift += SHIFT_INC;
    }
    
    return node;
  }
  
  
  static int getInitialShift (int key) {
    if ((key & 0xc0000000) != 0) return 30;
    if ((key & 0x3e000000) != 0) return 25;
    if ((key & 0x1f00000) != 0)  return 20;
    if ((key & 0xf8000) != 0)    return 15;
    if ((key & 0x7c00) != 0)     return 10;
    if ((key & 0x3e0) != 0)      return 5;
    return 0;
  }
  
  static int getFinalShift (int key) {
    if ((key & 0x1f) < 32)       return 0;
    if ((key & 0x3e0) != 0)      return 5;
    if ((key & 0x7c00) != 0)     return 10;
    if ((key & 0xf8000) != 0)    return 15;
    if ((key & 0x1f00000) != 0)  return 20;
    if ((key & 0x3e000000) != 0) return 25;
    return 30;
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

  public PersistentMsbIntMap<V> set (int key, V value){
    Result<V> result = Result.getNewResult();
    return set( key, value, result);
  }  
  
  public PersistentMsbIntMap<V> set (int key, V value, Result<V> result){
    
    int ish = getInitialShift(key);
    int fsh = getFinalShift(key);
    
    if (root == null){
      result.addedValue(value);
      Node<V> newRoot = createNode( ish, fsh, key, value, null);
      return new PersistentMsbIntMap<V>( 1, newRoot, ish);

    } else {            
      Node<V> newRoot;
      int newRootShift;
      
      if (ish <= rootShift) { // new key can be added to current root (key smaller than previous ones)
        newRoot = root.assoc( rootShift, fsh, key, value, result);
        newRootShift = rootShift;

      } else { // current root has to be merged into new one
        result.addedValue(value); // since shift count is different, there has to be a new node
        
        Node<V> mergeNode = propagateMergeNode( ish, rootShift, root);
        newRoot = createAndMergeNode( ish, fsh, key, value, mergeNode);
        newRootShift = ish;
      }
      
      if (root == newRoot){ // key and value were already there
        return this;
      } else { // could have been a replaced value that didn't change the size
        if (result.hasChangedSize()){
          return new PersistentMsbIntMap<V>( size+result.getSizeChange(), newRoot, newRootShift);
        } else {
          return new PersistentMsbIntMap<V>( size, newRoot, newRootShift);
        }
      }
    }
  }
  
  public V get (int key){
    if (root != null){
      int fsh = getFinalShift(key);
      return root.find( rootShift, fsh, key);
    } else {
      return null;
    }
  }

  public PersistentMsbIntMap<V> remove(int key){
    Result<V> result = Result.getNewResult();
    return remove( key, result);
  }  
  
  public PersistentMsbIntMap<V> remove(int key, Result<V> result){
    if (root == null){
      return this;
      
    } else {
      int fsh = getFinalShift(key);
      Node<V> newRoot = root.remove( rootShift, fsh, key, result);

      if (root == newRoot){ // nothing removed
        return this;
      } else {
        // <2do> we should check if we can increase the initialShift
        return new PersistentMsbIntMap<V>( size-1, newRoot, rootShift);
      }
    }
  }
  
  // bulk remove
  public PersistentMsbIntMap<V> removeAllSatisfying (Predicate<V> predicate){
    Result<V> result = Result.getNewResult();
    return removeAllSatisfying(predicate, result);
  }
    
  public PersistentMsbIntMap<V> removeAllSatisfying (Predicate<V> predicate, Result<V> result){
    if (root != null){
      result.clear();
      Node<V> newRoot = (Node<V>) root.removeAllSatisfying( predicate, result);
      
      // <2do> we should check if we can increase the initialShift

      return new PersistentMsbIntMap<V>( size+result.getSizeChange(), newRoot, rootShift);
      
    } else {
      return this;
    }
  }
  
  public int size(){
    return size;
  }
  
  public boolean isEmpty(){
    return size==0;
  }
    
  
  // iterators would require a snapshot (list), which kind of defeats the purpose
  
  public void process (Processor<V> processor){
    if (root != null){
      root.process(processor);
    }
  }
  
  public void processInKeyOrder (Processor<V> processor){
    process( processor);
  }
  
  public void printOn (PrintStream ps) {
    if (root != null) {
      root.printNodeInfoOn(ps);
      ps.println();
      root.printOn(ps, 1);
    }
  }
}
