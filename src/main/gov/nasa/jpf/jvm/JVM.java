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
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.JPFListenerException;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.choice.ThreadChoiceFromSet;
import gov.nasa.jpf.util.Source;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;


/**
 * This class represents the virtual machine. The virtual machine is able to
 * move backward and forward one transition at a time.
 */
public class JVM {

  protected static Logger log = JPF.getLogger("gov.nasa.jpf.jvm.JVM");

  /**
   * our execution context
   */
  JPF jpf;

  /**
   * The number of errors saved so far.
   * Used to generate the name of the error trail file.
   */
  protected static int error_id;

  /**
   * <2do> - this is a hack to be removed once there are no static references
   * anymore
   */
  protected static JVM jvm;

  static {
    initStaticFields();
  }

  protected SystemState ss;

  // <2do> - if you are confused about the various pieces of state and its
  // storage/backtrack structures, I'm with you. It's mainly an attempt to
  // separate non-policy VM state (objects), policy VM state (Scheduler)
  // and general JPF execution state, with special support for stack oriented
  // state restoration (backtracking).
  // this needs to be cleaned up and the principle reinstated


  protected String mainClassName;
  protected String[] args;  /** main() arguments */

  protected Path path;  /** execution path to current state */
  protected StringBuilder out;  /** buffer to store output along path execution */


  /**
   * various caches for VMListener state acqusition. NOTE - these are only
   * valid during notification
   *
   * <2do> get rid of the 'lasts' in favor of queries on the insn, the executing
   * thread, and the VM
   */
  protected Transition      lastTrailInfo;
  protected ClassInfo       lastClassInfo;
  protected ThreadInfo      lastThreadInfo;
  protected Instruction     lastInstruction;
  protected Instruction     nextInstruction;
  protected ElementInfo     lastElementInfo;
  protected ChoiceGenerator<?> lastChoiceGenerator;

  protected boolean isTraceReplay; // can be set by listeners to indicate this is a replay

  /** the repository we use to find out if we already have seen a state */
  protected StateSet stateSet;

  protected int newStateId;

  /** the structure responsible for storing and restoring backtrack info */
  protected Backtracker backtracker;

  /** optional serializer/restorer to support backtracker */
  protected StateRestorer<?> restorer;

  /** optional serializer to support stateSet */
  protected StateSerializer serializer;

  /** potential execution listeners */
  protected VMListener    listener;

  protected Config config; // that's for the options we use only once

  // JVM options we use frequently
  protected boolean runGc;
  protected boolean treeOutput;
  protected boolean pathOutput;
  protected boolean indentOutput;

  /**
   * VM instances are another example of evil throw-up ctors, but this is
   * justified by the fact that they are only created via (configured)
   * reflection from within the safe confines of the JPF ctor - which
   * shields clients against blowups
   */
  public JVM (JPF jpf, Config conf) {
    this.jpf = jpf; // so that we know who instantiated us

    // <2do> that's really a bad hack and should be removed once we
    // have cleaned up the reference chains
    jvm = this;

    config = conf;

    runGc = config.getBoolean("vm.gc", true);
    treeOutput = config.getBoolean("vm.tree_output", true);
    // we have to defer setting pathOutput until we have a reporter registered
    indentOutput = config.getBoolean("vm.indent_output",false);

try {
    initSubsystems(config);
    initFields(config);
} catch (Throwable t){
  t.printStackTrace(System.err);
}
  }

  public JPF getJPF() {
    return jpf;
  }

  public void initFields (Config config) {
    mainClassName = config.getTarget(); // we don't get here if it wasn't set
    args = config.getTargetArgs();

    path = new Path(mainClassName);
    out = null;

    ss = new SystemState(config, this);

    stateSet = config.getInstance("vm.storage.class", StateSet.class);
    if (stateSet != null) stateSet.attach(this);
    backtracker = config.getEssentialInstance("vm.backtracker.class", Backtracker.class);
    backtracker.attach(this);
  }

  protected void initSubsystems (Config config) {
    ClassInfo.init(config);
    ThreadInfo.init(config);
    MethodInfo.init(config);
    DynamicArea.init(config);
    StaticArea.init(config);
    NativePeer.init(config);
    FieldInstruction.init(config);
    ChoiceGenerator.init(config);

    // peer classes get initialized upon NativePeer creation
  }

  /**
   * do we see our model classes? Some of them cannot be used from the standard CLASSPATH, because they
   * are tightly coupled with the JPF core (e.g. java.lang.Class, java.lang.Thread,
   * java.lang.StackTraceElement etc.)
   * Our strategy here is kind of lame - we just look into java.lang.Class, if we find the 'int cref' field
   * (that's a true '42')
   */
  static boolean checkModelClassAccess () {
    ClassInfo ci = ClassInfo.getClassInfo("java.lang.Class");
    return (ci.getDeclaredInstanceField("cref") != null);
  }

