//
// Copyright (C) 2007 United States Government as represented by the
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

import gov.nasa.jpf.util.test.RawTest;

/**
 * testing various aspects of listeners on INVOKE instructions
 */
public class TestInvokeListener extends RawTest {
  
  public static void main (String[] args){
    TestInvokeListener t = new TestInvokeListener();
    
    if (!runSelectedTest(args, t)){
      assert t.testInstanceMethod(42.0, 1) == 43.0;
      assert t.testStaticMethod(42) == 43;
      assert t.testNativeInstanceMethod(42.0, 1) == 43.0;      
    }
  }
  
  public double testInstanceMethod (double d, int c){
    return d + c;
  }
  
  public static int testStaticMethod (int a){
    return a + 1;
  }
  
  public native double testNativeInstanceMethod (double d, int c);
}
