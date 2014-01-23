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

import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

/**
 * regression test for EnvironmentModel
 */
public class EnvironmentModelTest extends TestJPF {
  
  protected boolean checkExpectedTraces (EnvironmentModel m, String[] expected){
    for (Event e : m.endEvents()){
      String trace = Misc.toString( e.getTrace());
      for (int i=0; trace != null && i<expected.length; i++){
        String expectedTrace = expected[i];
        if (expectedTrace != null){
          if (trace.equals(expectedTrace)){
            System.out.println("found expected trace: " + trace);
            expected[i] = null; // discharged
            trace = null;
          }
        }
      }
      if (trace != null){
        System.out.println("unexpected trace: " + trace);
        return false;
      }
    }
    
    for (int i=0; i<expected.length; i++){
      if (expected[i] != null){
        System.out.println("missed trace: " + expected[i]);
        return false;
      }
    }

    return true;
  }
  
  
  //--------------------------------------------------------------------
  static class Model1 extends EnvironmentModel {
    @Override
    public Event createEventTree() {
      return 
        sequence(
          event("a"),
          alternatives(
            event("1"),
            iteration(2,
              event("x")
            )
          ),
          event("b")
        );
    }
  }

  @Test
  public void testBasicNesting(){
    Model1 m = new Model1();
    String[] expected = {
      "a1b",
      "axxb"
    };
    
    if (!checkExpectedTraces(m, expected)){
      fail("failed to match traces");
    }
  }
}
