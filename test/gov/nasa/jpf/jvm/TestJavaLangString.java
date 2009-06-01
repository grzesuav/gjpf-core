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
package gov.nasa.jpf.jvm;

import gov.nasa.jpf.util.test.RawTest;
import java.lang.reflect.InvocationTargetException;

/**
 * test of java.lang.String APIs
 */
public class TestJavaLangString extends RawTest {
  public static void main (String[] args) throws InvocationTargetException {
    TestJavaLangString t = new TestJavaLangString();

    if (!runSelectedTest(args, t)){
      runAllTests(args,t);
    }
  }

  public void testIntern () {
    String a = "Blah".intern();
    String b = new String("Blah");

    assert (a != b) : "'new String(intern) != intern' failed";

    String c = b.intern();

    assert (a == c) : "'(new String(intern)).intern() == intern' failed";
  }
  
  public void testToCharArray () {
    String s = "42";
    char[] c = s.toCharArray();
    
    assert c.length == 2;
    assert c[0] == '4';
    assert c[1] == '2';
  }
  
  public void testEquals() {
    String a = "one two";
    String b = "one" + " two";
    String c = "one three";
    
    assert a.equals(b);
    assert !a.equals(c);
  }
  
  public void testIndexOf() {
    String a = "bla.bla";
    int i1 = a.indexOf('.');
    int i2 = a.indexOf('@');
    
    assert i1 == 3;
    assert i2 == -1;
  }
}
