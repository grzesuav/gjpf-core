//
// Copyright (C) 2014 United States Government as represented by the
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
package java.lang;

import gov.nasa.jpf.annotation.NeverBreak;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * model of java.lang.ThreadLocal, which avoids global shared objects
 * that can otherwise considerably contribute to the state space
 */
public class ThreadLocal<T> {

  static class Entry<E> extends WeakReference<ThreadLocal<E>> {
    @NeverBreak
    E val;
    
    Entry (ThreadLocal<E> key, E val){
      super(key);
      this.val = val;
    }
    
    Entry<E> getChildEntry (){
      ThreadLocal<E> loc = get();
      if (loc instanceof InheritableThreadLocal){
        return new Entry<E>( loc, ((InheritableThreadLocal<E>)loc).childValue(val));
      } else {
        return null;
      }
    }
  }
  
  public ThreadLocal() {
  }
  
  /**
   * override to provide initial value 
   */
  protected T initialValue() {
    return null;
  }
    
  private native Entry<T> getEntry();
  private native void addEntry (Entry<T> e);
  private native void removeEntry (Entry<T> e);
  
  public T get() {
    Entry<T> e = getEntry();
    
    if (e == null){
      T v = initialValue();
      e = new Entry<T>(this, v);
      addEntry(e);
    }
    
    return e.val;
  }
  
  public void set (T v){
    Entry<T> e = getEntry();
    
    if (e != null){
      e.val = v;
      
    } else {
      e = new Entry<T>(this, v);
      addEntry(e);      
    }
  }
  
  public void remove(){
    Entry<T> e = getEntry();
    if (e != null){
      removeEntry(e);
    }
  }

  
  // Java 8 provides this as an internal type to be used from lib classes
  // ?? why is this not done with overridden initialValue() within the concrete ThreadLocal class
  static final class SuppliedThreadLocal<E> extends ThreadLocal<E> {

    // we need to preserve the modifiers since this might introduce races (supplier could be shared)
    private final Supplier<? extends E> sup;

    SuppliedThreadLocal(Supplier<? extends E> supplier) {
      sup = Objects.requireNonNull(supplier);
    }

    @Override
    protected E initialValue() {
      return sup.get();
    }
  }
}
