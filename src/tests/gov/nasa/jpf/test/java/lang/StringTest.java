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
package gov.nasa.jpf.test.java.lang;

import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

/**
 * test of java.lang.String APIs
 */
public class StringTest extends TestJPF {

  @Test
  public void testIntern() {
    if (verifyNoPropertyViolation()) {
      boolean c1 = Verify.getBoolean(); // to do some state storing / backtracking
      String a = "Blah".intern();
      String b = new String("Blah");

      assert (a != b) : "'new String(intern) != intern' failed";

      boolean c2 = Verify.getBoolean(); // to do some more storing / backtracking

      String c = b.intern();

      assert (a == c) : "'(new String(intern)).intern() == intern' failed";
    }
  }

  @Test
  public void testToCharArray() {
    if (verifyNoPropertyViolation()) {
      String s = "42";
      char[] c = s.toCharArray();

      assert c.length == 2;
      assert c[0] == '4';
      assert c[1] == '2';
    }
  }

  @Test
  public void testEquals() {
    if (verifyNoPropertyViolation()) {
      String a = "one two";
      String b = "one" + " two";
      String c = "one three";

      assert a.equals(b);
      assert !a.equals(c);
    }
  }

  @Test
  public void testIndexOf() {
    if (verifyNoPropertyViolation()) {
      String a = "bla.bla";
      int i1 = a.indexOf('.');
      int i2 = a.indexOf('@');

      assert i1 == 3;
      assert i2 == -1;
    }
  }

  @Test
  public void testCompareTo() {
    if (verifyNoPropertyViolation()) {
      String a = "aaa";
      String b = "bbb";

      assert a.compareTo(b) < 0;
      assert b.compareTo(a) > 0;
      assert a.compareTo(a) == 0;

      String longer = "aaaaaa";

      assert a.compareTo(longer) < 0;
    }
  }

  @Test
  public void testStartsWith() {
    if (verifyNoPropertyViolation()) {
      String str = "TestString";

      assert str.startsWith("Test") == true;
      assert str.startsWith("String", 4) == true;
      assert str.startsWith("StringString", 4) == false;
      assert str.startsWith("StrUng", 4) == false;
      assert str.startsWith("Test", -5) == false;
    }
  }

  @Test
  public void testEndsWith() {
    if (verifyNoPropertyViolation()) {
      String str = "TestString";

      assert str.endsWith("String") == true;
      assert str.endsWith("") == true;
      assert str.endsWith("StrUng") == false;
    }
  }

  @Test
  public void testTrim() {
    if (verifyNoPropertyViolation()) {
      assert "   Test    ".trim().equals("Test");
      assert "   Test".trim().equals("Test");
      assert "Test    ".trim().equals("Test");
      assert "Test".trim().equals("Test");
      assert "       ".trim().equals("");

    }
  }

  @Test
  public void testReplace() {
    if (verifyNoPropertyViolation()) {
     assert "hello".replace('l', 'a').equals("heaao") == true;
     assert "".replace('l', 'a').equals("") == true;
     assert "hello".replace('f', 'a').equals("hello") == true;
     assert "eve".replace('e', 'a').equals("ava") == true;
    }
  }

  @Test
  public void testNullChar(){
    if (verifyNoPropertyViolation()){
      String s = "\u0000";
      assertTrue( s.length() == 1);
      char c = s.charAt(0);
      assertTrue( Character.isISOControl(c));
    }
  }
}
