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
package gov.nasa.jpf;

import gov.nasa.jpf.jvm.classfile.ClassFile;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.search.SearchListener;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.VMListener;

/**
 * Adapter class that dummy implements both VMListener and SearchListener interfaces
 * Used to ease implementation of listeners that only process a few notifications
 */
public abstract class ListenerAdapter implements VMListener, SearchListener, PublisherExtension {

  //--- the VMListener interface
  public void vmInitialized(VM vm) {}
  public void instructionExecuted(VM vm) {}
  public void executeInstruction(VM vm) {}
  public void threadStarted(VM vm) {}
  public void threadWaiting (VM vm) {}
  public void threadNotified (VM vm) {}
  public void threadInterrupted (VM vm) {}
  public void threadScheduled (VM vm) {}
  public void threadBlocked (VM vm) {}
  public void threadTerminated(VM vm) {}
  public void loadClass (VM vm, ClassFile cf) {}
  public void classLoaded(VM vm) {}
  public void objectCreated(VM vm) {}
  public void objectReleased(VM vm) {}
  public void objectLocked (VM vm) {}
  public void objectUnlocked (VM vm) {}
  public void objectWait (VM vm) {}
  public void objectNotify (VM vm) {}
  public void objectNotifyAll (VM vm) {}
  public void gcBegin(VM vm) {}
  public void gcEnd(VM vm) {}
  public void exceptionThrown(VM vm) {}
  public void exceptionBailout(VM vm) {}
  public void exceptionHandled(VM vm) {}
  public void choiceGeneratorRegistered (VM vm) {}
  public void choiceGeneratorSet (VM vm) {}
  public void choiceGeneratorAdvanced (VM vm) {}
  public void choiceGeneratorProcessed (VM vm) {}
  public void methodEntered (VM vm) {}
  public void methodExited (VM vm) {}


  //--- the SearchListener interface
  public void stateAdvanced(Search search) {}
  public void stateProcessed(Search search) {}
  public void stateBacktracked(Search search) {}
  public void statePurged(Search search) {}
  public void stateStored(Search search) {}
  public void stateRestored(Search search) {}
  public void propertyViolated(Search search) {}
  public void searchStarted(Search search) {}
  public void searchConstraintHit(Search search) {}
  public void searchFinished(Search search) {}


  //--- PublisherExtension interface
  public void publishStart (Publisher publisher) {}
  public void publishTransition (Publisher publisher) {}
  public void publishPropertyViolation (Publisher publisher) {}
  public void publishConstraintHit (Publisher publisher) {}
  public void publishFinished (Publisher publisher) {}

}
