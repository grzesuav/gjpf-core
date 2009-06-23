//
//Copyright (C) 2009 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.
//
//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.
//
//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package gov.nasa.jpf.search;

import java.util.LinkedList;

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.VMState;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.choice.ThreadChoiceFromSet;

/**
 * <p>
 * Search strategy that limits the search by imposing a limit on the number of
 * thread preemptions (i.e., preempting context switches) that can occur in an
 * execution path.
 * </p>
 * <p>
 * Configuration parameters:<br>
 * <p>
 * <code>search.class = gov.nasa.jpf.search.IterativeContextBounding</code><br>
 * setting search class to this class
 * </p>
 * <p>
 * <code>search.preemption_limit = N</code><br>
 * where <code>N</code> is the number of allowed preemptions per execution path
 * </p>
 * 
 * <p>
 * The idea of iterative context bounding is based on the PLDI 2007 paper<br>
 * <i>
 * "Iterative Context Bounding for Systematic Testing of Multithreaded Programs"
 * </i><br>
 * by Madanlal Musuvathi and Shaz Qadeer
 * </p>
 * 
 * <p>
 * Effectively, this search implements the paper's <i>iterative</i>
 * context-bounding algorithm, i.e., the specific order of states that get
 * explored. This search enforces a limit on the number of preemptions (i.e.,
 * preemptive context switches) that are allowed during an execution. The search
 * is also performed in an iterative fashion; executions with a lower number of
 * preemptions are explored first. Therefore, if an error can be identified, it
 * will be discovered in an execution with the lowest possible number of
 * preemptions.
 * </p>
 * 
 * @author Igor Andjelkovic (igor.andjelkovic@gmail.com)
 * @author Steven Lauterburg (slauter2@cs.uiuc.edu)
 * @author Mirko Stojmenovic (mirko.stojmenovic@gmail.com)
 */

public class IterativeContextBounding extends Search {

  public static final String PREEMPTION_CONSTRAINT = "Preemptive Context Switch Limit";

  private int preemptionLimit;
  int maxDepth;
  private LinkedList<WorkItem> workQueue = new LinkedList<WorkItem>();
  private LinkedList<WorkItem> nextWorkQueue = new LinkedList<WorkItem>();

  public IterativeContextBounding(Config config, JVM vm)
      throws Config.Exception {
    super(config, vm);
    preemptionLimit = config.getInt("search.preemption_limit", -1);
  }

  public boolean requestBacktrack() {
    doBacktrack = true;
    return true;
  }

  public void search() {

    maxDepth = getMaxSearchDepth();

    ThreadInfo currentThread = vm.getCurrentThread();
    ThreadChoiceFromSet initialCG = new ThreadChoiceFromSet(
        new ThreadInfo[] { currentThread }, true);
    workQueue.add(new WorkItem(vm.getState(), initialCG));

    int currentPreemptionCount = 0;
    depth = 0;
    notifySearchStarted();
    boolean firstWorkItem = true;

    while (!done) {
      while (!workQueue.isEmpty()) {
        WorkItem w = workQueue.removeFirst();

        if (!firstWorkItem) {
          // Restores the desired state from before the context switch
          vm.restoreState(w.getVMState());
          depth = vm.getPathLength();

          notifyStateRestored();
        }
        firstWorkItem = false;

        ThreadChoiceFromSet tcg = w.getCG();
        vm.getSystemState().setNextChoiceGenerator(tcg);
        isNewState = true;

        searchWithoutPreemptions();

        if (done) {
          break;
        }
      }

      if (nextWorkQueue.isEmpty()) {
        done = true;
        break;
      }

      // If the limit is -1, there is no bound on the number of preemptions
      if (preemptionLimit != -1 && currentPreemptionCount >= preemptionLimit) {
        notifySearchConstraintHit(PREEMPTION_CONSTRAINT + ": "
            + preemptionLimit);
        done = true;
        break;
      }
      currentPreemptionCount++;

      LinkedList<WorkItem> tmp = workQueue;
      workQueue = nextWorkQueue;
      nextWorkQueue = tmp;
      nextWorkQueue.clear();
    }

    notifySearchFinished();
    System.out.println("Number of preemptive context switches in final path: "
        + currentPreemptionCount);
    System.out.println("Preemptive context switch limit: " + preemptionLimit);
  }

