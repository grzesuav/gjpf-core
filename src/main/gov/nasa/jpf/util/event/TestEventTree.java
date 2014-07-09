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

package gov.nasa.jpf.util.event;

/**
 * EventTree that can check traces and coverage against expected traces
 * for testing purposes.
 * 
 * This has little purpose except of keeping tree spec and expected traces
 * together in the same native class, so that we can check paths ad hoc
 * from regression tests without having to create expected path strings
 * in the JPF part of the test, only to translate them into native strings
 */
public class TestEventTree extends EventTree {
  
  protected String[] expected; // to be optionally initialized by subclasses
  
  public TestEventTree (){
    // nothing here
  }
  
  public TestEventTree (Event root){
    super(root);
  } 
  
  public boolean checkPath (Event lastEvent) {
    if (expected != null){
      return checkPath( lastEvent, expected);
    } else {
      System.err.println("warning: trying to check path of " + this + " without 'expected' specification");
      return true; // nothing to check
    }
  }

  @Override
  public boolean isCompletelyCovered (){
    if (expected != null){
      return isCompletelyCovered(expected);
    } else {
      System.err.println("warning: trying to check coverage of " + this + " without 'expected' specification");
      return true;
    }
  }
  
  public boolean isCompletelyCovered (String[] expected) {
    for (int i = 0; i < expected.length; i++) {
      if (expected[i] != null) {
        // no checkPath() call for this one
        return false;
      }
    }

    return true; // no un-visited expected trace left
  }

  public float getPathCoverage (String[] expected) {
    int n = 0;

    for (int i = 0; i < expected.length; i++) {
      if (expected[i] == null) {
        n++;
      }
    }

    return (float) n / expected.length;
  }
}
