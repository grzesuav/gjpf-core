//
// Copyright (C) 2012 United States Government as represented by the
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
package gov.nasa.jpf.test.java.net;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

import gov.nasa.jpf.util.test.TestJPF;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 * 
 * test of java.lang.ClassLoader API
 */
public class URLClassLoaderTest extends TestJPF {

  public class CustomizedClassLoader extends URLClassLoader {

    public CustomizedClassLoader(URL[] urls) {
        super(urls);
    }

    public CustomizedClassLoader(URL[] urls, ClassLoader parent) {
      super(urls, parent);
    }
    
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    public Class<?> getLoadedClass(String name) {
      return findLoadedClass(name);
    }
  }

  @Test
  public void testConstructor_NullPointerException() {
    if (verifyUnhandledException("java.lang.NullPointerException")) {
      new URLClassLoader(null);
    }
  }

  @Test 
  public void testConstructorEmptyURLs () {
    if (verifyNoPropertyViolation()) {
      URLClassLoader cl = new URLClassLoader(new URL[0]);
      assertNotNull(cl.getParent());
      assertEquals(cl.getParent(), ClassLoader.getSystemClassLoader());
    }
  }

  @Test
  public void testConstructorParent() {
    if (verifyNoPropertyViolation()) {
      URL[] urls = new URL[0];
      ClassLoader parent = new CustomizedClassLoader(urls);
      URLClassLoader cl =  new URLClassLoader(urls, parent);

      assertNotNull(parent.getParent());
      assertEquals(parent.getParent(), ClassLoader.getSystemClassLoader());

      assertNotNull(cl.getParent());
      assertEquals(cl.getParent(), parent);
    }
  }

  @Test
  public void testLoadClass_NoClassDefFoundError() throws ClassNotFoundException {
    if (verifyUnhandledException("java.lang.NoClassDefFoundError")) {
      URL[] urls = new URL[0];
      URLClassLoader cl = new URLClassLoader(urls);
      cl.loadClass("java/lang/Class");
    }
  }

  @Test
  public void testLoadClass_ClassNotFoundException() throws ClassNotFoundException {
    if (verifyUnhandledException("java.lang.ClassNotFoundException")) {
      URL[] urls = new URL[0];
      URLClassLoader cl =  new URLClassLoader(urls);
      cl.loadClass("java.lang.Does_Not_Exist");
    }
  }

  @Test
  public void testLoadClass_ClassNotFoundException2() throws ClassNotFoundException {
    if (verifyUnhandledException("java.lang.ClassNotFoundException")) {
      URL[] urls = new URL[0];
      URLClassLoader cl =  new URLClassLoader(urls);
      cl.loadClass("java.lang.Class.class");
    }
  }

  @Test
  public void testLoadClass() throws ClassNotFoundException {
   if (verifyNoPropertyViolation()) {
      URL[] urls = new URL[0];
      ClassLoader systemCl = ClassLoader.getSystemClassLoader();
      ClassLoader parent = new CustomizedClassLoader(urls);
      URLClassLoader cl =  new URLClassLoader(urls, parent);

      String cname = "java.lang.Class";
      Class<?> c1 = systemCl.loadClass(cname);
      Class<?> c2 = parent.loadClass(cname);
      Class<?> c3 = cl.loadClass(cname);

      assertSame(c1, c2);
      assertSame(c1, c3);
      // this test fails on the host JVM, cause java.lang.Class is loaded by
      // bootstrap classloader and therefore c1.getClassLoader() returns null,
      // but the test passes on JPF.
      assertSame(c1.getClassLoader(), systemCl);
    }
  }

  @Test
  public void testFindLoadedClass() throws ClassNotFoundException, MalformedURLException {
    if (verifyNoPropertyViolation()) {
      URL[] urls = new URL[0];
      CustomizedClassLoader ucl1 = new CustomizedClassLoader(urls);
      CustomizedClassLoader ucl2 = new CustomizedClassLoader(urls, ucl1);

      String cname = "java.lang.Class";

      Class<?> c = ucl2.loadClass(cname);
      assertNotNull(c);
      assertEquals(c.getName(), cname);

      // systemClassLoader is going to be the defining classloader
      assertNull(ucl2.getLoadedClass(cname));
      assertNull(ucl1.getLoadedClass(cname));
    }
  }
}
