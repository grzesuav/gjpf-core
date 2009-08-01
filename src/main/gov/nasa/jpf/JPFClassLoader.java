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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  static Pattern libClassPattern = Pattern.compile("(javax?|sun)\\.");

  HashMap<String,Class<?>> preloads = new HashMap<String,Class<?>>();


  public JPFClassLoader () {
    // JPF URLs will be added later on, mostly by JPF after we have a Config
    super(new URL[0]);

    // we need to get a CP entry to load JPF and Config from. If we have a
    // jpf.jar in the class.path, copy that. Otherwise use the jpf-core setting
    // from the site
    File core = getCoreFromClasspath();
    if (core == null){
      core = getCoreFromSite();
      if (core == null){ // out of luck
        throw new JPFClassLoaderException("no classpath entry for gov.nasa.jpf.JPF found (check site.properties)");
      }
    }

    try {
      URL url = core.toURI().toURL();
      addURL(url);  // now we can load JPF and Config
    } catch (MalformedURLException x){
      throw new JPFClassLoaderException("illegal init classpath: " + core);
    }

    // add our known preloads
    addPreloadedClass(JPFClassLoader.class);
    addPreloadedClass(JPFClassLoaderException.class);
  }

  public void addPreloadedClass (Class<?> cls){
    preloads.put(cls.getName(), cls);
  }

  //--- ClassLoader basics

  public Class<?> loadClass (String name, boolean resolve) throws ClassNotFoundException {

    if (libClassPattern.matcher(name).matches()) {
      return super.loadClass(name, resolve);

    } else {
      Class<?> cls = preloads.get(name);

      if (cls == null) {
        cls = findLoadedClass(name);

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
      }

      return cls;
    }
  }

  // we need to call this later-on, so it needs to be public
  public void addURL (URL url) {
    // we could check if it's already in there, but there is probably not
    // much runtime incentive
    super.addURL(url);
  }

  public String[] getClasspathEntries() {
    URL[] urls = getURLs();
    String[] cpEntries = new String[urls.length];

    for (int i=0; i<urls.length; i++){
      cpEntries[i] = urls[i].getPath();
    }

    return cpEntries;
  }

  //--- internals


  /**
   * return the classpath element for our init classes (JPF, Config) that is
   * configured in the site.properties.
   *
   * Note that we can't use Config for this since we still have to locate the
   * Config class to use from this cpe
   */
  protected File getCoreFromSite () {
    JPFSite site = JPFSite.getSite();
    File coreDir = site.getSiteCoreDir();

    if (coreDir != null && coreDir.isDirectory()){
      File buildDir = new File(coreDir, "build");
      if (buildDir.isDirectory()) {
        File jpfJar = new File(buildDir, "jpf.jar");
        if (jpfJar.isFile()) {
          return jpfJar;
        }
      }
    }

    return null;
  }

  protected File getCoreFromClasspath () {
    try {
      Class<?> tag = Class.forName("gov.nasa.jpf.$coreTag");

      URL url = tag.getResource("$coreTag.class"); // we know it's there if we can load the class
      String s = url.toString();

      String path = null;
      if (s.startsWith("jar:file:")){
        // strip leading "jar:file:" amd trailing "!.." jar path
        path = s.substring(9,s.indexOf('!'));
      } else if (s.startsWith("file:")) {
        // strip leading "file:" and trailing "/gov/nasa/jpf/$coreTag.class"
        path = s.substring(5,s.length()-28);
      } else {
        return null; // don't know what that is
      }
      
      File f = new File(path);
      if (f.exists()){
        return f;
      } else {
        return null; // maybe we should throw here
      }

    } catch (ClassNotFoundException cnfx){
      return null;
    }
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
