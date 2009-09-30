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

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.bytecode.RUNSTART;


/**
 * MJI NativePeer class for java.lang.Thread library abstraction
 */
public class JPF_java_lang_Thread {

  public static boolean isAlive____Z (MJIEnv env, int objref) {
    return getThreadInfo(env, objref).isAlive();
  }

  public static void setDaemon0__Z__V (MJIEnv env, int objref, boolean isDaemon) {
    ThreadInfo ti = getThreadInfo(env, objref);
    ti.setDaemon(isDaemon);
  }


  public static void setName0__Ljava_lang_String_2__V (MJIEnv env, int objref, int nameRef) {
    // it bails if you try to set a null name
    if (nameRef == -1) {
      env.throwException("java.lang.IllegalArgumentException");

      return;
    }

    // we have to intercept this to cache the name as a Java object
    // (to be stored in ThreadData)
    // luckily enough, it's copied into the java.lang.Thread object
    // as a char[], i.e. does not have to preserve identity
    // Note the nastiness in here - the java.lang.Thread object is only used
    // to get the initial values into ThreadData, and gets inconsistent
    // if this method is called (just works because the 'name' field is only
    // directly accessed from within the Thread ctors)
    ThreadInfo ti = getThreadInfo(env, objref);
    ti.setName(env.getStringObject(nameRef));
  }

  public static void setPriority0__I__V (MJIEnv env, int objref, int prio) {
    // again, we have to cache this in ThreadData for performance reasons
    ThreadInfo ti = getThreadInfo(env, objref);
    ti.setPriority(prio);
  }

  public static int countStackFrames____I (MJIEnv env, int objref) {
    return getThreadInfo(env, objref).countStackFrames();
  }

  public static int currentThread____Ljava_lang_Thread_2 (MJIEnv env, int clsObjRef) {
    ThreadInfo ti = env.getThreadInfo();

    return ti.getThreadObjectRef();
  }

  public static boolean holdsLock__Ljava_lang_Object_2__Z (MJIEnv env, int clsObjRef, int objref) {
    ThreadInfo  ti = env.getThreadInfo();
    ElementInfo ei = env.getElementInfo(objref);

    return ei.isLockedBy(ti);
  }

  /**
   * This method is the common initializer for all Thread ctors, and the only
   * single location where we can init our ThreadInfo, but it is PRIVATE
   */

  // wow, that's almost like C++
  public static void init0__Ljava_lang_ThreadGroup_2Ljava_lang_Runnable_2Ljava_lang_String_2J__V (MJIEnv env,
                                                                                                  int objref,
                                                                                                  int rGroup,
                                                                                                  int rRunnable,
                                                                                                  int rName,
                                                                                                  long stackSize) {
    ThreadInfo newThread = createThreadInfo(env, objref);
    newThread.init(rGroup, rRunnable, rName, stackSize, true);
  }

  public static void interrupt____V (MJIEnv env, int objref) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();
    ChoiceGenerator<?> cg = ss.getChoiceGenerator();

    ThreadInfo interruptedThread = getThreadInfo( env, objref);

