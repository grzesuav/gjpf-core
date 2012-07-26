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


public class PersistentStagingMsbIntMap<V> extends PersistentMsbIntMap<V> {

  protected final int stagingBase;
  protected final Node<V> stagingNode;
  
  
  static <V> Node<V> createNodeFromStagingValues (int stagingSize, Object[] a){
    
    if (stagingSize == 32){
      return new FullNode<V>(a); 
      
    } else if (stagingSize == 1){
      for (int i=0; i<32; i++){
        Object o = a[i];
        if (o != null){
          return new OneNode<V>(i, o);
        }
      }
      return null; // can't happen, we have a stagingSize > 0
          
    } else {
      int bitmap = 0;
      int bit = 1;
      Object[] values = new Object[stagingSize];
      
      for (int i = 0, j = 0; j < stagingSize; i++) {
        Object o = a[i];
        if (o != null){
          bitmap |= bit;
          values[j++] = o;
        }
        
        bit <<= 1;
      }
      
      return new BitmapNode<V>(bitmap, values);
    }
  }
  
  public PersistentStagingMsbIntMap(){
    super();
    
    stagingBase = -1;
    stagingNode = null;
  }

  protected PersistentStagingMsbIntMap (int size, Node<V> root, int initialShift, int stagingBase, Node<V> stagingNode){
    super(size, root, initialShift);

    this.stagingBase = stagingBase;
    this.stagingNode = stagingNode; 
  }
 
  public V get (int key){
    int fsh = getFinalShift(key);
  
    int k = (key >>> fsh);
    if ((k >>> 5) == stagingBase){ // we have a staging hit
      k &= 0x1f;
      return stagingNode.getValue(k);
      
    } else { // look it up in the trie
      return super.get(key);
    }
  }
  
  public PersistentMsbIntMap<V> set (int key, V value, Result<V> result){
    
    int ish = getInitialShift(key);
    int fsh = getFinalShift(key);
  
    int k = (key >>> fsh);
    
    if ((k >>> 5) == stagingBase){ // we have a staging hit
      k &= 0x1f;
      Node<V> newStagingNode = stagingNode.setValue((k & 0x1f), value, result);
      return new PersistentStagingMsbIntMap<V>( size + result.getSizeChange(), root, rootShift, stagingBase, newStagingNode);
      
    } else { // staging miss
      //--- put the staging node into the map
      if (root == null){
        
      }
      
      //--- check if we already have values for the next staging
      
      //--- populate the new staging values
    }
    
    
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

}
