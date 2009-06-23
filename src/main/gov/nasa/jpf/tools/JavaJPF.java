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
package gov.nasa.jpf.tools;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class JavaJPF {
  static void printTrimmedStackTrace(Throwable e) {
    StackTraceElement[] st = e.getStackTrace();
    LinkedList<StackTraceElement> newStList = new LinkedList<StackTraceElement>();
    int minStuff = Thread.currentThread().getStackTrace().length + 2;
    int i = 0;
    boolean internal = true;
    for (;i < st.length && (i <= minStuff || internal); i++) {
      StackTraceElement tmp = st[st.length - 1 - i];
      String cname = tmp.getClassName();
      internal = 
        cname.startsWith("java.lang.") || 
        cname.startsWith("sun.reflect.");
      if (!internal) {
        newStList.addFirst(tmp);
      }
    }
    for (;i < st.length; i++) {
      StackTraceElement tmp = st[st.length - 1 - i];
      newStList.addFirst(tmp);
    }
    StackTraceElement[] newSt =
      newStList.toArray(new StackTraceElement[newStList.size()]);
    e.setStackTrace(newSt);
    e.printStackTrace();
  }

  static void showUsage() {
    System.out.println("Usage: \"java [<vm-option>..] [RunTool] gov.nasa.jpf.tools.JavaJPF [<jpf-option>..] [<app> [<app-arg>..]]");
    System.out.println("  <jpf-option> : -c <config-file>  : name of config properties file (default \"jpf.properties\")");
    System.out.println("               | -help  : print usage information");
    System.out.println("               | -show  : print configuration dictionary contents");
    System.out.println("               | +<key>=<value>  : add or override key/value pair to config dictionary");
    System.out.println("  <app>        : application class or *.xml error trace file");
    System.out.println("  <app-arg>    : arguments passed into main(String[]) if application class");
  }

  public static void invokeMain(Class<?> clazz, String[] args)
  throws NoSuchMethodException, InvocationTargetException {
    Class<?>[] params = new Class [] { args.getClass() };
    Method main = clazz.getMethod("main", params);
    if (!Modifier.isStatic(main.getModifiers())) {
      throw new NoSuchMethodException("main method in " +
          clazz + " not static!");
    }
    try {
      main.invoke(null, new Object[] { args });
    } catch (IllegalAccessException e) {
      throw new NoSuchMethodException("main method in " +
          clazz + " not accessible!");
    }
  }

  public static void main(String[] args) {

    Config conf = JPF.createConfig(args);

    if (JPF.isHelpRequest(args)) {
      showUsage();
    }

    if (JPF.isPrintConfigRequest(args)) {
      conf.print(new PrintWriter(System.out));
    }

    String targetClassName = conf.getTargetArg();
    if (conf.getTargetArg() == null) return;

    ClassLoader cl;
    String vmClassPath = conf.getString("vm.classpath");
    if (vmClassPath == null) {
      cl = JavaJPF.class.getClassLoader();
    } else {
      StringTokenizer cpToks =
        new StringTokenizer(vmClassPath, File.pathSeparator);
      ArrayList<URL> cpList = new ArrayList<URL>();
      while (cpToks.hasMoreTokens()) {
        try {
          cpList.add(new File(cpToks.nextToken()).toURI().toURL());
        } catch (MalformedURLException e) {
          // shouldn't happen
          e.printStackTrace();
          System.exit(2);
        }
      }
      URL[] cpArr = cpList.toArray(new URL[cpList.size()]);
      cl = new URLClassLoader(cpArr, JavaJPF.class.getClassLoader());
    }

    try {
      Class<?> mainClass = cl.loadClass(targetClassName);
      invokeMain(mainClass,conf.getTargetArgParameters());
    } catch (ClassNotFoundException e) {
      printTrimmedStackTrace(e);
      System.exit(1);
    } catch (NoSuchMethodException e) {
      printTrimmedStackTrace(e);
      System.exit(1);
    } catch (InvocationTargetException ite) {
      Throwable e = ite.getTargetException();
      printTrimmedStackTrace(e);
      System.exit(1);
    }
  }

}
