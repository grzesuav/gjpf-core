//
// Copyright (C) 2014 United States Government as represented by the
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
package gov.nasa.jpf.test.vm.basic;

import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.Verify;
import org.junit.Test;

/**
 * regression test for Java 8 default methods
 */
public class DefaultMethodTest extends TestJPF {
  
  //------------------------------------------ non-ambiguous recursive lookup
  
  interface A1 {
    default int getValue(){
      return 42;
    }
  } 
  
  interface B1 extends A1 {
    // nothing
  }
  
  static class C1 implements A1 {
    // nothing
  }
  
  static class D1 extends C1 {
    // nothing
  } 
  
  @Test
  public void testSingleMethod (){
    if (verifyNoPropertyViolation()){
      D1 o = new D1();
      int result = o.getValue();
      System.out.println(result);
      assertTrue (result == 42);
    }
  }
  
  //------------------------------------------ ambiguity resolution
  
  interface B2 {
    default int getValue(){
      return 3;
    }
  }
  
  static class D2 implements A1, B2 {
    @Override
    public int getValue(){
      return A1.super.getValue() + B2.super.getValue();
    }
  }
  
  @Test
  public void testExplicitDelegation (){
    if (verifyNoPropertyViolation()){
      D2 o = new D2();
      int result = o.getValue();
      System.out.println(result);
      assertTrue (result == 45);
    }    
  }
  
  
  // <2do> how to test IncompatibleClassChangeError without explicit classfile restore?
}
