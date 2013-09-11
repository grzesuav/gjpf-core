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
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.Predicate;
import gov.nasa.jpf.vm.choice.MultiProcessThreadChoice;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;

import java.util.ArrayList;

/**
 * A VM implementation that simulates running multiple applications within the same
 * JPF process (centralized model checking of distributed applications).
 * This is achieved by executing each application in a separate thread group,
 * using separate SystemClassLoader instances to ensure proper separation of types / static fields.
 * 
 * 
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 * 
 * To use this jpf.properties includes,
 *              vm.class = gov.nasa.jpf.vm.MultiProcessVM 
 */
public class MultiProcessVM extends VM {

  static final int MAX_APP = 32;

  ApplicationContext[] appCtxs;
  
  public MultiProcessVM (JPF jpf, Config conf) {
    super(jpf, conf);
    
    appCtxs = createApplicationContexts();
  }

  /**
   * <2do> this should also handle command line specs such as "jpf ... tgt1 tgt1_arg ... -- tgt2 tgt2_arg ... 
   */
  ApplicationContext[] createApplicationContexts(){
    String[] targets;

    int replicate = config.getInt("target.replicate", 0);
    if(replicate>0) {
      String target = config.getProperty("target");
      targets = new String[replicate];
      for(int i=0; i<replicate; i++) {
        targets[i] = target;
      }
    } else {
      targets = config.getStringEnumeration("target", MAX_APP);
    }

    if (targets == null){
      throw new JPFConfigException("no applications specified, check 'target.N' settings");
    }
    
    ArrayList<ApplicationContext> list = new ArrayList<ApplicationContext>(targets.length);
    for (int i=0; i<targets.length; i++){
      if (targets[i] != null){ // there might be holes in the array
        String clsName = targets[i];
        if (!isValidClassName(clsName)) {
          throw new JPFConfigException("main class not a valid class name: " + clsName);
        }
        
        String argsKey;
        String entryKey;
        String hostKey;
        if(replicate>0) {
          argsKey = "target.args";
          entryKey = "target.entry";
          hostKey = "target.host";
        } else {
          argsKey = "target.args." + i;
          entryKey = "target.entry." + i;
          hostKey = "target.host." + i;
        }
        
        String[] args = config.getCompactStringArray(argsKey);
        if (args == null){
          args = EMPTY_ARGS;
        }
        
        String mainEntry = config.getString(entryKey, "main([Ljava/lang/String;)V");
        
        String host = config.getString(hostKey, "localhost");
        
        SystemClassLoaderInfo sysCli = createSystemClassLoaderInfo(list.size());
    
        ApplicationContext appCtx = new ApplicationContext( i, clsName, mainEntry, args, host, sysCli);
        list.add( appCtx);
      }
    }
    
    return list.toArray(new ApplicationContext[list.size()]);
  }

  @Override
  public boolean initialize(){
    try {
      ThreadInfo tiFirst = null;
      
      for (int i=0; i<appCtxs.length; i++){
        ThreadInfo tiMain = initializeMainThread(appCtxs[i], i);
        if (tiMain == null) {
          return false; // bail out
        }
        if (tiFirst == null){
          tiFirst = tiMain;
        }
      }

      initSystemState(tiFirst);
      initialized = true;
      notifyVMInitialized();
      
      return true;
      
    } catch (JPFConfigException cfe){
      log.severe(cfe.getMessage());
      return false;
    } catch (ClassInfoException cie){
      log.severe(cie.getMessage());
      return false;
    }
    // all other exceptions are JPF errors that should cause stack traces
  }

  @Override
  public int getNumberOfApplications(){
    return appCtxs.length;
  }
  
  @Override
  protected ChoiceGenerator<?> getInitialCG () {
    return new MultiProcessThreadChoice("<root>", getThreadList().getRunnableThreads(), true);
  }
  
  @Override
  public ApplicationContext getApplicationContext(int objRef) {
    VM vm = VM.getVM();

    ClassInfo ci = vm.getElementInfo(objRef).getClassInfo();
    while(!ci.isObjectClassInfo()) {
      ci = ci.getSuperClass();
    }

    ClassLoaderInfo sysLoader = ci.getClassLoaderInfo();
    ApplicationContext[] appContext = vm.getApplicationContexts();
    
    for(int i=0; i<appContext.length; i++) {
      if(appContext[i].getSystemClassLoader() == sysLoader) {
        return appContext[i];
      }
    }
    return null;
  }
  
