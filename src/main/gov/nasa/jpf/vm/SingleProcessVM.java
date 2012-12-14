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
    systemClassLoader = createSystemClassLoader(config.getTarget(), config.getTargetArgs());
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
}
