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

import gov.nasa.jpf.JPFException;
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

  public static int findLoadedClass__Ljava_lang_String_2__Ljava_lang_Class_2 (MJIEnv env, int objRef, int nameRef) {
    Heap heap = env.getHeap();
    String cname = env.getStringObject(nameRef);

    int gid = heap.get(objRef).getIntField("clRef");
    ClassLoaderInfo cl = env.getVM().getClassLoader(gid);

    ClassInfo ci = cl.getDefinedClassInfo(cname);
    if(ci != null) {
      return ci.getClassObjectRef();
    }

    return MJIEnv.NULL;
  }

  public static int findSystemClass__Ljava_lang_String_2__Ljava_lang_Class_2 (MJIEnv env, int objRef, int nameRef) {
    String cname = env.getStringObject(nameRef);

    checkForIllegalName(env, cname);
    if(env.hasException()) {
      return MJIEnv.NULL;
    }

    ClassLoaderInfo cl = ClassLoaderInfo.getCurrentSystemClassLoader();

    ClassInfo ci = cl.tryGetResolvedClassInfo(cname);
    if (ci == null){
      env.throwException("java.lang.ClassNotFoundException", cname);
      return MJIEnv.NULL;
    }

    if(!ci.isRegistered()) {
      ci.registerClass(env.getThreadInfo());
    }

    return ci.getClassObjectRef();
  }

  public static int defineClass0__Ljava_lang_String_2_3BII__Ljava_lang_Class_2 
           (MJIEnv env, int objRef, int nameRef, int bufferRef, int offset, int length) {
    Heap heap = env.getHeap();

    // retrieve ClassLoaderInfo instance
    int gid = heap.get(objRef).getIntField("clRef");
    ClassLoaderInfo cl = env.getVM().getClassLoader(gid);

    String cname = env.getStringObject(nameRef);
    byte[] buffer = env.getByteArrayObject(bufferRef);

    return defineClass(env, cl, cname, buffer, offset, length, null);
  }
  
  protected static int defineClass (MJIEnv env, ClassLoaderInfo cl, String cname, byte[] buffer, int offset, int length, ClassPath.Match match) {

    if(!check(env, cname, buffer, offset, length)) {
      return MJIEnv.NULL;
    }

    if(match == null) {
      match = cl.getMatch(cname);
    }

    // determine whether that the corresponding class is already defined by this 
    // classloader, if so this attempt is invalid and loading throws a LinkageError.
    if(cl.getDefinedClassInfo(cname) != null) {
      env.throwException("java.lang.LinkageError");
      return MJIEnv.NULL;
    }

    ClassInfo ci = null; 
    try {
      ci = cl.getResolvedClassInfo(cname, buffer, offset, length, match);
    } catch(JPFException e) {
      env.throwException("java.lang.ClassFormatError");
      return MJIEnv.NULL;
    }

    if (!ci.isRegistered()) {
      ThreadInfo ti = env.getThreadInfo();
      ci.registerClass(ti);
    }

    return ci.getClassObjectRef();
  }

  protected static boolean check(MJIEnv env, String cname, byte[] buffer, int offset, int length) {
    // throw SecurityExcpetion if the package prefix is java
    checkForProhibitedPkg(env, cname);

    // throw NoClassDefFoundError if the given class does name might 
    // not be a valid binary name
    checkForIllegalName(env, cname);

    // throw IndexOutOfBoundsException if buffer length is not consistent
    // with offset
    checkData(env, buffer, offset, length);

    return !env.hasException();
  }

  protected static void checkForProhibitedPkg(MJIEnv env, String name) {
    if(name != null && name.startsWith("java.")) {
      env.throwException("java.lang.SecurityException", "Prohibited package name: " + name);
    }
  }

  protected static void checkForIllegalName(MJIEnv env, String name) {
    if((name == null) || (name.length() == 0)) {
      return;
    }

    if((name.indexOf('/') != -1) || (name.charAt(0) == '[')) {
      env.throwException("java.lang.NoClassDefFoundError", "IllegalName: " + name);
    }
  }

  protected static void checkData(MJIEnv env, byte[] buffer, int offset, int length) {
    if(offset<0 || length<0 || offset+length > buffer.length) {
      env.throwException("java.lang.IndexOutOfBoundsException");
    }
  }
}