  @Override
  public ApplicationContext[] getApplicationContexts(){
    return appCtxs;
  }

  
  @Override
  public String getSUTName() {
    StringBuilder sb = new StringBuilder();
    
    for (int i=0; i<appCtxs.length; i++){
      if (i>0){
        sb.append("+");
      }
      sb.append(appCtxs[i].mainClassName);
    }
    
    return sb.toString();
  }

  @Override
  public String getSUTDescription(){
    StringBuilder sb = new StringBuilder();
    
    for (int i=0; i<appCtxs.length; i++){
      if (i>0){
        sb.append('+'); // "||" would be more fitting, but would screw up filenames
      }

      ApplicationContext appCtx = appCtxs[i];
      sb.append(appCtx.mainClassName);
      sb.append('.');
      sb.append(Misc.upToFirst(appCtx.mainEntry, '('));

      sb.append('(');
      String[] args = appCtx.args;
      for (int j = 0; j < args.length; j++) {
        if (j > 0) {
          sb.append(',');
        }
        sb.append('"');
        sb.append(args[j]);
        sb.append('"');
      }
      sb.append(')');
    }
    
    return sb.toString();
  }

  
  @Override
  public boolean isSingleProcess() {
    return false;
  }

  @Override
  public boolean isEndState () {
    return !getThreadList().hasMoreThreadsToRun();
  }

  @Override
  // Note - for now we just check for global deadlocks not the local ones which occur within a
  // scope of a single progress
  public boolean isDeadlocked () { 
    boolean hasNonDaemons = false;
    boolean hasBlockedThreads = false;

    if (ss.isBlockedInAtomicSection()) {
      return true; // blocked in atomic section
    }

    ThreadInfo[] threads = getThreadList().getThreads();
    int len = threads.length;

    for (int i=0; i<len; i++){
      ThreadInfo ti = threads[i];
      if (ti.isAlive()) {
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
    ApplicationContext appCtx = ti.getApplicationContext();

    int len = threads.length;
    for (int i=0; i<len; i++){
      ThreadInfo t = threads[i];
      if (t != ti){
        if (t.getApplicationContext() == appCtx){
          if (t.isRunnable() && t.isDaemon()) {
            return true;
          }
        }
      }
    }
      
    return false;
  }

  //----------- Methods for acquiring ThreadInfos within an application ----------//

  /**
   * Returns all the threads that belong to the same application as ti
   */
  public ThreadInfo[] getAppThreads (ThreadInfo ti) {
    ThreadInfo[] threads = getThreadList().getThreads();
    ApplicationContext appCtx = ti.getApplicationContext();
    
    int len = threads.length;
    int n = 0;
    for (int i=0; i<len; i++){
      if (threads[i].getApplicationContext() == appCtx) n++;
    }
    
    ThreadInfo[] appThreads = new ThreadInfo[n];
    for (int i=0, j=0; j<n; i++){
      ThreadInfo t = threads[i];
      if (t.getApplicationContext() == appCtx){
        appThreads[j++] = t;
      }
    }

    return appThreads;
  }

  Predicate<ThreadInfo> getAppPredicate (final ThreadInfo ti){
     // we could probably use a cached object here if the caller is synchronized
    return new Predicate<ThreadInfo>(){
      public boolean isTrue (ThreadInfo t){
        return t.appCtx == ti.appCtx;
      }
    }; 
  }
  
  public ThreadInfo[] getRunnableAppThreads (ThreadInfo ti) {    
    return getThreadList().getRunnableThreads( getAppPredicate(ti));
  }

  public int getRunnableAppThreadCount (ThreadInfo ti) {
    return getThreadList().getRunnableThreadCount( getAppPredicate(ti));
  }

  public ThreadInfo[] getRunnableAppThreadsWith (ThreadInfo ti) {
    return getThreadList().getRunnableThreadsWith(ti,  getAppPredicate(ti));
  }

  public ThreadInfo[] getRunnableAppThreadsWithout( ThreadInfo ti) {
    return getThreadList().getRunnableThreadsWithout(ti,  getAppPredicate(ti));
  }
  
  public int getLiveAppThreadCount (ThreadInfo ti) {
    return getThreadList().getLiveThreadCount( getAppPredicate(ti));
  }
}
