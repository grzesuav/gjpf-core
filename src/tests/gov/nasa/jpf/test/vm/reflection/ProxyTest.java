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

package gov.nasa.jpf.test.vm.reflection;

import gov.nasa.jpf.util.test.TestJPF;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.junit.Test;

/**
 * regression test for java.lang.reflect.Proxy
 */
public class ProxyTest extends TestJPF {
  
  interface Ifc {
    int foo (int a);
  }
  
  class MyHandler implements InvocationHandler {
    int data;
    
    MyHandler (int d){
      data = d;
    }
    
    public Object invoke (Object proxy, Method mth, Object[] args){
      int a = (Integer)args[0];
      System.out.println("proxy invoke of " + mth);
      //System.out.println(" on " + proxy);
      System.out.println(" with " + a);

      return Integer.valueOf(data + a);
    }
  }

  @Test
  public void basicProxyTest (){
    if (verifyNoPropertyViolation()){
      MyHandler handler = new MyHandler(42);
      Ifc proxy = (Ifc)Proxy.newProxyInstance( Ifc.class.getClassLoader(),
                                               new Class[] { Ifc.class },
                                               handler);

      int res = proxy.foo(1);
      System.out.println(res);
      assertTrue( res == 43);
    }
  }
  
  //--------------- proxy for annotation
  @Retention(RetentionPolicy.RUNTIME)
  @interface AnnoIfc {
    int baz();
  }
  
  class AnnoHandler implements InvocationHandler {
    public Object invoke (Object proxy, Method mth, Object[] args){
      System.out.println("proxy invoke of " + mth);
      return Integer.valueOf(42);
    }
  }
  
  @Test
  public void annoProxyTest (){
    if (verifyNoPropertyViolation()){
      InvocationHandler handler = new AnnoHandler();
      AnnoIfc proxy = (AnnoIfc)Proxy.newProxyInstance( AnnoIfc.class.getClassLoader(),
                                               new Class[] { AnnoIfc.class },
                                               handler);

      int res = proxy.baz();
      System.out.println(res);
      assertTrue( res == 42);
    }
  }
}
