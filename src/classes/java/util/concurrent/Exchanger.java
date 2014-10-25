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

package java.util.concurrent;

/**
 * model class for java.util.concurrent.Exchanger
 * We model because the original class goes to great lengths implementing
 * memory based synchronization, using execution time and spins
 * 
 * Exchangers are also per se shared objects, so we want to minimize field
 * access from bytecode
 */
public class Exchanger<V> {
  
  // created on native side and pinned down until transaction is complete
  static class Exchange<T> {
    Thread waiterThread;
    boolean waiterTimedOut;
    
    T waiterData;
    T responderData;
  }
  
  //-- only accessed from native methods
  private Exchange<V> exchange;
  
  
  public native V exchange(V value) throws InterruptedException;

  private native V exchange0 (V value, long timeoutMillis) throws InterruptedException, TimeoutException;
  
  // unfortunately we can't directly go native here without duplicating the TimeUnit conversion in the peer
  public V exchange(V value, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
    long to = unit.convert(timeout,TimeUnit.MILLISECONDS);
    return exchange0( value, to);
  }
}
