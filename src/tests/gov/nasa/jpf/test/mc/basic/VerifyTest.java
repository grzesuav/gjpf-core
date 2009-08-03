//
// Copyright (C) 2009 United States Government as represented by the
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
package gov.nasa.jpf.test.mc.basic;


import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;


/**
 * test various Verify APIs
 */
public class VerifyTest extends TestJPF {

  public static void main (String[] args) {
    runTestsOfThisClass(args);
  }

  @Test public void testBreak () {

    if (verifyNoPropertyViolation()) {
      int y = 4;
      int x = 0;

      while (x != y) { // JPF should state match on the backjump
        x = x + 1;
        if (x > 3) {
          x = 0;
        }

        Verify.breakTransition(); // this should eventually state match
      }

      assert false : "we should never get here";
    }
  }

  //... and many more to come
}
