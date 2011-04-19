//
// Copyright (C) 2011 United States Government as represented by the
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

package gov.nasa.jpf.test.mc.data;

import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;


/**
 * JPF regression test for JSON test object creation
 * @author Ivan Mushketik
 */
public class JSONTest extends TestJPF {
  
  class MySup {
    int j;
  }


  @Test
  public void testFillFromJSONSingleClass() {
    if (verifyNoPropertyViolation()) {
      MySup sup = Verify.createFromJSON(MySup.class, "{'j' : 123 }");

      assert sup.j == 123;
    }
  }

  class MyClass extends MySup {
    int i;
  }

  @Test
  public void testFillFromJSONInheritance() {
    if (verifyNoPropertyViolation()) {
      MyClass sup = Verify.createFromJSON(MyClass.class, "{'j':123, 'i':321 }");

      assert sup.j == 123;
      assert sup.i == 321;
    }
  }

  class Primitives {
    boolean z;
    byte b;
    short s;
    int i;
    long l;
    float f;
    double d;
  }

  @Test
  public void testFillPrivimitivesFromJSON() {
    if (verifyNoPropertyViolation()) {
      String json = "{'z': true,'b': 10,'s': 1000,'i': 321, 'l': 123456,'f': 12.34,'d': 23.45}";
      Primitives p = Verify.createFromJSON( Primitives.class, json);

      assert p.z == true;
      assert p.b == 10;
      assert p.s == 1000;
      assert p.i == 321;
      assert p.l == 123456;
      assertEquals(12.34, p.f, 0.001);
      assertEquals(23.45, p.d, 0.001);
    }
  }

  class IntArr {
    int ints[];
  }

  @Test
  public void testFillIntArrayFromJSON() {
    if (verifyNoPropertyViolation()) {
      IntArr ia = Verify.createFromJSON( IntArr.class, "{'ints': [1, 2, 3]}");

      assert ia.ints[0] == 1;
      assert ia.ints[1] == 2;
      assert ia.ints[2] == 3;
    }
  }

  class Boxed {
    Boolean t;
    Boolean f;
    Byte b;
    Short s;
    Integer i;
    Long l;
    Float fl;
    Double d;
  }

  @Test
  public void testFillBoxedPrimitivesFromJSON() {
    if (verifyNoPropertyViolation()) {
      String json = "{'t':true, 'f':false, 'b':10, 's':1000,'i':321, 'l':123456, 'fl':12.34, 'd':23.45 }";
      Boxed b = Verify.createFromJSON( Boxed.class, json);

      assert b.t == true;
      assert b.f == false;
      assert b.b == 10;
      assert b.s == 1000;
      assert b.i == 321;
      assert b.l == 123456;
      assertEquals(12.34, b.fl, 0.001);
      assertEquals(23.45, b.d, 0.001);
    }
  }

  class PrimitiveArrays {
    boolean bools[];
    byte bytes[];
    short shorts[];
    int ints[];
    long longs[];
    float floats[];
    double doubles[];
  }

  @Test
  public void testFillPrimitiveArrays() {
    if (verifyNoPropertyViolation()) {
      String json = "{"
              + "\"bools\" : [true, false, true],"
              + "\"bytes\" : [-40, -30, -20],"
              + "\"shorts\" : [2, 3, 4],"
              + "\"ints\" : [1, 2, 3],"
              + "\"longs\" : [1000, 2000, 3000],"
              + "\"floats\" : [12.34, 23.45, 34.56],"
              + "\"doubles\" : [-12.34, -23.45, -34.56]"
              + "}";
      PrimitiveArrays pa = Verify.createFromJSON( PrimitiveArrays.class, json);

      assert pa.bools[0] == true;
      assert pa.bools[1] == false;
      assert pa.bools[2] == true;

      assert pa.bytes[0] == -40;
      assert pa.bytes[1] == -30;
      assert pa.bytes[2] == -20;

      assert pa.shorts[0] == 2;
      assert pa.shorts[1] == 3;
      assert pa.shorts[2] == 4;

      assert pa.ints[0] == 1;
      assert pa.ints[1] == 2;
      assert pa.ints[2] == 3;

      assert pa.longs[0] == 1000;
      assert pa.longs[1] == 2000;
      assert pa.longs[2] == 3000;

      assertEquals(12.34, pa.floats[0], 0.0001);
      assertEquals(23.45, pa.floats[1], 0.0001);
      assertEquals(34.56, pa.floats[2], 0.0001);

      assertEquals(-12.34, pa.doubles[0], 0.0001);
      assertEquals(-23.45, pa.doubles[1], 0.0001);
      assertEquals(-34.56, pa.doubles[2], 0.0001);
    }
  }

