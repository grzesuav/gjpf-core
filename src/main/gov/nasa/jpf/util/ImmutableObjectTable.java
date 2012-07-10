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

/**
 * an immutable Vector that is implemented as a 
 */
public class ImmutableObjectTable<V> {

  private static class Result<V> {
    boolean changedValueCount;
    V oldValue;
  }
  
  private abstract static class Node<V> implements Cloneable {
    // optional value for this node, in case it represents both a leaf and
    // a key prefix for other nodes
    final V value;
    
    protected Node (V value){
      this.value = value;
    }
    protected abstract Node<V> cloneWithValue (V value);
    
    abstract Node<V> assoc (int shift, int key, V value, Result<V> result);
    abstract V find (int shift, int key);
    abstract Node<V> remove (int shift, int key, Result<V> result);
    abstract void process (Processor<V> proc);
    abstract void process (int level, int pos, Processor<V> proc, CountDown countDown);
  }
  
  private final static class BitmapNode<V> extends Node<V> {
    
    final int bitmap; // key partition bit positions of non-null child nodes or values
    // note: we don't use bit 0 since we only need 31 positions (then we promote to full node)
    
    final Object[] nodesOrValues; // dense child|value array indexed by bitmap bitcount of pos
    
    BitmapNode (V value, int bitmap, Object[] nodesOrValues){
      super(value);
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

    // note: ctor construction is about 5x faster than cloning for a low number of fields
    // and simple field assignment ctors, so we don't bother with Object.clone()
    
    protected final Node<V> cloneWithValue (V value){
      return new BitmapNode<V>( value, bitmap, nodesOrValues);
    }
    
    private final Node<V> cloneWithReplacedNodeOrValue (int idx, Object o){
      return new BitmapNode<V>(value, bitmap, cloneArrayWithReplaced(idx, o));
    }

    private final Node<V> cloneWithAddedNodeOrValue (int bit, int idx, Object o){
      if (nodesOrValues.length == 31){
        return new FullNode<V>(value, nodesOrValues, idx, o); 
      } else {
        return new BitmapNode<V>(value, bitmap | bit, cloneArrayWithAdded(idx, o));
      }
    }
    
    private final Node<V> cloneWithoutNodeOrValue (int bit, int idx){
      if (nodesOrValues.length == 2){
        Object o = (idx == 0) ? nodesOrValues[1] : nodesOrValues[0];
        int i = Integer.numberOfTrailingZeros(bitmap ^ bit);
        return new OneNode<V>( value, i, o);
      } else {
        return new BitmapNode<V>(value, bitmap ^ bit, cloneArrayWithout(idx));
      }
    }
    
    //--- the recursive operations
    
    public V find (int shift, int key){
      int ks = key >>> shift;
      int bit = 1 << (ks & 0x01f); // bitpos = index into bitmap
      if ((bitmap & bit) != 0) { // we  have a node or value for this index
        int idx = Integer.bitCount( bitmap & (bit -1));
        Object o = nodesOrValues[idx];
        boolean isAtLeafLevel = (ks >>> 5) == 0;
        
        if (o instanceof Node){ // recurse down
          Node<V> node = (Node<V>)o;
          
          if (isAtLeafLevel){ // at leaf level
            return node.value;
          } else {
            return node.find( shift+5, key);
          }
          
        } else {
          if (isAtLeafLevel){
            return (V)o;
          } else {
            return null;
          }
        }
        
      } else {
        return null;
      }
    }
    
    public Node<V> assoc (int shift, int key, V value, Result<V> result){
      int ks = key >>> shift;
      int bit = 1 << (ks & 0x01f); // bitpos = index into bitmap
      int idx = Integer.bitCount( bitmap & (bit -1));
      boolean isAtLeafLevel = (ks >>> 5) == 0;

      if ((bitmap & bit) != 0) { // we already have an entry for this index
        Object o = nodesOrValues[idx];
        
        if (o instanceof Node){ // we already have a node
          Node<V> node = (Node<V>)o;
          if (isAtLeafLevel){ // leaf level, check node value
            if (value == node.value){ // already there
              return this;
              
            } else {
              result.changedValueCount = (node.value == null);
              result.oldValue = node.value;
              
              node = node.cloneWithValue(value);
              return cloneWithReplacedNodeOrValue( idx, node);
            }
            
          } else { // not at leaf level, needs recursive descent
            node = node.assoc( shift+5, key, value, result);
            if (node == o){ // nothing changed by child node(s)
              return this;
            } else {
              return cloneWithReplacedNodeOrValue( idx, node);
            }
          }

        } else { // we already have a value
          V currentValue = (V)o;
          if (isAtLeafLevel){ // leaf level, check value
            if (value == currentValue){ // it's already there
              return this;
            } else { // replace value
              result.oldValue = currentValue;
              return cloneWithReplacedNodeOrValue( idx, value);
            }
            
          } else { // not at leaf level, we need to replace the value with a Node
            result.changedValueCount = true;
            Node<V> node = createNode( shift+5, key, value, currentValue);
            return cloneWithReplacedNodeOrValue( idx, node);            
          }
        }

      } else { // neither child node nor value for this bit yet
        result.changedValueCount = true;
        
        if (isAtLeafLevel){ // we can directly store it as a value
          return cloneWithAddedNodeOrValue( bit, idx, value);
          
        } else { // needs to be a node
          Node<V> node = createNode( shift+5, key, value, null);
          return cloneWithAddedNodeOrValue( bit, idx, node);          
        }
      }      
    }

    public Node<V> remove (int shift, int key, Result<V> result){
      int ks = key >>> shift;
      int bit = 1 << (ks & 0x01f); // bitpos = index into bitmap
      boolean isAtLeafLevel = (ks >>> 5) == 0;
      
      if ((bitmap & bit) != 0) { // we have a node or value for this index
        int idx = Integer.bitCount( bitmap & (bit -1));
        Object o = nodesOrValues[idx];
        
        if (o instanceof Node){
          Node<V> node = (Node<V>)o;
          
          if (isAtLeafLevel){
            if (node.value != null){
              result.changedValueCount = true;
              result.oldValue = node.value;
              node = node.cloneWithValue(null);
              return cloneWithReplacedNodeOrValue(idx, node);
            } else {
              return this; // nothing to remove
            }
            
          } else {
            node = node.remove(shift + 5, key, result); // recurse down

            if (node == o) { // nothing removed by children
              return this;
              
            } else { // children did change
              if (node != null) { // we still have children
                return cloneWithReplacedNodeOrValue( idx, node);
                
              } else { // no children left
                if (bitmap == bit) { // this was our last child
                  return null;
                } else {
                  return cloneWithoutNodeOrValue( bit, idx);
                }
              }
            }
          }
          
        } else { // we have a value for this index
          if (isAtLeafLevel){
            result.changedValueCount = true;
            result.oldValue = (V)o;
            
            if (bitmap == bit) { // last value
              return null;
            } else {
              return cloneWithoutNodeOrValue( bit, idx);
            }
            
          } else {
            return this; // the key isn't in this map
          }
        }
        
      } else { // nothing stored for this key
        return this;
      }
    }
    
    public void process (Processor<V> proc){
      if (value != null){
        proc.process(value);
      }
      
      for (int i=0; i<nodesOrValues.length; i++){
        Object o = nodesOrValues[i];
        if (o instanceof Node){
          ((Node<V>)o).process(proc);
        } else {
          proc.process((V)o);
        }
      }
    }
    
    public void process (int levelDistance, int pos, Processor<V> proc, CountDown countDown){
      Object[] nv = nodesOrValues;
      
      if (levelDistance == 0){
        int bit = 2 << pos; // we don't use bit 0
        if ((bitmap & bit) != 0) {
          int idx = Integer.bitCount(bitmap & (bit - 1));
          Object o=nv[pos];
          if (o instanceof Node){
            Node<V> node = (Node<V>)o;
            if (node.value != null){
              proc.process(node.value);
              countDown.dec();
            }
          } else {
            proc.process((V)o);
            countDown.dec();
          }
        }
      } else {
        for (int i=0; i<nv.length; i++){
          Object o = nv[i];
          if (o instanceof Node){
            Node<V> node = (Node<V>)o;
            node.process(levelDistance-1, pos, proc, countDown);
          }
        }        
      }
    }
  }
  
