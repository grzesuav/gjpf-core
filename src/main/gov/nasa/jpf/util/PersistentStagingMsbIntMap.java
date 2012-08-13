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

import gov.nasa.jpf.util.PersistentIntMap.Node;
import gov.nasa.jpf.util.PersistentIntMap.Result;

import java.io.PrintStream;

public class PersistentStagingMsbIntMap<V> extends PersistentMsbIntMap<V> {

  protected final int stagingBase;
  protected final Node<V> stagingNode;
  protected final Node<V> sourceNode; // that gets replaced by the stagingNode
  
  public PersistentStagingMsbIntMap(){
    super();
    
    stagingBase = -1; // make it miss the first time
    stagingNode = null;
    sourceNode = null;
  }

  protected PersistentStagingMsbIntMap (int size, Node<V> root, int rootShift, Node<V> stagingNode, int stagingBase, Node<V> sourceNode){
    super(size, root, rootShift);

    this.stagingBase = stagingBase;
    this.stagingNode = stagingNode;
    this.sourceNode = sourceNode;
  }
    
  /**
   * the internal single value setter - only used on the stageNode, hence we don't have to keep track of the node base
   */
  protected Node<V> setStageNodeValue (Node<V> node, int idx, V value, Result<V> result){
    Object o = node.getElement(idx);

    if (o != null){                        // we got something for index 0
      if (o instanceof Node){              // we've got a node
        Node<V> changedNode = assocNodeValue( (Node<V>)o, value, result);
        if (changedNode == o){
          return node;
        } else {
          return node.cloneWithReplacedElement( idx, changedNode);
        }

      } else {                             // we've got a value
        V v = (V)o;
        if (v == value){
          return node;
        } else {
          node = node.cloneWithReplacedElement( idx, value);
          result.replacedValue( node, v);
          return node;
        }
      }

    } else {                               // we didn't have anything for this index
      node = node.cloneWithAddedElement( idx, value);
      result.addedValue( node, value);
      return node;
    }
  }
  
