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

package gov.nasa.jpf.tool;

import gov.nasa.jpf.JPFSite;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;


/**
 * starter class to use the (minimal) ant installation that comes with
 * jpf-core
 */
public class RunAnt {

  public static void main (String[] args){

    JPFSite site = JPFSite.getSite();
    File coreDir = site.getCoreDir();

    if (coreDir == null){
      abort("no JPF core dir found - do you have a (valid) ~/.jpf/site.properties file?");
    }

    ArrayList<URL> urlList = new ArrayList<URL>();

    addJavac(urlList);
    addJPFToolJars(urlList,coreDir);

    URL[] urls = urlList.toArray(new URL[urlList.size()]);
    URLClassLoader cl = new URLClassLoader(urls, RunAnt.class.getClassLoader());

    try {
      Class<?> jpfCls = cl.loadClass("org.apache.tools.ant.Main");

      Class<?>[] argTypes = { String[].class };
		  Method mainMth = jpfCls.getMethod("main", argTypes);

      mainMth.invoke(null, new Object[] { args });

    } catch (ClassNotFoundException cnfx){
      abort("cannot find org.apache.tools.ant.Main");
    } catch (NoSuchMethodException nsmx){
      abort("no org.apache.tools.ant.Main.main(String[]) method found");
    } catch (IllegalAccessException iax){
      abort("no \"public static void main(String[])\" method in org.apache.tools.ant.Main");
    } catch (InvocationTargetException ix) {
      ix.getCause().printStackTrace();
    }

    // we let the InvocationTargetException pass
  }

  static void abort (String msg){
    System.err.println("ERROR: " + msg);
    System.exit(1);
  }
  
  static void addJavac(List<URL> list) {
    File jar = null;
    char sc = File.separatorChar;
    String javaHome = System.getProperty("java.home");
    String os = System.getProperty("os.name");

    if ("Mac OS X".equals(os)){
      // nothing to do, it's in classes.jar
    } else {
      // on Linux and Windows it's in ${java.home}/lib/tools.jar
      File toolsJar = new File(javaHome + sc + "lib" + sc + "tools.jar");
      if (toolsJar.isFile()){
        try {
          list.add(toolsJar.toURI().toURL());
        } catch (MalformedURLException ex) {
          abort("malformed URL: " + toolsJar.getAbsolutePath());
        }
      } else {
        abort("can't find javac, no " + toolsJar.getPath());
      }
    }
  }

  static void addJPFToolJars (List<URL> list, File jpfCoreDir) {
    boolean foundAnt = false;
    char sc = File.separatorChar;
    File libDir = new File(jpfCoreDir.getPath() + sc + "tools" + sc + "lib");

    if (libDir.isDirectory()){
      for (File f : libDir.listFiles()) {
        String name = f.getName();
        if (name.endsWith(".jar")) {
          try {
            list.add(f.toURI().toURL());
          } catch (MalformedURLException ex) {
            abort("malformed URL: " + f.getAbsolutePath());
          }
          if (f.getName().equals("ant.jar")){
            foundAnt = true;
          }
        }
      }
    }

    if (!foundAnt){
      abort("no ant.jar found in " + libDir.getAbsolutePath());
    }
  }
}