  // a node that has only one element and hence doesn't need an array
  static class OneNode<V> extends Node<V> {
    final int idx; // could be byte, but that's probably not faster
    final Object nodeOrValue;
    
    OneNode (V value, int idx, Object o){
      super(value);
      this.idx = idx;
      this.nodeOrValue = o;
    }
    
    protected final Node<V> cloneWithValue(V newValue){
      return new OneNode<V>(newValue, idx, nodeOrValue);
    }
    
    private final Node<V> cloneWithReplacedNodeOrValue( Object o){
      return new OneNode<V>(value, idx, o);
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
      
      return new BitmapNode<V>(value, bitmap, a);
    }
    
    //--- Node interface
    
    public Node<V> assoc (int shift, int key, V value, Result<V> result){      
      int ks = key >>> shift;
      int idx = ks & 0x1f;
      boolean isAtLeafLevel = (ks == idx);

      if (idx == this.idx) { // we already have an entry for this index
        Object o = nodeOrValue;
        
        if (o instanceof Node){ // we already have a node
          Node<V> node = (Node<V>)o;
          if (isAtLeafLevel){ // leaf level, check node value
            if (value == node.value){ // already there
              return this;
              
            } else {
              result.changedValueCount = (node.value == null);
              result.oldValue = node.value;
              
              node = node.cloneWithValue(value);
              return cloneWithReplacedNodeOrValue( node);
            }
            
          } else { // not at leaf level, needs recursive descent
            node = node.assoc( shift+5, key, value, result);
            if (node == o){ // nothing changed by child node(s)
              return this;
            } else {
              return cloneWithReplacedNodeOrValue( node);
            }
          }

        } else { // we already have a value
          V currentValue = (V)o;
          if (isAtLeafLevel){ // leaf level, check value
            if (value == currentValue){ // it's already there
              return this;
            } else { // replace value
              result.oldValue = currentValue;
              return cloneWithReplacedNodeOrValue( value);
            }
            
          } else { // not at leaf level, we need to replace the value with a Node
            result.changedValueCount = true;
            Node<V> node = createNode( shift+5, key, value, currentValue);
            return cloneWithReplacedNodeOrValue( node);            
          }
        }

      } else { // neither child node nor value for this index, we have to add
        result.changedValueCount = true;
        
        if (isAtLeafLevel){ // we can directly store it as a value
          return cloneWithAddedNodeOrValue( idx, value);
          
        } else { // needs to be a node
          Node<V> node = createNode( shift+5, key, value, null);
          return cloneWithAddedNodeOrValue( idx, node);          
        }
      }      
    }
    
