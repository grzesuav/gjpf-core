//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.test.vm.reflection;

import gov.nasa.jpf.util.test.TestJPF;
import java.lang.reflect.Constructor;
import org.junit.Test;

/**
 * regression test for constructor reflection
 */
public class ConstructorTest extends TestJPF {

  static class X {

    private String a;

    public X(String x) {
      this.a = x;
      System.out.println(x);
    }
  }

  @Test
  public void testConstructorCall() {
    if (verifyNoPropertyViolation()){
      try {
        Class<X> cls = X.class;
        Constructor<X> ctor = cls.getDeclaredConstructor(new Class<?>[] { String.class });

        X x = ctor.newInstance("I'm an X");
        
        assertNotNull(x); 
      } catch (Throwable t){
        fail("ctor invocation failed: " + t);
      }
    }
  }

  static class I {
    private Integer i;

    public I(Integer i) {
      this.i = i;
    }
  }

  @Test
  public void testConstructorCallInteger() {
    if (verifyNoPropertyViolation()) {
      try {
        Class<I> cls = I.class;
        Constructor<I> ctor = cls.getDeclaredConstructor(new Class<?>[] {Integer.class });

        I obj = ctor.newInstance(42);
        assertNotNull(obj);
        assertEquals(new Integer(42), obj.i);
      } catch (Throwable t) {
        fail("ctor invocation with Integer failed: " + t);
      }
    }
  }

  static class C {
    public C(int i) {
    }

    public C(int... i) {

    }
  }

  @Test
  public void testIfVarArgs() throws NoSuchMethodException {
    if (verifyNoPropertyViolation()) {
      Constructor c1 = C.class.getConstructor(int.class);
      Constructor c2 = C.class.getConstructor(int[].class);

      assert c1.isVarArgs() == false;
      assert c2.isVarArgs() == true;
    }
  }

  static class Ex {
    public Ex(int i) throws NullPointerException, RuntimeException, NoSuchMethodException {

    }
  }

  @Test
  public void testGetExceptionTypes() throws NoSuchMethodException {
    if (verifyNoPropertyViolation()) {
      Class<?> expectedExceptions[] = {NullPointerException.class,
                                       RuntimeException.class,
                                       NoSuchMethodException.class};

      Class<?> resultExceptions[] = Ex.class.getConstructor(int.class).getExceptionTypes();

      assert resultExceptions.length == expectedExceptions.length;

      for (int i = 0; i < resultExceptions.length; i++)
        assert resultExceptions[i].equals(expectedExceptions[i]);
    }
  }

  static class C1 {
    public C1(int i) {}
    public C1(int i, byte b) {}
  }

  static class C2 {
    public C2(int i) {}
  }

  @Test
  public void testConstructorsEquals() throws NoSuchMethodException {
    if (verifyNoPropertyViolation()) {
      Constructor c11 = C1.class.getConstructor(int.class);
      Constructor c11new = C1.class.getConstructor(int.class);
      Constructor c12 = C1.class.getConstructor(int.class, byte.class);
      Constructor c2 = C2.class.getConstructor(int.class);

      assert c11.equals(c11) == true;
      assert c11.equals(c11new) == true;
      assert c11.equals(c12) == false;
      assert c11.equals(c2) == false;
      assert c11.equals(null) == false;
    }
  }
}
