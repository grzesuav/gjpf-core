//
// Copyright (C) 2012 United States Government as represented by the
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
package gov.nasa.jpf.jvm;

import java.net.MalformedURLException;
import java.net.URL;

import gov.nasa.jpf.classfile.ClassPath;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 * 
 * Native peer for java.net.URLClassLoader
 */
public class JPF_java_net_URLClassLoader extends JPF_java_lang_ClassLoader{

  public static void addURL0__Ljava_lang_String_2__V (MJIEnv env, int objRef, int urlRef) throws MalformedURLException {
    ClassLoaderInfo cl = env.getClassLoaderInfo(objRef);
    ClassPath cp = cl.getClassPath();
    String url = env.getStringObject(urlRef);

    String path = null;
    URL u = new URL(url);
    String protocol = u.getProtocol();
    if(protocol.equals("file")) {
      path = u.getFile();
    } else if(protocol.equals("jar")){
      path = url.substring(url.lastIndexOf(':')+1, url.indexOf('!'));
    } else {
      // we don't support other protocols for now!
      return;
    }

    cp.addPathName(path);
  }

  public static int findClass__Ljava_lang_String_2__Ljava_lang_Class_2 (MJIEnv env, int objRef, int nameRef) {
    String typeName = env.getStringObject(nameRef);

    // retrieve the classloader
    ClassLoaderInfo cl = env.getClassLoaderInfo(objRef);

    // check if the given type is in the classloader search path
    String className = Types.getClassNameFromTypeName(typeName);

    // check if this class  has been already defined
    ClassInfo ci = cl.getDefinedClassInfo(className);
    if(ci != null) {
      return ci.getClassObjectRef();
    }

    ClassPath.Match match = cl.getMatch(typeName);
    if(match != null) {
      byte[] buffer = match.getBytes();
      try{
        return defineClass(env, cl, typeName, buffer, 0, buffer.length, match);
      } catch(LoadOnJPFRequired rre) {
        env.repeatInvocation();
        return MJIEnv.NULL;
      }
    } else{
      env.throwException("java.lang.ClassNotFoundException", className);
      return MJIEnv.NULL;
    }
  }

  public static int findResource0__Ljava_lang_String_2__Ljava_lang_String_2 (MJIEnv env, int objRef, int resRef){
    String rname = env.getStringObject(resRef);

    ClassLoaderInfo cl = env.getClassLoaderInfo(objRef);

    String resourcePath = cl.findResource(rname);

    return env.newString(resourcePath);
  }

  public static int findResources0__Ljava_lang_String_2___3Ljava_lang_String_2 (MJIEnv env, int objRef, int resRef) {
    String rname = env.getStringObject(resRef);

    ClassLoaderInfo cl = env.getClassLoaderInfo(objRef);

    String[] resources = cl.findResources(rname);

    return env.newStringArray(resources);
  }
}
