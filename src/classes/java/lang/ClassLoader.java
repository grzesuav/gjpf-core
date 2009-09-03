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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

/**
 * very very rudimentary beginning of a ClassLoader model. We skip
 * the 'abstract' for now, since all we want is resource lookup
 */
public class ClassLoader {

  static ClassLoader systemClassLoader = new ClassLoader();
  
  ClassLoader parent;  
  
  //--- internals
  private native void init0();
  private native String getResourcePath (String rname);
  
  
  protected ClassLoader() {
    // the system ClassLoader ctor
    init0();
  }
  
  protected ClassLoader (ClassLoader parent){
    this.parent = parent;
    init0();
  }
    
  public URL getResource (String rname) {
    String resourcePath = getResourcePath(rname);
    try {
      return new URL(resourcePath);
    } catch (MalformedURLException x){
      return null;
    }
  }

  public Enumeration<URL> getResources (String name){
    Vector<URL> list = new Vector<URL>(); // we need an enumeration  
    getResources0(list, name);
    return list.elements();
  }
  private void getResources0(Vector list, String name){
    if (parent != null){
      parent.getResources0(list, name);
    }
    URL url = getResource(name);
    if (url != null){
      list.add(url);
    }
  }

  public InputStream getResourceAsStream (String rname){
    return null;
  }
  
  public static ClassLoader getSystemClassLoader () {
    return systemClassLoader;
  }
  
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
  
  protected Class<?> findLoadedClass(String name){
    throw new UnsupportedOperationException("ClassLoader.findLoadClass(String) not yet supported");    
  }
  
  protected void resolveClass(Class<?> cls){
    throw new UnsupportedOperationException("ClassLoader.resolveClass(Class<?>) not yet supported");        
  }
  
  public ClassLoader getParent(){
	  return parent;	  
  }
}