    public V find (int shift, int key){
      int ks = key >>> shift;
      int idx = (ks & 0x01f);

      if (idx == this.idx) { // we  have a node or value for this index
        Object o = nodeOrValue;
        boolean isAtLeafLevel = (ks == idx);
        
        if (o instanceof Node){ // recurse down
          Node<V> node = (Node<V>)o;
          
          if (isAtLeafLevel){ // at leaf level
            return node.value;
          } else {
            return node.find( shift+5, key);
          }
          
        } else {
          if (isAtLeafLevel){
            return (V)o;
          } else {
            return null;
          }
        }
        
      } else {
        return null;
      }
    }
    
    public Node<V> remove (int shift, int key, Result<V> result){
      int ks = key >>> shift;
      int idx = (ks & 0x01f);
      boolean isAtLeafLevel = (ks == idx);
      
      if (idx == this.idx) { // we have a node or value for this index
        Object o = nodeOrValue;
        
        if (o instanceof Node){
          Node<V> node = (Node<V>)o;
          
          if (isAtLeafLevel){
            if (node.value != null){
              result.changedValueCount = true;
              result.oldValue = node.value;
              node = node.cloneWithValue(null);
              return cloneWithReplacedNodeOrValue(node);
            } else {
              return this; // nothing to remove
            }
            
          } else {
            node = node.remove(shift + 5, key, result); // recurse down

            if (node == o) { // nothing removed by child
              return this;
              
            } else { // child did change
              if (node != null) { // we still have children
                return cloneWithReplacedNodeOrValue(node);
                
              } else { // no child left
                return null;
              }
            }
          }
          
        } else { // we have a value for this index
          if (isAtLeafLevel){
            result.changedValueCount = true;
            result.oldValue = (V)o;
            
            return null; // no child left
            
          } else {
            return this; // the key isn't in this map
          }
        }
        
      } else { // nothing stored for this key
        return this;
      }
    }