  private void searchWithoutPreemptions() {
    boolean isCurrentThreadEnabled = false;

    while (!done) {
      if (!isNewState || isEndState || isIgnoredState) {
        if (!backtrack()) { // backtrack not possible, done
          return;
        }

        depth--;

        notifyStateBacktracked();
      }

      if (forward()) {
        notifyStateAdvanced();

        if (hasPropertyTermination()) {
          done = true;
          return;
        }

        depth++;

        if (isNewState) {
          if (depth >= maxDepth) {
            isEndState = true;
            notifySearchConstraintHit(DEPTH_CONSTRAINT + ": " + maxDepth);
          }

          if (!checkStateSpaceLimit()) {
            notifySearchConstraintHit(FREE_MEMORY_CONSTRAINT + ": "
                + minFreeMemory);
            // can't continue, we exhausted our memory
            done = true;
            return;
          }

          // We do not need to process the next choice generator if the state
          // is an end state or if it will be ignored.
          if (!isEndState && !isIgnoredState) {
            VMState currVMState = vm.getState();
            ChoiceGenerator<?> gen = vm.getSystemState()
                .getNextChoiceGenerator();

            // If the next choice generator is a thread choice set, we
            // process it. Otherwise, we continue executing without changes.
            if (gen instanceof ThreadChoiceFromSet) { // the next cg is a thread choice set
              ThreadChoiceFromSet tcfs = (ThreadChoiceFromSet) gen;

              // Determine if the current thread is enabled. Also create
              // a list of all enabled threads that are not the current thread.
              ThreadInfo[] nextChoices = tcfs.getAllThreadChoices();
              LinkedList<ThreadInfo> enabledThreads = new LinkedList<ThreadInfo>();
              isCurrentThreadEnabled = false;
              for (ThreadInfo t : nextChoices) {
                if (isThreadEnabled(t)) {
                  if (t.getIndex() == vm.getCurrentThread().getIndex()) {
                    isCurrentThreadEnabled = true;
                  } else {
                    enabledThreads.add(t);
                  }
                }
              }
              if (isCurrentThreadEnabled) {
                // If the current thread is enabled, the context switch is preemptive.
                // We replace the next choice generator with a new choice generator
                // containing only the current thread to continue with.
                ThreadChoiceFromSet tcg = new ThreadChoiceFromSet(
                    new ThreadInfo[] { vm.getCurrentThread() }, true);
                vm.getSystemState().setNextChoiceGenerator(tcg);

                // If other threads are enabled, we also create a new choice
                // generator with those threads and add it to the next work queue
                if (enabledThreads.size() > 0) {
                  ThreadInfo[] tia = enabledThreads.toArray(new ThreadInfo[0]);
                  ThreadChoiceFromSet tcgNext = new ThreadChoiceFromSet(tia,
                      true);
                  nextWorkQueue.add(new WorkItem(currVMState, tcgNext));
                }
              }
            }
          }
        }
      } else { // state was processed
        notifyStateProcessed();
      }
    }
  }

  private boolean isThreadEnabled(ThreadInfo t) {
    boolean isEnabled = (t.getStatusName().equals("RUNNING"))
        || (t.getStatusName().equals("INTERRUPTED"))
        || (t.getStatusName().equals("NOTIFIED"));
    return isEnabled;
  }

  public boolean supportsBacktrack() {
    return true;
  }

  private static class WorkItem {
    protected VMState vmState;
    protected ThreadChoiceFromSet set;

    public WorkItem(VMState vmState, ThreadChoiceFromSet set) {
      this.vmState = vmState;
      this.set = set;
    }

    public VMState getVMState() {
      return vmState;
    }

    public ThreadChoiceFromSet getCG() {
      return set;
    }
  }
}
