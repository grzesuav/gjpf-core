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

  public static void main (String[] args) {

    JPFClassLoader cl = new JPFClassLoader();

    try {
      Class<?> jpfCls = cl.loadClass("gov.nasa.jpf.JPF");

      Class<?>[] argTypes = { String[].class };
		  Method mainMth = jpfCls.getMethod("main", argTypes);

      int modifiers = mainMth.getModifiers();
      if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)){
        System.err.println("no \"public static void main(String[])\" method in gov.nasa.jpf.JPF");
        return;
      }

      mainMth.invoke(null, new Object[] { args });

    } catch (ClassNotFoundException cnfx){
      System.err.println("error: cannot find gov.nasa.jpf.JPF");
    } catch (NoSuchMethodException nsmx){
      System.err.println("error: no gov.nasa.jpf.JPF.main(String[]) method found");
    } catch (IllegalAccessException iax){
      // we already checked for that, but anyways
      System.err.println("no \"public static void main(String[])\" method in gov.nasa.jpf.JPF");
    } catch (InvocationTargetException ix) {
      // should already be reported by JPF
      //ix.getCause().printStackTrace();
    }
  }
}
