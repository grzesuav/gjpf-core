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

import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.util.test.RawTest;

/**
 * raw test for field/operand/local attribute handling
 */
public class AttrsTest extends RawTest {

  public static void main (String[] selectedMethods){
    runTests( new AttrsTest(), selectedMethods);
  }

  static int sInt;
  int iInt;

  static double sDouble;
  double iDouble;

  int echoInt (int a){
    return a;
  }

  public void testIntPropagation () {
    int i = 42; // this gets attributed
    Verify.setLocalAttribute("i", 42); // this overwrites whatever the ISTORE listener did set on 'i'

    iInt = echoInt(i);
    sInt = iInt;
    int j = sInt; // now j should have the initial i attribute, and value 42

    int attr = Verify.getLocalAttribute("j");
    Verify.print("@ 'j' attribute after assignment: " + attr);
    Verify.println();

    assert attr == 42;
  }

  native double goNative (double d, int i);

  public void testNativeMethod () {
    Verify.setLocalAttribute("this", 1);

    double d = 42.0;
    Verify.setLocalAttribute("d", 2);

    int i = 42;
    Verify.setLocalAttribute("i", 3);

    double result = goNative(d, i);
    int attr = Verify.getLocalAttribute("result");

    Verify.print("@ 'result' attribute: " + attr);
    Verify.println();

    assert attr == 6;
  }

  public void testInvokeListener () {
    Verify.setLocalAttribute("this", 1);

    double d = 42.0;
    Verify.setLocalAttribute("d", 2);

    int i = 42;
    Verify.setLocalAttribute("i", 3);

    double result = goNative(d, i); // that's going to be listened on
    int attr = Verify.getLocalAttribute("result");

    Verify.print("@ 'result' attribute: " + attr);
    Verify.println();

    assert attr == 6;

    int r = goModel(d, i);  // that's listened for, too
    assert r == 6;
  }

  int goModel (double d, int i) {
    int a1 = Verify.getLocalAttribute("d");
    int a2 = Verify.getLocalAttribute("i");

    return a1*a2;
  }

  double echoDouble (double d){
    return d;
  }

  public void testDoublePropagation () {
    double d = 42.0; // this gets attributed
    Verify.setLocalAttribute("d", 42);  // this overwrites whatever the ISTORE listener did set on 'd'

    iDouble = echoDouble(d);
    sDouble = iDouble;

    //double r = sDouble; // now r should have the same attribute
    double r = echoDouble(d);

    int attr = Verify.getLocalAttribute("r");
    Verify.print("@ 'r' attribute after assignment: " + attr);
    Verify.println();

    assert attr == 42;
  }

  public void testExplicitRef () {
    int attr = Verify.getFieldAttribute(this, "iDouble");
    Verify.print("@ 'iDouble' attribute before set: ", Integer.toString(attr));
    Verify.println();

    Verify.setFieldAttribute(this, "iDouble", 42);

    attr = Verify.getFieldAttribute(this, "iDouble");
    Verify.print("@ 'iDouble' attribute after set: ", Integer.toString(attr));
    Verify.println();

    assert attr == 42;
  }

  public void testExplicitArrayRef () {
    int attr;
    double[] myArray = new double[10];

    attr = Verify.getElementAttribute(myArray, 5);
    Verify.print("@ 'myArray[5]' attribute before set: ", Integer.toString(attr));
    Verify.println();

    Verify.setElementAttribute(myArray, 5, 42);

    attr = Verify.getElementAttribute(myArray, 5);
    Verify.print("@ 'myArray[5]' attribute after set: ", Integer.toString(attr));
    Verify.println();

    assert attr == 42;
  }

  public void testArraycopy () {
    int attr;
    double[] a1 = new double[10];
    double[] a2 = new double[10];

    Verify.setElementAttribute(a1, 3, 42);
    System.arraycopy(a1,1,a2,0,3);

    attr = Verify.getElementAttribute(a2,2);
    assert attr == 42;
  }

  double ddd;

  public void testArrayPropagation() {
    int attr;
    double[] a1 = new double[10];
    double[] a2 = new double[10];

    Verify.setElementAttribute(a1, 3, 42);

    //attr = Verify.getElementAttribute(a1,3);
    //System.out.println(attr);

    ddd = a1[3];
    //Verify.setFieldAttribute(this,"ddd",42);
    //attr = Verify.getFieldAttribute(this,"ddd");
    //System.out.println("@ ddd : " + attr);

    double d = ddd;
    //ccc = d;
    //attr = Verify.getFieldAttribute(this,"ccc");
    //System.out.println("ccc ; " + attr);

    //double d = a1[3]; // now d should have the attr
    a2[0] = d;
    attr = Verify.getElementAttribute(a2,0);
    System.out.println("@ a2[0] : " + attr);

    assert attr == 42;
  }

  public void testBacktrack() {
    int v = 42; // need to init or the compiler does not add it to the name table
    Verify.setLocalAttribute("v", 42);

    boolean b = Verify.getBoolean(); // restore point
    System.out.println(b);

    int  attr = Verify.getLocalAttribute("v");
    System.out.println(attr);

    Verify.setLocalAttribute("v", -1);
    attr = Verify.getLocalAttribute("v");
    System.out.println(attr);
  }
  
  public void testInteger() {
    int v = 42;
    Verify.setLocalAttribute("v", 4200);
    
    // explicit
    Integer o = new Integer(v);
    int j = o.intValue();
    int  attr = Verify.getLocalAttribute("j");
    assert attr == 4200;
    
    // semi autoboxed
    j = o;
    boolean b = Verify.getBoolean(); // just cause some backtracking damage
    attr = Verify.getLocalAttribute("j");
    assert attr == 4200;
    
    /** this does not work because of cached, preallocated Integer objects)
    // fully autoboxed
    Integer a = v;
    j = a;
    attr = Verify.getLocalAttribute("j");
    assert attr == 4200;
    **/
  }
}
