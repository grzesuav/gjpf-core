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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.bytecode.EXECUTENATIVE;
import gov.nasa.jpf.jvm.bytecode.INVOKESTATIC;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.jvm.choice.BreakGenerator;
import gov.nasa.jpf.jvm.choice.ThreadChoiceFromSet;
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.SparseObjVector;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Represents a thread. It contains the state of the thread, static
 * information about the thread, and the stack frames.
 * Race detection and lock order also store some information
 * in this data structure.
 *
 * Note that we preserve identities according to their associated java.lang.Thread object
 * (threadData.objref). This esp. means along the same path, a ThreadInfo reference
 * is kept invariant
 */
public class ThreadInfo
     implements Iterable<StackFrame>, Comparable<ThreadInfo>, Cloneable, Restorable<ThreadInfo> {

  static Logger log = JPF.getLogger("gov.nasa.jpf.jvm.ThreadInfo");

  //--- our internal thread states
  public enum State {
    NEW,  // means created but not yet started
    RUNNING,
    BLOCKED,  // waiting to acquire a lock
    UNBLOCKED,  // was BLOCKED but can acquire the lock now
    WAITING,  // waiting to be notified
    TIMEOUT_WAITING,
    NOTIFIED,  // was WAITING and got notified, but is still blocked
    INTERRUPTED,  // was WAITING and got interrupted
    TIMEDOUT,  // was TIMEOUT_WAITING and timed out
    TERMINATED,
    SLEEPING
  };


  static final int[] emptyRefArray = new int[0];

  static ThreadInfo currentThread;
  static ThreadInfo mainThread;


  protected class StackIterator implements Iterator<StackFrame> {
    StackFrame frame = top;

    public boolean hasNext() {
      return frame != null;
    }

    public StackFrame next() {
      if (frame != null){
        StackFrame ret = frame;
        frame = frame.getPrevious();
        return ret;

      } else {
        throw new NoSuchElementException();
      }
    }

    public void remove() {
      throw new UnsupportedOperationException("can't remove StackFrames");
    }
  }

  protected class InvokedStackIterator extends StackIterator implements Iterator<StackFrame> {
    InvokedStackIterator() {
      frame = getLastInvokedStackFrame();
    }

    public StackFrame next() {
      if (frame != null){
        StackFrame ret = frame;
        frame = null;
        for (StackFrame f=ret.getPrevious(); f != null; f = f.getPrevious()){
          if (f.isInvoked()){
            frame = f;
            break;
          }
        }
        return ret;

      } else {
        throw new NoSuchElementException();
      }
    }
  }


  protected ExceptionInfo pendingException;

  /** backtrack-relevant Information about the thread */
  protected ThreadData threadData;


  /** the top stack frame */
  protected StackFrame top = null;

  /** the current stack depth (number of frames) */
  protected int stackDepth;

  /**
   * Reference of the thread list it is in.
   * <2do> - bad cross ref (ThreadList should know about ThreadInfo, but not vice versa)
   */
  protected ThreadList list;

  /** thread list index */
  protected int index;


  static final int ATTR_DATA_CHANGED  = 0x1000000;
  static final int ATTR_STACK_CHANGED = 0x2000000;
  static final int ATTR_HAS_CHANGED_BEFORE = 0x400000;

  static final int ATTR_ANY_CHANGED = (ATTR_DATA_CHANGED | ATTR_STACK_CHANGED);
  static final int ATTR_SET_DATA_CHANGED = (ATTR_DATA_CHANGED | ATTR_HAS_CHANGED_BEFORE);
  static final int ATTR_SET_STACK_CHANGED = (ATTR_STACK_CHANGED | ATTR_HAS_CHANGED_BEFORE);
  static final int ATTR_CHANGESET = ATTR_DATA_CHANGED | ATTR_STACK_CHANGED | ATTR_HAS_CHANGED_BEFORE;

  protected int attributes;


  /** the first insn in the current transition */
  protected boolean isFirstStepInsn;

  /** shall we skip the next insn */
  protected boolean skipInstruction;

  /** store the last executed insn in the path */
  protected boolean logInstruction;


  /** the last returned direct call frame */
  protected DirectCallStackFrame returnedDirectCall;

  /** the next insn to execute (null prior to execution) */
  protected Instruction nextPc;

  /**
   * not so nice we cross-couple the NativePeers with ThreadInfo,
   * but to carry on with the JNI analogy, a MJIEnv is clearly
   * owned by a thread (even though we don't have much ThreadInfo
   * state dependency in here (yet), hence the simplistic init)
   */
  MJIEnv env;

  /**
   * the VM we are running on. Bad backlink, but then again, we can't really
   * test a ThreadInfo outside a VM context anyways.
   * <2do> If we keep 'list' as a field, 'vm' might be moved over there
   * (all threads in the list share the same VM)
   */
  JVM vm;

  /**
   * !! this is a volatile object, i.e. it has to be re-computed explicitly
   * !! after each backtrack (we don't want to duplicate state storage)
   * list of lock objects currently held by this thread.
   * unfortunately, we cannot organize this as a stack, since it might get
   * restored (from the heap) in random order
   */
  LinkedList<ElementInfo> lockedObjects;

  /**
   * !! this is also volatile -> has to be reset after backtrack
   * the reference of the object if this thread is blocked or waiting for
   */
  int lockRef = -1;


  Memento<ThreadInfo> memento; // cache for unchanged ThreadInfos

  /**
   * this is where we keep ThreadInfos, indexed by their java.lang.Thread objRef, to
   * enable us to keep ThreadInfo identities across backtracked and restored states
   */
  static final SparseObjVector<ThreadInfo> threadInfos = new SparseObjVector<ThreadInfo>();

  // the following parameters are configurable. Would be nice if we could keep
  // them on a per-instance basis, but there are a few locations
  // (e.g. ThreadList) where we loose the init context, and it's questionable
  // if we want to change this at runtime, or POR might make sense on a per-thread
  // basis

  /** do we halt on each throw, i.e. don't look for an exception handler?
   * Useful to find empty handler blocks, or misusd exceptions
   */
  static String[] haltOnThrow;

  /** is on-the-fly partial order in effect? */
  static boolean porInEffect;

  /** do we treat access of fields referring to objects that are reachable
   * from different threads as boundary steps (i.e. starting a new Transition)?
   */
  static boolean porFieldBoundaries;

  /** detect field synchronization (find locks which are used to synchronize
   * field access - if we have viable candidates, and we find the locks taken,
   * we don't treat access of the corresponding field as a boundary step
   */
  static boolean porSyncDetection;

  static int checkBudgetCount;

  static boolean init (Config config) {
    currentThread = null;
    mainThread = null;
    
    threadInfos.clear();

    haltOnThrow = config.getStringArray("vm.halt_on_throw");
    porInEffect = config.getBoolean("vm.por");
    porFieldBoundaries = porInEffect && config.getBoolean("vm.por.field_boundaries");
    porSyncDetection = porInEffect && config.getBoolean("vm.por.sync_detection");
    checkBudgetCount = config.getInt("vm.budget.check_count", 9999);

    return true;
  }

  /**
   * Creates a new thread info. It is associated with the object
   * passed and sets the target object as well.
   */
  public ThreadInfo (JVM vm, int objRef) {
    init( vm, objRef);

    env = new MJIEnv(this);
    //threadInfos.set(objRef, this); // our ThreadInfo repository

    // there can only be one
    if (mainThread == null) {
      mainThread = this;
      currentThread = this;
    }
  }

  public Memento<ThreadInfo> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  
  public static ThreadInfo getMainThread () {
    return mainThread;
  }

  private void init (JVM vm, int objRef) {

    ElementInfo ei = vm.getElementInfo(objRef);

    this.vm = vm;

    threadData = new ThreadData();
    threadData.state = State.NEW;
    threadData.ci = ei.getClassInfo();
    threadData.objref = objRef;
    threadData.target = MJIEnv.NULL;
    threadData.lockCount = 0;
    threadData.suspendCount = 0;
    // this is nasty - 'priority', 'name', 'target' and 'group' are not taken
    // from the object, but set within the java.lang.Thread ctors

    top = null;
    stackDepth = 0;

    lockedObjects = new LinkedList<ElementInfo>();

    markUnchanged();
    attributes |= ATTR_SET_DATA_CHANGED;
  }

  /**
   * if we already had a ThreadInfo object for this java.lang.Thread object, make
   * sure we reset it. It will be restored to proper state afterwards
   */
  static ThreadInfo createThreadInfo (JVM vm, int objRef) {

    // <2do> this relies on heap symmetry! fix it
    ThreadInfo ti = threadInfos.get(objRef);

    if (ti == null) {
      ti = new ThreadInfo(vm, objRef);
      threadInfos.set(objRef, ti);

    } else {
      ti.init(vm, objRef);
    }

    vm.addThread(ti);

    return ti;
  }

  /**
   * just retrieve the ThreadInfo object for this java.lang.Thread object. This method is
   * only valid after the thread got created
   */
  public static ThreadInfo getThreadInfo(int objRef) {
    if (objRef >= 0) { 
      return threadInfos.get(objRef);
    } else {
      return null;
    }
  }



  public static ThreadInfo getCurrentThread() {
    return currentThread;
  }


  public boolean isExecutingAtomically () {
    return vm.getSystemState().isAtomic();
  }

  public boolean holdsLock (ElementInfo ei) {
    return lockedObjects.contains(ei);
  }

  public JVM getVM () {
    return vm;
  }

  public boolean isFirstStepInsn() {
    return isFirstStepInsn;
  }

  /**
   * to be used from methods called from listeners, to find out if we are in a
   * pre- or post-exec notification
   */
  public boolean isPreExec() {
    return (nextPc == null);
  }

  public boolean usePor () {
    return porInEffect;
  }

  public boolean usePorFieldBoundaries () {
    return porFieldBoundaries;
  }

  public boolean usePorSyncDetection () {
    return porSyncDetection;
  }

  void setListInfo (ThreadList tl, int idx) {
    list = tl;
    index = idx;
  }

  /**
   * Checks if the thread is waiting to execute a nondeterministic choice
   * due to an abstraction, i.e. due to a Bandera.choose() call
   *
   * <2do> that's probably deprecated
   */
  public boolean isAbstractionNonDeterministic () {
    if (getPC() == null) {
      return false;
    }

    return getPC().examineAbstraction(vm.getSystemState(), vm.getKernelState(), this);
  }

  //--- various thread state related methods

  /**
   * Updates the status of the thread.
   */
  public void setState (State newStatus) {
    State oldStatus = threadData.state;

    if (oldStatus != newStatus) {

      assert (oldStatus != State.TERMINATED) : "can't resurrect thread " + this + " to " + newStatus.name();

      threadDataClone().state = newStatus;

      switch (newStatus) {
      case NEW:
        break; // Hmm, shall we report a thread object creation?
      case RUNNING:
        // nothing. the notifyThreadStarted has to happen from
        // Thread.start(), since the thread could have been blocked
        // at the time with a sync run() method
        assert lockRef == -1;
        break;
      case TERMINATED:
        vm.notifyThreadTerminated(this);
        break;
      case BLOCKED:
        assert lockRef != -1;
        vm.notifyThreadBlocked(this);
        break;
      case UNBLOCKED:
        assert lockRef == -1;
        break; // nothing to notify
      case WAITING:
        assert lockRef != -1;
        vm.notifyThreadWaiting(this);
        break;
      case INTERRUPTED:
        vm.notifyThreadInterrupted(this);
        break;
      case NOTIFIED:
        assert lockRef != -1;
        vm.notifyThreadNotified(this);
        break;
      }

      if (log.isLoggable(Level.FINE)){
        log.fine("setStatus of " + getName() + " from "
                 + oldStatus.name() + " to " + newStatus.name());
      }
    }
  }

  void setBlockedState (int objref) {
    
    State currentState = threadData.state;
    switch (currentState){
      case NEW:
      case RUNNING:
      case UNBLOCKED:
        lockRef = objref;
        setState(State.BLOCKED);
        break;

      default:
        assert false : "thread " + this + "can't be blocked in state: " + currentState.name();
    }
  }

  void setNotifiedState() {
    State currentState = threadData.state;
    switch (currentState){
      case BLOCKED:
      case NOTIFIED:
        // can happen in a Thread.join()
        break;
      case WAITING:
      case TIMEOUT_WAITING:
        setState(State.NOTIFIED);
        break;

      default:
        assert false : "thread " + this + "can't be notified in state: " + currentState.name();
    }
  }

  /**
   * Returns the current status of the thread.
   */
  public State getState () {
    return threadData.state;
  }


  /**
   * Returns true if this thread is either RUNNING or UNBLOCKED
   */
  public boolean isRunnable () {
    if (threadData.suspendCount != 0)
      return false;

    switch (threadData.state) {
    case RUNNING:
    case UNBLOCKED:
      return true;
    case SLEEPING:
      return true;    // that's arguable, but since we don't model time we treat it like runnable
    case TIMEDOUT:
      return true;    // would have been set to blocked if it couldn't reacquire the lock
    default:
      return false;
    }
  }

  public boolean willBeRunnable () {
    if (threadData.suspendCount != 0)
      return false;

    switch (threadData.state) {
    case RUNNING:
    case UNBLOCKED:
      return true;
    case TIMEOUT_WAITING: // it's not yet, but it will be at the time it gets scheduled
    case SLEEPING:
      return true;
    default:
      return false;
    }
  }

  public boolean isNew () {
    return (threadData.state == State.NEW);
  }

  public boolean isTimeoutRunnable () {
    if (threadData.suspendCount != 0)
      return false;

    switch (threadData.state) {

    case RUNNING:
    case UNBLOCKED:
    case SLEEPING:
      return true;

    case TIMEOUT_WAITING:
      // depends on if we can re-acquire the lock
      //assert lockRef != -1 : "timeout waiting but no blocked object";
      if (lockRef != -1){
        ElementInfo ei = vm.getElementInfo(lockRef);
        return ei.canLock(this);
      } else {
        return true;
      }

    default:
      return false;
    }
  }

  public boolean isTimedOut() {
    return (threadData.state == State.TIMEDOUT);
  }

  public boolean isTimeoutWaiting() {
    return (threadData.state == State.TIMEOUT_WAITING);
  }

  public void setTimedOut() {
    setState(State.TIMEDOUT);
  }

  public void setTerminated() {
    setState(State.TERMINATED);
  }

  public void resetTimedOut() {
    // should probably check for TIMEDOUT
    setState(State.TIMEOUT_WAITING);
  }

  public void setSleeping() {
    setState(State.SLEEPING);
  }

  public boolean isSleeping(){
    return (threadData.state == State.SLEEPING);
  }

  public void setRunning() {
    setState(State.RUNNING);
  }

  /**
   * An alive thread is anything but TERMINATED or NEW
   */
  public boolean isAlive () {
    State state = threadData.state;
    return (state != State.TERMINATED && state != State.NEW);
  }

  public boolean isWaiting () {
    State state = threadData.state;
    return (state == State.WAITING) || (state == State.TIMEOUT_WAITING);
  }

  public boolean isNotified () {
    return (threadData.state == State.NOTIFIED);
  }

  public boolean isUnblocked () {
    State state = threadData.state;
    return (state == State.UNBLOCKED) || (state == State.TIMEDOUT);
  }

  public boolean isBlocked () {
    return (threadData.state == State.BLOCKED);
  }

  public boolean isTerminated () {
    return (threadData.state == State.TERMINATED);
  }

  MethodInfo getExitMethod() {
    MethodInfo mi = getClassInfo().getMethod("exit()V", true);
    return mi;
  }

  public boolean isBlockedOrNotified() {
    State state = threadData.state;
    return (state == State.BLOCKED) || (state == State.NOTIFIED);
  }

  public String getStateName () {
    return threadData.getState().name();
  }


  public boolean getBooleanLocal (String lname) {
    return Types.intToBoolean(getLocalVariable(lname));
  }

  public boolean getBooleanLocal (int lindex) {
    return Types.intToBoolean(getLocalVariable(lindex));
  }

  public boolean getBooleanReturnValue () {
    return Types.intToBoolean(peek());
  }

  public byte getByteLocal (String lname) {
    return (byte) getLocalVariable(lname);
  }

  public byte getByteLocal (int lindex) {
    return (byte) getLocalVariable(lindex);
  }


  public byte getByteReturnValue () {
    return (byte) peek();
  }

  public Iterator<StackFrame> iterator () {
    return new StackIterator();
  }

  public Iterable<StackFrame> invokedStackFrames () {
    return new Iterable<StackFrame>() {
      public Iterator<StackFrame> iterator() {
        return new InvokedStackIterator();
      }
    };
  }

  /**
   * this returns a copy of the StackFrames in reverse order. Note this is
   * redundant because the frames are linked explicitly
   * @deprecated - use Iterable<StackFrame>
   */
  @Deprecated
  public List<StackFrame> getStack() {
    ArrayList<StackFrame> list = new ArrayList<StackFrame>(stackDepth);

    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      list.add(frame);
    }

    Collections.reverse(list);

    return list;
  }

  /**
   * returns StackFrames which have been entered through a corresponding
   * invoke instruction (in top first order)
   */
  public List<StackFrame> getInvokedStackFrames() {
    ArrayList<StackFrame> list = new ArrayList<StackFrame>(stackDepth);

    int i = stackDepth-1;
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (frame.isInvoked()){
        list.set(i--, frame);
      }
    }

    return list;
  }

  public List<StackFrame> getChangedStackFrames() {
    ArrayList<StackFrame> list = new ArrayList<StackFrame>();

    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (!frame.hasChanged()){
        list.add(frame);
      }
    }

    return list;
  }

  public int getStackDepth() {
    return stackDepth;
  }

  public StackFrame getCallerStackFrame (int offset){
    int n = offset;
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (n < 0){
        break;
      } else if (n == 0){
        return frame;
      }
      n--;
    }
    return null;
  }

  public StackFrame getLastInvokedStackFrame() {
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (!frame.isInvoked()){
        return frame;
      }
    }

    return null;
  }

  public StackFrame getLastNonSyntheticStackFrame (){
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (!frame.isSynthetic()){
        return frame;
      }
    }

    return null;
  }

  /**
   * Returns the this pointer of the callee from the stack.
   */
  public int getCalleeThis (MethodInfo mi) {
    return top.getCalleeThis(mi);
  }

  /**
   * Returns the this pointer of the callee from the stack.
   */
  public int getCalleeThis (int size) {
    return top.getCalleeThis(size);
  }

  public ClassInfo getClassInfo (int objref) {
    return env.getClassInfo(objref);
  }

  public boolean isCalleeThis (ElementInfo r) {
    if (top == null || r == null) {
      return false;
    }

    Instruction pc = getPC();

    if (pc == null ||
        !(pc instanceof InvokeInstruction) ||
        pc instanceof INVOKESTATIC) {
      return false;
    }

    InvokeInstruction call = (InvokeInstruction) pc;

    return getCalleeThis(Types.getArgumentsSize(call.getInvokedMethodSignature()) + 1) == r.getIndex();
  }

  public char getCharLocal (String lname) {
    return (char) getLocalVariable(lname);
  }

  public char getCharLocal (int lindex) {
    return (char) getLocalVariable(lindex);
  }

  public char getCharReturnValue () {
    return (char) peek();
  }

  /**
   * Returns the class information.
   */
  public ClassInfo getClassInfo () {
    return threadData.ci;
  }

  public double getDoubleLocal (String lname) {
    return Types.longToDouble(getLongLocalVariable(lname));
  }

  public double getDoubleLocal (int lindex) {
    return Types.longToDouble(getLongLocalVariable(lindex));
  }

  public double getDoubleReturnValue () {
    return Types.longToDouble(longPeek());
  }

  public MJIEnv getEnv() {
    return env;
  }

  public float getFloatLocal (String lname) {
    return Types.intToFloat(getLocalVariable(lname));
  }

  public float getFloatLocal (int lindex) {
    return Types.intToFloat(getLocalVariable(lindex));
  }

  public float getFloatReturnValue () {
    return Types.intToFloat(peek());
  }

  public int getIntLocal (String lname) {
    return getLocalVariable(lname);
  }

  public int getIntLocal (int lindex) {
    return getLocalVariable(lindex);
  }

  public int getIntReturnValue () {
    return peek();
  }

  public boolean isInterrupted (boolean resetStatus) {
    ElementInfo ei = getElementInfo(getThreadObjectRef());
    boolean status =  ei.getBooleanField("interrupted");

    if (resetStatus && status) {
      ei.setBooleanField("interrupted", false);
    }

    return status;
  }

  /**
   * return our internal thread number (order of creation)
   */
  public int getIndex () {
    return index;
  }

  /**
   * record what this thread is being blocked on.
   */
  void setLockRef (int objref) {
/**
    assert ((lockRef == -1) || (lockRef == objref)) :
      "attempt to overwrite lockRef: " + vm.getHeap().get(lockRef) +
      " with: " + vm.getHeap().get(objref);
**/
    lockRef = objref;
  }

  /**
   * thread is not blocked anymore
   * needs to be public since we have to use it from INVOKECLINIT (during call skipping)
   */
  public void resetLockRef () {
    lockRef = -1;
  }

  public ElementInfo getLockObject () {
    if (lockRef == -1) {
      return null;
    } else {
      return vm.getElementInfo(lockRef);
    }
  }

  /**
   * Returns the line number of the program counter of the top stack frame.
   */
  public int getLine () {
    if (top == null) {
      return -1;
    } else {
      return top.getLine();
    }
  }
  
  public LocalVarInfo[] getLocalVars() {
    return top.getLocalVars();
  }
  
  @Deprecated  // Use getLocalVars() instead
  public String[] getLocalNames () {
    return top.getLocalVariableNames();
  }

  /**
   * Sets the value of a local variable.
   */
  public void setLocalVariable (int idx, int v, boolean ref) {
    topClone().setLocalVariable(idx, v, ref);
  }

  /**
   * Returns the value of a local variable.
   */
  public int getLocalVariable (int idx) {
    return top.getLocalVariable(idx);
  }

  /**
   * Gets the value of a local variable from its name.
   */
  public int getLocalVariable (String name) {
    return top.getLocalVariable(name);
  }

  /**
   * Checks if a local variable is a reference.
   */
  public boolean isLocalVariableRef (int idx) {
    return top.isLocalVariableRef(idx);
  }


  /**
   * Gets the type associated with a local variable.
   */
  public String getLocalVariableType (String name) {
    return top.getLocalVariableType(name);
  }

  /**
   * Sets the number of locks held at the time of a wait.
   */
  public void setLockCount (int l) {
    if (threadData.lockCount != l) {
      threadDataClone().lockCount = l;
    }
  }

  /**
   * Returns the number of locks in the last wait.
   */
  public int getLockCount () {
    return threadData.lockCount;
  }
    
  /**
    * Increments the suspend counter.
    * @return true if the suspend counter was 0 before this call (e.g. the thread was just suspended)
    */
  public boolean suspend() {
    return threadDataClone().suspendCount++ == 0;
  }

  /**
    * Decrements the suspend counter if > 0.
    * @return true if the suspend counter was 1 before the call (e.g. the thread was just resumed)
    */
  public boolean resume() {
    return (threadData.suspendCount > 0) && (--threadDataClone().suspendCount == 0);
  }
  
  public boolean isSuspended() {
    return threadData.suspendCount > 0;
  }

  public LinkedList<ElementInfo> getLockedObjects () {
    return lockedObjects;
  }

  public int[] getLockedObjectReferences () {
    int nLocks = lockedObjects.size();
    if (nLocks > 0) {
      int[] a = new int[lockedObjects.size()];
      int i = 0;
      for (ElementInfo e : lockedObjects) {
        a[i++] = e.getIndex();
      }
      return a;

    } else {
      return emptyRefArray;
    }
  }

  public long getLongLocal (String lname) {
    return getLongLocalVariable(lname);
  }

  public long getLongLocal (int lindex) {
    return getLongLocalVariable(lindex);
  }

  /**
   * Sets the value of a long local variable.
   */
  public void setLongLocalVariable (int idx, long v) {
    topClone().setLongLocalVariable(idx, v);
  }

  /**
   * Returns the value of a long local variable.
   */
  public long getLongLocalVariable (int idx) {
    return top.getLongLocalVariable(idx);
  }

  /**
   * Gets the value of a long local variable from its name.
   */
  public long getLongLocalVariable (String name) {
    return top.getLongLocalVariable(name);
  }

  public long getLongReturnValue () {
    return longPeek();
  }

  /**
   * returns the current method in the top stack frame, which is always a
   * bytecode method (executed by JPF)
   */
  public MethodInfo getTopMethod () {
    if (top != null) {
      return top.getMethodInfo();
    } else {
      return null;
    }
  }

  /**
   * returns the currently executing MethodInfo, which can be a native/MJI method
   */
  public MethodInfo getMethod() {
    MethodInfo mi = vm.getLastMethodInfo();
    if (mi != null){
      return mi;
    } else {
      return getTopMethod();
    }
  }

  public boolean isInCtor () {
    // <2do> - hmm, if we don't do this the whole stack, we miss factored
    // init funcs
    MethodInfo mi = getMethod();
    if (mi != null) {
      return mi.isCtor();
    } else {
      return false;
    }
  }

  public boolean isCtorOnStack (int objRef){
    for (StackFrame f = top; f != null; f = f.getPrevious()){
      if (f.getThis() == objRef && f.getMethodInfo().isCtor()){
        return true;
      }
    }

    return false;
  }

  public boolean isClinitOnStack (ClassInfo ci){
    for (StackFrame f = top; f != null; f = f.getPrevious()){
      MethodInfo mi = f.getMethodInfo();
      if (mi.isClinit(ci)){
        return true;
      }
    }

    return false;
  }


  public String getName () {
    return threadData.name;
  }


  public ElementInfo getObjectLocal (String lname) {
    return vm.getElementInfo(getLocalVariable(lname));
  }

  public ElementInfo getObjectLocal (int lindex) {
    return vm.getElementInfo(getLocalVariable(lindex));
  }

  /**
   * Returns the object reference.
   */
  public int getThreadObjectRef () {
    return threadData.objref;
  }

  public ElementInfo getObjectReturnValue () {
    return vm.getElementInfo(peek());
  }

  // might return composite
  public Object getOperandAttr () {
    return top.getOperandAttr();
  }
  public <T> T getOperandAttr (Class<T> attrType){
    return top.getOperandAttr(attrType);
  }

  // might return composite
  public Object getLongOperandAttr () {
    return top.getLongOperandAttr();
  }
  public <T> T getLongOperandAttr (Class<T> attrType){
    return top.getLongOperandAttr(attrType);
  }



  // might return composite
  public Object getOperandAttr (int opStackOffset) {
    return top.getOperandAttr(opStackOffset);
  }
  public <T> T getOperandAttr( Class<T> attrType, int opStackOffset){
    return top.getOperandAttr(attrType,opStackOffset);
  }

  // setting operand attributes assumes the operand is already on the stack

  /**
   * use this version if only the attr has changed, but not the value
   * (otherwise state management won't work)
   */
  public void setOperandAttr (Object attr) {
    topClone().setOperandAttr(attr);
  }

  public void setLongOperandAttr (Object attr) {
    topClone().setLongOperandAttr(attr);
  }


  public void setReturnValue (Object ret){
    topClone().setReturnValue(ret);
  }

  public void setReturnAttr (Object attr){
    topClone().setReturnAttr(attr);
  }

  /**
   * use this version if the value is also changed, which means we don't
   * have to clone here
   */
  public void setOperandAttrNoClone (Object attr) {
    top.setOperandAttr(attr);
  }

  public void setLongOperandAttrNoClone (Object attr) {
    top.setLongOperandAttr(attr);
  }


  public void setLocalAttr (int localIndex, Object attr){
    topClone().setLocalAttr(localIndex, attr);
  }

  public void setLocalAttrNoClone (int localIndex, Object attr){
    top.setLocalAttr(localIndex, attr);
  }

  // might return composite
  public Object getLocalAttr (int localIndex){
    return top.getLocalAttr(localIndex);
  }
  public <T> T getLocalAttr (Class<T> attrType, int localIndex){
    return top.getLocalAttr(attrType, localIndex);
  }

  /**
   * Checks if the top operand is a reference.
   */
  public boolean isOperandRef () {
    return top.isOperandRef();
  }

  /**
   * Checks if an operand is a reference.
   */
  public boolean isOperandRef (int idx) {
    return top.isOperandRef(idx);
  }

  /**
   * Sets the program counter of the top stack frame.
   */
  public void setPC (Instruction pc) {
    topClone().setPC(pc);
  }

  public void advancePC () {
    topClone().advancePC();
  }

  /**
   * Returns the program counter of the top stack frame.
   */
  public Instruction getPC () {
    if (top != null) {
      return top.getPC();
    } else {
      return null;
    }
  }

  public Instruction getNextPC () {
    return nextPc;
  }

  public short getShortLocal (String lname) {
    return (short) getLocalVariable(lname);
  }

  public short getShortLocal (int lindex) {
    return (short) getLocalVariable(lindex);
  }

  public short getShortReturnValue () {
    return (short) peek();
  }

  /**
   * get the current stack trace of this thread
   * this is called during creation of a Throwable, hence we should skip
   * all throwable ctors in here
   * <2do> this is only a partial solution,since we don't catch exceptions
   * in Throwable ctors yet
   */
  public String getStackTrace () {
    StringBuilder sb = new StringBuilder(256);

    for (StackFrame sf = top; sf != null; sf = sf.getPrevious()){
      MethodInfo mi = sf.getMethodInfo();

      if (mi.isCtor()){
        ClassInfo ci = mi.getClassInfo();
        if (ci.isInstanceOf("java.lang.Throwable")) {
          continue;
        }
      }

      sb.append("\tat ");
      sb.append(sf.getStackTraceInfo());
      sb.append('\n');
    }

    return sb.toString();
  }


  /**
   * Returns the information necessary to store.
   *
   * <2do> pcm - not clear to me how lower stack frames can contribute to
   * a different threadinfo state hash - only the current one can be changed
   * by the executing method
   */
  public void dumpStoringData (IntVector v) {
    v = null;  // Get rid of IDE warnings
  }

  public String getStringLocal (String lname) {
    return vm.getElementInfo(getLocalVariable(lname)).asString();
  }

  public String getStringLocal (int lindex) {
    return vm.getElementInfo(getLocalVariable(lindex)).asString();
  }

  public String getStringReturnValue () {
    return vm.getElementInfo(peek()).asString();
  }

  /**
   * Sets the target of the thread.
   */
  public void setTarget (int t) {
    if (threadData.target != t) {
      threadDataClone().target = t;
    }
  }

  /**
   * Returns the object reference of the target.
   */
  public int getTarget () {
    return threadData.target;
  }

  /**
   * Returns the pointer to the object reference of the executing method
   */
  public int getThis () {
    return top.getThis();
  }

  public ElementInfo getThisElementInfo(){
    return getElementInfo(getThis());
  }

  public boolean isThis (ElementInfo r) {
    if (r == null) {
      return false;
    }

    if (top == null) {
      return false;
    }

    return getMethod().isStatic()
      ? false : r.getIndex() == getLocalVariable(0);
  }

  public boolean atMethod (String mname) {
    return top != null && getMethod().getCompleteName().equals(mname);
  }

  public boolean atPosition (int position) {
    if (top == null) {
      return false;
    } else {
      Instruction pc = getPC();
      return pc != null && pc.getPosition() == position;
    }
  }

  public boolean atReturn () {
    if (top == null) {
      return false;
    } else {
      Instruction pc = getPC();
      return pc instanceof ReturnInstruction;
    }
  }


  /**
   * reset any information that has to be re-computed in a backtrack
   * (i.e. hasn't been stored explicitly)
   */
  void resetVolatiles () {
    // resetting lock sets goes here
    lockedObjects = new LinkedList<ElementInfo>();

    // the ref of the object we are blocked on or waiting for
    lockRef = -1;
  }

  /**
   * this is used when restoring states
   */
  void updateLockedObject (ElementInfo ei) {
    lockedObjects.add(ei);
    // don't notify here, it's just a restore
  }

  void addLockedObject (ElementInfo ei) {
    lockedObjects.add(ei);
    vm.notifyObjectLocked(this, ei);
  }

  void removeLockedObject (ElementInfo ei) {
    lockedObjects.remove(ei);
    vm.notifyObjectUnlocked(this, ei);
  }

  /**
   * Clears the operand stack of all value.
   */
  public void clearOperandStack () {
    topClone().clearOperandStack();
  }

  public Object clone() {
    try {
      // threadData and top StackFrame are copy-on-write, so we should not have to clone them
      // lockedObjects are state-volatile and restored explicitly after a backtrack
      return super.clone();

    } catch (CloneNotSupportedException cnsx) {
      return null;
    }
  }

  LinkedList<ElementInfo> cloneLockedObjects() {
    LinkedList<ElementInfo> lo = new LinkedList<ElementInfo>();

    for (ElementInfo ei : lockedObjects) {
      lo.add((ElementInfo)ei.clone());
    }

    return lo;
  }


  /**
   * Returns the number of stack frames.
   */
  public int countStackFrames () {
    return stackDepth;
  }

  /**
   * get a stack snapshot that consists of an array of {mthId,pc} pairs.
   * strip stackframes that execute instance methods of the exception object
   */
  public int[] getSnapshot (int xObjRef) {
    StackFrame frame = top;
    int n = stackDepth;

    if (xObjRef != MJIEnv.NULL){ // filter out exception method frames
      for (;frame != null; frame = frame.getPrevious()){
        if (frame.getThis() != xObjRef){
          break;
        }
        n--;
      }
    }

    int j=0;
    int[] snap = new int[n*2];

    for (; frame != null; frame = frame.getPrevious()){
      snap[j++] = frame.getMethodInfo().getGlobalId();
      snap[j++] = frame.getPC().getOffset();
    }

    return snap;
  }

  /**
   * turn a snapshot into an JPF array of StackTraceElements, which means
   * a lot of objects. Do this only on demand
   */
  public int createStackTraceElements (int[] snapshot) {
    int n = snapshot.length/2;
    int nVisible=0;
    StackTraceElement[] list = new StackTraceElement[n];
    for (int i=0, j=0; i<n; i++){
      int methodId = snapshot[j++];
      int pcOffset = snapshot[j++];
      StackTraceElement ste = new StackTraceElement( methodId, pcOffset);
      if (!ste.ignore){
        list[nVisible++] = ste;
      }
    }

    Heap heap = vm.getHeap();
    int aref = heap.newArray("Ljava/lang/StackTraceElement;", nVisible, this);
    ElementInfo aei = heap.get(aref);
    for (int i=0; i<nVisible; i++){
      int eref = list[i].createJPFStackTraceElement();
      aei.setElement( i, eref);
    }

    return aref;
  }

  void print (PrintWriter pw, String s) {
    if (pw != null){
      pw.print(s);
    } else {
      vm.print(s);
    }
  }

  public void printStackTrace (int objRef) {
    printStackTrace(null, objRef);
  }

  public void printPendingExceptionOn (PrintWriter pw) {
    if (pendingException != null) {
      printStackTrace( pw, pendingException.getExceptionReference());
    }
  }

  /**
   * the reason why this is kind of duplicated (there is also a StackFrame.getPositionInfo)
   * is that this might be working off a StackTraceElement[] that is created when the exception
   * is created. At the time printStackTrace() is called, the StackFrames in question
   * are most likely already be unwinded
   */
  public void printStackTrace (PrintWriter pw, int objRef) {
    // 'env' usage is not ideal, since we don't know from what context we are called, and
    // hence the MJIEnv calling context might not be set (no Method or ClassInfo)
    // on the other hand, we don't want to re-implement all the MJIEnv accessor methods

    print(pw, env.getClassInfo(objRef).getName());
    int msgRef = env.getReferenceField(objRef,"detailMessage");
    if (msgRef != MJIEnv.NULL) {
      print(pw, ": ");
      print(pw, env.getStringObject(msgRef));
    }
    print(pw, "\n");

    // try the 'stackTrace' field first, it might have been set explicitly
    int aRef = env.getReferenceField(objRef, "stackTrace"); // StackTrace[]
    if (aRef != MJIEnv.NULL) {
      int len = env.getArrayLength(aRef);
      for (int i=0; i<len; i++) {
        int steRef = env.getReferenceArrayElement(aRef, i);
        if (steRef != MJIEnv.NULL){  // might be ignored (e.g. direct call)
          StackTraceElement ste = new StackTraceElement(steRef);
          ste.printOn( pw);
        }
      }

    } else { // fall back to use the snapshot stored in the exception object
      aRef = env.getReferenceField(objRef, "snapshot");
      int[] snapshot = env.getIntArrayObject(aRef);
      int len = snapshot.length/2;

      for (int i=0, j=0; i<len; i++){
        int methodId = snapshot[j++];
        int pcOffset = snapshot[j++];
        StackTraceElement ste = new StackTraceElement( methodId, pcOffset);
        ste.printOn( pw);
      }
    }

    int causeRef = env.getReferenceField(objRef, "cause");
    if ((causeRef != objRef) && (causeRef != MJIEnv.NULL)){
      print(pw, "Caused by: ");
      printStackTrace(pw, causeRef);
    }
  }

  class StackTraceElement {
    String clsName, mthName, fileName;
    int line;
    boolean ignore;


    StackTraceElement (int methodId, int pcOffset) {
      if (methodId == MethodInfo.REFLECTION_CALL) {
        clsName = "java.lang.reflect.Method";
        mthName = "invoke";
        fileName = "Native Method";
        line = -1;

      } else if (methodId == MethodInfo.DIRECT_CALL) {
        ignore = true;

      } else {
        MethodInfo mi = MethodInfo.getMethodInfo(methodId);
        if (mi != null) {
          clsName = mi.getClassName();
          mthName = mi.getName();

          fileName = mi.getStackTraceSource();
          line = mi.getLineNumber(mi.getInstruction(pcOffset));

        } else { // this sounds like a bug
          clsName = "?";
          mthName = "?";
          fileName = "?";
          line = -1;
        }
      }
    }

    StackTraceElement (int sRef){
      clsName = env.getStringObject(env.getReferenceField(sRef, "clsName"));
      mthName = env.getStringObject(env.getReferenceField(sRef, "mthName"));
      fileName = env.getStringObject(env.getReferenceField(sRef, "fileName"));
      line = env.getIntField(sRef, "line");
    }

    int createJPFStackTraceElement() {
      if (ignore) {
        return MJIEnv.NULL;
      } else {
        Heap heap = vm.getHeap();

        ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.StackTraceElement");
        int sRef = heap.newObject(ci, ThreadInfo.this);

        ElementInfo  sei = heap.get(sRef);
        sei.setReferenceField("clsName", heap.newString(clsName, ThreadInfo.this));
        sei.setReferenceField("mthName", heap.newString(mthName, ThreadInfo.this));
        sei.setReferenceField("fileName", heap.newString(fileName, ThreadInfo.this));
        sei.setIntField("line", line);

        return sRef;
      }
    }

    void printOn (PrintWriter pw){
      if (!ignore){
        // the usual behavior is to print only the filename, strip the path
        if (fileName != null){
          int idx = fileName.lastIndexOf(File.separatorChar);
          if (idx >=0) {
            fileName = fileName.substring(idx+1);
          }
        }

        print(pw, "\tat ");
        if (clsName != null){
          print(pw, clsName);
          print(pw, ".");
        } else { // some synthetic methods don't belong to classes
          print(pw, "[no class] ");
        }
        print(pw, mthName);

        if (fileName != null){
          print(pw, "(");
          print(pw, fileName);
          if (line >= 0){
            print(pw, ":");
            print(pw, Integer.toString(line));
          }
          print(pw, ")");
        } else {
          //print(pw, "<no source>");
        }

        print(pw, "\n");
      }
    }
  }

  /**
   * <2do> pcm - this is not correct! We have to call a proper ctor
   * for the Throwable (for now, we just explicitly set the details)
   * but since this is not used with user defined exceptions (it's only
   * called from within the VM, i.e. with standard exceptions), we for
   * now skip the hassle of doing direct calls that would change the
   * call stack
   */
  int createException (ClassInfo ci, String details, int causeRef){
    Heap heap = vm.getHeap();
    int objref = heap.newObject(ci, this);
    int msgref = -1;

    ElementInfo ei = heap.get(objref);

    if (details != null) {
      msgref = heap.newString(details, this);
      ei.setReferenceField("detailMessage", msgref);
    }

    // store the stack snapshot
    int[] snap = getSnapshot(MJIEnv.NULL);
    int aref = env.newIntArray(snap);
    ei.setReferenceField("snapshot", aref);

    ei.setReferenceField("cause", (causeRef != MJIEnv.NULL)? causeRef : objref);

    return objref;
  }

  /**
   * Creates and throws an exception. This is what is used if the exception is
   * thrown by the VM (or a listener)
   */
  public Instruction createAndThrowException (ClassInfo ci, String details) {
    if (!ci.isRegistered()) {
      ci.registerClass(this);
    }

    if (!ci.isInitialized()){
      if (ci.initializeClass(this)) {
        return getPC();
      }
    }

    int objref = createException(ci,details, MJIEnv.NULL);
    return throwException(objref);
  }

  /**
   * Creates an exception and throws it.
   */
  public Instruction createAndThrowException (String cname) {
    return createAndThrowException(cname, null);
  }

  public Instruction createAndThrowException (String cname, String details) {
    try {
      ClassInfo ci = ClassInfo.getResolvedClassInfo(cname);
      return createAndThrowException(ci, details);

    } catch (NoClassInfoException cx){
      try {
        ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.NoClassDefFoundError");
        return createAndThrowException(ci, cx.getMessage());

      } catch (NoClassInfoException cxx){
        throw new JPFException("no java.lang.NoClassDefFoundError class");
      }
    }
  }

  /**
   * Duplicates a value on the top stack frame.
   */
  public void dup () {
    topClone().dup();
  }

  /**
   * Duplicates a long value on the top stack frame.
   */
  public void dup2 () {
    topClone().dup2();
  }

  /**
   * Duplicates a long value on the top stack frame.
   */
  public void dup2_x1 () {
    topClone().dup2_x1();
  }

  /**
   * Duplicates a long value on the top stack frame.
   */
  public void dup2_x2 () {
    topClone().dup2_x2();
  }

  /**
   * Duplicates a value on the top stack frame.
   */
  public void dup_x1 () {
    topClone().dup_x1();
  }

  /**
   * Duplicates a value on the top stack frame.
   */
  public void dup_x2 () {
    topClone().dup_x2();
  }

  /**
   * execute a step using on-the-fly partial order reduction
   */
  protected boolean executeStep (SystemState ss) throws JPFException {
    Instruction pc = getPC();
    Instruction nextPc = null;

    currentThread = this;

    // this constitutes the main transition loop. It gobbles up
    // insns until there either is none left anymore in this thread,
    // or it didn't execute (which indicates the insn registered a CG for
    // subsequent invocation)
    isFirstStepInsn = true; // so that potential CG generators know
    do {
      nextPc = executeInstruction();

      if (ss.breakTransition()) {
        // shortcut break if there was no progress (a ChoiceGenerator was created)
        // or if the state is explicitly set as ignored
        break;
      } else {
        pc = nextPc;
      }

      isFirstStepInsn = false;

    } while (pc != null);

    return true;
  }


  /**
   * Execute next instruction.
   */
  public Instruction executeInstruction () {
    Instruction pc = getPC();
    SystemState ss = vm.getSystemState();
    KernelState ks = vm.getKernelState();

    // the default, might be changed by the insn depending on if it's the first
    // time we exec the insn, and whether it does its magic in the top (before break)
    // or bottom half (re-exec after break) of the exec
    logInstruction = true;
    skipInstruction = false;
    nextPc = null;

    if (log.isLoggable(Level.FINER)) {
      log.fine( pc.getMethodInfo().getCompleteName() + " " + pc.getPosition() + " : " + pc);
    }

    // this is the pre-execution notification, during which a listener can perform
    // on-the-fly instrumentation or even replace the instruction alltogether
    vm.notifyExecuteInstruction(this, pc);

    if (!skipInstruction) {
      // execute the next bytecode
      nextPc = pc.execute(ss, ks, this);
    }

    if (logInstruction) {
      ss.recordExecutionStep(pc);
    }

    // here we have our post exec bytecode exec observation point
    vm.notifyInstructionExecuted(this, pc, nextPc);

    // set+return the next insn to execute if we did not return from the last stack frame.
    // Note that 'nextPc' might have been set by a listener, and/or 'top' might have
    // been changed by executing an invoke, return or throw (handler), or by
    // pushing overlay calls on the stack
    if (top != null) {
      // <2do> this is where we would have to handle general insn repeat
      setPC(nextPc);
      return nextPc;
    } else {
      return null;
    }
  }

  /**
   * execute instruction hidden from any listeners, and do not
   * record it in the path
   */
  public Instruction executeInstructionHidden () {
    Instruction pc = getPC();
    SystemState ss = vm.getSystemState();
    KernelState ks = vm.getKernelState();

    nextPc = null; // reset in case pc.execute blows (this could be behind an exception firewall)

    if (log.isLoggable(Level.FINE)) {
      log.fine( pc.getMethodInfo().getCompleteName() + " " + pc.getPosition() + " : " + pc);
    }

    nextPc = pc.execute(ss, ks, this);

    // we did not return from the last frame stack
    if (top != null) { // <2do> should probably bomb otherwise
      setPC(nextPc);
    }

    return nextPc;
  }


  /**
   * is this after calling Instruction.execute()
   * used by instructions and listeners
   */
  public boolean isPostExec() {
    return (nextPc != null);
  }

  public void reExecuteInstruction() {
    nextPc = getPC();
  }

  public boolean willReExecuteInstruction() {
    return (getPC() == nextPc);
  }

  /**
   * skip the next bytecode. To be used by listeners to on-the-fly replace
   * instructions. Note that you have to explicitly call setNextPc() in this case
   */
  public void skipInstruction () {
    skipInstruction = true;
  }

  public boolean isInstructionSkipped() {
    return skipInstruction;
  }

  public void skipInstructionLogging () {
    logInstruction = false;
  }

  /**
   * explicitly set the next insn to execute. To be used by listeners that
   * replace bytecode exec (during 'executeInstruction' notification
   *
   * Note this is dangerous because you have to make sure the operand stack is
   * in a consistent state. This also will fail if someone already ordered
   * reexecution of the current instruction
   */
  public void setNextPC (Instruction insn) {
    nextPc = insn;
  }


  /**
   * Executes a method call. Be aware that it executes the whole method as one atomic
   * step. Arguments have to be already on the provided stack
   *
   * This only works for non-native methods, and does not allow any choice points,
   * so you have to know very well what you are doing.
   *
   * Instructions executed by this method are still fully observable and stored in
   * the path
   */
  public void executeMethodAtomic (DirectCallStackFrame frame) {

    pushFrame(frame);
    int    depth = countStackFrames();
    Instruction pc = frame.getPC();
    SystemState ss = vm.getSystemState();

    ss.incAtomic(); // to shut off avoidable context switches (MONITOR_ENTER and wait() can still block)

    while (depth <= countStackFrames()) {
      Instruction nextPC = executeInstruction();

      if (ss.getNextChoiceGenerator() != null) {
        // BANG - we can't have CG's here
        // should be rather an ordinary exception
        // createAndThrowException("java.lang.AssertionError", "choice point in sync executed method: " + frame);
        throw new JPFException("choice point in atomic method execution: " + frame);
      } else {
        pc = nextPC;
      }
    }

    vm.getSystemState().decAtomic();

    nextPc = null;

    // the frame was already removed by the RETURN insn of the frame's method
  }

  /**
   * execute method atomically, but also hide it from listeners and do NOT add
   * executed instructions to the path.
   *
   * this can be even more confusing than executeMethodAtomic(), since
   * nothing prevents such a method from changing the program state, and we
   * wouldn't know for what reason by looking at the trace
   *
   * this method should only be used if we have to execute test application code
   * like hashCode() or equals() from native code, e.g. to silently check property
   * violations
   *
   * executeMethodHidden also acts as an exception firewall, since we don't want
   * any silently executed code fall back into the visible path (for
   * no observable reason)
   */
  public void executeMethodHidden (DirectCallStackFrame frame) {

    pushFrame(frame);
    
    int depth = countStackFrames(); // this includes the DirectCallStackFrame
    Instruction pc = frame.getPC();

    vm.getSystemState().incAtomic(); // to shut off avoidable context switches (MONITOR_ENTER and wait() can still block)

    while (depth <= countStackFrames()) {
      Instruction nextPC = executeInstructionHidden();

      if (pendingException != null) {

      } else {
        if (nextPC == pc) {
          // BANG - we can't have CG's here
          // should be rather an ordinary exception
          // createAndThrowException("java.lang.AssertionError", "choice point in sync executed method: " + frame);
          throw new JPFException("choice point in hidden method execution: " + frame);
        } else {
          pc = nextPC;
        }
      }
    }

    vm.getSystemState().decAtomic();

    nextPc = null;

    // the frame was already removed by the RETURN insn of the frame's method
  }

  public Heap getHeap () {
    return vm.getHeap();
  }

  public ElementInfo getElementInfo (int ref) {
    Heap heap = vm.getHeap();
    return heap.get(ref);
  }

  // we get our last stackframe popped, so it's time to close down
  // NOTE: it's the callers responsibility to do the notification on the thread object
  public void finish () {
    setState(State.TERMINATED);

    int     objref = getThreadObjectRef();
    ElementInfo ei = getElementInfo(objref);
    cleanupThreadObject(ei);

    // stack is gone, so reachability might change
    vm.activateGC();
  }

  void cleanupThreadObject (ElementInfo ei) {
    // ideally, this should be done by calling Thread.exit(), but this
    // does a ThreadGroup.remove(), which does a lot of sync stuff on the shared
    // ThreadGroup object, which might create lots of states. So we just nullify
    // the Thread fields and remove it from the ThreadGroup from here

    int grpRef = ei.getReferenceField("group");
    cleanupThreadGroup(grpRef, ei.getIndex());

    ei.setReferenceField("group", MJIEnv.NULL);
    ei.setReferenceField("threadLocals", MJIEnv.NULL);
    ei.setReferenceField("inheritableThreadLocals", MJIEnv.NULL);
  }

  void cleanupThreadGroup (int grpRef, int threadRef) {
    if (grpRef != MJIEnv.NULL) {
      ElementInfo eiGrp = getElementInfo(grpRef);

      int threadsRef = eiGrp.getReferenceField("threads");
      if (threadsRef != MJIEnv.NULL) {
        ElementInfo eiThreads = getElementInfo(threadsRef);
        if (eiThreads.isArray()) {
          int nthreads = eiGrp.getIntField("nthreads");

          for (int i=0; i<nthreads; i++) {
            int tref = eiThreads.getElement(i);

            if (tref == threadRef) { // compact the threads array
              int n1 = nthreads-1;
              for (int j=i; j<n1; j++) {
                eiThreads.setElement(j, eiThreads.getElement(j+1));
              }
              eiThreads.setElement(n1, MJIEnv.NULL);

              eiGrp.setIntField("nthreads", n1);
              if (n1 == 0) {
                eiGrp.lock(this);
                eiGrp.notifiesAll();
                eiGrp.unlock(this);
              }

              // <2do> we should probably also check if we have to set it destroyed

              return;
            }
          }
        }
      }
    }
  }

  public void hash (HashData hd) {
    threadData.hash(hd);

    for (StackFrame f = top; f != null; f = f.getPrevious()){
      f.hash(hd);
    }
  }

  public void interrupt () {

    ElementInfo eiThread = getElementInfo(getThreadObjectRef());

    State status = getState();

    switch (status) {
    case RUNNING:
    case BLOCKED:
    case UNBLOCKED:
    case NOTIFIED:
    case TIMEDOUT:
      // just set interrupt flag
      eiThread.setBooleanField("interrupted", true);
      break;

    case WAITING:
    case TIMEOUT_WAITING:
      eiThread.setBooleanField("interrupted", true);
      setState(State.INTERRUPTED);

      // since this is potentially called w/o owning the wait lock, we
      // have to check if the waiter goes directly to UNBLOCKED
      ElementInfo eiLock = getElementInfo(lockRef);
      if (eiLock.canLock(this)) {
        resetLockRef();
        setState(State.UNBLOCKED);
        eiLock.setMonitorWithoutLocked(this);
      }
      break;

    case NEW:
    case TERMINATED:
      // ignore
      break;

    default:
    }
  }

  /**
   * Peeks the top long value from the top stack frame.
   */
  public long longPeek () {
    return top.longPeek();
  }

  /**
   * Peeks a long value from the top stack frame.
   */
  public long longPeek (int n) {
    return top.longPeek(n);
  }

  /**
   * Pops the top long value from the top stack frame.
   */
  public long longPop () {
    return topClone().longPop();
  }

  /**
   * Pushes a long value of the top stack frame.
   */
  public void longPush (long v) {
    topClone().longPush(v);
  }


  /**
   * mark all objects during gc phase1 which are reachable from this threads
   * root set (Thread object, Runnable, stack)
   * @aspects: gc
   */
  void markRoots (Heap heap) {

    // 1. mark the Thread object itself
    heap.markThreadRoot(threadData.objref, index);

    // 2. and its runnable
    if (threadData.target != -1) {
      heap.markThreadRoot(threadData.target,index);
    }

    // 3. now all references on the stack
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      frame.markThreadRoots(heap, index);
    }
  }


  /**
   * replace the top frame - this is a dangerous method that should only
   * be used from Restoreres and to restore operators and locals in post-execution notifications
   * to their pre-execution contents
   */
  public void setTopFrame (StackFrame frame) {
    top = frame;

    // since we have swapped the top frame, the stackDepth might have changed
    int n = 0;
    for (StackFrame f = frame; f != null; f = f.getPrevious()){
      n++;
    }
    stackDepth = n;
  }

  /**
   * Peeks the top value from the top stack frame.
   */
  public int peek () {
    if (top != null) {
      return top.peek();
    } else {
      // <?> not really sure what to do here, but if the stack is gone, so is the thread
      return -1;
    }
  }

  /**
   * Peeks a int value from the top stack frame.
   */
  public int peek (int n) {
    if (top != null) {
      return top.peek(n);
    } else {
      // <?> see peek()
      return -1;
    }
  }


  /**
   * Pops the top value from the top stack frame.
   */
  public int pop () {
    if (top != null) {
      return topClone().pop();
    } else {
      // <?> see peek()
      return -1;
    }
  }

  /**
   * Pops a set of values from the top stack frame.
   */
  public void pop (int n) {
    if (top != null) {
      topClone().pop(n);
    }
  }


  /**
   * Adds a new stack frame for a new called method.
   */
  public void pushFrame (StackFrame frame) {

    frame.setPrevious(top);

    top = frame;
    stackDepth++;

    // a new frame cannot have been stored yet, so we don't need to clone on the next mod
    // note this depends on not pushing a frame in the top half of a CG method
    markTfChanged(top);

    returnedDirectCall = null;
  }

  /**
   * Removes a stack frame
   */
  public StackFrame popFrame() {
    StackFrame frame = top;

    //--- do our housekeeping
    if (frame.hasAnyRef()) {
      vm.getSystemState().activateGC();
    }
    //if (frame.modifiesState()){ // ?? move to special return insns?
    //  markTfChanged();
    //}

    if (stackDepth > 0){
      top = frame.getPrevious();
      stackDepth--;
    } else {
      top = null;
    }

    return top;
  }

  /**
   * removing DirectCallStackFrames is a bit different (only happens from
   * DIRECTCALLRETURN insns)
   */
  public StackFrame popDirectCallFrame() {
    assert top instanceof DirectCallStackFrame;

    // we don't need to mark anything as changed because we didn't
    // use references in this stackframe

    returnedDirectCall = (DirectCallStackFrame)top;

    if (stackDepth > 0){
      top = top.getPrevious();
      stackDepth--;
    } else {
      top = null;
    }

    return top;
  }

  public boolean hasReturnedFromDirectCall () {
    // this is reset each time we push a new frame
    return (returnedDirectCall != null);
  }

  public boolean hasReturnedFromDirectCall(String directCallId){
    return (returnedDirectCall != null &&
            returnedDirectCall.getMethodName().equals(directCallId));
  }

  public DirectCallStackFrame getReturnedDirectCall () {
    return returnedDirectCall;
  }


  public String getStateDescription () {
    StringBuilder sb = new StringBuilder("thread index=");
    sb.append(index);
    sb.append(',');
    sb.append(threadData.getFieldValues());

    return sb.toString();
  }

  /**
   * Prints the content of the stack
   */
  public void printStackContent () {
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      frame.printStackContent();
    }
  }

  /**
   * Prints current stacktrace information
   */
  public void printStackTrace () {
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      frame.printStackTrace();
    }
  }


  //--- those are the transfer operations between operand stack and locals
  public void push (int v, boolean ref) {
    topClone().push(v, ref);
  }

  public void pushRef (int ref) {
    topClone().pushRef(ref);
  }

  public void push (int v) {
    topClone().push(v);
  }

  public void pushLocal (int localIndex){
    topClone().pushLocal(localIndex);
  }

  public void pushLongLocal (int localIndex){
    topClone().pushLongLocal(localIndex);
  }

  public void storeOperand (int localIndex){
    topClone().storeOperand(localIndex);
  }

  public void storeLongOperand (int localIndex){
    topClone().storeLongOperand(localIndex);
  }

  /**
   * Removes the arguments of a method call.
   */
  public void removeArguments (MethodInfo mi) {
    int i = mi.getArgumentsSize();

    if (i != 0) {
      pop(i);
    }
  }

  /**
   * Swaps two entry on the stack.
   */
  public void swap () {
    topClone().swap();
  }

  boolean haltOnThrow (String exceptionClassName){
    if ((haltOnThrow != null) && (haltOnThrow.length > 0)){
      for (String s : haltOnThrow) {
        if (s.equalsIgnoreCase("any") ||
            exceptionClassName.startsWith(s)){
          return true;
        }
      }
    }

    return false;
  }


  /**
   * unwind stack frames until we find a matching handler for the exception object
   */
  public Instruction throwException (int exceptionObjRef) {
    Heap heap = vm.getHeap();
    ElementInfo ei = heap.get(exceptionObjRef);
    ClassInfo ci = ei.getClassInfo();
    String cname = ci.getName();
    StackFrame handlerFrame = null; // the stackframe that has a matching handler (if any)
    ExceptionHandler matchingHandler = null; // the matching handler we found (if any)

    // first, give the VM a chance to intercept (we do this before changing anything)
    Instruction insn = vm.handleException(this, exceptionObjRef);
    if (insn != null){
      return insn;
    }

    // we don't have to store the stacktrace explicitly anymore, since that is now
    // done in the Throwable ctor (more specifically the native fillInStackTrace)
    pendingException = new ExceptionInfo(this, ei);

    vm.notifyExceptionThrown(this, ei);

    if (haltOnThrow(cname)) {
      // shortcut - we don't try to find a handler for this one but bail immediately
      NoUncaughtExceptionsProperty.setExceptionInfo(pendingException);
      throw new UncaughtException(this, exceptionObjRef);
    }

    // check if we find a matching handler, and if we do store it. Leave the
    // stack untouched so that listeners can still inspect it
    for (StackFrame frame = top; (frame != null) && (handlerFrame == null); frame = frame.getPrevious()) {
      MethodInfo mi = frame.getMethodInfo();

      // that means we have to turn the exception into an InvocationTargetException
      if (mi.isReflectionCallStub()) {
        ci               = ClassInfo.getResolvedClassInfo("java.lang.reflect.InvocationTargetException");
        exceptionObjRef  = createException(ci, cname, exceptionObjRef);
        cname            = ci.getName();
        ei               = heap.get(exceptionObjRef);
        pendingException = new ExceptionInfo(this, ei);
      }

      insn = frame.getPC();
      int position = insn.getPosition();

      ExceptionHandler[] exceptions = mi.getExceptions();
      if (exceptions != null) {
        // checks the exceptions caught (in order of handler definitions)
        for (ExceptionHandler handler : exceptions){
          // checks if it falls in the right range
          if ((position >= handler.getBegin()) && (position < handler.getEnd())) {
            // checks if this type of exception is caught here (null means 'any')
            String en = handler.getName();
            if ((en == null) || ci.isInstanceOf(en)) {
              handlerFrame = frame;
              matchingHandler = handler;
              break;
            }
          }
        }
      }

      if ((handlerFrame == null) && mi.isFirewall()) {
        // this method should not let exceptions pass into lower level stack frames
        // (e.g. for <clinit>, or hidden direct calls)
        // <2do> if this is a <clinit>, we should probably turn into an
        // ExceptionInInitializerError first
        unwindTo(frame);
        NoUncaughtExceptionsProperty.setExceptionInfo(pendingException);
        throw new UncaughtException(this, exceptionObjRef);
      }
    }

    if (handlerFrame == null) {
      // no handler -> uncaught exception
      // Note - the stack has not been modified yet, it is still in the same state
      // where the exception was thrown
      NoUncaughtExceptionsProperty.setExceptionInfo(pendingException);
      throw new UncaughtException(this, exceptionObjRef);

    } else { // we found a matching handler
      unwindTo(handlerFrame);

      // according to the VM spec, before transferring control to the handler we have
      // to reset the operand stack to contain only the exception reference
      // (4.9.2 - "4. merge the state of the operand stack..")
      clearOperandStack();
      push(exceptionObjRef, true);

      // jump to the exception handler and set pc so that listeners can see it
      int handlerOffset = matchingHandler.getHandler();
      insn = handlerFrame.getMethodInfo().getInstructionAt(handlerOffset);
      setPC(insn);

      // notify before we reset the pendingException
      vm.notifyExceptionHandled(this);

      pendingException = null; // handled, no need to keep it

      return insn;
    }
  }

  private void unwindTo (StackFrame newTopFrame){
    for (StackFrame frame = top; frame != newTopFrame; frame = frame.getPrevious()) {
      MethodInfo mi = frame.getMethodInfo();
      mi.leave(this); // that takes care of releasing locks
      vm.notifyExceptionBailout(this); // notify before we pop the frame
      popFrame();
    }
  }

  public ExceptionInfo getPendingException () {
    return pendingException;
  }

  /**
   * watch out - just clearing it might cause an infinite loop
   * if we don't drop frames and/or advance the pc
   */
  public void clearPendingException () {
    NoUncaughtExceptionsProperty.setExceptionInfo(null);
    pendingException = null;
  }

  /**
   * Returns a clone of the thread data. To be called every time we change some ThreadData field
   * (which unfortunately includes lock counts, hence this should be changed)
   */
  protected ThreadData threadDataClone () {
    if ((attributes & ATTR_DATA_CHANGED) != 0) {
      // already cloned, so we don't have to clone
    } else {
      // reset, so that next storage request would recompute tdIndex
      markTdChanged();
      list.ks.changed();

      threadData = threadData.clone();
    }

    return threadData;
  }

  public void restoreThreadData(ThreadData td) {
    threadData = td;
  }

  /**
   * request a reschedule no matter what the next insn is
   * Note this unconditionally creates and registers a ThreadCG, even if there is
   * only one runnable thread (ourself). This is intended to be used from
   * within Listeners that need to break transitions / store states in locations
   * only they know about.
   * Note also this differs from Thread.yield() in that yield() is handled by
   * the SchedulerFactory, and it is at its discretion to just ignore it, either
   * because yield in itself is not POR relevant (doesn't change anything), or
   * because there might be only one runnable thread. In both cases, the transition
   * would not be broken.
   * If there is more than one runnable thread, this also differs from
   * breakTransition(), which will continue with the same thread
   */
  public void reschedule (boolean forceBreak) {
    ThreadInfo[] runnables = list.getRunnableThreads();

    if (forceBreak || (runnables.length > 1)) {
      ThreadChoiceGenerator cg = new ThreadChoiceFromSet("reschedule",runnables,true);
      SystemState ss = vm.getSystemState();
      ss.setNextChoiceGenerator(cg); // this breaks the transition
    }
  }

  /**
   * this is a version that unconditionally breaks the current transition
   * without really adding choices. It only goes on with the same thread
   * (to avoid state explosion).
   *
   * if the current transition is already marked as ignored, this method does nothing
   *
   * NOTE: this neither means that we ignore the current transition, nor that
   * it is an end state
   */
  public void breakTransition() {
    SystemState ss = vm.getSystemState();

    if (!ss.isIgnored()){
      // isIgnored is not a normal backtrack, so we shouldn't set a CG
      // (this is used quite often in conjunction
      BreakGenerator cg = new BreakGenerator( "breakTransition", this, false);
      ss.setNextChoiceGenerator(cg); // this breaks the transition
    }
  }

  /**
   * this breaks the current transition with a CG that forces an end state (i.e.
   * has no choices)
   * this only takes effect if the current transition is not already marked
   * as ignored
   */
  public void breakTransition(boolean isTerminator) {
    SystemState ss = vm.getSystemState();

    if (!ss.isIgnored()){
      BreakGenerator cg = new BreakGenerator( "breakTransition", this, isTerminator);
      ss.setNextChoiceGenerator(cg); // this breaks the transition
    }
  }


  public boolean checkPorFieldBoundary () {
    return isFirstStepInsn && porFieldBoundaries && list.hasOtherRunnablesThan(this);
  }

  public boolean hasOtherRunnables () {
    return list.hasOtherRunnablesThan(this);
  }

  protected void markUnchanged() {
    attributes &= ~ATTR_ANY_CHANGED;

    for (StackFrame f = top; f != null && f.hasChanged(); f = f.getPrevious()){
      f.setChanged(false);
    }
  }

  protected void markTfChanged(StackFrame frame) {
    frame.setChanged(true);

    attributes |= ATTR_SET_STACK_CHANGED;
    list.ks.changed();
  }

  protected void markTdChanged() {
    attributes |= ATTR_SET_DATA_CHANGED;
    list.ks.changed();
  }

  public StackFrame getCallerStackFrame() {
    if (top != null){
      return top.getPrevious();
    } else {
      return null;
    }
  }

  public boolean hasDataChanged() {
    return (attributes & ATTR_DATA_CHANGED) != 0;
  }

  public boolean hasStackChanged() {
    return (attributes & ATTR_STACK_CHANGED) != 0;
  }

  public boolean hasChanged() {
    return (attributes & ATTR_ANY_CHANGED) != 0;
  }

  public boolean isNewOrChanged() {
    return (attributes & ATTR_CHANGESET) != ATTR_HAS_CHANGED_BEFORE;
  }

  /**
   * Returns a clone of the top stack frame.
   */
  protected StackFrame topClone () {
    if (!top.hasChanged()) {
      top = top.clone();
      markTfChanged(top);
    }
    return top;
  }

  /**
   * Returns the top stack frame.
   */
  public StackFrame getTopFrame () {
    return top;
  }

  public StackFrame getStackFrameExecuting (Instruction insn, int offset){
    int n = offset;
    StackFrame frame = top;

    for (; (n > 0) && frame != null; frame = frame.getPrevious()){
      n--;
    }

    for(; frame != null; frame = frame.getPrevious()){
      if (frame.getPC() == insn){
        return frame;
      }
    }

    return null;
  }

  public String toString() {
    return "ThreadInfo [name=" + getName() + ",index=" + index + ']';
  }

  void setDaemon (boolean isDaemon) {
    threadDataClone().isDaemon = isDaemon;
  }

  public boolean isDaemon () {
    return threadData.isDaemon;
  }

  MJIEnv getMJIEnv () {
    return env;
  }

  void setName (String newName) {
    threadDataClone().name = newName;

    // see 'setPriority()', only that it's more serious here, because the
    // java.lang.Thread name is stored as a char[]
  }

  public void setPriority (int newPrio) {
    if (threadData.priority != newPrio) {
      threadDataClone().priority = newPrio;

      // note that we don't update the java.lang.Thread object, but
      // use our threadData value (which works because the object
      // values are just used directly from the Thread ctors (from where we pull
      // it out in our ThreadInfo ctor), and henceforth only via our intercepted
      // native getters
    }
  }

  public int getPriority () {
    return threadData.priority;
  }

  /**
   * this is the method that factorizes common Thread object initialization
   * (get's called by all ctors).
   * BEWARE - it's hidden magic (undocumented), and should be replaced by our
   * own Thread impl at some point
   */
  void init (int rGroup, int rRunnable, int rName, long stackSize,
             boolean setPriority) {
    rGroup = 0;            // Get rid fo IDE warnings
    stackSize = 0;
    setPriority = false;
     
    ElementInfo ei = vm.getElementInfo(rName);

    threadDataClone();
    threadData.name = ei.asString();
    threadData.target = rRunnable;
    //threadData.status = NEW; // should not be neccessary

    // stackSize and setPriority are only used by native subsystems
  }


  /**
   * Comparison for sorting based on index.
   */
  public int compareTo (ThreadInfo that) {
    return this.index - that.index;
  }
  
  /**
   * only for debugging purposes
   */
  public void checkConsistency(boolean isStore){
    checkAssertion(threadData != null, "no thread data");
    
    // if the thread is runnable, it can't be blocked
    if (isRunnable()){
      checkAssertion(lockRef == -1, "runnable thread with non-null lockRef: " + lockRef) ;
    }
    
    // every thread that has been started and is not terminated has to have a stackframe with a next pc
    if (!isTerminated() && !isNew()){
      checkAssertion( stackDepth > 0, "empty stack " + getState());
      checkAssertion( top != null, "no top frame");
      checkAssertion( top.getPC() != null, "no top PC");
    }
    
    // if we are timedout, the top pc has to be either on a native Object.wait() or Unsafe.park()
    if (isTimedOut()){
      Instruction insn = top.getPC();
      checkAssertion( insn instanceof EXECUTENATIVE, "thread timedout outside of native method");
      
      // this is a bit dangerous in case we introduce new timeout points
      MethodInfo mi = ((EXECUTENATIVE)insn).getExecutedMethod();
      String mname = mi.getUniqueName();
      checkAssertion( mname.equals("wait(J") || mname.equals("park(ZJ"), "timedout thread outside timeout method: " + mi.getCompleteName());
    }
    
    if (lockRef != -1){
      // object we are blocked on has to exist
      ElementInfo ei = this.getElementInfo(lockRef);
      checkAssertion( ei != null, "thread locked on non-existing object: " + lockRef);
      
      // we have to be in the lockedThreads list of that objects monitor
      checkAssertion( ei.isLocking(this), "thread blocked on non-locking object: " + ei);
      
      // can't be blocked on a lock we own (but could be in waiting before giving it up)
      if (!isWaiting() && lockedObjects != null && !lockedObjects.isEmpty()){
        for (ElementInfo lei : lockedObjects){
            checkAssertion( lei.getIndex() != lockRef, "non-waiting thread blocked on owned lock: " + lei);
        }
      }
      
      // the state has to be BLOCKED, NOTIFIED, WAITING or TIMEOUT_WAITING
      checkAssertion( isWaiting() || isBlockedOrNotified(), "locked thread not waiting, blocked or notified");
      
    } else { // no lockRef set
      checkAssertion( !isWaiting() && !isBlockedOrNotified(), "non-locked thread is waiting, blocked or notified");
    }
    
    // if we have locked objects, we have to be the locking thread of each of them
    if (lockedObjects != null && !lockedObjects.isEmpty()){
      for (ElementInfo ei : lockedObjects){
        ThreadInfo lti = ei.getLockingThread();
        if (lti != null){
          checkAssertion(lti == this, "not the locking thread for locked object: " + ei + " owned by " + lti);
        } else {
          // can happen for a nested monitor lockout
        }
      }
    }

    if (!isStore){
      // all stack frames have to be set to unchanged
      for (StackFrame f = top; f != null; f = f.getPrevious()){
        checkAssertion( !f.hasChanged(), "changed stackframe upon restore: " + f);
      }
    }
  }
  
  protected void checkAssertion(boolean cond, String failMsg){
    if (!cond){
      System.out.println("!!!!!! failed thread consistency: "  + this + ": " + failMsg);
      vm.dumpThreadStates();
      assert false;
    }
  }
}