    if (!ti.isFirstStepInsn()) { // first time we see this (may be the only time)
      interruptedThread.interrupt();
    } else {
      cg = ss.getSchedulerFactory().createInterruptCG( interruptedThread);
      if (cg != null) {
        ss.setNextChoiceGenerator(cg);
        env.repeatInvocation();
      }
    }
  }

  // these could be in the model, but we keep it symmetric, which also saves
  // us the effort of avoiding unwanted shared object field access CGs
  public static boolean isInterrupted____Z (MJIEnv env, int objref) {
    ThreadInfo ti = getThreadInfo( env, objref);
    return ti.isInterrupted(false);
  }

  public static boolean interrupted____Z (MJIEnv env, int clsObjRef) {
    ThreadInfo ti = env.getThreadInfo();
    return ti.isInterrupted(true);
  }


  public static void start____V (MJIEnv env, int objref) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();
    ChoiceGenerator<?> cg = ss.getChoiceGenerator();
    JVM vm = ti.getVM();

    if (!ti.isFirstStepInsn()) { // first time we see this (may be the only time)

      ThreadInfo newThread = getThreadInfo(env, objref);
      // check if this thread was already started. If it's still running, this
      // is a IllegalThreadStateException. If it already terminated, it just gets
      // silently ignored in Java 1.4, but the 1.5 spec explicitly marks this
      // as illegal, so we adopt this by throwing an IllegalThreadState, too
      if (newThread.getState() != ThreadInfo.State.NEW) {
        env.throwException("java.lang.IllegalThreadStateException");
        return;
      }

      // Outch - that's bad. we have to dig this out from the innards
      // of the java.lang.Thread class
      int target = newThread.getTarget();

      if (target == -1) {
        // note that we don't set the 'target' field, since java.lang.Thread doesn't
        target = objref;
      }

      // better late than never
      newThread.setTarget(target);

      // we don't do this during thread creation because the thread isn't in
      // the GC root set before it actually starts to execute. Until then,
      // it's just an ordinary object

      vm.notifyThreadStarted(newThread);

      ElementInfo ei = env.getElementInfo(target);
      ClassInfo   ci = ei.getClassInfo();
      MethodInfo  run = ci.getMethod("run()V", true);

      StackFrame runFrame = new StackFrame(run,target);
      // the first insn should be our own, to prevent confusion with potential
      // CGs of the first insn in run() (e.g. Verify.getXX()) - we just support
      // one CG per insn.
      // the RUNSTART will also do the locking if the newThread has a sync run()
      runFrame.setPC(new RUNSTART(run));
      newThread.pushFrame(runFrame);

      if (run.isSynchronized()) {
        if (!ei.canLock(newThread)){
          ei.block(newThread);
        } else {
          ei.registerLockContender(newThread);
        }
      }

      if (!newThread.isBlocked()){
        newThread.setState( ThreadInfo.State.RUNNING);
      }

      // <2do> now that we have another runnable, we should re-compute
      // reachability so that subsequent potential breaks work correctly
      if (newThread.usePor()){ // means we use on-the-fly POR
        //env.getSystemState().activateGC();
        env.getDynamicArea().analyzeHeap(false); // sledgehammer mark
      }

      // now we have a new thread, create a CG for scheduling it
      cg = ss.getSchedulerFactory().createThreadStartCG( newThread);
      if (cg != null) {
        ss.setNextChoiceGenerator(cg);
        env.repeatInvocation();
      }
    }
  }

  public static void yield____V (MJIEnv env, int clsObjRef) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();
    ChoiceGenerator<?> cg = ss.getChoiceGenerator();

    if (!ti.isFirstStepInsn()) { // first time we see this (may be the only time)
      cg = ss.getSchedulerFactory().createThreadYieldCG( ti);
      if (cg != null) {
        ss.setNextChoiceGenerator(cg);
        env.repeatInvocation();
      }
    } else {
      // nothing to do, this was just a forced reschedule
    }
  }

  public static void sleep__JI__V (MJIEnv env, int clsObjRef, long millis, int nanos) {
    ThreadInfo ti = env.getThreadInfo();
    SystemState ss = env.getSystemState();
    ChoiceGenerator<?> cg = ss.getChoiceGenerator();

    if (!ti.isFirstStepInsn()) { // first time we see this (may be the only time)
      cg = ss.getSchedulerFactory().createThreadSleepCG( ti, millis, nanos);
      if (cg != null) {
        ss.setNextChoiceGenerator(cg);
        env.repeatInvocation();

        ti.setSleeping();
      }
    } else {
      // we don't set it RUNNING again here because we want all other scheduling
      // choices in this CG to see this thread as sleeping - not just the ones
      // that are scheduled before this one
    }
  }

  public static void suspend____ (MJIEnv env, int threadObjRef) {
    ThreadInfo operator = env.getThreadInfo();
    ThreadInfo target = getThreadInfo(env, threadObjRef);
    SystemState ss = env.getSystemState();

    // The first time through here, we need to let other threads (if any) have a chance to call suspend and resume.  Also, need to let the target thread have a chance to do some work.
    // The second time through here, we need to suspend the target thread and remove it from execution (if transitioned from resumed to suspended).

    if (target.isTerminated()) {
      return;
    }

    if (!operator.isFirstStepInsn()) {
      ChoiceGenerator cg = ss.getSchedulerFactory().createThreadSuspendCG();
      if (cg != null) {
        ss.setNextChoiceGenerator(cg);
        env.repeatInvocation();
        return;
      }
    }

    if (target.suspend()) {  // No sense in adding a CG if the thread didn't transition from suspended to resumed.
      ChoiceGenerator cg = ss.getSchedulerFactory().createThreadSuspendCG();
      if (cg != null) {
        ss.setNextChoiceGenerator(cg);
      }
    }
  }

  public static void resume____ (MJIEnv env, int threadObjRef) {
    ThreadInfo operator = env.getThreadInfo();
    ThreadInfo target = getThreadInfo(env, threadObjRef);
    SystemState ss = env.getSystemState();

    assert operator != target : "A thread is calling resume on itself when it should have been suspended!";

    // The first time through here, we need to let other threads (if any) have a chance to call suspend and resume.
    // The second time through here, we need to get the target thread scheduled for execution (if transitioned from suspended to resumed).

    if (target.isTerminated()) {
      return;
    }

    if (!operator.isFirstStepInsn()) {
      ChoiceGenerator cg = ss.getSchedulerFactory().createThreadResumeCG();
      if (cg != null) {
        ss.setNextChoiceGenerator(cg);
        env.repeatInvocation();
        return;
      }
    }

    if (target.resume()) {  // No sense in adding a CG if the thread didn't transition from suspended to resumed.
      ChoiceGenerator cg = ss.getSchedulerFactory().createThreadResumeCG();
      if (cg != null) {
        ss.setNextChoiceGenerator(cg);
      }
    }
  }

  public static long getId____J (MJIEnv env, int objref) {
    // doc says it only has to be valid and unique during lifetime of thread, hence we just use
    // the ThreadList index
    ThreadInfo ti = getThreadInfo(env, objref);
    return ti.getIndex();
  }

  public static int getState0____I (MJIEnv env, int objref) {
    // return the state index with respect to one of the public Thread.States
    ThreadInfo ti = getThreadInfo(env, objref);

    switch (ti.getState()) {
    case NEW:             return 1;
    case RUNNING:         return 2;
    case BLOCKED:         return 0;
    case UNBLOCKED:       return 2;
    case WAITING:         return 5;
    case TIMEOUT_WAITING: return 4;
    case SLEEPING:        return 4;
    case NOTIFIED:        return 2;
    case INTERRUPTED:     return 2;
    case TIMEDOUT:        return 2;
    case TERMINATED:      return 3;
    default:
      throw new JPFException("illegal thread state: " + ti.getState());
    }
  }

  // it's synchronized
  /*
  public static void join__ (MJIEnv env, int objref) {
    ThreadInfo ti = getThreadInfo(env,objref);

    if (ti.isAlive()) {
      env.wait(objref);
    }
  }
   */

  protected static ThreadInfo createThreadInfo (MJIEnv env, int objref) {
    return ThreadInfo.createThreadInfo(env.getVM(), objref);
  }

  static ThreadInfo getThreadInfo (MJIEnv env, int objref) {
    return ThreadInfo.getThreadInfo(env.getVM(), objref);
  }

}
