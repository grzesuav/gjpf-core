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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 * this class is a wrapper for starting JPF so that it sets the classpath
 * automatically from the configures JPF extensions
 */
public class Main {

  static final String DEFAULT_STARTUP_CLASS = "gov.nasa.jpf.JPF";

  public static void main (String[] args) {

    String startupClsName = null;

    if (args.length > 1 && args[0].equals("-a")){
      // we have an explicit startup class name argument
      startupClsName = checkClassName(args[1]);
      if (startupClsName == null){
        System.err.println("error: not a valid class name: " + args[1]);
        return;
      }

      String[] a = new String[args.length - 2];
      System.arraycopy(args, 2, a, 0, a.length);
      args = a;

    } else {
      startupClsName = DEFAULT_STARTUP_CLASS;
    }

    try {
      /**
      JPFClassLoader cl = new JPFClassLoader(args);

      //--- set the path elements for the JPFClassLoader from config
      Config conf = new Config(args);
      for (File p : conf.getPathArray("native_classpath")){
        cl.addPathElement(p.getAbsolutePath());
      }
      conf.setClassLoader(cl);

      //--- add the preloaded classes
      cl.addPreloadedClass(JPFClassLoader.class);
      cl.addPreloadedClass(JPFClassLoaderException.class);
      cl.addPreloadedClass(Config.class);
      cl.addPreloadedClass(JPFConfigException.class);
      **/
      
      Config conf = new Config(args);
      String[] nativeCp = conf.getCompactStringArray("native_classpath");
      URLClassLoader cl = URLClassLoader.newInstance(getURLs(nativeCp));
      conf.setClassLoader(cl);

      //--- load the startup class through the JPFClassLoader
      Class<?> startupCls = cl.loadClass(startupClsName);

      //--- call the (best) startup class entry
      if (!call( startupCls, "start", new Object[] {conf,args})){
        if (!call( startupCls, "main", new Object[] {args})){
          System.err.println("error: cannot find public static 'start(Config,String[])' or 'main(String[])' in " + startupClsName);
        }
      }

    } catch (ClassNotFoundException cnfx){
      System.err.println("error: cannot find " + startupClsName);
    } 
  }

  private static boolean call( Class<?> cls, String mthName, Object[] args){
    try {
      Class<?>[] argTypes = new Class<?>[args.length];
      for (int i=0; i<args.length; i++){
        argTypes[i] = args[i].getClass();
      }

      Method m = cls.getDeclaredMethod(mthName, argTypes);

      int modifiers = m.getModifiers();
      if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)){
        m.invoke(null, args);
        return true;
      }

    } catch (NoSuchMethodException nsmx){
      return false;
    } catch (IllegalAccessException iax){
      return false;
    } catch (InvocationTargetException ix) {
      ix.getCause().printStackTrace();
      // should already be reported by JPF
      return true;
    } catch (JPFClassLoaderException ex){
      System.err.println("error: " + ex);
      return false;
    }

    return false;
  }

  private static String checkClassName (String cls){
    if (cls == null || cls.isEmpty()){
      return null;
    }

    if (cls.charAt(0) == '.'){
      cls = "gov.nasa.jpf" + cls;
    }
    
    return cls;
  }



  static URL[] getURLs (String[] paths){
    ArrayList<URL> urls = new ArrayList<URL>();

    for (String p : paths) {
      File f = new File(p);
      if (f.exists()) {
        try {
          urls.add(f.toURI().toURL());
        } catch (MalformedURLException x) {
          throw new RuntimeException("illegal native_classpath element: " + p);
        }
      }
    }

    return urls.toArray(new URL[urls.size()]);
  }

}
