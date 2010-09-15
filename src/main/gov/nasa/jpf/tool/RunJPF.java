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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFShell;
import gov.nasa.jpf.util.FileUtils;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 * This class is a wrapper for loading JPF or a JPFShell through a classloader
 * that got initialized from a Config object (i.e. 'native_classpath').
 *
 * This is the main-class entry in the executable RunJPF.jar, which does not
 * require any JPF specific classpath settings, provided the site.properties
 * is configured correctly
 *
 * NOTE this class is not allowed to use any types that would require
 * loading JPF classes during class resolution - this would result in
 * NoClassDefFoundErrors if the respective class is not in RunJPF.jar
 */
public class RunJPF extends Run {

  public static final int HELP = 1;
  public static final int SHOW = 2;
  public static final int LOG = 4;

  static final String JPF_CLASSNAME = "gov.nasa.jpf.JPF";

  public static void main (String[] args) {
    try {
      int options = getOptions(args);

      if (args.length == 0 || isOptionEnabled(HELP, options)) {
        showUsage();
        return;
      }
      if (isOptionEnabled(LOG, options)) {
        Config.enableLogging(true);
      }

      Config conf = new Config(args);

      if (isOptionEnabled(SHOW, options)) {
        conf.printEntries();
      }

      ClassLoader cl = conf.initClassLoader(RunJPF.class.getClassLoader());

      // using JPFShell is Ok since it is just a simple non-derived interface
      // note this uses a <init>(Config) ctor in the shell class if there is one
      JPFShell shell = conf.getInstance("shell", JPFShell.class);
      if (shell != null) {
        shell.start( removeConfigArgs(args)); // responsible for exception handling itself

      } else {
        // we have to load JPF explicitly through the URLClassLoader, and
        // call its start() via reflection - interfaces would only work if
        // we instantiate a JPF object here, which would force us to duplicate all
        // the logging and event handling that preceedes JPF instantiation
        Class<?> jpfCls = cl.loadClass(JPF_CLASSNAME);
        if (!call( jpfCls, "start", new Object[] {conf,args})){
          error("cannot find 'public static start(Config,String[])' in " + JPF_CLASSNAME);
        }
      }
    } catch (NoClassDefFoundError ncfx){
      ncfx.printStackTrace();
    } catch (ClassNotFoundException cnfx){
      error("cannot find " + JPF_CLASSNAME);
    } catch (InvocationTargetException ix){
      // should already be handled by JPF
      ix.getCause().printStackTrace();
    }
  }

  public static int getOptions (String[] args){
    int mask = 0;

    if (args != null){

      for (int i = 0; i < args.length; i++) {
        String a = args[i];
        if ("-help".equals(a)){
          args[i] = null;
          mask |= HELP;

        } else if ("-show".equals(a)) {
          args[i] = null;
          mask |= SHOW;

        } else if ("-log".equals(a)){
          args[i] = null;
          mask |= LOG;
        }
      }
    }

    return mask;
  }

  public static boolean isOptionEnabled (int option, int mask){
    return ((mask & option) != 0);
  }

  static void showUsage() {
    System.out.println("Usage: \"java [<vm-option>..] -jar ...RunJPF.jar [<jpf-option>..] [<app> [<app-arg>..]]");
    System.out.println("  <jpf-option> : -help  : print usage information");
    System.out.println("               | -log   : print configuration initialization steps");
    System.out.println("               | -show  : print configuration dictionary contents");
    System.out.println("               | +<key>=<value>  : add or override key/value pair to config dictionary");
    System.out.println("  <app>        : *.jpf application properties file pathname | fully qualified application class name");
    System.out.println("  <app-arg>    : arguments passed into main() method of application class");
  }

}
