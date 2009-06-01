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
package gov.nasa.jpf.mc;

import gov.nasa.jpf.util.test.RawTest;
import gov.nasa.jpf.jvm.Verify;

public class TestPartialTrace extends RawTest {

  void testPartialTrace () {
    int a = Verify.getInt(0,42);
    int b = Verify.getInt(100, 142);
    
    System.out.println("pre-trace choice: " + a + ',' + b);
    Verify.incrementCounter(0);
    
    Verify.storeTraceAndTerminateIf(!Verify.isTraceReplay(),
                                    "testPartialTrace.trace", "cut off here..");
    
    int c = Verify.getInt(0,3);
    Verify.incrementCounter(0);
    System.out.println("post-trace choice: " + a + ',' + b + ',' + c);
  }
  
  void testReplay () {
    int i1 = Verify.getInt(0, 5);
    int i2 = Verify.getInt(0, 5);
    int i3 = Verify.getInt(0, 5);
    boolean b1 = Verify.getBoolean();
    int i4 = Verify.getInt(0,3);
    
    assert !(i1 == 0 && i2 == 1 && i3 == 2 && b1 && i4 == 3); 
  }
  
  public static void main(String[] args) {
    TestPartialTrace t = new TestPartialTrace();
    
    if (!runSelectedTest(args, t)){
      runAllTests(args,t);
    }
  }
}