    public void process (Processor<V> proc){
      if (value != null){
        proc.process(value);
      }

      if (nodeOrValue instanceof Node){
        ((Node<V>)nodeOrValue).process(proc);
      } else {
        proc.process((V)nodeOrValue);
      }
    }
    
    public void process (int levelDistance, int pos, Processor<V> proc, CountDown countDown){
      if (levelDistance == 0){
        if (pos == idx){
          if (nodeOrValue instanceof Node){
            Node<V> node = (Node<V>)nodeOrValue;
            if (node.value != null){
              proc.process(node.value);
              countDown.dec();
            }
          } else {
            proc.process((V)nodeOrValue);
            countDown.dec();
          }
        }
      } else {
        if (nodeOrValue instanceof Node){
          Node<V> node = (Node<V>)nodeOrValue;
          node.process(levelDistance-1, pos, proc, countDown);
        }
      }
    }
  }
  
  // a Node that has 32 non-null elements, and hence doesn't add and doesn't need a bitmap
  static class FullNode<V> extends Node<V> {
    final Object[] nodesOrValues;

    FullNode (V value, Object[] a){
      super(value);
      nodesOrValues = a;
    }
          
    FullNode(V value, Object[] a31, int idx, Object o){
      super(value);
      
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
    
    protected final Node<V> cloneWithValue(V newValue){
      return new FullNode<V>( newValue, nodesOrValues);
    }
    
    private final Node<V> cloneWithReplacedNodeOrValue(int idx, Object o){
      Object[] a = nodesOrValues.clone();
      a[idx] = o;
      return new FullNode<V>(value, a);
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
      
      return new BitmapNode<V>( value, bitmap, a);
    }
    
    public Node<V> assoc(int shift, int key, V value, Result<V> result) {
      int ks = key >>> shift;
      int idx = ks & 0x01f;
      boolean isAtLeafLevel = (ks >>> 5) == 0;
      
      Object o = nodesOrValues[idx];
      // this is a full node, so we don't have to check if we have a node or value
      
      if (o instanceof Node){ // we had a node for this index
        Node<V> node = (Node<V>)o;
        if (isAtLeafLevel){
          if (node.value == value){
            return this;
          } else {
            result.oldValue = node.value;
            node = node.cloneWithValue( value);
            return cloneWithReplacedNodeOrValue(idx, node);
          }
        } else {
          node = node.assoc(shift + 5, key, value, result);
          if (node == o) { // nothing added by children
            return this;
          } else {
            return cloneWithReplacedNodeOrValue(idx, node);
          }
        }

      } else { // we have a previous value for this index
        V currentValue = (V)o;
        if (isAtLeafLevel){
          if (currentValue == value) {
            return this; // nothing to do, the value is already there
          } else {
            result.oldValue = currentValue;
            return cloneWithReplacedNodeOrValue( idx, value);
          }

        } else { // this is not the leaf level, promote value to node
          result.changedValueCount = true;
          Node<V> node = createNode(shift + 5, key, value, currentValue);
          return cloneWithReplacedNodeOrValue(idx, node);
        }
      }
    }

    public V find (int shift, int key) {
      int ks = key >>> shift;      
      int idx = ks & 0x01f;
      boolean isAtLeafLevel = (ks >>> 5) == 0;
      
      Object o = nodesOrValues[idx];
      
      if (isAtLeafLevel){
        if (o instanceof Node){
          return ((Node<V>)o).value;
        } else {
          return (V)o;
        }
        
      } else {
        if (o instanceof Node){
          return ((Node<V>)o).find(shift+5, key);
        } else {
          return null;
        }        
      }
    }

    public Node<V> remove(int shift, int key, Result<V> result) {
      int ks = key >>> shift;      
      int idx = ks & 0x01f;
      boolean isAtLeafLevel = (ks >>> 5) == 0;
      
      Object o = nodesOrValues[idx];
      
      if (o instanceof Node){
        Node<V> node = (Node<V>)o;
        if (isAtLeafLevel){
          if (node.value != null){
            result.changedValueCount = true;
            result.oldValue = node.value;
            node = node.cloneWithValue(null);
            return cloneWithReplacedNodeOrValue(idx, node);
          } else {
            return this; // nothing to remove
          }
          
        } else { // not at leaf level, recurse down
          node = node.remove( shift+5, key, result);
          if (node == o){
            return this; // nothing got removed
          } else {
            if (node == null){
              return cloneWithoutNodeOrValue(idx);
            } else {
              return cloneWithReplacedNodeOrValue(idx, node);
            }
          }
        }
        
      } else { // remove value
        V currentValue = (V)o;
        if (isAtLeafLevel){
          result.changedValueCount = true;
          result.oldValue = currentValue;
          return cloneWithoutNodeOrValue(idx);
        } else {
          return this; // nothing to remove
        }
      }
    }

    public void process(Processor<V> proc) {
      if (value != null){
        proc.process(value);
      }
      
      for (int i=0; i<32; i++){
        Object o=nodesOrValues[i];
        if (o instanceof Node){
          ((Node<V>)o).process(proc);
        } else {
          proc.process( (V)o);
        }
      }
    }
    
    public void process (int levelDistance, int pos, Processor<V> proc, CountDown countDown){
      Object[] nv = nodesOrValues;
      
      if (levelDistance == 0){
        Object o=nv[pos];
        if (o instanceof Node){
          Node<V> node = (Node<V>)o;
          if (node.value != null){
            proc.process(node.value);
            countDown.dec();
          }
        } else {
          proc.process((V)o);
          countDown.dec();
        }
      } else {
        for (int i=0; i<32; i++){
          Object o = nv[i];
          if (o instanceof Node){
            Node<V> node = (Node<V>)o;
            node.process(levelDistance-1, pos, proc, countDown);
          }
        }        
      }
    }
  }

