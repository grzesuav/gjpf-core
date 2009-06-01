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

import gov.nasa.jpf.util.test.RawTest;

/**
 * test cast operations
 */
public class CastTest extends RawTest {
  
  public static void main (String[] selectedMethods) {
    runTests(new CastTest(),selectedMethods);
  }

  @SuppressWarnings("cast")
  public void testCast () {
    B b = new B();
    A a = b;

    B bb = (B)a;
    K k = (K)a;
    I i = (I)a;
    
    C c = new C();
    k = (K)c;
  }

  public void testCastFail () {
    A a = new A();

    I i = (I) a;
  }

  public void testArrayCast () {
    String[] sa = new String[1];
    Object o = sa;
    
    Object[] ol = (Object[])o; // that should succeed
  }
  
  public void testArrayCastFail() {
    String[] sa = new String[1];
    Object o = sa;
    
    Number[] na = (Number[])o; // that should fail
  }
  
  public void testPrimitiveArrayCast() {
    int[] a = new int[10];
    Object o = a;
    int[] b = (int[])o;
  }
  
  //--- helper types and methods
  
  static interface I {
  }

  static interface J extends I {
  }

  static interface K {
  }
  
  static class A implements K {
  }

  static class B extends A implements J {
  }
  
  static class C extends B {
  }
}