  class InnerClass {
    int i;
  }

  class OuterClass {
    long l;
    InnerClass ic;
  }

  @Test
  public void testInnerClassFilling() {
    if (verifyNoPropertyViolation()) {
      String json =
                "{"
              +   "'l' : 1234,"
              +   "'ic' : {"
              +      "'i' : 4321"
              +   "}"
              + "}";

      OuterClass oc = Verify.createFromJSON( OuterClass.class, json);

      assert oc.l == 1234;
      assert oc.ic.i == 4321;
    }
  }

  @Test
  public void testFillingWhenInnerClassIsNull() {
    if (verifyNoPropertyViolation()) {
      String json = "{"
              + "\"l\" : 1234,"
              + "\"ic\" : null"
              + "}";

      OuterClass oc = Verify.createFromJSON( OuterClass.class, json);

      assert oc.l == 1234;
      assert oc.ic == null;
    }
  }

  class MultiArray {
    int intsInts[][];
  }

  @Test
  public void testMultiArrayFilling() {
    if (verifyNoPropertyViolation()) {
      String json = "{"
              + "\"intsInts\" : [[1, 2, 3], [4, 5, 6]]"
              + "}";

      MultiArray ma = Verify.createFromJSON( MultiArray.class, json);

      assert ma.intsInts[0][0] == 1;
      assert ma.intsInts[0][1] == 2;
      assert ma.intsInts[0][2] == 3;

      assert ma.intsInts[1][0] == 4;
      assert ma.intsInts[1][1] == 5;
      assert ma.intsInts[1][2] == 6;
    }
  }

  class BoxIntsArr {
    Integer ints[];
  }

  @Test
  public void testBoxedTypesArrayFilling() {
    if (verifyNoPropertyViolation()) {
      String json = "{"
              + "\"ints\" : [1, 2, 3]"
              + "}";

      BoxIntsArr bia = Verify.createFromJSON( BoxIntsArr.class, json);

      assert bia.ints[0] == 1;
      assert bia.ints[1] == 2;
      assert bia.ints[2] == 3;
    }
  }

  class IC {
    int i;
  }

  class ArrayOfObjects {
    IC cls[];
  }

  @Test
  public void testArrayOfObjectsFilling() {
    if (verifyNoPropertyViolation()) {
      String json = "{"
              + "\"cls\" : [{\"i\" : 1}, {\"i\" : 2}, {\"i\" : 3}]"
              + "}";

      ArrayOfObjects aoo = Verify.createFromJSON( ArrayOfObjects.class, json);

      assert aoo.cls[0].i == 1;
      assert aoo.cls[1].i == 2;
      assert aoo.cls[2].i == 3;
    }
  }

  class MultObjectsArr {
    IC cls[][];
  }

  @Test
  public void testFillingMultArrayOfObjects() {
      if (verifyNoPropertyViolation()) {
      String json = "{"
              + "\"cls\" : ["
              + "[{\"i\" : 1}, {\"i\" : 2}, {\"i\" : 3}],"
              + "[{\"i\" : 4}, {\"i\" : 5}, {\"i\" : 6}],"
              + "[{\"i\" : 7}, {\"i\" : 8}, {\"i\" : 9}]"
              + "]"
              + "}";

      MultObjectsArr moa = Verify.createFromJSON( MultObjectsArr.class, json);

      assert moa.cls[0][0].i == 1;
      assert moa.cls[0][1].i == 2;
      assert moa.cls[0][2].i == 3;

      assert moa.cls[1][0].i == 4;
      assert moa.cls[1][1].i == 5;
      assert moa.cls[1][2].i == 6;

      assert moa.cls[2][0].i == 7;
      assert moa.cls[2][1].i == 8;
      assert moa.cls[2][2].i == 9;
    }
  }

  class ClassWithString {
    String s1;
    String s2;
  }

  @Test
  public void testFillStringValue() {
    if (verifyNoPropertyViolation()) {
      String json = "{"
              + "\"s1\" : \"val\","
              + "\"s2\" : null"
              + "}";

      ClassWithString cws = Verify.createFromJSON( ClassWithString.class, json);

      assert cws.s1.equals("val") == true;
      assert cws.s2 == null;
    }
  }
}
