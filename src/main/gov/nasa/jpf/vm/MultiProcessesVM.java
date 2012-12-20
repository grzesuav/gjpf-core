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

import java.util.List;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.JPFTargetException;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 * 
 * Includes the VM initialization for executing multiple Java processes 
 * 
 * To use this jpf.properties includes,
 *              vm.class = gov.nasa.jpf.vm.MultiProcessVM 
 */
public class MultiProcessesVM extends VM {

  public static int NUM_PRC = 0;

  protected SystemClassLoaderInfo[] systemClassLoaders;

  public MultiProcessesVM (JPF jpf, Config conf) {
    super(jpf, conf);
  }

  @Override
  public void initSystemClassLoaders (Config config) {
    checkTarget(config);

    String[] target = config.getProcessesTargets();
    List<String[]> targetArgs = config.getProcessesTargetArgs();

    NUM_PRC = target.length;
    systemClassLoaders = new SystemClassLoaderInfo[NUM_PRC];

    for(int i=0; i<NUM_PRC; i++) {
      systemClassLoaders[i] = createSystemClassLoader(target[i], targetArgs.get(i));
    }
  }

  @Override
  public void checkTarget(Config config) {
    String[] targets = config.getProcessesTargets();

    if(targets ==null) {
      throw new JPFTargetException("no target class specified, terminating");
    }

    for(String target: targets) {
      if (target == null || (target.length() == 0)) {
        throw new JPFTargetException("no target class specified, terminating");
      }
    }
  }

  public boolean checkTergetClassNames() {
    for(SystemClassLoaderInfo cl: systemClassLoaders) {

      String className = cl.getMainClassName();
      if (!checkClassName(className)) {
        log.severe("Not a valid main class: " + cl.getMainClassName());
        return false;
      }
    }
    return true;
  }

  protected void registerStartupClasses() {
    for(SystemClassLoaderInfo cl: systemClassLoaders) {
      // temporarily cache the systemClassLoader
      systemClassLoader = cl;
      cl.registerStartupClasses(this);
      systemClassLoader = null;
    }
  }

  protected boolean checkStartupQueues() {
    for(SystemClassLoaderInfo cl: systemClassLoaders) {
      if (cl.getStartupQueue() == null) {
        return false;
      }
    }
    return true;
  }

  protected boolean checkModelClassAccesses() {
    for(SystemClassLoaderInfo cl: systemClassLoaders) {
      if (!checkModelClassAccess(cl)) {
        return false;
      }
    }
    return true;
  }

  protected void initMainThreads() {
    for(SystemClassLoaderInfo cl: systemClassLoaders) {
      // temporarily cache the systemClassLoader
      systemClassLoader = cl;
      cl.initMainThread();
      systemClassLoader = null;
    }
  }

  protected void createStartupClassObjects() {
    for(SystemClassLoaderInfo cl: systemClassLoaders) {
      cl.createStartupClassObjects();
    }
  }

  protected void pushMainEntry() {
    for(SystemClassLoaderInfo cl: systemClassLoaders) {
      cl.pushMainEntry();
    }
  }

  protected void pushClinits() {
    for(SystemClassLoaderInfo cl: systemClassLoaders) {
      cl.pushClinits();
    }
  }

  protected void registerThreadListCleanup() {
    for(SystemClassLoaderInfo cl: systemClassLoaders) {
      // temporarily cache the systemClassLoader
      systemClassLoader = cl;
      cl.registerThreadListCleanup();
      systemClassLoader = null;
    }
  }

  @Override
  public boolean initialize () {
    ClassInfoException cie = null;

    if (!checkTergetClassNames()) {
      return false;
    }

    ThreadInfo.currentThread = systemClassLoaders[0].getMainThread();

    // from here, we get into some bootstrapping process
    //  - first, we have to load class structures (fields, supers, interfaces..)
    //  - second, we have to create a thread (so that we have a stack)
    //  - third, with that thread we have to create class objects
    //  - forth, we have to push the clinit methods on this stack
    try {
      registerStartupClasses();

      if (!checkStartupQueues()) {
        log.severe("error initializing startup classes (check 'classpath' and 'target')");
        return false;
      }

      if (!checkModelClassAccesses()) {
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

    try {
      // Collections.<clinit> yet because there's no stack before we have a tiMain
      // thread. Let's hope none of the init classes creates threads in their <clinit>.
      initMainThreads();

      //-- from this point on the systemClassLoader cache must be NULL --//
      assert systemClassLoader == null;

      if(cie != null) {
        SystemClassLoaderInfo cl = (SystemClassLoaderInfo)cie.getClassLoaderInfo();
        cl.getMainThread().getEnv().throwException(cie.getExceptionClass(), cie.getMessage());
        return false;
      }

      // now that we have a tiMain thread, we can finish the startup class init
      createStartupClassObjects();

      // pushClinit the call stack with the clinits we've picked up, followed by tiMain()
      pushMainEntry();
      pushClinits();

      initSystemState(systemClassLoaders[0].getMainThread());
      registerThreadListCleanup();
    } catch (ClassInfoException e) {
      SystemClassLoaderInfo cl = (SystemClassLoaderInfo)cie.getClassLoaderInfo();
      // If the main thread is not created due to an error thrown while loading a class, 
      // bail out immediately
      if(cl.getMainThread() == null) {
        throw new JPFException("loading of the class " + e.getFaildClass() + " faild");
      } else{
        cl.getMainThread().getEnv().throwException(e.getExceptionClass(), e.getMessage());
        return false;
      }
    }

    initialized = true;
    notifyVMInitialized();

    return true;
  }

  @Override
  public boolean isSingleProcess() {
    return false;
  }

  @Override
  public SystemClassLoaderInfo getSystemClassLoader () {
    return ClassLoaderInfo.getCurrentSystemClassLoader();
  }

  @Override
  public SystemClassLoaderInfo getSystemClassLoader(ThreadInfo ti) {

    // For the case of the multi-procresss VM, the only place that the 
    // systemClassLoader cache is not null, it during the VM initialization.
    // Any time after that, this is obtain by looking into the class
    // hierarchy of the thread
    if(systemClassLoader!=null) {
      return systemClassLoader; 
    }

    ClassInfo ci = ti.getClassInfo();

    while(!ci.isThreadClassInfo()) {
      ci = ci.getSuperClass();
    }

    return (SystemClassLoaderInfo)ci.getClassLoaderInfo();
  }

  public String[] getMainClassNames() {
    String[] mainClsNames = new String[NUM_PRC];

    for(int i=0; i<NUM_PRC; i++) {
      mainClsNames[i] = systemClassLoaders[i].getMainClassName();
    }

    return mainClsNames;
  }

  @Override
  public String getSuT() {
    String SuT = "";
    for(int i=0; i<NUM_PRC; i++) {
      ClassInfo ciMain = systemClassLoaders[i].getMainClassInfo();
      if(SuT.length()>0) {
        SuT += " | ";
      }
      SuT += ciMain.getSourceFileName();
    }

    return SuT;
  }
}