  /**
   * an assoc version that merges the staging node on-the-fly, without redundant node cloning  
   */
  protected Node<V> assoc (int shift, int finalShift, int key, V value, Node<V> node, Result<V> result){
    int k = (key >>> shift);
    
    if (node == sourceNode) {
      // takes care of old staging node parent of new staging node, in which case we can do
      // the merge on-the-fly (in descending branch of recursion)
      node = stagingNode;
      result.merged = true;
    }
    
    int levelIdx = k & 0x1f;
    Object o = node.getElement(levelIdx);

    if (o != null){                       // we already have a node or value for this index

      if (o instanceof Node){             // we had a node for this index
        Node<V> newElement;
        if (shift == finalShift){         // at value level
          newElement = assocNodeValue( node, value, result);
        } else {                          // not at value level yet, recurse
          newElement = assoc( shift-5, finalShift, key, value, (Node<V>)o, result);
        }
        if (newElement == o){           // nothing changed
          return node;
        } else {                          // element got replaced, clone the node
          return node.cloneWithReplacedElement( levelIdx, newElement);
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
  
  /**
   * replace sourceNode with stagingNode, clone parent nodes up to mergeNode, which is
   * the first one that is on the path that was just cloned for the new stagingNode
   * 
   * NOTE - this has to be called AFTER modifying the trie due to a stagingNode miss
   */
  protected Node<V> mergeStagingNode (int shift, int key, Node<V> node, boolean isSharedParent) {
    if (node == sourceNode) {
      return stagingNode;
      
    } else {
      int nk = (key >>> shift);
      int k = (stagingBase >>> shift);
      int idx = (k & 0x1f);      
      Node<V> childNode = (Node<V>) node.getElement(idx); // it's got to be a node
      
      if (k == nk) {
        // nothing to merge, this is still on the shared path
        return mergeStagingNode( shift-5, key, childNode, true);
        
      } else {
        Node<V> newChildNode = mergeStagingNode( shift-5, key, childNode, false);
        if (isSharedParent) { // this is the merge node -> overwrite
          node.setElement(idx, newChildNode);
          return node;
          
        } else { // we are below the merge node -> clone
          return node.cloneWithReplacedElement(idx, newChildNode);
        }
      }
    }
  }
  
  @Override
  public PersistentStagingMsbIntMap<V> set (int key, V value, Result<V> result){ 
    int fsh = getFinalShift(key);  
    int k = (key >>> fsh);
    result.clear();
    
    if (((k >>> 5) << (fsh+5)) == stagingBase){       // stagingNode hit (this should be the statistically dominant case)
      int levelIdx = k & 0x1f;
      Node<V> newStagingNode = setStageNodeValue( stagingNode, levelIdx, value, result);
      if (newStagingNode != stagingNode) {
        Node<V> newRoot = (stagingNode == root) ? newStagingNode : root;
        return new PersistentStagingMsbIntMap<V>(size + result.changeCount, newRoot, rootShift, newStagingNode, stagingBase, sourceNode);
      } else { // it was already there
        return this;
      }
      
    } else {                             // stagingNode miss
      int ish = getInitialShift(key);
      int newStagingBase = (k >>> 5) << (fsh+5);
      
      if (root == null){                 // the very first node  
        Node<V> newRoot = createNode( ish, fsh, key, value, null, result);
        Node<V> newStagingNode = result.valueNode;
        return new PersistentStagingMsbIntMap<V>( 1, newRoot, ish, newStagingNode, newStagingBase, newStagingNode);

      } else {                           // set new & merge old stagingNode
        Node<V> newRoot;
        int newRootShift;
        
        if (ish <= rootShift) { // new key can be added to current root (key smaller than previous ones)
          newRoot = assoc( rootShift, fsh, key, value, root, result);
          newRootShift = rootShift;

        } else { // current root has to be merged into new one
          Node<V> mergeNode = propagateMergeNode( ish, rootShift, root);
          newRoot = createAndMergeNode( ish, fsh, key, value, mergeNode, result);
          newRootShift = ish;
          
          if (stagingNode == root) {
            result.merged = true;
          }
        }
        
        Node<V> newStagingNode = result.valueNode;
                
        if (root == newRoot){ // key and value were already there
          return this;
          
        } else { // could have been a replaced value that didn't change the size
          if (!result.merged) { // the old stagingNode wasn't on the cloned path of the new one
            // this cannot change the root since that it is always on both the new staging and old staging path
            mergeStagingNode( newRootShift, key, newRoot, true);
          }

          int newSize = size + result.changeCount;
          return new PersistentStagingMsbIntMap<V>( newSize, newRoot, newRootShift, newStagingNode, newStagingBase, newStagingNode);
        }
      }
    }
  }
  
  public V get (int key){
    int fsh = getFinalShift(key);
  
    int k = (key >>> fsh);
    if (((k >>> 5) << (fsh+5)) == stagingBase){ // we have a staging hit
      k &= 0x1f;
      Object o = stagingNode.getElement(k);
      if (o == null || o instanceof Node) { // ?? NodeValue?
        return null;
      } else {
        return (V)o;
      }
      
    } else { // look it up in the trie
      //return super.get(key);
      if (root != null){
        int shift = rootShift;
        Node<V> node = root;

        for (;;){
          int levelIdx = (key>>>shift) & 0x1f;
          Object o = node.getElement(levelIdx);
          if (o != null){                      // do we have something for this index
            if (o instanceof Node){            // we have a node
              node = (Node<V>)o;
              if (shift == fsh){        // at value level
                return node.getNodeValue();
              } else {                         // shift to next level (tail recursion)
                shift -= 5;
                continue;
              }
              
            } else {                           // we have a value
              if (shift == fsh){        // at value level
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
  }
  
  
  protected Node<V> removeStageNodeValue (Node<V> node, int idx, Result<V> result){
    Object o = node.getElement(idx);
    
    if (o != null) {
      if (o instanceof Node) {
        Node<V> newNodeElement = removeNodeValue((Node<V>)o, result);
        if (newNodeElement == null) {
          return node.cloneWithoutElement(idx);
        } else if (newNodeElement == o) {
          return node;
        } else {
          return node.cloneWithReplacedElement(idx, newNodeElement);
        }
        
      } else { // we had a value at idx
        node = node.cloneWithoutElement(idx);
        result.removedValue(node, (V)o);
        return node;
      }
      
    } else { // nothing to remove
      return node;      
    }
  }
  
  protected Node<V> remove (int shift, int finalShift, int key, Node<V> node, Result<V> result){
    int k = (key >>> shift);
    
    if (node == sourceNode) {
      // takes care of old staging node parent of new staging node, in which case we can do
      // the merge on-the-fly (in descending branch of recursion)
      node = stagingNode;
      result.merged = true;
    }
    
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
  
  @Override
  public PersistentStagingMsbIntMap<V> remove(int key, Result<V> result){
    int fsh = getFinalShift(key);  
    int k = (key >>> fsh);
    result.clear();
    
    if (((k >>> 5) << (fsh+5)) == stagingBase){       // stagingNode hit (this should be the statistically dominant case)
      int levelIdx = k & 0x1f;
      Node<V> newStagingNode = removeStageNodeValue( stagingNode, levelIdx, result);
      if (newStagingNode != stagingNode) {
        Node<V> newRoot = (stagingNode == root) ? newStagingNode : root;
        return new PersistentStagingMsbIntMap<V>(size + result.changeCount, newRoot, rootShift, newStagingNode, stagingBase, sourceNode);

      } else {
        return this; // nothing removed
      }
      
    } else {
      if (root == null) {
        return this;

      } else {
        int newStagingBase = (k >>> 5) << (fsh+5);
        
        Node<V> newRoot = remove(rootShift, fsh, key, root, result);
        Node<V> newStagingNode = result.valueNode;

        if (root == newRoot) { // nothing removed
          return this;
          
        } else {
          if (!result.merged) { // the old stagingNode wasn't on the cloned path of the new one
            // this cannot change the root since that it is always on both the new staging and old staging path
            mergeStagingNode( rootShift, key, newRoot, true);
          }
         
          int newSize = size + result.changeCount;
          return new PersistentStagingMsbIntMap<V>( newSize, newRoot, rootShift, newStagingNode, newStagingBase, newStagingNode);
        }
      }
    }
  }

  
  @Override
  protected Node<V> removeAllSatisfying (Node<V> node, Predicate<V> pred, Result<V> result) {
    if (node == sourceNode) {
      node = stagingNode;      
    }
    
    return node.removeAllSatisfying( this, pred, result);
  }
  
  @Override
  public PersistentStagingMsbIntMap<V> removeAllSatisfying (Predicate<V> predicate, Result<V> result){
    if (root != null){
      result.clear();
      Node<V> newRoot = (Node<V>) root.removeAllSatisfying( this, predicate, result);
      
      // this always merges the stagingNode
      return new PersistentStagingMsbIntMap<V>( size+result.getSizeChange(), newRoot, rootShift, null, -1, null);
      
    } else {
      return this;
    }
  }
  
  @Override
  public void processNode (Node<V> node, Processor<V> processor) {
    if (node == sourceNode) {
      node = stagingNode;
    }
    node.process(this, processor);
  }
  
  public void printOn (PrintStream ps) {
    if (root != null) {
      ps.print("root: ");
      root.printNodeInfoOn(ps);
      ps.println();
      root.printOn(ps, 1);
    }
    
    if (stagingNode != null) {
      ps.print("staging: ");
      stagingNode.printNodeInfoOn(ps);
      ps.println();
      stagingNode.printOn(ps, 1);
    }
  }
}
