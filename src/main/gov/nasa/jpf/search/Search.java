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
package gov.nasa.jpf.search;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Error;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.JPFListenerException;
import gov.nasa.jpf.Property;
import gov.nasa.jpf.State;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.Path;
import gov.nasa.jpf.jvm.ThreadList;
import gov.nasa.jpf.jvm.Transition;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjArray;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * the mother of all search classes. Mostly takes care of listeners, keeping
 * track of state attributes and errors. This class mainly keeps the
 * general search info like depth, configured properties etc.
 */
public abstract class Search {

  protected static Logger log = JPF.getLogger("gov.nasa.jpf.search");
  
  public static final String DEPTH_CONSTRAINT = "Search Depth";
  public static final String QUEUE_CONSTRAINT = "Search Queue Size";
  public static final String FREE_MEMORY_CONSTRAINT = "Free Memory Limit";

  protected ArrayList<Error> errors = new ArrayList<Error>();

  protected int       depth = 0;
  protected JVM       vm;

  ArrayList<Property> properties;

  // the forward() attributes, e.g. used by the listeners
  protected boolean isEndState = false;
  protected boolean isNewState = true;
  protected boolean isIgnoredState = false;

  protected boolean matchDepth;
  protected long    minFreeMemory;
  protected int     depthLimit;

  protected String lastSearchConstraint;

  // these states control the search loop
  protected boolean done = false;
  protected boolean doBacktrack = false;
  SearchListener     listener;

  Config config; // to later-on access settings that are only used once (not ideal)

  // statistics
  //int maxSearchDepth = 0;

  /** storage to keep track of state depths */
  final IntVector stateDepth = new IntVector();

  protected Search (Config config, JVM vm) {
    this.vm = vm;
    this.config = config;

    depthLimit = config.getInt("search.depth_limit", -1);
    matchDepth = config.getBoolean("search.match_depth");
    minFreeMemory = config.getMemorySize("search.min_free", 1024<<10);

    properties = getProperties(config);
    if (properties.isEmpty()) {
      log.severe("no property");
    }
  }

  public Config getConfig() {
    return config;
  }
  
  public abstract void search ();

  public void addProperty (Property newProperty) {
    properties.add(newProperty);
  }

  public void removeProperty (Property oldProperty) {
     properties.remove(oldProperty);
  }

  /**
   * return set of configured properties
   * note there is a nameclash here - JPF 'properties' have nothing to do with
   * Java properties (java.util.Properties)
   */
  protected ArrayList<Property> getProperties (Config config) {
    Class<?>[] argTypes = { Config.class, Search.class };
    Object[] args = { config, this };

    ArrayList<Property> list = config.getInstances("search.properties", Property.class,
                                                   argTypes, args);

    return list;
  }

  protected boolean hasPropertyTermination () {
    if (isPropertyViolated()) {
      if (done) {
        return true;
      }
    }

    return false;
  }

  boolean isPropertyViolated () {
    for (Property p : properties) {
      if (!p.check(this, vm)) {
        error(p, vm.getClonedPath(), vm.getThreadList());
        return true;
      }
    }

    return false;
  }

  public void addListener (SearchListener newListener) {
    listener = SearchListenerMulticaster.add(listener, newListener);
  }

  public boolean hasListenerOfType (Class<?> type) {
    return SearchListenerMulticaster.containsType(listener,type);
  }
  
  public void removeListener (SearchListener removeListener) {
    listener = SearchListenerMulticaster.remove(listener,removeListener);
  }

  public List<Error> getErrors () {
    return errors;
  }

  public String getLastSearchContraint() {
    return lastSearchConstraint;
  }

  public Error getLastError() {
    int i=errors.size()-1;
    if (i >=0) {
      return errors.get(i);
    } else {
      return null;
    }
  }

  public JVM getVM() {
    return vm;
  }

  public boolean isEndState () {
    return isEndState;
  }

  public boolean hasNextState () {
    return !isEndState();
  }

  public boolean isNewState () {
    boolean isNew = vm.isNewState();

    if (matchDepth) {
      int id = vm.getStateId();

      if (isNew) {
        setStateDepth(id, depth);
      } else {
        return depth < getStateDepth(id);
      }
    }

    return isNew;
  }

  public boolean isVisitedState () {
    return !isNewState();
  }

  public int getDepth () {
    return depth;
  }

  public String getSearchConstraint () {
    return lastSearchConstraint;
  }

  public Transition getTransition () {
    return vm.getLastTransition();
  }

  public int getStateNumber () {
    return vm.getStateId();
  }

  public boolean requestBacktrack () {
    return false;
  }

  public boolean supportsBacktrack () {
    return false;
  }

  public boolean supportsRestoreState () {
    // not supported by default
    return false;
  }

  protected int getMaxSearchDepth () {
    int searchDepth = Integer.MAX_VALUE;

    if (depthLimit > 0) {
      int initialDepth = vm.getPathLength();

      if ((Integer.MAX_VALUE - initialDepth) > depthLimit) {
        searchDepth = depthLimit + initialDepth;
      }
    }

    return searchDepth;
  }

