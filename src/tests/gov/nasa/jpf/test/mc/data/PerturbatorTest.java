//
// Copyright (C) 2009 United States Government as represented by the
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
 *
 */
public class PerturbatorTest extends TestJPF {

  int data = 42;

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  @Test
  public void testIntFieldPerturbation() {

    if (!isJPFRun()){
      Verify.resetCounter(0);
    }

    if (verifyNoPropertyViolation("+listener=.listener.Perturbator",
                                  "+perturb.fields=data",
                                  "+perturb.data.class=.perturb.IntOverUnder",
                                  "+perturb.data.field=gov.nasa.jpf.test.mc.data.PerturbatorTest.data",
                                  "+perturb.data.delta=1")){
      System.out.println("instance field perturbation test");
      int d = data;
      System.out.print("d = ");
      System.out.println(d);

      Verify.incrementCounter(0);
      switch (Verify.getCounter(0)){
        case 1: assert d == 43; break;
        case 2: assert d == 42; break;
        case 3: assert d == 41; break;
        default:
          assert false : "wrong counter value: " + Verify.getCounter(0);
      }

    } else {
      assert Verify.getCounter(0) == 3;
    }
  }

  @Test
  public void testFieldPerturbationLocation() {

    if (!isJPFRun()){
      Verify.resetCounter(0);
    }

    if (verifyNoPropertyViolation("+listener=.listener.Perturbator",
                                  "+perturb.fields=data",
                                  "+perturb.data.class=.perturb.IntOverUnder",
                                  "+perturb.data.field=gov.nasa.jpf.test.mc.data.PerturbatorTest.data",
                                  "+perturb.data.location=PerturbatorTest.java:87",
                                  "+perturb.data.delta=1")){
      System.out.println("instance field location perturbation test");

      int x = data; // this should not be perturbed
      System.out.print("x = ");
      System.out.println(x);

      int d = data; // this should be
      System.out.print("d = ");
      System.out.println(d);

      Verify.incrementCounter(0);

    } else {
      assert Verify.getCounter(0) == 3;
    }
  }

  int foo (int i) {
    return i;
  }

  @Test
  public void testIntReturnPerturbation() {

    if (!isJPFRun()){
      Verify.resetCounter(0);
    }

    if (verifyNoPropertyViolation("+listener=.listener.Perturbator",
                                  "+perturb.returns=foo",
                                  "+perturb.foo.class=.perturb.IntOverUnder",
                                  "+perturb.foo.method=gov.nasa.jpf.test.mc.data.PerturbatorTest.foo(int)",
                                  "+perturb.foo.location=PerturbatorTest.java:121",
                                  "+perturb.foo.delta=1")){
      System.out.println("int return perturbation test");

      int d = foo(-1); // this should not be perturbed
      System.out.print("foo() = ");
      System.out.println(d);

      d = foo(42); // this should be
      System.out.print("foo() = ");
      System.out.println(d);

      Verify.incrementCounter(0);
      switch (Verify.getCounter(0)){
        case 1: assert d == 43; break;
        case 2: assert d == 42; break;
        case 3: assert d == 41; break;
        default:
          assert false : "wrong counter value: " + Verify.getCounter(0);
      }

    } else {
      assert Verify.getCounter(0) == 3;
    }
  }

}
