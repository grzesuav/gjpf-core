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
public interface PersistentIntMap<V> {
  
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
    
    protected int changeCount;
    
    protected void finalize(){
      synchronized (Result.class){
        changeCount = 0;
        cachedResult = this;
      }
    }

    public void clear(){
      changeCount = 0;
    }
    
    void replacedValue(V oldValue) {}
    
    void addedValue(V newValue) {
      changeCount++;
    }
    
    void removedValue(V oldValue) {
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

    public void clear(){
      changeCount = 0;
      changedValue = null;
    }
        
    void replacedValue(V oldValue) {
      changedValue = oldValue;
    }
    
    void addedValue(V newValue) {
      changeCount++;
      changedValue = ObjectList.add(changedValue, newValue);
    }
    
    void removedValue(V oldValue) {
      changeCount--;
      changedValue = ObjectList.add( changedValue, oldValue);
    }
    
    public int getSizeChange(){
      return changeCount;
    }
    
    public boolean hasChangedSize(){
      return changeCount != 0;
    }
    
    public V getChanged(){
      return (V) ObjectList.getFirst(changedValue);
    }
    
    public Iterator<V> getAllChanged (){
      return (Iterator<V>)ObjectList.iterator(changedValue);
    }    
  }
  
  //--- these are our primary operations
  V get (int key);
  
  PersistentIntMap<V> set (int key, V value);
  PersistentIntMap<V> set (int key, V value, Result<V> result);  
  
  PersistentIntMap<V> remove (int key);
  PersistentIntMap<V> remove (int key, Result<V> result);
  
  PersistentIntMap<V> removeAllSatisfying (Predicate<V> predicate);
  PersistentIntMap<V> removeAllSatisfying (Predicate<V> predicate, Result<V> result);

  void process (Processor<V> processor);
  void processInKeyOrder (Processor<V> processor);


  void printOn (PrintStream ps);

  boolean isEmpty();
  int size();
}
