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
package gov.nasa.jpf.vm;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;
import org.junit.runner.JUnitCore;

public class TestSharedAttrJPF extends TestJPF {

  static final String TEST_CLASS = "gov.nasa.jpf.jvm.TestSharedAttr";


  public static void main (String[] args) {
    JUnitCore.main("gov.nasa.jpf.jvm.TestSharedAttrJPF");
  }


  /**************************** tests **********************************/

  @Test
  public void testShared () {
    String[] args = { "+vm.por=true", "+vm.por.field_boundaries=true", "+vm.por.sync_detection=false",  
        TEST_CLASS, "testShared" };

    assertionError(args);
  }

  @Test
  public void testNonShared () {
    String[] args = { "+vm.por=true", "+vm.por.field_boundaries=true", "+vm.por.sync_detection=false",  
        TEST_CLASS, "testNonShared" };

    noPropertyViolation(args);
  }

  @Test
  public void testSharedStaticRoot () {
    String[] args = { "+vm.por=true", "+vm.por.field_boundaries=true", "+vm.por.sync_detection=false",  
        TEST_CLASS, "testSharedStaticRoot" };

    assertionError(args);
  }

}
