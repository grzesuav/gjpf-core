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

package gov.nasa.jpf.util.script;

/**
 * EventTree that can check traces and coverage against expected traces
 * Mostly useful for testing purposes
 */
public abstract class TestEventTree extends EventTree {

  /**
   * to be initialized by subclass ctors
   */
  protected String[] expected;
  
  @Override
  public boolean checkTrace (Event lastEvent) {
    String trace = lastEvent.getTrace(null);

    for (int i = 0; i < expected.length; i++) {
      if (trace.equals(expected[i])) {
        expected[i] = null;
        return true;
      }
    }

    return false; // unexpected trace
  }

  @Override
  public boolean isCompletelyCovered () {
    for (int i = 0; i < expected.length; i++) {
      if (expected[i] != null) {
        // no checkTrace() call for this one
        return false;
      }
    }

    return true; // no un-visited expected trace left
  }

  @Override
  public float getCoverage () {
    int n = 0;

    for (int i = 0; i < expected.length; i++) {
      if (expected[i] == null) {
        n++;
      }
    }

    return (float) n / expected.length;
  }
}
