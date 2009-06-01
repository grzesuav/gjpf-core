//
// Copyright (C) 2005 United States Government as represented by the
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

import gov.nasa.jpf.jvm.Verify;

class MyType {
  String id;
  MyType (String id) { this.id = id; }
  public String toString() { return ("MyType " + id); }
}

@SuppressWarnings("unused")
public class TestDataChoice {
  int intField=42;
  double doubleField=-42.2;
  
  public static void main (String[] args) { 
    TestDataChoice t = new TestDataChoice();
    
    if (args.length > 0) {
      // just run the specified tests
      for (int i = 0; i < args.length; i++) {
        String func = args[i];

        // note that we don't use reflection here because this would
        // blow up execution/test scope under JPF
        if ("testIntFromSet".equals(func)) {
          t.testIntFromSet();
        } else if ("testDoubleFromSet".equals(func)) {
            t.testDoubleFromSet();
        } else if ("testTypedObjectChoice".equals(func)) {
          t.testTypedObjectChoice();
                      
        } else {
          throw new IllegalArgumentException("unknown test function");
        }
      }
    } else {
      // that's mainly for our standalone test verification
      t.testIntFromSet();
      t.testDoubleFromSet();
      t.testTypedObjectChoice();
    }
  }

  void testIntFromSet () {
    int localVar=43;  // read by choice generator
    
    int i = Verify.getInt("my_int_from_set");
    Verify.incrementCounter(0);
    System.out.println(i);    
  }
  
  void testDoubleFromSet () {
    double localVar=4200.0; // read by choice generator
    
    double d = Verify.getDouble("my_double_from_set");
    Verify.incrementCounter(0);
    System.out.println(d);
  }
  
  void testTypedObjectChoice () {
    MyType o1 = new MyType("one");
    MyType o2 = new MyType("two");
    
    Object o = Verify.getObject("my_typed_object");
    Verify.incrementCounter(0);
    System.out.println(o);
  }
}
