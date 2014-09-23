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

package gov.nasa.jpf.vm;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.util.InstructionState;

/**
 * peer class for java.util.concurrent.Exchanger
 */
public class JPF_java_util_concurrent_Exchanger extends NativePeer {

  static class ExchangeState extends InstructionState {
    int exchangeRef; // has to be a ref because the ElementInfo is modified
    boolean isWaiter;

    static ExchangeState createWaiterState (MJIEnv env, int exchangeRef){
      ExchangeState s = new ExchangeState();
      
      s.exchangeRef = exchangeRef;
      s.isWaiter = true;
      return s;
    }
    
    static ExchangeState createResponderState (MJIEnv env, int exchangeRef){
      ExchangeState s = new ExchangeState();
      
      s.exchangeRef = exchangeRef;
      s.isWaiter = false;
      return s;      
    }
  }
  
  ElementInfo createExchangeObject (MJIEnv env, int waiterDataRef){
    ElementInfo ei = env.newElementInfo("java.util.concurrent.Exchanger$Exchange");
    ei.setReferenceField("waiterData", waiterDataRef);
    ei.setReferenceField("waiterThread", env.getThreadInfo().getThreadObjectRef());
    return ei;
  }
  
  private int repeatInvocation (MJIEnv env, StackFrame frame, ElementInfo exchange, ExchangeState state){
    frame.addFrameAttr(state);
    env.registerPinDown(exchange);
    env.repeatInvocation();
    return MJIEnv.NULL;
  }

  private long computeTimeout (MJIEnv env, int timeUnitRef, long to){
    if (timeUnitRef == MJIEnv.NULL){
      return 0;
    } else {
      // <2do> this has to do proper conversion of timeunits to millis
      return to;
    }
  }
  
  //--- native methods
  
  @MJI
  public int exchange__Ljava_lang_Object_2__Ljava_lang_Object_2 (MJIEnv env, int objRef, int dataRef){
    return exchange__Ljava_lang_Object_2JLjava_util_concurrent_TimeUnit_2__Ljava_lang_Object_2(
              env, objRef, dataRef, 0L, MJIEnv.NULL);
  }
    
  @MJI
  public int exchange__Ljava_lang_Object_2JLjava_util_concurrent_TimeUnit_2__Ljava_lang_Object_2 
              (MJIEnv env, int objRef, int dataRef, long timeout, int timeUnitRef){
    ThreadInfo ti = env.getThreadInfo();
    StackFrame frame = ti.getModifiableTopFrame();
    ExchangeState state = frame.getFrameAttr(ExchangeState.class);
    
    if (state == null){ // first time for waiter or responder
      int eRef = env.getReferenceField(objRef, "exchange");
      
      if (eRef == MJIEnv.NULL){ // first waiter, this has to block
        ElementInfo ei = createExchangeObject(env, dataRef);
        eRef = ei.getObjectRef();
        env.setReferenceField(objRef, "exchange", eRef);
        
        timeout = computeTimeout(env, timeUnitRef, timeout);
        
        ei.wait(ti, timeout, false);
        
        if (ti.getScheduler().setsWaitCG(ti, timeout)) {
          return repeatInvocation(env, frame, ei, ExchangeState.createWaiterState(env, eRef));
        } else {
          throw new JPFException("blocked exchange() waiter without transition break");
        }
        
      } else { // first responder (can reschedule)
        ElementInfo ei = ti.getModifiableElementInfo(eRef);        
        ei.setReferenceField("responderData", dataRef);
        state = ExchangeState.createResponderState(env, eRef);
        
        if (ei.getBooleanField("waiterTimedOut")){
          // depending on own timeout, this might deadlock because the waiter already timed out 
          ei.wait(ti, timeout, false);

          if (ti.getScheduler().setsWaitCG(ti, timeout)) {
            return repeatInvocation(env, frame, ei, state);
          } else {
            throw new JPFException("blocked exchange() responder without transition break");
          }          
        }

        // if we get here, the waiter is still waiting and we return right away
                
        ei.notifies(env.getSystemState(), ti, false); // this changes the tiWaiter status
        env.setReferenceField(objRef, "exchange", MJIEnv.NULL); // re-arm Exchange object
                
        if (ti.getScheduler().setsNotifyCG(ti)){
          return repeatInvocation(env, frame, ei, state);
        }
        
        return ei.getReferenceField("waiterData");
      }
      
    } else { // re-execution(s)
      ElementInfo ei = env.getElementInfo(state.exchangeRef);

      int retRef = MJIEnv.NULL;
      
      if (ti.isInterrupted(true)) {
        env.throwException("java.lang.InterruptedException");

      } else if (ti.isTimedOut()){
        if (state.isWaiter){
          ei = ei.getModifiableInstance();
          ei.setBooleanField("waiterTimedOut", true);
        }
        env.throwException("java.util.concurrent.TimeoutException");
        
      } else {
        retRef = ei.getReferenceField( state.isWaiter ? "responderData" : "waiterData");
      }
      
      //-- processed
      frame.removeFrameAttr(state);
      ei = ei.getModifiableInstance();
      env.releasePinDown(ei);
      return retRef;
    }
  }
}
