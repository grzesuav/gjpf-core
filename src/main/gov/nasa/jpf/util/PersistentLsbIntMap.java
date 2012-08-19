package gov.nasa.jpf.util;

import gov.nasa.jpf.util.PersistentIntMap.Node;
import gov.nasa.jpf.util.PersistentIntMap.Result;

import java.io.PrintStream;

public class PersistentLsbIntMap<V> extends PersistentIntMap<V> {

  static <V> Node<V> createNode (int shift, int key, V value, V nodeValue, Result<V> result){ // <refactor>
    int k = ( key >>> shift);
    int idx = k & 0x1f;
    Node<V> node;
    
    if (k < 32) {
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
      node = createNode( shift+5, key, value, nodeValue, result);
      node = new OneNode<V>(idx, node);
    }
    
    return node;
  }
    
  public PersistentLsbIntMap() {
    // nothing
  }
  
  protected PersistentLsbIntMap (int size, Node<V> root) {
    super( size, root);
  }

  //--- set(key,val)
  
  protected Node<V> assoc (int shift, int key, V value, Node<V> node, Result<V> result){
    int k = key >>> shift;
    int levelIdx = k & 0x1f;
    Object o = node.getElement(levelIdx);

    if (o != null){                       // we already have a node or value for this index

      if (o instanceof Node){             // we had a node for this index
        Node<V> replacedNode;
        if (k < 32){                      // at value level
          replacedNode = assocNodeValue( node, value, result);
        } else {                          // not at value level yet, recurse
          replacedNode = assoc( shift+5, key, value, (Node<V>)o, result);
        }
        if (replacedNode == o){           // nothing changed
          return node;
        } else {                          // element got replaced, clone the node
          return node.cloneWithReplacedElement( levelIdx, replacedNode);
        }        

      } else {                            // we had a value for this index
        if (k < 32){                      // at value level
          if (value == o){                // nothing changed
            return node;
          } else {                        // clone with updated value
            node = node.cloneWithReplacedElement( levelIdx, value);
            result.replacedValue( node, (V)o);
            return node;
          }

        } else {                          // not at value level yet
          Node<V> addedNode = createNode( shift+5, key, value, (V)o, result);
          return node.cloneWithReplacedElement( levelIdx, addedNode);
        }
      }

    } else { // nothing for this index yet
      if (k < 32){                        // at value level
        node = node.cloneWithAddedElement( levelIdx, value);
        result.addedValue( node, value);

      } else {                            // not at value level yet
        Node<V> addedNode = createNode( shift+5, key, value, null, result);
        node = node.cloneWithAddedElement( levelIdx, addedNode);
      }
      return node;
    }
  }

  @Override
  public PersistentIntMap<V> set(int key, V value, Result<V> result) {
    result.clear();
        
    if (root == null){
      Node<V> newRoot = createNode( 0, key, value, null, result);
      return new PersistentLsbIntMap<V>( 1, newRoot);

    } else {            
      Node<V> newRoot = assoc( 0, key, value, root, result);
      
      if (root == newRoot){ // key and value were already there
        return this;
      } else { // could have been a replaced value that didn't change the size
        int newSize = size + result.getSizeChange();
        return new PersistentLsbIntMap<V>( newSize, newRoot);
      }
    }
  }


  //--- get(key)
  
  @Override
  public V get(int key) {  // <refactor>
    if (root != null) {
      int shift = 0;
      Node<V> node = root;

      for (;;) {
        int k = key >>> shift;
        int levelIdx = k & 0x1f;
        Object o = node.getElement(levelIdx);
        if (o != null) { // do we have something for this index
          if (o instanceof Node) { // we have a node
            node = (Node<V>) o;
            if (k < 32) { // at value level
              return node.getNodeValue();
            } else { // shift to next level (tail recursion)
              shift += 5;
              continue;
            }

          } else { // we have a value
            if (k < 32) { // at value level
              return (V) o;
            } else {
              return null; // can't go down, so it's not there
            }
          }

        } else { // nothing for this index
          return null;
        }
      }

    } else { // no root
      return null;
    }
  }
  
  //--- remove(key)
  
  protected Node<V> remove (int shift, int key, Node<V> node, Result<V> result){
    int k = (key >>> shift);
    int levelIdx = k & 0x1f;
    Object o = node.getElement(levelIdx);

    if (o != null){                        // we got something for this index
      if (o instanceof Node){              // we've got a node
        Node<V> newNodeElement;
        if (k < 32){          // at value level
          newNodeElement = removeNodeValue( (Node<V>)o, result);
        } else {                           // not yet at value level, recurse downwards
          newNodeElement = remove(shift+5, key, (Node<V>)o, result);
        }
        if (newNodeElement == null){       // nothing left
          return node.cloneWithoutElement( levelIdx);
        } else if (newNodeElement == o){   // nothing changed
          return node;
        } else {
          return node.cloneWithReplacedElement( levelIdx, newNodeElement);
        }

      } else {                             // we've got a value
        if (k < 32){          // at value level
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
  public PersistentIntMap<V> remove(int key, Result<V> result) {
    if (root == null){
      return this;
      
    } else {
      result.clear();
      
      Node<V> newRoot = remove( 0, key, root, result);

      if (root == newRoot){ // nothing removed
        return this;
      } else {
        return new PersistentLsbIntMap<V>( size-1, newRoot);
      }
    }
  }

  //--- removeAll
  
  @Override
  protected Node<V> removeAllSatisfying( Node<V> node, Predicate<V> predicate, Result<V> result) {
    return node.removeAllSatisfying( this, predicate, result);
  }

  @Override
  public PersistentIntMap<V> removeAllSatisfying(Predicate<V> predicate, Result<V> result) {
    if (root != null){
      result.clear();
      Node<V> newRoot = (Node<V>) root.removeAllSatisfying( this, predicate, result);
      
      return new PersistentLsbIntMap<V>( size+result.getSizeChange(), newRoot);
      
    } else {
      return this;
    }
  }

}