  static boolean checkClassName (String clsName) {
    if ( !clsName.matches("[a-zA-Z_$][a-zA-Z_$0-9.]*")) {
      return false;
    }

    // well, those two could be part of valid class names, but
    // in all likeliness somebody specified a filename instead of
    // a classname
    if (clsName.endsWith(".java")) {
      return false;
    }
    if (clsName.endsWith(".class")) {
      return false;
    }

    return true;
  }

  /**
   * load and initialize startup classes, return 'true' if successful.
   *
   * This loads a bunch of core library classes, initializes the main thread,
   * and then all the required startup classes, but excludes the static init of
   * the main class. Note that whatever gets executed in here should NOT contain
   * any non-determinism, since we are not backtrackable yet, i.e.
   * non-determinism in clinits should be constrained to the app class (and
   * classes used by it)
   */
  public boolean initialize () {

    if (!checkClassName(mainClassName)) {
      log.severe("not a valid class name: " + mainClassName);
      return false;
    }

    // from here, we get into some bootstrapping process
    //  - first, we have to load class structures (fields, supers, interfaces..)
    //  - second, we have to create a thread (so that we have a stack)
    //  - third, with that thread we have to create class objects
    //  - forth, we have to push the clinit methods on this stack
    List<ClassInfo> clinitQueue = registerStartupClasses();

    if (clinitQueue== null) {
      log.severe("error initializing startup classes (check 'classpath')");
      return false;
    }

    if (!checkModelClassAccess()) {
      log.severe( "error during VM runtime initialization: wrong model classes (check 'classpath')");
      return false;
    }

    // create the thread for the main class
    // note this is incomplete for Java 1.3 where Thread ctors rely on main's
    // 'inheritableThreadLocals' being set to 'Collections.EMPTY_SET', which
    // pulls in the whole Collections/Random smash, but we can't execute the
    // Collections.<clinit> yet because there's no stack before we have a main
    // thread. Let's hope none of the init classes creates threads in their <clinit>.
    ThreadInfo main = createMainThread();

    // now that we have a main thread, we can finish the startup class init
    createStartupClassObjects(clinitQueue, main);

    // initialize the call stack with the clinits we've picked up, followed by main()
    pushMain(config);
    pushClinits(clinitQueue, main);

    initSystemState(main);
    return true;
  }

  protected void initSystemState (ThreadInfo mainThread){
    // the first transition probably doesn't have much choice (unless there were
    // threads started in the static init), but we want to keep it uniformly anyways
    ChoiceGenerator<?> cg = new ThreadChoiceFromSet(getThreadList().getRunnableThreads(), true);
    ss.setNextChoiceGenerator(cg);
    ss.setStartThread(mainThread);

    ss.recordSteps(hasToRecordSteps());

    if (!pathOutput) { // don't override if explicitly requested
      pathOutput = hasToRecordPathOutput();
    }
  }

  /**
   * be careful - everything that's executed from within here is not allowed
   * to depend on static class init having been done yet
   *
   * we have to do the initialization excplicitly here since we can't execute
   * bytecode yet (which would need a ThreadInfo context)
   */
  protected ThreadInfo createMainThread () {
    DynamicArea da = getDynamicArea();

    // first we need a group for this baby (happens to be called "main")

    int tObjRef = da.newObject(ClassInfo.getClassInfo("java.lang.Thread"), null);
    int grpObjref = createSystemThreadGroup(tObjRef);

    ElementInfo ei = da.get(tObjRef);
    ei.setReferenceField("group", grpObjref);
    ei.setReferenceField("name", da.newString("main", null));
    ei.setIntField("priority", Thread.NORM_PRIORITY);

    int permitRef = da.newObject(ClassInfo.getClassInfo("java.lang.Thread$Permit"),null);
    ElementInfo eiPermitRef = da.get(permitRef);
    eiPermitRef.setBooleanField("isTaken", true);
    ei.setReferenceField("permit", permitRef);

    // we need to keep the attributes on the JPF side in sync here
    // <2do> factor out the Thread/ThreadInfo creation so that it's less
    // error prone (even so this is the only location it's required for)
    ThreadInfo ti = ThreadInfo.createThreadInfo(this, tObjRef);
    ti.setPriority(java.lang.Thread.NORM_PRIORITY);
    ti.setName("main");
    ti.setState(ThreadInfo.State.RUNNING);

    return ti;
  }

  protected int createSystemThreadGroup (int mainThreadRef) {
    DynamicArea da = getDynamicArea();

    int ref = da.newObject(ClassInfo.getClassInfo("java.lang.ThreadGroup"), null);
    ElementInfo ei = da.get(ref);

    // since we can't call methods yet, we have to init explicitly (BAD)
    // <2do> - this isn't complete yet

    int grpName = da.newString("main", null);
    ei.setReferenceField("name", grpName);

    ei.setIntField("maxPriority", java.lang.Thread.MAX_PRIORITY);

    int threadsRef = da.newArray("Ljava/lang/Thread;", 4, null);
    ElementInfo eiThreads = da.get(threadsRef);
    eiThreads.setElement(0, mainThreadRef);

    ei.setReferenceField("threads", threadsRef);

    ei.setIntField("nthreads", 1);

    return ref;
  }