  public int getDepthLimit () {
    return depthLimit;
  }

  protected SearchState getSearchState () {
    return new SearchState(this);
  }

  // can be used by SearchListeners to create path-less errors (liveness)
  public void error (Property property) {
    error(property, null, null);
  }

  protected void error (Property property, Path path, ThreadList threadList) {

    boolean getAllErrors = config.getBoolean("search.multiple_errors");

    if (getAllErrors) {
      path = path.clone(); // otherwise we are going to overwrite it
      threadList = (ThreadList)threadList.clone(); // this makes it a snapshot (deep) clone
    }
    Error error = new Error(errors.size()+1, property, path, threadList);

    String fname = config.getString("search.error_path");
    if (fname != null) {
      if (getAllErrors) {
        int i = fname.lastIndexOf('.');

        if (i >= 0) {
          fname = fname.substring(0, i) + '-' + errors.size() +
                  fname.substring(i);
        }
      }
    }

    errors.add(error);

    if (getAllErrors) {
      done = false;
      isIgnoredState = true;
    } else {
      done = true;
    }

    notifyPropertyViolated();

    if (getAllErrors){
      // do this AFTER we notified listeners (one of the listeners might
      // be actually the property, and it might get confused if it's already rest)
      property.reset();
    }
  }

  protected void notifyStateAdvanced () {
    if (listener != null) {
      try {
        listener.stateAdvanced(this);
      } catch (Throwable t){
        throw new JPFListenerException("exception during stateAdvanced() notification", t);
      }
    }
  }

  protected void notifyStateProcessed () {
    if (listener != null) {
      try {
        listener.stateProcessed(this);
      } catch (Throwable t){
        throw new JPFListenerException("exception during stateProcessed() notification", t);
      }
    }
  }

  protected void notifyStateStored () {
    if (listener != null) {
      try {
        listener.stateStored(this);
      } catch (Throwable t){
        throw new JPFListenerException("exception during stateStored() notification", t);
      }
    }
  }

  protected void notifyStateRestored () {
    if (listener != null) {
      try {
        listener.stateRestored(this);
      } catch (Throwable t){
        throw new JPFListenerException("exception during stateRestored() notification", t);
      }
    }
  }

  protected void notifyStateBacktracked () {
    if (listener != null) {
      try {
        listener.stateBacktracked(this);
      } catch (Throwable t){
        throw new JPFListenerException("exception during stateBacktracked() notification", t);
      }
    }
  }

  protected void notifyPropertyViolated () {
    if (listener != null) {
      try {
        listener.propertyViolated(this);
      } catch (Throwable t){
        throw new JPFListenerException("exception during propertyViolated() notification", t);
      }
    }
  }

  protected void notifySearchStarted () {
    if (listener != null) {
      try {
        listener.searchStarted(this);
      } catch (Throwable t){
        throw new JPFListenerException("exception during searchStarted() notification", t);
      }
    }
  }

  public void notifySearchConstraintHit (String constraintId) {
    if (listener != null) {
      try {
        lastSearchConstraint = constraintId;
        listener.searchConstraintHit(this);
      } catch (Throwable t){
        throw new JPFListenerException("exception during searchConstraintHit() notification", t);
      }
    }
  }

  protected void notifySearchFinished () {
    if (listener != null) {
      try {
        listener.searchFinished(this);
      } catch (Throwable t){
        throw new JPFListenerException("exception during searchFinished() notification", t);
      }
    }
  }

  protected boolean forward () {
    boolean ret = vm.forward();

    if (ret) {
      isNewState = isNewState();
    } else {
      isNewState = false;
    }

    isIgnoredState = false; // only set by search listener
    isEndState = vm.isEndState();

    return ret;
  }

  protected boolean backtrack () {
    isNewState = false;
    isEndState = false;
    isIgnoredState = false;

    return vm.backtrack();
  }

  public void setIgnoredState (boolean b) {
    isIgnoredState = b;
  }

  protected void restoreState (State state) {
    // not supported by default
  }

  /** this can be used by listeners to terminate the search */
  public void terminate () {
    done = true;
  }

  void setStateDepth (int stateId, int depth) {
    stateDepth.set(stateId, depth + 1);
  }

  int getStateDepth (int stateId) {
    int depthPlusOne = stateDepth.get(stateId);
    if (depthPlusOne <= 0) {
      throw new JPFException("Asked for depth of unvisited state");
    } else {
      return depthPlusOne - 1;
    }
  }

  /**
   * check if we have a minimum amount of free memory left. If not, we rather want to stop in time
   * (with a threshold amount left) so that we can report something useful, and not just die silently
   * with a OutOfMemoryError (which isn't handled too gracefully by most VMs)
   */
  boolean checkStateSpaceLimit () {
    Runtime rt = Runtime.getRuntime();

    long avail = rt.freeMemory();

    // we could also just check for a max number of states, but what really
    // limits us is the memory required to store states

    if (avail < minFreeMemory) {
      // try to collect first
      rt.gc();
      avail = rt.freeMemory();

      if (avail < minFreeMemory) {
        // Ok, we give up, threshold reached
        return false;
      }
    }

    return true;
  }
}

