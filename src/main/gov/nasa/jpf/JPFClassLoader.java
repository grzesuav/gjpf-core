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
package gov.nasa.jpf;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

/**
 * this is the class loader we use if JPF is started via  gov.nasa.jpf.Main
 * the purpose is to add whatever is configured in the site.properties or
 * an explicit "+native_classpath" setting to the class path, in addition to
 * JPF required libraries that were not specified when invoking Main
 *
 * the twist is that jpf core classes will be accessible through the
 * classpath of whatever loaded Main *and* this JPFClassLoader, hence we can't
 * go with the standard parent-first delegation model. If we do, all core
 * classes will be loaded by the current CL, which then is not able to resolve
 * references that require core libs (which were not in the CP).
 *
 * The solution is to reverse the lookup order, but filter out all JPF classes we
 * know have been already loaded (including JPFClassLoader itself).
 *
 * The second trick is that we base this on an extended URLClassLoader, so that
 * JPF at runtime can determine if it should automatically extend the set
 * of CP URLS. Initially, we only have the JPF core in there (deduced from
 * the JPF CP element of the current CL)
 */
public class JPFClassLoader extends URLClassLoader {


  public JPFClassLoader () {
    // JPF URLs will be added later on
    super(JPFSite.getSite().getNativeCpURLs(),
          JPFClassLoader.class.getClassLoader());
  }


  //--- ClassLoader basics

  public Class<?> loadClass (String name, boolean resolve) throws ClassNotFoundException {

    if (name.startsWith("java.")) { //<2do> should also cover javax
      return super.loadClass(name, resolve);

    } else if (name.equals("gov.nasa.jpf.JPFClassLoader")){
      return JPFClassLoader.class;

    } else if (name.equals("gov.nasa.jpf.JPFSite")){
      return JPFSite.class;

    } else {
      Class<?> cls = findLoadedClass(name);

      if (cls == null) {
        try {
          cls = findClass(name);
          if (resolve) {
            resolveClass(cls);
          }

        } catch (ClassNotFoundException e) {
          cls = super.loadClass(name, resolve);
        }
      }

      return cls;
    }
  }

  // we need to call this later-on, so it needs to be public
  public void addURL (URL url) {
    super.addURL(url);
  }


  // for debugging purposes
  public void printEntries() {
    System.out.println("JPFClassLoader.getURLs() :");
    for (URL url : this.getURLs()){
      System.out.print("  ");
      System.out.println(url);
    }
  }
}
