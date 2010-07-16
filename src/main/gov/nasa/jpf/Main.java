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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * this class is a wrapper for starting JPF so that it sets the classpath
 * automatically from the configures JPF extensions
 */
public class Main {

  static final String DEFAULT_APP_CLASS = "gov.nasa.jpf.JPF";

  public static void main (String[] args) {

    String appClsName = null;

    if (args.length > 1 && args[0].equals("-a")){
      appClsName = checkClassName(args[1]);
      if (appClsName == null){
        System.err.println("error: not a valid class name: " + args[1]);
        return;
      }

      String[] a = new String[args.length - 2];
      System.arraycopy(args, 2, a, 0, a.length);
      args = a;
    }

    try {
      JPFClassLoader cl = new JPFClassLoader(args);

      if (appClsName == null){
        appClsName = DEFAULT_APP_CLASS;
      }

      // in case we start something else than JPF
      // <2do> this is badly redudant/overlapping with Config, check if we
      // can't instantiate Config here
      cl.addStartupClasspath(args, appClsName);

      // we assume we need the core no matter what the appClsName is
      cl.addCoreClasspath(args);

      Class<?> appCls = cl.loadClass(appClsName);

      Class<?>[] argTypes = { String[].class };
		  Method mainMth = appCls.getMethod("main", argTypes);

      int modifiers = mainMth.getModifiers();
      if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)){
        System.err.println("no \"public static void main(String[])\" method in" + appClsName);
        return;
      }

      mainMth.invoke(null, new Object[] { args });

    } catch (ClassNotFoundException cnfx){
      System.err.println("error: cannot find " + appClsName);
    } catch (NoSuchMethodException nsmx){
      System.err.println("error: no main(String[]) method found in " + appClsName);
    } catch (IllegalAccessException iax){
      // we already checked for that, but anyways
      System.err.println("no \"public static void main(String[])\" method in " + appClsName);
    } catch (InvocationTargetException ix) {
      ix.getCause().printStackTrace();
      // should already be reported by JPF
    } catch (JPFClassLoaderException ex){
      System.err.println(ex);
    }
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
}