  protected List<ClassInfo> registerStartupClasses () {
    ArrayList<ClassInfo> queue = new ArrayList<ClassInfo>(32);

    String[] startupClasses = {  // order matters
        // bare essentials
        "java.lang.Object",
        "java.lang.Class",
        "java.lang.ClassLoader",

        // the builtin types (and their arrays)
        "boolean",
        "[Z",
        "byte",
        "[B",
        "char",
        "[C",
        "short",
        "[S",
        "int",
        "[I",
        "long",
        "[J",
        "float",
        "[F",
        "double",
        "[D",
        "void",

        // the box types
        "java.lang.Boolean",
        "java.lang.Character",
        "java.lang.Short",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Float",
        "java.lang.Double",

        // standard system classes
        "java.lang.String",
        "java.lang.ThreadGroup",
        "java.lang.Thread",
        "java.lang.Thread$State",
        "java.io.PrintStream",
        "java.io.InputStream",
        "java.lang.System",

        mainClassName
    };

    for (String clsName : startupClasses) {
      ClassInfo ci = ClassInfo.getClassInfo(clsName);
      if (ci != null) {
        registerStartupClass(ci, queue);
      } else {
        log.severe("can't find startup class: " + clsName);
        return null;
      }
    }

    return queue;
  }


  // note this has to be in order - we don't want to init a derived class before
  // it's parent is initialized
  void registerStartupClass (ClassInfo ci, List<ClassInfo> queue) {
    StaticArea sa = getStaticArea();

    if (!queue.contains(ci)) {

      if (ci.getSuperClass() != null) {
        registerStartupClass( ci.getSuperClass(), queue);
      }

      queue.add(ci);

      if (!sa.containsClass(ci.getName())){
        sa.addClass(ci);
      }
    }
  }


  protected void createStartupClassObjects (List<ClassInfo> queue, ThreadInfo ti){
    for (ClassInfo ci : queue) {
      ci.createClassObject(ti);
    }
  }

  protected void pushClinits (List<ClassInfo> queue, ThreadInfo ti) {
    // we have to traverse backwards, since what gets pushed last is executed first
    for (ListIterator<ClassInfo> it=queue.listIterator(queue.size()); it.hasPrevious(); ) {
      ClassInfo ci = it.previous();

      MethodInfo mi = ci.getMethod("<clinit>()V", false);
      if (mi != null) {
        MethodInfo stub = mi.createDirectCallStub("[clinit]");
        StackFrame frame = new DirectCallStackFrame(stub);
        ti.pushFrame(frame);
      } else {
        ci.setInitialized();
      }
    }
  }

  protected void pushMain (Config config) {
    DynamicArea da = ss.ks.da;
    ClassInfo ci = ClassInfo.getClassInfo(mainClassName);
    MethodInfo mi = ci.getMethod("main([Ljava/lang/String;)V", false);
    ThreadInfo ti = ss.getThreadInfo(0);

    if (mi == null || !mi.isStatic()) {
      throw new JPFException("no main() method in " + ci.getName());
    }

    ti.pushFrame(new StackFrame(mi, null));

    int argsObjref = da.newArray("Ljava/lang/String;", args.length, null);
    ElementInfo argsElement = ss.ks.da.get(argsObjref);

    for (int i = 0; i < args.length; i++) {
      int stringObjref = da.newString(args[i], null);
      argsElement.setElement(i, stringObjref);
    }
    ti.setLocalVariable(0, argsObjref, true);
  }

  public void addListener (VMListener newListener) {
    listener = VMListenerMulticaster.add(listener, newListener);
  }

  public boolean hasListenerOfType (Class<?> listenerCls) {
    return VMListenerMulticaster.containsType(listener,listenerCls);
  }

  public void removeListener (VMListener removeListener) {
    listener = VMListenerMulticaster.remove(listener,removeListener);
  }

  public void setTraceReplay (boolean isReplay) {
    isTraceReplay = isReplay;
  }

  public boolean isTraceReplay() {
    return isTraceReplay;
  }

  public boolean hasToRecordSteps() {
    // we have to record if there either is a reporter that has
    // a 'trace' topic, or there is an explicit request
    return jpf.getReporter().hasToReportTrace()
             || config.getBoolean("vm.store_steps");
  }

  public boolean hasToRecordPathOutput() {
    return jpf.getReporter().hasToReportOutput();
  }

