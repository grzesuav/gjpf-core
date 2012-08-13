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
  
  static final int DONT_CARE = 0;
  
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
  
  //--- these are our primary operations
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

  public abstract boolean isEmpty();
  public abstract int size();
}
