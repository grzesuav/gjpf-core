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
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * raw test for java.util.concurrent.atomic.AtomicLongFieldUpdater
 */
public class TestAtomicLongFieldUpdater {

  static {
    Verify.setProperties("cg.enumerate_cas=true");
  }

  long value;

  void testField () {
    AtomicLongFieldUpdater<TestAtomicLongFieldUpdater> upd =
      AtomicLongFieldUpdater.newUpdater(TestAtomicLongFieldUpdater.class,"value");

    final long v1   = 723489234098734534L;
    final long v2   = 256092348679304843L;
    final long nogo = 823468902346907854L;
    value = v1;

    assert upd.compareAndSet(this,v1,v2);
    assert value == v2;

    assert !upd.compareAndSet(this,v1,nogo);
    assert value == v2;

    assert value == upd.get(this);

    assert v2  == upd.getAndSet(this, v1);
    assert value == v1;

    upd.set(this, v2);
    assert value == v2;

    upd.lazySet(this, v1);
    assert value == v1;

    assert upd.weakCompareAndSet(this,v1,v2);
    assert value == v2;

    assert !upd.weakCompareAndSet(this,v1,nogo);
    assert value == v2;

    assert v2     == upd.getAndAdd(this, 5);
    assert v2 + 5 == value;
  }

  //-------------------- driver to execute single test methods
  public static void main(String[] args) throws InvocationTargetException {
    TestAtomicLongFieldUpdater t = new TestAtomicLongFieldUpdater();
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