  protected void notifyChoiceGeneratorSet (ChoiceGenerator<?>cg) {
    if (listener != null) {
      try {
        lastChoiceGenerator = cg;
        listener.choiceGeneratorSet(this);
        lastChoiceGenerator = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during choiceGeneratorSet() notification", t);
      }
    }
  }

  protected void notifyChoiceGeneratorAdvanced (ChoiceGenerator<?>cg) {
    if (listener != null) {
      try {
        lastChoiceGenerator = cg;
        listener.choiceGeneratorAdvanced(this);
        lastChoiceGenerator = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during choiceGeneratorAdvanced() notification", t);
      }
    }
  }

  protected void notifyChoiceGeneratorProcessed (ChoiceGenerator<?>cg) {
    if (listener != null) {
      try {
        lastChoiceGenerator = cg;
        listener.choiceGeneratorProcessed(this);
        lastChoiceGenerator = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during choiceGeneratorProcessed() notification", t);
      }
    }
  }

  protected void notifyExecuteInstruction (ThreadInfo ti, Instruction insn) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        lastInstruction = insn;

        listener.executeInstruction(this);

        //nextInstruction = null;
        //lastInstruction = null;
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during executeInstruction() notification", t);
      }
    }
  }

  protected void notifyInstructionExecuted (ThreadInfo ti, Instruction insn, Instruction nextInsn) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        lastInstruction = insn;
        nextInstruction = nextInsn;

        listener.instructionExecuted(this);

        //nextInstruction = null;
        //lastInstruction = null;
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during instructionExecuted() notification", t);
      }

    }
  }

  protected void notifyThreadStarted (ThreadInfo ti) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        listener.threadStarted(this);
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during threadStarted() notification", t);
      }
    }
  }

  // NOTE: the supplied ThreadInfo does NOT have to be the running thread, as this
  // notification can occur as a result of a lock operation in the current thread
  protected void notifyThreadBlocked (ThreadInfo ti) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        lastElementInfo = ti.getLockObject();
        listener.threadBlocked(this);
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during threadBlocked() notification", t);
      }
    }
  }

  protected void notifyThreadWaiting (ThreadInfo ti) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        listener.threadWaiting(this);
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during threadWaiting() notification", t);
      }
    }
  }

  protected void notifyThreadNotified (ThreadInfo ti) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        listener.threadNotified(this);
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during threadNotified() notification", t);
      }
    }
  }

  protected void notifyThreadInterrupted (ThreadInfo ti) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        listener.threadInterrupted(this);
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during threadInterrupted() notification", t);
      }
    }
  }

  protected void notifyThreadTerminated (ThreadInfo ti) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        listener.threadTerminated(this);
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during threadTerminated() notification", t);
      }
    }
  }

  protected void notifyThreadScheduled (ThreadInfo ti) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        listener.threadScheduled(this);
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during threadScheduled() notification", t);
      }
    }
  }

  protected void notifyClassLoaded (ClassInfo ci) {
    if (listener != null) {
      try {
        lastClassInfo = ci;
        listener.classLoaded(this);
        //lastClassInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during classLoaded() notification", t);
      }
    }
  }

  protected void notifyObjectCreated (ThreadInfo ti, ElementInfo ei) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        lastElementInfo = ei;

        listener.objectCreated(this);

        //lastElementInfo = null;
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during objectCreated() notification", t);
      }
    }
  }

  protected void notifyObjectReleased (ElementInfo ei) {
    if (listener != null) {
      try {
        lastElementInfo = ei;
        listener.objectReleased(this);
        //lastElementInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during objectReleased() notification", t);
      }
    }
  }

  protected void notifyObjectLocked (ThreadInfo ti, ElementInfo ei){
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        lastElementInfo = ei;

        listener.objectLocked(this);

        //lastElementInfo = null;
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during objectLocked() notification", t);
      }
    }
  }

  protected void notifyObjectUnlocked (ThreadInfo ti, ElementInfo ei) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        lastElementInfo = ei;

        listener.objectUnlocked(this);

        //lastElementInfo = null;
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during objectUnlocked() notification", t);
      }
    }
  }

  protected void notifyObjectWait (ThreadInfo ti, ElementInfo ei) {
    if (listener != null) {
      try { 
        lastThreadInfo = ti;
        lastElementInfo = ei;

        listener.objectWait(this);

        //lastElementInfo = null;
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during objectWait() notification", t);
      }
    }
  }

  protected void notifyObjectNotifies (ThreadInfo ti, ElementInfo ei) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        lastElementInfo = ei;

        listener.objectNotify(this);

        //lastElementInfo = null;
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during objectNotifies() notification", t);
      }
    }
  }

  protected void notifyObjectNotifiesAll (ThreadInfo ti, ElementInfo ei) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        lastElementInfo = ei;

        listener.objectNotifyAll(this);

        //lastElementInfo = null;
        //lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during objectNotifiesAll() notification", t);
      }
    }
  }

  protected void notifyGCBegin () {
    if (listener != null) {
      try {
        listener.gcBegin(this);
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during gcBegin() notification", t);
      }
    }
  }

  protected void notifyGCEnd () {
    if (listener != null) {
      try {
        listener.gcEnd(this);
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during gcEnd() notification", t);
      }
    }
  }

  protected void notifyExceptionThrown (ThreadInfo ti, ElementInfo ei) {
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        lastElementInfo = ei;

        listener.exceptionThrown(this);

        lastElementInfo = null;
        lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during exceptionThrown() notification", t);
      }
    }
  }

  protected void notifyExceptionBailout (ThreadInfo ti){
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        listener.exceptionBailout(this);
        lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during exceptionBailout() notification", t);
      }
    }
    
  }

  protected void notifyExceptionHandled (ThreadInfo ti){
    if (listener != null) {
      try {
        lastThreadInfo = ti;
        listener.exceptionHandled(this);
        lastThreadInfo = null;
      } catch (UncaughtException x) {
        throw x;
      } catch (JPF.ExitException x) {
        throw x;
      } catch (Throwable t){
        throw new JPFListenerException("exception during exceptionHandler() notification", t);
      }
    }

  }

  // VMListener acquisition
  public int getThreadNumber () {
    if (lastThreadInfo != null) {
      return lastThreadInfo.getIndex();
    } else {
      return -1;
    }
  }

  // VMListener acquisition
  public String getThreadName () {
    ThreadInfo ti = ThreadInfo.getCurrentThread();

    return ti.getName();
  }

  // VMListener acquisition
  Instruction getInstruction () {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    return ti.getPC();
  }


  public int getAbstractionNonDeterministicThreadCount () {
    int n = 0;
    int imax = ss.getThreadCount();

    for (int i = 0; i < imax; i++) {
      ThreadInfo th = ss.getThreadInfo(i);

      if (th.isAbstractionNonDeterministic()) {
        n++;
      }
    }

    return n;
  }

  public int getAliveThreadCount () {
    return getThreadList().getLiveThreadCount();
  }

  public ExceptionInfo getPendingException () {
    ThreadInfo ti = ThreadInfo.getCurrentThread();

    if (ti != null){
      return ti.getPendingException();
    } else {
      return null;
    }
  }

  public boolean isBoringState () {
    return ss.isBoring();
  }

  public StaticElementInfo getClassReference (String name) {
    return ss.ks.sa.get(name);
  }

  public boolean hasPendingException () {
    return (ThreadInfo.currentThread.pendingException != null);
  }

  public boolean isDeadlocked () {
    return ss.isDeadlocked();
  }

  public boolean isEndState () {
    // note this uses 'alive', not 'runnable', hence isEndStateProperty won't
    // catch deadlocks - but that would be NoDeadlockProperty anyway
    return ss.isEndState();
  }

  public Exception getException () {
    return ss.getUncaughtException();
  }

  public boolean isInterestingState () {
    return ss.isInteresting();
  }

  public Step getLastStep () {
    Transition trail = ss.getTrail();
    if (trail != null) {
      return trail.getLastStep();
    }

    return null;
  }

  public Transition getLastTransition () {
    if (path.size() == 0) {
      return null;
    }
    return path.get(path.size() - 1);
  }

  /**
   * answer the ClassInfo that was loaded most recently
   * part of the VMListener state acqusition (only valid from inside of
   * notification)
   */
  public ClassInfo getLastClassInfo () {
    return lastClassInfo;
  }

  /**
   * answer the ThreadInfo that was most recently started or finished
   * part of the VMListener state acqusition (only valid from inside of
   * notification)
   */
  public ThreadInfo getLastThreadInfo () {
    return lastThreadInfo;
  }

  /**
   * answer the last executed Instruction
   * part of the VMListener state acqusition (only valid from inside of
   * notification)
   */
  public Instruction getLastInstruction () {
    return lastInstruction;
  }

  /**
   * answer the next Instruction to execute in the current thread
   * part of the VMListener state acqusition (only valid from inside of
   * notification)
   */
  public Instruction getNextInstruction () {
    return nextInstruction;
  }

  /**
   * answer the Object that was most recently created or collected
   * part of the VMListener state acqusition (only valid from inside of
   * notification)
   */
  public ElementInfo getLastElementInfo () {
    return lastElementInfo;
  }

  /**
   * return the most recently used CoiceGenerator
   */
  public ChoiceGenerator<?>getLastChoiceGenerator () {
    return lastChoiceGenerator;
  }

  /**
   * answer the ClassInfo that was loaded most recently
   * part of the VMListener state acquisition
   */
  public ClassInfo getClassInfo () {
    return lastClassInfo;
  }

  public ClassInfo getClassInfo (int objref) {
    if (objref != MJIEnv.NULL) {
      return getDynamicArea().get(objref).getClassInfo();
    } else {
      return null;
    }
  }

  public String getMainClassName () {
    return mainClassName;
  }

  public ClassInfo getMainClassInfo () {
    return ClassInfo.getClassInfo(mainClassName);
  }

  public String[] getArgs () {
    return args;
  }

  /**
   * NOTE: only use this locally, since the path is getting modified by the VM
   *
   * The path only contains all states when queried from a stateAdvanced() notification.
   * If this is called from an instructionExecuted() (or other VMListener), and you need
   * the ongoing transition in it, you have to call updatePath() first
   */
  public Path getPath () {
    return path;
  }

  /**
   * this is the ongoing transition. Note that it is not yet stored in the path
   * if this is called from a VMListener notification
   */
  public Transition getCurrentTransition() {
    return ss.getTrail();
  }

  /**
   * use that one if you have to store the path for subsequent use
   *
   * NOTE: without a prior call to updatePath(), this does NOT contain the
   * ongoing transition. See getPath() for usage from a VMListener
   */
  public Path getClonedPath () {
    return path.clone();
  }

  public int getPathLength () {
    return path.size();
  }


  public int getRunnableThreadCount () {
    return ss.getRunnableThreadCount();
  }

  public ThreadList getThreadList () {
    return getKernelState().getThreadList();
  }

  /**
   * Bundles up the state of the system for export
   */
  public VMState getState () {
    return new VMState(this);
  }

  /**
   * Gets the system state.
   */
  public SystemState getSystemState () {
    return ss;
  }

  public KernelState getKernelState () {
    return ss.ks;
  }

  public Config getConfig() {
    return config;
  }

  public Backtracker getBacktracker() {
    return backtracker;
  }

  @SuppressWarnings("unchecked")
  public <T> StateRestorer<T> getRestorer() {
    if (restorer == null) {
      if (serializer instanceof StateRestorer) {
        restorer = (StateRestorer<?>) serializer;
      } else if (stateSet instanceof StateRestorer) {
        restorer = (StateRestorer<?>) stateSet;
      } else {
        // config read only if serializer is not also a restorer
        restorer = config.getInstance("vm.restorer.class", StateRestorer.class);
        if (serializer instanceof IncrementalChangeTracker &&
            restorer instanceof IncrementalChangeTracker) {
          config.throwException("Incompatible serializer and restorer!");
        }
      }
      restorer.attach(this);
    }

    return (StateRestorer<T>) restorer;
  }

  public StateSerializer getSerializer() {
    if (serializer == null) {
      serializer = config.getEssentialInstance("vm.serializer.class",
                                      StateSerializer.class);
      serializer.attach(this);
    }
    return serializer;
  }

  /**
   * Returns the stateSet if states are being matched.
   */
  public StateSet getStateSet() {
    return stateSet;
  }

  /**
   * return the current SystemState's ChoiceGenerator object
   */
  public ChoiceGenerator<?> getChoiceGenerator () {
    return ss.getChoiceGenerator();
  }

  public boolean isTerminated () {
    return ss.ks.isTerminated();
  }

  public void print (String s) {
    if (treeOutput) {
      System.out.print(s);
    }

    if (pathOutput) {
      appendOutput(s);
    }
  }

  public void println (String s) {
    if (treeOutput) {
      if (indentOutput){
        StringBuilder indent = new StringBuilder();
        int i;
        for (i = 0;i<=path.size();i++) {
          indent.append('|').append(i);
        }
        indent.append("|").append(s);
        System.out.println(indent);
      }
      else {
        System.out.println(s);
      }
    }

    if (pathOutput) {
      appendOutput(s);
      appendOutput('\n');
    }
  }

  public void print (boolean b) {
    if (treeOutput) {
      System.out.print(b);
    }

    if (pathOutput) {
      appendOutput(Boolean.toString(b));
    }
  }

  public void print (char c) {
    if (treeOutput) {
      System.out.print(c);
    }

    if (pathOutput) {
      appendOutput(c);
    }
  }

  public void print (int i) {
    if (treeOutput) {
      System.out.print(i);
    }

    if (pathOutput) {
      appendOutput(Integer.toString(i));
    }
  }

  public void print (long l) {
    if (treeOutput) {
      System.out.print(l);
    }

    if (pathOutput) {
      appendOutput(Long.toString(l));
    }
  }

  public void print (double d) {
    if (treeOutput) {
      System.out.print(d);
    }

    if (pathOutput) {
      appendOutput(Double.toString(d));
    }
  }

  public void print (float f) {
    if (treeOutput) {
      System.out.print(f);
    }

    if (pathOutput) {
      appendOutput(Float.toString(f));
    }
  }

  public void println () {
    if (treeOutput) {
      System.out.println();
    }

    if (pathOutput) {
      appendOutput('\n');
    }
  }


  void appendOutput (String s) {
    if (out == null) {
      out = new StringBuilder();
    }
    out.append(s);
  }

  void appendOutput (char c) {
    if (out == null) {
      out = new StringBuilder();
    }
    out.append(c);
  }

  /**
   * this is here so that we can intercept it in subclassed VMs
   */
  public Instruction handleException (ThreadInfo ti, int xObjRef){
    return null;
  }

  public void storeTrace (String fileName, String comment, boolean verbose) {
    ChoicePoint.storeTrace(fileName, mainClassName, args, comment,
                           ss.getChoiceGenerators(), verbose);
  }

  public void storePathOutput () {
    pathOutput = true;
  }

  public ThreadInfo[] getLiveThreads () {
    int n = ss.getThreadCount();
    ThreadInfo[] list = new ThreadInfo[n];
    for (int i=0; i<n; i++){
      list[i] = ss.getThreadInfo(i);
    }
    return list;
  }

  /**
   * print call stacks of all live threads
   * this is also used for debugging purposes, so we can't move it to the Reporter system
   * (it's also using a bit too much internals for that)
   */
  public void printLiveThreadStatus (PrintWriter pw) {
    int imax = ss.getThreadCount();
    int n=0;

    for (int i = 0; i < imax; i++) {
      ThreadInfo ti = ss.getThreadInfo(i);
      List<StackFrame> stack = ti.getStack();

      if (stack.size() > 0) {
        n++;
        //pw.print("Thread: ");
        //pw.print(ti.getName());
        pw.println(ti.getStateDescription());

        LinkedList<ElementInfo> locks = ti.getLockedObjects();
        if (!locks.isEmpty()) {
          pw.print("  owned locks:");
          boolean first = true;
          for (ElementInfo e : locks) {
            if (first) {
              first = false;
            } else {
              pw.print(",");
            }
            pw.print(e);
          }
          pw.println();
        }

        ElementInfo ei = ti.getLockObject();
        if (ei != null) {
          if (ti.getState() == ThreadInfo.State.WAITING) {
            pw.print( "  waiting on: ");
          } else {
            pw.print( "  blocked on: ");
          }
          pw.println(ei);
        }

        pw.println("  call stack:");
        for (int j=stack.size()-1; j>=0; j--) { // we have to print this reverse
          StackFrame frame = stack.get(j);
          if (!frame.isDirectCallFrame()) {
            pw.print("\tat ");
            pw.println(frame.getStackTraceInfo());
          }
        }

        pw.println();
      }
    }

    if (n==0) {
      pw.println("no live threads");
    }
  }

  // just a Q&D debugging aid
  void dumpThreadStates () {
    java.io.PrintWriter pw = new java.io.PrintWriter(System.out, true);
    printLiveThreadStatus(pw);
    pw.flush();
  }

  /**
   * Moves one step backward. This method and forward() are the main methods
   * used by the search object.
   * Note this is called with the state that caused the backtrack still being on
   * the stack, so we have to remove that one first (i.e. popping two states
   * and restoring the second one)
   */
  public boolean backtrack () {
    boolean success = backtracker.backtrack();
    if (success) {
      // restore the path
      path.removeLast();
      lastTrailInfo = path.getLast();

      return ((ss.getId() != StateSet.UNKNOWN_ID) || (stateSet == null));
    } else {
      return false;
    }
  }

  /**
   * store the current SystemState's Trail in our path, after updating it
   * with whatever annotations the JVM wants to add.
   * This is supposed to be called after each transition we want to keep
   */
  public void updatePath () {
    Transition t = ss.getTrail();
    Transition tLast = path.getLast();

    // NOTE: don't add the transition twice, this is public and might get called
    // from listeners, so the transition object might get changed

    if (tLast != t) {
      // <2do> we should probably store the output directly in the TrailInfo,
      // but this might not be our only annotation in the future

      // did we have output during the last transition? If yes, add it
      if ((out != null) && (out.length() > 0)) {
        t.setOutput( out.toString());
        out.setLength(0);
      }

      path.add(t);
    }
  }

  /**
   * try to advance the state
   * forward() and backtrack() are the two primary interfaces towards the Search
   * driver
   * return 'true' if there was an un-executed sequence out of the current state,
   * 'false' if it was completely explored
   * note that the caller still has to check if there is a next state, and if
   * the executed instruction sequence led into a new or already visited state
   */
  public boolean forward () {
    while (true) { // loop until we find a state that isn't ignored
      try {
        // saves the current state for backtracking purposes of depth first
        // searches and state observers. If there is a previously cached
        // kernelstate, use that one
        backtracker.pushKernelState();

        // cache this before we execute (and increment) the next insn(s)
        lastTrailInfo = path.getLast();

        // execute the instruction(s) to get to the next state
        // this changes the SystemState (e.g. finds the next thread to run)
        if (ss.nextSuccessor(this)) {
          //for debugging locks:  -peterd
          //ss.ks.da.verifyLockInfo();

          if (ss.isIgnored()) {
            // do it again
            backtracker.backtrackKernelState();
            continue;

          } else { // this is the normal forward that executed insns, and wasn't ignored
            // runs the garbage collector (if necessary), which might change the
            // KernelState (DynamicArea). We need to do this before we hash the state to
            // find out if it is a new one
            // Note that we don't collect if there is a pending exception, since
            // we want to preserve as much state as possible for debug purposes
            if (runGc && !hasPendingException()) {
              ss.gcIfNeeded();
            }

            // saves the backtrack information. Unfortunately, we cannot cache
            // this (except of the optional lock graph) because it is changed
            // by the subsequent operations (before we return from forward)
            backtracker.pushSystemState();

            updatePath();
            break;
          }

        } else { // state was completely explored, no transition ocurred
          backtracker.popKernelState();
          return false;
        }

      } catch (UncaughtException e) {
        backtracker.pushSystemState(); // we need this in case we backtrack (multiple_errors)
        updatePath(); // or we loose the last transition
        // something blew up, so we definitely executed something (hence return true)
        return true;
      } catch (RuntimeException e) {
        throw e;
        //throw new JPFException(e);
      }
    }

    if (stateSet != null) {
      newStateId = stateSet.size();
      int id = stateSet.addCurrent();
      ss.setId(id);
    } else { // this is 'state-less' model checking, i.e. we don't match states
      ss.setId(newStateId++); // but we still should have states numbered in case listeners use the id
    }

    // the idea is that search objects or observers can query the state
    // *after* forward/backtrack was called, and that all changes of the
    // System/KernelStates happen from *within* forward/backtrack, i.e. the
    // (expensive) getBacktrack/storingData operations can be cached and used
    // w/o re-computation in the next forward pushXState()
    //cacheKernelState(); // for subsequent getState() and the next forward()

    return true;
  }


  /**
   * Prints the current stack trace. Just for debugging purposes
   */
  public void printCurrentStackTrace () {
    ThreadInfo th = ThreadInfo.getCurrentThread();

    if (th != null) {
      th.printStackTrace();
    }
  }


  public void restoreState (VMState state) {
    if (state.path == null) {
      throw new JPFException("tried to restore partial VMState: " + state);
    }
    backtracker.restoreState(state.getBkState());
    path = state.path.clone();
  }

  public void activateGC () {
    ss.activateGC();
  }

  public void retainStateAttributes (boolean isRetained){
    ss.retainAttributes(isRetained);
  }

  public void forceState () {
    ss.setForced(true);
  }

  /**
   * override the state matching - ignore this state, no matter if we changed
   * the heap or stacks.
   * use this with care, since it prunes whole search subtrees
   */
  public void ignoreState () {
    ss.setIgnored(true);
  }

  /**
   * imperatively break the transition to enable state matching
   */
  public void breakTransition () {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    ti.breakTransition();
  }

  /**
   * answers if the current state already has been visited. This is mainly
   * used by the searches (to control backtracking), but could also be useful
   * for observers to build up search graphs (based on the state ids)
   */
  public boolean isNewState() {
    if (stateSet != null) {
      if (ss.isForced()){
        return true;
      } else if (ss.isIgnored()){
        return false;
      } else {
        return newStateId == ss.getId();
      }
    } else {
      return true;
    }
  }

  /**
   * get the numeric id for the current state
   * Note: this can be called several times (by the search and observers) for
   * every forward()/backtrack(), so we want to cache things a bit
   */
  public int getStateId() {
    return ss.getId();
  }

  public int getStateCount() {
    return newStateId;
  }

  public void addThread (ThreadInfo ti) {
    // link the new thread into the list
    ThreadList tl = getThreadList();

    int idx = tl.add(ti);
    ti.setListInfo(tl, idx);  // link back the thread to the list

    getKernelState().changed();
  }


  public ThreadInfo createThread (int objRef) {
    ThreadInfo ti = ThreadInfo.createThreadInfo(this, objRef);

    // we don't add this thread to the threadlist before it starts to execute
    // since it would otherwise already be a root object

    return ti;
  }

  public static JVM getVM () {
    // <2do> remove this, no more static refs!
    return jvm;
  }

  /**
   * initialize all our static fields. Called from <clinit> and reset
   */
  static void initStaticFields () {
    error_id = 0;
  }

  /**
   * return the 'heap' object, which is a global service
   */
  public DynamicArea getDynamicArea () {
    return ss.ks.da;
  }

  public ThreadInfo getCurrentThread () {
    return ThreadInfo.currentThread;
  }

  /**
   * same for "loaded classes", but be advised it will probably go away at some point
   */
  public StaticArea getStaticArea () {
    return ss.ks.sa;
  }

  /**
   * <2do> this is where we will hook in our time modeling
   */
  public long getTime () {
    return 1L;
  }

  public void resetNextCG() {
    if (ss.nextCg != null) {
      ss.nextCg.reset();
    }
  }


}




