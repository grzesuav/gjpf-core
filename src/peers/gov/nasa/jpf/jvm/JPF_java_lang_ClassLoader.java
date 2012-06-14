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
import gov.nasa.jpf.jvm.bytecode.Instruction;

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

  public static int getClassObject (MJIEnv env, ClassInfo ci){
    ThreadInfo ti = env.getThreadInfo();
    Instruction insn = ti.getPC();

    if (insn.requiresClinitExecution(ti, ci)) {
      env.repeatInvocation();
      return MJIEnv.NULL;
    }

    return ci.getClassObjectRef();
  }

  public static int loadClass0__Ljava_lang_String_2__Ljava_lang_Class_2 (MJIEnv env, int objRef, int nameRef) {
    Heap heap = env.getHeap();
    String cname = env.getStringObject(nameRef);

    int gid = heap.get(objRef).getIntField("clRef");
    ClassLoaderInfo cl = env.getVM().getClassLoader(gid);

    ClassInfo ci = cl.tryGetResolvedClassInfo(cname);
    if (ci == null){
      env.throwException("java.lang.ClassNotFoundException", cname);
      return MJIEnv.NULL;
    }

    return getClassObject(env, ci);
  }

  public static int defineClass0__Ljava_lang_String_2_3BII__Ljava_lang_Class_2 
      (MJIEnv env, int objRef, int nameRef, int bufferRef, int offset, int length) {

    Heap heap = env.getHeap();
    String clsName = env.getStringObject(nameRef);
    byte[] buffer = env.getByteArrayObject(bufferRef);

    if(!isValidName(clsName)) {
      env.throwException("java.lang.NoClassDefFoundError", "IllegalName: " + clsName);
      return MJIEnv.NULL;
    }

    if(isProhibited(clsName)) {
      env.throwException("java.lang.SecurityException", "Prohibited package name: " + clsName);
      return MJIEnv.NULL;
    }

    if(offset<0 || length<0 || offset+length>buffer.length) {
      env.throwException("java.lang.IndexOutOfBoundsException");
      return MJIEnv.NULL;
    }

    int gid = heap.get(objRef).getIntField("clRef");
    ClassLoaderInfo cl = env.getVM().getClassLoader(gid);

    ClassPath.Match match = cl.getMatch(clsName);
    if(!match.getBytes().equals(buffer)) {
      env.throwException("java.lang.ClassFormatError");
      return MJIEnv.NULL;
    }

    ClassInfo ci = null; 
    try {
      ci = cl.getResolvedClassInfo(clsName, buffer, offset, length, match);
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

  private static boolean isProhibited(String name) {
    return (name != null) && name.startsWith("java.");
  }

  private static boolean isValidName(String name) {
    if ((name == null) || (name.length() == 0)) {
      return true;
    }

    if ((name.indexOf('/') != -1) && (name.charAt(0) == '[')) {
      return false;
    }

    return true;
  }
}
