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

package gov.nasa.jpf.jvm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * raw test for java.util.concurrent.atomic.AtomicReferenceFieldUpdater
 */
public class TestAtomicReferenceFieldUpdater {

  static {
    Verify.setProperties("cg.enumerate_cas=true");
  }

  String str;
  byte[] buf;

  void testStringField () {
    AtomicReferenceFieldUpdater<TestAtomicReferenceFieldUpdater,String> upd =
      AtomicReferenceFieldUpdater.newUpdater(TestAtomicReferenceFieldUpdater.class,String.class,"str");

    String s1 = "one";
    String s2 = "two";
    str = s1;

    System.out.println(str);
    assert upd.compareAndSet(this,s1,s2);
    System.out.println(str);
    assert str == s2;

    assert !upd.compareAndSet(this,s1,"nogo");
    assert str == s2;
    assert str == upd.get(this);

    assert s2  == upd.getAndSet(this, s1);
    assert str == s1;

    upd.set(this, s2);
    assert str == s2;

    upd.lazySet(this, s1);
    assert str == s1;

    assert upd.weakCompareAndSet(this,s1,s2);
    assert str == s2;

    assert !upd.weakCompareAndSet(this,s1,"nogo");
    assert str == s2;
  }

  void testByteArrayField() {
    AtomicReferenceFieldUpdater<TestAtomicReferenceFieldUpdater,byte[]> upd =
      AtomicReferenceFieldUpdater.newUpdater(TestAtomicReferenceFieldUpdater.class,byte[].class,"buf");

    byte[] b1 = new byte[10];
    byte[] b2 = new byte[5];

    buf = b1;
    System.out.println(buf);
    assert upd.compareAndSet(this,b1, b2);
    System.out.println(buf);
    assert (buf == b2);

    assert !upd.compareAndSet(this,b1, new byte[3]);
    assert (buf == b2);
  }

  //-------------------- driver to execute single test methods
  public static void main(String[] args) throws InvocationTargetException {
    TestAtomicReferenceFieldUpdater t = new TestAtomicReferenceFieldUpdater();
    Class<?> cls = t.getClass();
    Object[] a = new Object[0];

    if (args.length > 0) {
      // just run the specified tests
      for (int i = 0; i < args.length; i++) {
        String func = args[i];

        try {
          Method m = cls.getMethod(func);
          m.invoke(t, a);
        } catch (NoSuchMethodException e) {
          throw new IllegalArgumentException("unknown test function: " + func);
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException("illegal access of function: " + func);
        }
      }
    }
  }

}
