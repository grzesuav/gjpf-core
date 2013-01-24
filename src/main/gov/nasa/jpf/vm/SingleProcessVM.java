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
package gov.nasa.jpf.vm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFTargetException;
import gov.nasa.jpf.JPFException;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 * 
 * Includes the VM initialization for executing single Java process
 * 
 * To use this jpf.properties includes,
 *              vm.class = gov.nasa.jpf.vm.SingleProcessVM
 */
public class SingleProcessVM extends VM {

  protected SingleProcessVM (){}

  public SingleProcessVM (JPF jpf, Config conf) {
    super(jpf, conf);
  }

  @Override
  public void initSystemClassLoaders (Config config) {
    checkTarget(config);
    int mainThreadId = 0;
    systemClassLoader = createSystemClassLoader(config.getTarget(), mainThreadId, config.getTargetArgs());
  }

  @Override
  protected ThreadInfo createMainThreadInfo(int id) {
    return new ThreadInfo(this, id);
  }

  @Override
  protected ThreadInfo createThreadInfo (int objRef, int groupRef, int runnableRef, int nameRef) {
    return new ThreadInfo( this, objRef, groupRef, runnableRef, nameRef);
  }

  @Override
  public void checkTarget(Config config) {
    String target = config.getTarget();
    if (target == null || (target.length() == 0)) {
      throw new JPFTargetException("no target class specified, terminating");
    }
  }

  /**
   * load and pushClinit startup classes, return 'true' if successful.
   *
   * This loads a bunch of core library classes, initializes the tiMain thread,
   * and then all the required startup classes, but excludes the static init of
   * the tiMain class. Note that whatever gets executed in here should NOT contain
   * any non-determinism, since we are not backtrackable yet, i.e.
   * non-determinism in clinits should be constrained to the app class (and
   * classes used by it)
   */
  @Override
  public boolean initialize () {
    ClassInfoException cie = null;

    if (!checkClassName(systemClassLoader.getMainClassName())) {
      log.severe("Not a valid main class: " + systemClassLoader.getMainClassName());
      return false;
    }

    ThreadInfo.currentThread = systemClassLoader.getMainThread();

    // from here, we get into some bootstrapping process
    //  - first, we have to load class structures (fields, supers, interfaces..)
    //  - second, we have to create a thread (so that we have a stack)
    //  - third, with that thread we have to create class objects
    //  - forth, we have to push the clinit methods on this stack
    try {
      systemClassLoader.registerStartupClasses(this);

      if (systemClassLoader.getStartupQueue() == null) {
        log.severe("error initializing startup classes (check 'classpath' and 'target')");
        return false;
      }

      if (!checkModelClassAccess(systemClassLoader)) {
        log.severe( "error during VM runtime initialization: wrong model classes (check 'classpath')");
        return false;
      }
    } catch (ClassInfoException e) {
      // If loading a system class is failed, bail out immediately
      if(e.checkSystemClassFailure()) {
        throw new JPFException("loading the system class " + e.getFaildClass() + " faild");
      }

      // If loading of a non-system class failed, just store it & throw a JPF exception
      // once the main thread is created
      cie = e;
    }

    ThreadInfo tiMain = systemClassLoader.getMainThread();

    try {
      // Collections.<clinit> yet because there's no stack before we have a tiMain
      // thread. Let's hope none of the init classes creates threads in their <clinit>.
      systemClassLoader.initMainThread();

      if(cie != null) {
        tiMain.getEnv().throwException(cie.getExceptionClass(), cie.getMessage());
        return false;
      }

      // now that we have a tiMain thread, we can finish the startup class init
      systemClassLoader.createStartupClassObjects();

      // pushClinit the call stack with the clinits we've picked up, followed by tiMain()
      systemClassLoader.pushMainEntry();
      systemClassLoader.pushClinits();

      initSystemState(tiMain);
      systemClassLoader.registerThreadListCleanup();
    } catch (ClassInfoException e) {
      // If the main thread is not created due to an error thrown while loading a class, 
      // bail out immediately
      if(tiMain == null) {
        throw new JPFException("loading of the class " + e.getFaildClass() + " faild");
      } else{
        tiMain.getEnv().throwException(e.getExceptionClass(), e.getMessage());
        return false;
      }
    }

    initialized = true;
    notifyVMInitialized();
    
    return true;
  }

  @Override
  public SystemClassLoaderInfo getSystemClassLoader() {
    return systemClassLoader;
  }

  @Override
  public SystemClassLoaderInfo getSystemClassLoader(ThreadInfo ti) {
    return systemClassLoader;
  }

  @Override
  public String getSuT() {
    ClassInfo ciMain = systemClassLoader.getMainClassInfo();
    return ciMain.getSourceFileName();
  }

  /**
   * The program is terminated if there are no alive threads, and there is no nonDaemon left.
   * 
   * NOTE - this is only approximated in real life. Daemon threads can still run for a few cycles
   * after the last non-daemon died, which opens an interesting source of errors we
   * actually might want to check for
   */
  @Override
  public boolean isEndState () {
    // note this uses 'alive', not 'runnable', hence isEndStateProperty won't
    // catch deadlocks - but that would be NoDeadlockProperty anyway
    return !getThreadList().hasMoreThreadsToRun();
  }

  @Override
  public boolean isDeadlocked () { 
    boolean hasNonDaemons = false;
    boolean hasBlockedThreads = false;

    if (ss.isBlockedInAtomicSection()) {
      return true; // blocked in atomic section
    }

    ThreadInfo[] threads = getThreadList().getThreads();

    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      
      if (ti.isAlive()){
        hasNonDaemons |= !ti.isDaemon();

        // shortcut - if there is at least one runnable, we are not deadlocked
        if (ti.isTimeoutRunnable()) { // willBeRunnable() ?
          return false;
        }

        // means it is not NEW or TERMINATED, i.e. live & blocked
        hasBlockedThreads = true;
      }
    }

    return (hasNonDaemons && hasBlockedThreads);
  }

  @Override
  public boolean hasOnlyDaemonRunnablesOtherThan (ThreadInfo ti){
    ThreadInfo[] threads = getThreadList().getThreads();
    int n = threads.length;

    for (int i=0; i<n; i++) {
      ThreadInfo t = threads[i];
      if (t != ti) {
        if (t.isRunnable() && t.isDaemon()) {
          return true;
        }
      }
    }

    return false;
  }

}
