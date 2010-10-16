//
// Copyright (C) 2006 United States Government as represented by the
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
package gov.nasa.jpf.jvm;

import gov.nasa.jpf.JPFListener;


/**
 * helper class to avoid indirection if there is just one observer
 * (the usual case). Typical 'Container' pattern implementation
 */
public class VMListenerMulticaster implements VMListener
{
  VMListener head;
  VMListener tail;

  public static VMListener add (VMListener oldListener, VMListener newListener) {
    if (newListener == null) {
      return oldListener;
    }
    if (oldListener == null) {
      return newListener;
    }
    if (newListener == oldListener) {
      return oldListener;
    }
    
    if (oldListener instanceof VMListenerMulticaster) {
      // filter out multiple registrations
      VMListenerMulticaster c = (VMListenerMulticaster)oldListener;
      while (c != null) {
        if (c.tail == newListener) {
          return oldListener;
        }
        if (c.head == newListener) {
          return oldListener;
        } else {
          if (c.head instanceof VMListenerMulticaster) {
            c = (VMListenerMulticaster) c.head;
          } else {
            break;
          }
        }
      }
    }
    
    return new VMListenerMulticaster(oldListener, newListener);
  }

  public static boolean containsType (VMListener listener, Class<?> type) {
    if (listener == null) {
      return false;
    }
    
    if (listener instanceof VMListenerMulticaster) {
      VMListenerMulticaster l = (VMListenerMulticaster)listener;
      while (l != null) {
        if (type.isAssignableFrom(l.tail.getClass())) {
          return true;
        }
        if (l.head instanceof VMListenerMulticaster) {
          l = (VMListenerMulticaster)l.head;          
        } else {
          return type.isAssignableFrom(l.head.getClass()); 
        }
      }
      return false;
      
    } else {
      return type.isAssignableFrom(listener.getClass());
    }
  }
  
  public static VMListener remove (VMListener oldListener, VMListener removeListener){
    if (oldListener == removeListener) {
      return null;
    }
    if (oldListener instanceof VMListenerMulticaster){
      return ((VMListenerMulticaster)oldListener).remove( removeListener);
    }
    
    return oldListener;
  }
  
  protected VMListener remove (VMListener listener) {
    if (listener == head) {
      return tail;
    }
    if (listener == tail){
      return head;
    }
    
    VMListenerMulticaster h,t;
    if (head instanceof VMListenerMulticaster) {
      h = (VMListenerMulticaster)head;
      if (tail instanceof VMListenerMulticaster){
        t = (VMListenerMulticaster)tail;
        return new VMListenerMulticaster( h.remove(listener),t.remove(listener));
      } else {
        return new VMListenerMulticaster( h.remove(listener), tail);
      }
    } else if (tail instanceof VMListenerMulticaster) {
      t = (VMListenerMulticaster)tail;      
      return new VMListenerMulticaster( head, t.remove(listener));
    }
    
    return this;
  }

  
  public VMListenerMulticaster (VMListener h, VMListener t) {
    head = h;
    tail = t;
  }

  public void executeInstruction (JVM vm) {
    head.executeInstruction( vm);
    tail.executeInstruction( vm);
  }
  
  public void instructionExecuted (JVM vm) {
    head.instructionExecuted(vm);
    tail.instructionExecuted(vm);
  }
  
  public void threadStarted (JVM vm) {
    head.threadStarted(vm);
    tail.threadStarted(vm);
  }

  public void threadWaiting (JVM vm) {
    head.threadWaiting(vm);
    tail.threadWaiting(vm);    
  }

  public void threadNotified (JVM vm) {
    head.threadNotified(vm);
    tail.threadNotified(vm);    
  }

  public void threadInterrupted (JVM vm) {
    head.threadInterrupted(vm);
    tail.threadInterrupted(vm);    
  }

  public void threadBlocked (JVM vm) {
    head.threadBlocked(vm);
    tail.threadBlocked(vm);    
  }  
  
  public void threadTerminated (JVM vm) {
    head.threadTerminated(vm);
    tail.threadTerminated(vm);
  }

  public void threadScheduled (JVM vm) {
    head.threadScheduled(vm);
    tail.threadScheduled(vm);
  }
  
  public void classLoaded (JVM vm) {
    head.classLoaded(vm);
    tail.classLoaded(vm);
  }
  
  public void objectCreated (JVM vm) {
    head.objectCreated(vm);
    tail.objectCreated(vm);
  }
  
  public void objectReleased (JVM vm) {
    head.objectReleased(vm);
    tail.objectReleased(vm);
  }

  public void gcBegin (JVM vm) {
    head.gcBegin(vm);
    tail.gcBegin(vm);
  }

  public void gcEnd (JVM vm) {
    head.gcEnd(vm);
    tail.gcEnd(vm);
  }
  
  public void exceptionThrown (JVM vm) {
    head.exceptionThrown(vm);
    tail.exceptionThrown(vm);
  }

  public void exceptionBailout (JVM vm) {
    head.exceptionBailout(vm);
    tail.exceptionBailout(vm);
  }

  public void exceptionHandled (JVM vm) {
    head.exceptionHandled(vm);
    tail.exceptionHandled(vm);
  }

  public void objectLocked (JVM vm) {
    head.objectLocked(vm);
    tail.objectLocked(vm); 
  }

  public void objectUnlocked (JVM vm) {
    head.objectUnlocked(vm);
    tail.objectUnlocked(vm);    
  }

  public void objectWait (JVM vm) {
    head.objectWait(vm);
    tail.objectWait(vm);
  }

  public void objectNotify (JVM vm) {
    head.objectNotify(vm);
    tail.objectNotify(vm);    
  }

  public void objectNotifyAll (JVM vm) {
    head.objectNotifyAll(vm);
    tail.objectNotifyAll(vm);    
  }

  public void choiceGeneratorRegistered (JVM vm) {
    head.choiceGeneratorRegistered(vm);
    tail.choiceGeneratorRegistered(vm);
  }

  public void choiceGeneratorSet (JVM vm) {
    head.choiceGeneratorSet(vm);
    tail.choiceGeneratorSet(vm);        
  }

  public void choiceGeneratorAdvanced (JVM vm) {
    head.choiceGeneratorAdvanced(vm);
    tail.choiceGeneratorAdvanced(vm);    
  }

  public void choiceGeneratorProcessed (JVM vm) {
    head.choiceGeneratorProcessed(vm);
    tail.choiceGeneratorProcessed(vm);    
  }

  public void methodEntered (JVM vm) {
    head.methodEntered(vm);
    tail.methodEntered(vm);
  }

  public void methodExited (JVM vm) {
    head.methodExited(vm);
    tail.methodExited(vm);
  }

}

