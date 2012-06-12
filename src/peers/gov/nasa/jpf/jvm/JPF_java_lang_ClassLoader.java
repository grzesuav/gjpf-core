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
package gov.nasa.jpf.jvm;

import gov.nasa.jpf.classfile.ClassPath;

/**
 * native peer for our (totally incomplete) ClassLoader model
 */
public class JPF_java_lang_ClassLoader {

  public static void $init____V (MJIEnv env, int objRef) {
    ClassLoaderInfo systemCl = ClassLoaderInfo.getCurrentSystemClassLoader();
    $init__Ljava_lang_ClassLoader_2__V (env, objRef, systemCl.getClassLoaderObjectRef());
  }

  public static void $init__Ljava_lang_ClassLoader_2__V (MJIEnv env, int objRef, int parentRef) {
    Heap heap = env.getHeap();

    //--- Retrieve the parent ClassLoaderInfo
    ElementInfo ei = heap.get(parentRef);
    int parentGId = ei.getIntField("clRef");
    ClassLoaderInfo parent = env.getVM().getClassLoader(parentGId);

    //--- create the internal representation of the classloader
    ClassLoaderInfo cl = new ClassLoaderInfo(env.getVM(), objRef, new ClassPath(), parent);

    //--- initialize the java.lang.ClassLoader object
    ei = heap.get(objRef);
    ei.setIntField("clRef", cl.getGlobalId());

    // we should use the following block if we ever decide to make systemClassLoader 
    // unavailable if(parent.isSystemClassLoader) {
    //  // we don't want to make the systemCLassLoader available through SUT
    //  parentRef = MJIEnv.NULL;
    // }

    ei.setReferenceField("parent", parentRef);
  }

  public static int getSystemClassLoader____Ljava_lang_ClassLoader_2 (MJIEnv env, int clsObjRef) {
    return ClassLoaderInfo.getCurrentSystemClassLoader().getClassLoaderObjectRef();
  }

  public static int getResource0__Ljava_lang_String_2__Ljava_lang_String_2 (MJIEnv env, int objRef, int resRef){
    Heap heap = env.getHeap();
    String rname = env.getStringObject(resRef);

    int gid = heap.get(objRef).getIntField("clRef");
    ClassLoaderInfo cl = env.getVM().getClassLoader(gid);

    String resourcePath = cl.findResource(rname);

    return env.newString(resourcePath);
  }

  public static int getResources0__Ljava_lang_String_2___3Ljava_lang_String_2 (MJIEnv env, int objRef, int resRef) {
    Heap heap = env.getHeap();
    String rname = env.getStringObject(resRef);

    int gid = heap.get(objRef).getIntField("clRef");
    ClassLoaderInfo cl = env.getVM().getClassLoader(gid);

    String[] resources = cl.findResources(rname);

    return env.newStringArray(resources);
  }
}