  //--- static ImmutableObjectTable methods
  
  static <V> Node<V> createNode (int shift, int key, V value, V nodeValue){
    int ks = key >>> shift;
    int idx = (ks & 0x01f);
    boolean isAtLeafLevel = (idx == ks);
    
    Object o;
    if (isAtLeafLevel){
      o = value;
    } else {
      o = createNode( shift+5, key, value, (V)null);
    }
    
    return new OneNode<V>( nodeValue, idx, o);
  }

  
  //--- invariant instance data
  final int size;
  final Node<V> root;
  final Result<V> result;
  
  public ImmutableObjectTable(){
    size = 0;
    root = null;
    result = null;
  }

  private ImmutableObjectTable (int size, Node<V> root, Result<V> result){
    this.size = size;
    this.root = root;
    this.result = result;
  }

  public ImmutableObjectTable<V> set (int key, V value){
    Result<V> result = new Result<V>();
    
    if (root == null){
      result.changedValueCount = true;
      return new ImmutableObjectTable<V>( 1, createNode( 0, key, value, null), result);

    } else {
      Node<V> newRoot = root.assoc( 0, key, value, result);

      if (root == newRoot){ // key and value were already there
        return this;
      } else { // could have been a replaced value that didn't change the size
        if (result.changedValueCount){
          return new ImmutableObjectTable<V>( size+1, newRoot, result);
        } else {
          return new ImmutableObjectTable<V>( size, newRoot, result);
        }
      }
    }
  }
  
  public V get (int key){
    if (root != null){
      return root.find(0, key);
    } else {
      return null;
    }
  }
  
  public ImmutableObjectTable<V> remove(int key){
    if (root == null){
      return this;
      
    } else {
      Result<V> result = new Result<V>();
      Node<V> newRoot = root.remove( 0, key, result);

      if (root == newRoot){ // nothing removed
        return this;
      } else {
        return new ImmutableObjectTable<V>( size-1, newRoot, result);
      }
    }
  }
  
  public int size(){
    return size;
  }
  
  public boolean isEmpty(){
    return size==0;
  }
  
  public boolean hasChangedSize(){
    return (result != null) && (result.changedValueCount);
  }
  
  public V getOldValue(){
    if (result != null){
      return result.oldValue;
    } else {
      return null;
    }
  }
  
  // iterators would require a snapshot (list), which kind of defeats the purpose
  
  public void process (Processor<V> processor){
    if (root != null){
      root.process(processor);
    }
  }
  
  public void processInKeyOrder (Processor<V> processor){
    if (root != null){
      CountDown countDown = new CountDown(size);
      
      try {
        for (int level = 0; level < 7; level++) {
          for (int i = 0; i < 32; i++) {
            root.process(level, i, processor, countDown);
          }
        }
      } catch (CountDown c){
        // nothing, we just want to terminate the iterations
      }
    }
  }
}
