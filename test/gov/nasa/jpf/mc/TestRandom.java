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

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import gov.nasa.jpf.util.test.RawTest;
import gov.nasa.jpf.jvm.Verify;

/**
 * test of gov.nasa.jpf.jvm.Verify nondeterministic data initailization
 */
public class TestRandom extends RawTest {
  public static void main (String[] args) throws InvocationTargetException {
    TestRandom t = new TestRandom();

    if (!runSelectedTest(args, t)){
      t.testRandom(3);
    }
  }

  public void testRandom (int n) {
    int i = Verify.random(n); // we should backtrack 0..n times to this location
    Verify.incrementCounter(0); // counter '0' should have value (n+1) after JPF is done
    System.out.println(i);
  }
  
  public void testJavaUtilRandom () {
    Random random = new Random(42);      // (1)
    
    int a = random.nextInt(4);           // (2)
    System.out.print("a=");
    System.out.println(a);
    
    //... lots of code here
    
    int b = random.nextInt(3);           // (3)
    System.out.print("a=");
    System.out.print(a);
    System.out.print(",b=");
    System.out.println(b);
    
   
    int c = a/(b+a -2);                  // (4)
    System.out.print("a=");
    System.out.print(a);
    System.out.print(",b=");
    System.out.print(b);
    System.out.print(",c=");
    System.out.println(c);

  }
}
