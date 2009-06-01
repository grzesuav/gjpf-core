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
package gov.nasa.jpf.vm;

import gov.nasa.jpf.util.test.RawTest;
import java.lang.reflect.Method;

public class TestMethodReflection extends RawTest {
  
  public static void main (String[] args) {
    TestMethodReflection t = new TestMethodReflection();

    if (!runSelectedTest(args, t)){
      runAllTests(args,t);
    }
  }
  
  
  double data = 42.0;
  
  public double foo (int a, double d, String s) {
    assert data == 42.0 : "wrong object data";
    assert a == 3 : "wrong int parameter value";
    assert d == 3.33 : "wrong double parameter value";
    assert "Blah".equals(s) : "wrong String parameter value";
    
    return 123.456;
  }

  public void testInstanceMethodInvoke () {
    TestMethodReflection o = new TestMethodReflection();
    
    try {
      Class<?> cls = o.getClass();
      Method m = cls.getMethod("foo", int.class, double.class, String.class);
      
      Object res = m.invoke(o, new Integer(3), new Double(3.33), "Blah");
      double d = ((Double)res).doubleValue();
      assert d == 123.456 : "wrong return value";
      
    } catch (Throwable t) {
      t.printStackTrace();
      assert false : " unexpected exception: " + t;
    }
  }
  

  static class Boo {
    static int d = 42;
  }
  static class Faz {
    static int d = 4200;
  }
  
  public Boo getBoo () {
    return null;
  }
  
  public void testReturnType () {
    TestMethodReflection o = new TestMethodReflection();
    try {
      Class<?> cls = o.getClass();
      Method m = cls.getMethod("getBoo");
      Class<?> rt = m.getReturnType();
      String s = rt.getName();
      assert "gov.nasa.jpf.jvm.TestMethodReflection$Boo".equals(s) : "wrong return type: " + s;
      
    } catch (Throwable t) {
      t.printStackTrace();
      assert false : " unexpected exception in Method.getReturnType(): " + t;
    }    
  }
  
  public void boofaz (Boo b, Faz f) {
  }
  
  public void testParameterTypes () {
    TestMethodReflection o = new TestMethodReflection();
    try {
      Class<?> cls = o.getClass();
      for (Method m : cls.getMethods()) {
        if (m.getName().equals("boofaz")) {
          Class<?>[] pt = m.getParameterTypes();
          assert "gov.nasa.jpf.jvm.TestMethodReflection$Boo".equals(pt[0].getName())
            : "wrong parameter type 0: " + pt[0].getName();
          assert "gov.nasa.jpf.jvm.TestMethodReflection$Faz".equals(pt[1].getName())
            : "wrong parameter type 1: " + pt[1].getName();          
        }
      }
            
    } catch (Throwable t) {
      t.printStackTrace();
      assert false : " unexpected exception in Method.getParameterTypes(): " + t;
    }    
  }
  
}
