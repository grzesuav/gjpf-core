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
package java.lang;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.Vector;

import sun.misc.CompoundEnumeration;

/**
 * very very rudimentary beginning of a ClassLoader model. We skip
 * the 'abstract' for now, since all we want is resource lookup
 */
public abstract class ClassLoader {
  
  ClassLoader parent;

  // This is JPF internal identifier which set to the globalId of the classLoader
  private int clRef;

  //--- internals

  protected static boolean registerAsParallelCapable() {
    return true; // dummy, in prep for jdk7
  }

  protected ClassLoader() {
    // constructed on the native side
  }

  protected ClassLoader (ClassLoader parent){
    // constructed on the native side
  }

  private native String getResource0 (String rname);

  public URL getResource(String name) {
    URL url = null;

    if(parent == null) {
      String resourcePath = getSystemClassLoader().getResource0(name);
      try {
        url = new URL(resourcePath);
      } catch (MalformedURLException x){
        url = null;
      }
    } else {
      url = parent.getResource(name);
    }

    if (url == null) {
      url = findResource(name);
    }
    return url;
  }

  /**
   * Finds the resource with the given name. Class loader implementations
   * should override this method to specify where to find resources.
   */
  protected URL findResource(String name) {
      return null;
  }

  private native String[] getResources0 (String rname);

  /**
   * Returns an array of URL including all resources with the given name 
   * found in the classpath of this classloader.
   */
  private Enumeration<URL> getResourcesURL(String name) {
    String[] urls = getResources0(name);
    Vector<URL> list = new Vector<URL>(0);
    for(String url: urls) {
      try {
        list.add(new URL(url));
      } catch (MalformedURLException x){
        // process the rest
      }
    }

    return list.elements();
  }

  public Enumeration<URL> getResources(String name) throws IOException {
    Enumeration[] resEnum = new Enumeration[2];

    if(parent == null) {
      resEnum[0] = getSystemClassLoader().getResourcesURL(name);
    } else{
      resEnum[0] = parent.getResources(name);
    }
    resEnum[1] = findResources(name);

    return new CompoundEnumeration(resEnum);
  }

  /**
   * Returns an enumeration representing all the resources with the given 
   * name. Class loader implementations should override this method to 
   * specify where to load resources from.
   */
  protected Enumeration<URL> findResources(String name) throws IOException {
      return (new Vector<URL>()).elements();
  }

  public InputStream getResourceAsStream (String name){
    URL foundResource = getResource(name);
    if (foundResource != null) {
      try {
        return foundResource.openStream();
      } catch (IOException e) {
        System.err.println("cannot open resource " + name);
      }
    }
    return null;
  }

  public native static ClassLoader getSystemClassLoader ();

  public static URL getSystemResource(String name){
    return getSystemClassLoader().getResource(name);
  }

  public static InputStream getSystemResourceAsStream(String name) {
    return getSystemClassLoader().getResourceAsStream(name);
  }

  public static Enumeration<URL> getSystemResources(String name) throws IOException {
    return getSystemClassLoader().getResources(name);
  }

  public ClassLoader getParent() {
    return parent;
  }

  // that has to be fixed. For now it returns a class that is directly 
  // defined by this classloader. But it has to return an initiated class.
  protected native final Class<?> findLoadedClass(String name);

  //--- not yet supported methods
  
  protected  Class<?> defineClass (String name, byte[] b, int off, int len) {
    throw new UnsupportedOperationException("ClassLoader.defineClass() not yet supported");
    //return null;
  }
  
  // Author : Taehoon Lee
  public Class<?> loadClass(String clsName)throws ClassNotFoundException{
	  Class<?> c= Class.forName(clsName);
	  return c;
  }

  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    throw new UnsupportedOperationException("ClassLoader.loadClass(String,boolean) not yet supported");    
  }
  
  protected void resolveClass(Class<?> cls){
    throw new UnsupportedOperationException("ClassLoader.resolveClass(Class<?>) not yet supported");        
  }

  protected final Class<?> findSystemClass (String name){
    throw new UnsupportedOperationException("ClassLoader.findSystemClass() not yet supported");
  }

  protected final Class<?> defineClass(String name,byte[] b,int off,int len,ProtectionDomain protectionDomain){
    throw new UnsupportedOperationException("ClassLoader.defineClass() not yet supported");
  }
}
