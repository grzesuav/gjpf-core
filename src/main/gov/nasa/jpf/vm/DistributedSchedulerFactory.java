//
// Copyright (C) 2013 United States Government as represented by the
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
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;


/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 * 
 * This scheduler factory is used for distributed application
 */
public class DistributedSchedulerFactory extends DefaultSchedulerFactory {

  public DistributedSchedulerFactory (Config config, VM vm, SystemState ss) {
    super(config, vm, ss);
  }

  /**************************************** our choice acquisition methods ***/

  /**
   * get list of all runnable threads in the same process as ti
   */
  protected ThreadInfo[] getRunnables(ThreadInfo ti) {
    ThreadList tl = vm.getThreadList();
    int mainGrpRef = ti.getMainGroupRef();
    return filter(tl.getRunnableThreadsInGroup(mainGrpRef));
  }

  /**
   * return a list of runnable choices, or null if there is only one, 
   *  in the same process as ti
   */
  protected ThreadInfo[] getRunnablesIfChoices(ThreadInfo ti) {
    ThreadList tl = vm.getThreadList();
    int mainGrpRef = ti.getMainGroupRef();
    int n = tl.getRunnableThreadCountInGroup(mainGrpRef);

    if ((n > 1) || (n == 1 && breakSingleChoice)){
      return filter(tl.getRunnableThreadsInGroup(mainGrpRef));
    } else {
      return null;
    }
  }

  protected ThreadInfo[] getRunnablesWith (ThreadInfo ti) {
    ThreadList tl = vm.getThreadList();
    int mainGrpRef = ti.getMainGroupRef();
    return filter( tl.getRunnableThreadsWithInGroup(ti, mainGrpRef));
  }

  protected ThreadInfo[] getRunnablesWithout (ThreadInfo ti) {
    ThreadList tl = vm.getThreadList();
    int mainGrpRef = ti.getMainGroupRef();
    return filter( tl.getRunnableThreadsWithoutInGroup(ti, mainGrpRef));
  }

  /************************************ the public interface towards the insns ***/

  @Override
  public ChoiceGenerator<ThreadInfo> createThreadTerminateCG (ThreadInfo terminateThread) {
    // terminateThread is already TERMINATED at this point
    ThreadList tl = vm.getThreadList();

    if (tl.hasAnyAliveThread()) {
      return new ThreadChoiceFromSet( THREAD_TERMINATE, super.getRunnablesWithout(terminateThread), true);
    } else {
      return null;
    }
  }
}
