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
package gov.nasa.jpf.jvm;

import java.util.ArrayList;
import java.util.Set;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


/**
 * MJI NativePeer class for java.lang.Class library abstraction
 */
public class JPF_java_lang_Class {
  
  public static void init (Config conf){
    // we create Method and Constructor objects, so we better make sure these
    // classes are initialized (they already might be)
    JPF_java_lang_reflect_Method.init(conf);
    JPF_java_lang_reflect_Constructor.init(conf);    
  }
  
  public static boolean isArray____Z (MJIEnv env, int robj) {
    ClassInfo ci = env.getReferredClassInfo( robj);
    return ci.isArray();
  }

  public static int getComponentType____Ljava_lang_Class_2 (MJIEnv env, int robj) {
    if (isArray____Z(env, robj)) {
      ThreadInfo ti = env.getThreadInfo();
      Instruction insn = ti.getPC();
      ClassInfo ci = env.getReferredClassInfo( robj).getComponentClassInfo();

      if (insn.requiresClinitCalls(ti, ci)) {
        env.repeatInvocation();
        return MJIEnv.NULL;
      }

      return ci.getClassObjectRef();
    }

    return MJIEnv.NULL;
  }

  public static boolean isInstance__Ljava_lang_Object_2__Z (MJIEnv env, int robj,
                                                         int r1) {
    ElementInfo sei = env.getClassElementInfo(robj);
    ClassInfo   ci = sei.getClassInfo();
    ClassInfo   ciOther = env.getClassInfo(r1);
    return (ciOther.isInstanceOf(ci.getName()));
  }

  public static boolean isInterface____Z (MJIEnv env, int robj){
    ClassInfo ci = env.getReferredClassInfo( robj);
    return ci.isInterface();
  }
  
  public static boolean isAssignableFrom__Ljava_lang_Class_2__Z (MJIEnv env, int rcls,
                                                              int r1) {
    ElementInfo sei1 = env.getClassElementInfo(rcls);
    ClassInfo   ci1 = sei1.getClassInfo();

    ElementInfo sei2 = env.getClassElementInfo(r1);
    ClassInfo   ci2 = sei2.getClassInfo();

    return ci2.isInstanceOf( ci1.getName());
  }
  
  public static int getAnnotations_____3Ljava_lang_annotation_Annotation_2 (MJIEnv env, int robj){    
    ClassInfo ci = env.getReferredClassInfo( robj);
    AnnotationInfo[] ai = ci.getAnnotations();

    try {
      return env.newAnnotationProxies(ai);
    } catch (ClinitRequired x){
      env.handleClinitRequest(x.getRequiredClassInfo());
      return MJIEnv.NULL;
    }
  }
  
  public static int getAnnotation__Ljava_lang_Class_2__Ljava_lang_annotation_Annotation_2 (MJIEnv env, int robj,
                                                                                int annoClsRef){
    ClassInfo ci = env.getReferredClassInfo( robj);
    ClassInfo aci = env.getReferredClassInfo(annoClsRef);
    
    AnnotationInfo ai = ci.getAnnotation(aci.getName());
    if (ai != null){
      ClassInfo aciProxy = ClassInfo.getAnnotationProxy(aci);
      
      try {
        return env.newAnnotationProxy(aciProxy, ai);
      } catch (ClinitRequired x){
        env.handleClinitRequest(x.getRequiredClassInfo());
        return MJIEnv.NULL;
      }
    } else {
      return MJIEnv.NULL;
    }
  }
  
  public static int getPrimitiveClass__Ljava_lang_String_2__Ljava_lang_Class_2 (MJIEnv env,
                                                            int rcls, int stringRef) {
    String clsName = env.getStringObject(stringRef);

    // we don't really have to check for a valid class name here, since
    // this is a package default method that just gets called from
    // the clinit of box classes
    // note this does NOT return the box class (e.g. java.lang.Integer), which
    // is a normal, functional class, but a primitive class (e.g. 'int') that
    // is rather a strange beast (not even Object derived)
    StaticArea        sa = env.getStaticArea();
    StaticElementInfo ei = sa.get(clsName);
    int               cref = ei.getClassObjectRef();
    env.setBooleanField(cref, "isPrimitive", true);
    return cref;
  }

  public static boolean desiredAssertionStatus____Z (MJIEnv env, int robj) {
    ClassInfo ci = env.getReferredClassInfo(robj);
    return ci.areAssertionsEnabled();
  }

  public static int getClassObject (MJIEnv env, ClassInfo ci){
    ThreadInfo ti = env.getThreadInfo();
    Instruction insn = ti.getPC();

    if (insn.requiresClinitCalls(ti, ci)) {
      env.repeatInvocation();
      return MJIEnv.NULL;
    }

    StaticElementInfo ei = ci.getStaticElementInfo();
    int ref = ei.getClassObjectRef();

    return ref;
  }
  
  public static int forName__Ljava_lang_String_2__Ljava_lang_Class_2 (MJIEnv env,
                                                                       int rcls,
                                                                       int clsNameRef) {
    String            clsName = env.getStringObject(clsNameRef);
    
    ClassInfo ci = ClassInfo.tryGetResolvedClassInfo(clsName);
    if (ci == null){
      env.throwException("java.lang.ClassNotFoundException", clsName);
      return MJIEnv.NULL;
    }
    
    return getClassObject(env, ci);
  }

  /**
   * this is an example of a native method issuing direct calls - otherwise known
   * as a round trip.
   * We don't have to deal with class init here anymore, since this is called
   * via the class object of the class to instantiate
   */
  public static int newInstance____Ljava_lang_Object_2 (MJIEnv env, int robj) {
    ThreadInfo ti = env.getThreadInfo();
    DirectCallStackFrame frame = ti.getReturnedDirectCall();

    if (frame != null){
      return frame.pop();

    } else {
      ClassInfo ci = env.getReferredClassInfo(robj);   // what are we

      if(ci.isAbstract()){ // not allowed to instantiate
        env.throwException("java.lang.InstantiationException");
        return MJIEnv.NULL;
      }

      int objRef = env.newObject(ci);  // create the thing

      MethodInfo mi = ci.getMethod("<init>()V", true);
      if (mi != null) { // direct call required for initialization

        // <2do> - still need to handle protected
        if (mi.isPrivate()){
          env.throwException("java.lang.IllegalAccessException", "cannot access non-public member of class " + ci.getName());
          return MJIEnv.NULL;
        }

        MethodInfo stub = mi.createDirectCallStub("[init]");
        frame = new DirectCallStackFrame(stub, 2,0);
        frame.push( objRef, true);
        // Hmm, we borrow the DirectCallStackFrame to cache the object ref
        // (don't try that with a normal StackFrame)
        frame.dup();
        ti.pushFrame(frame);

        return MJIEnv.NULL;

      } else {
        return objRef; // no initialization required
      }
    }
  }
  
  public static int getSuperclass____Ljava_lang_Class_2 (MJIEnv env, int robj) {
    ClassInfo ci = env.getReferredClassInfo( robj);
    ClassInfo sci = ci.getSuperClass();
    if (sci != null) {
      return sci.getClassObjectRef();
    } else {
      return MJIEnv.NULL;
    }
  }

  public static int getClassLoader____Ljava_lang_ClassLoader_2 (MJIEnv env, int objref){
    // <2do> - that's a shortcut hack for now, since we don't support user defined
    // ClassLoaders yet
    int clRef = env.getStaticReferenceField("java.lang.ClassLoader", "systemClassLoader");
    return clRef;
  }

  static int getMethod (MJIEnv env, int clsRef, String mname, int argTypesRef,
                        boolean isRecursiveLookup, boolean publicOnly) {

    ClassInfo ci = env.getReferredClassInfo( clsRef);
    
    StringBuilder sb = new StringBuilder(mname);
    sb.append('(');
    int nParams = argTypesRef != MJIEnv.NULL ? env.getArrayLength(argTypesRef) : 0;
    for (int i=0; i<nParams; i++) {
      int cRef = env.getReferenceArrayElement(argTypesRef, i);
      ClassInfo cit = env.getReferredClassInfo( cRef);
      String tname = cit.getName();
      String tcode = tname;
      tcode = Types.getTypeSignature(tcode, false);
      sb.append(tcode);
    }
    sb.append(')');
    String fullMthName = sb.toString();

    MethodInfo mi = ci.getReflectionMethod(fullMthName, isRecursiveLookup);
    if (mi == null || (publicOnly && !mi.isPublic())) {
      env.throwException("java.lang.NoSuchMethodException", ci.getName() + '.' + fullMthName);
      return MJIEnv.NULL;
      
    } else {
      return createMethodObject(env,mi);      
    }
  }

  static int createMethodObject (MJIEnv env, MethodInfo mi) {
    // NOTE - we rely on Constructor and Method peers being initialized
    if (mi.isCtor()){
      return JPF_java_lang_reflect_Constructor.createConstructorObject(env,mi);
    } else {
      return JPF_java_lang_reflect_Method.createMethodObject(env,mi);      
    }
  }
  
  public static int getDeclaredMethod__Ljava_lang_String_2_3Ljava_lang_Class_2__Ljava_lang_reflect_Method_2 (MJIEnv env, int clsRef,
                                                                                                     int nameRef, int argTypesRef) {
    String mname = env.getStringObject(nameRef);
    return getMethod(env, clsRef, mname, argTypesRef, false, false);
  }

  
  public static int getDeclaredConstructor___3Ljava_lang_Class_2__Ljava_lang_reflect_Constructor_2 (MJIEnv env,
                                                                                               int clsRef,
                                                                                               int argTypesRef){
    int ctorRef =  getMethod(env,clsRef,"<init>",argTypesRef,false, false);
    return ctorRef;
  }
  
  public static int getMethod__Ljava_lang_String_2_3Ljava_lang_Class_2__Ljava_lang_reflect_Method_2 (MJIEnv env, int clsRef,
                                                                                                     int nameRef, int argTypesRef) {
    String mname = env.getStringObject(nameRef);
    return getMethod( env, clsRef, mname, argTypesRef, true, true);
  }

  private static void addDeclaredMethodsRec (HashMap<String,MethodInfo>methods, ClassInfo ci){
    ClassInfo sci = ci.getSuperClass();
    if (sci != null){
      addDeclaredMethodsRec(methods,sci);
    }

    for (String ifcName : ci.getInterfaces()){
      ClassInfo ici = ClassInfo.getResolvedClassInfo(ifcName); // has to be already defined, so no exception
      addDeclaredMethodsRec(methods,ici);
    }

    for (MethodInfo mi : ci.getDeclaredMethodInfos()) {
      // filter out non-public, <clinit> and <init>
      if (mi.isPublic() && (mi.getName().charAt(0) != '<')) {
        String mname = mi.getUniqueName();

        if (!(ci.isInterface() && methods.containsKey(mname))){
          methods.put(mname, mi);
        }
      }
    }
  }

  public static int getMethods_____3Ljava_lang_reflect_Method_2 (MJIEnv env, int objref) {
    ClassInfo ci = env.getReferredClassInfo(objref);

    // collect all the public, non-ctor instance methods
    if (!ci.isPrimitive()) {
      HashMap<String,MethodInfo> methods = new HashMap<String,MethodInfo>();
      addDeclaredMethodsRec(methods,ci);
      
      int n = methods.size();
      int aref = env.newObjectArray("Ljava/lang/reflect/Method;", n);
      int i=0;

      for (MethodInfo mi : methods.values()){
        int mref = createMethodObject(env, mi);
        env.setReferenceArrayElement(aref,i++,mref);
      }

      return aref;

    } else {
      return env.newObjectArray("Ljava/lang/reflect/Method;", 0);
    }
  }
  
  public static int getDeclaredMethods_____3Ljava_lang_reflect_Method_2 (MJIEnv env, int objref) {
    ClassInfo ci = env.getReferredClassInfo(objref);
    MethodInfo[] methodInfos = ci.getDeclaredMethodInfos();
    
    // we have to filter out the ctors and the static init
    int nMth = methodInfos.length;
    for (int i=0; i<methodInfos.length; i++){
      if (methodInfos[i].getName().charAt(0) == '<'){
        methodInfos[i] = null;
        nMth--;
      }
    }
    
    int aref = env.newObjectArray("Ljava/lang/reflect/Method;", nMth);
    
    for (int i=0, j=0; i<methodInfos.length; i++) {
      if (methodInfos[i] != null){
        int mref = createMethodObject(env, methodInfos[i]);
        env.setReferenceArrayElement(aref,j++,mref);
      }
    }
    
    return aref;
  }
  
  static int getConstructors (MJIEnv env, int objref, boolean publicOnly){
    ClassInfo ci = env.getReferredClassInfo(objref);
    ArrayList<MethodInfo> ctors = new ArrayList<MethodInfo>();
    
    // we have to filter out the ctors and the static init
    for (MethodInfo mi : ci.getDeclaredMethodInfos()){
      if (mi.getName().equals("<init>")){
        if (!publicOnly || mi.isPublic()) {
          ctors.add(mi);
        }
      }
    }
    
    int nCtors = ctors.size();
    int aref = env.newObjectArray("Ljava/lang/reflect/Constructor;", nCtors);
    
    for (int i=0; i<nCtors; i++){
      env.setReferenceArrayElement(aref, i, createMethodObject(env, ctors.get(i)));
    }
    
    return aref;
  }
  
  public static int getConstructors_____3Ljava_lang_reflect_Constructor_2 (MJIEnv env, int objref){
    return getConstructors(env, objref, true);
  }  
  
  public static int getDeclaredConstructors_____3Ljava_lang_reflect_Constructor_2 (MJIEnv env, int objref){
    return getConstructors(env, objref, false);
  }
  
  public static int getConstructor___3Ljava_lang_Class_2__Ljava_lang_reflect_Constructor_2 (MJIEnv env, int clsRef,
                                                                                       int argTypesRef){
    // <2do> should only return a public ctor 
    return getMethod(env,clsRef, "<init>",argTypesRef,false,true);
  }
  
  public static int getDeclaredFields_____3Ljava_lang_reflect_Field_2 (MJIEnv env, int objRef) {
    
    ThreadInfo ti = env.getThreadInfo();
    Instruction insn = ti.getPC();
    ClassInfo fci = ClassInfo.getResolvedClassInfo("java.lang.reflect.Field");
    
    if (insn.requiresClinitCalls(ti, fci)) {
      env.repeatInvocation();
      return MJIEnv.NULL;
    }

    
    ClassInfo ci = env.getReferredClassInfo(objRef);
    int nInstance = ci.getNumberOfDeclaredInstanceFields();
    int nStatic = ci.getNumberOfStaticFields();
    int aref = env.newObjectArray("Ljava/lang/reflect/Field;", nInstance + nStatic);
    int i, j=0;
    
    for (i=0; i<nStatic; i++) {
      FieldInfo fi = ci.getStaticField(i);
      int regIdx = JPF_java_lang_reflect_Field.registerFieldInfo(fi);
      int eidx = env.newObject(fci);
      ElementInfo ei = env.getElementInfo(eidx);
      
      ei.setIntField("regIdx", regIdx);
      env.setReferenceArrayElement(aref,j++,eidx);
    }    
    
    for (i=0; i<nInstance; i++) {
      FieldInfo fi = ci.getDeclaredInstanceField(i);
      
      int regIdx = JPF_java_lang_reflect_Field.registerFieldInfo(fi);
      int eidx = env.newObject(fci);
      ElementInfo ei = env.getElementInfo(eidx);
      
      ei.setIntField("regIdx", regIdx);
      env.setReferenceArrayElement(aref,j++,eidx);
    }
    
    return aref;
  }
  
  static int getField (MJIEnv env, int clsRef, int nameRef, boolean isRecursiveLookup) {
    ClassInfo ci = env.getReferredClassInfo( clsRef);
    String fname = env.getStringObject(nameRef);
    FieldInfo fi = null;
    
    if (isRecursiveLookup) {
      fi = ci.getInstanceField(fname);
      if (fi == null) {
        fi = ci.getStaticField(fname);
      }      
    } else {
        fi = ci.getDeclaredInstanceField(fname);
        if (fi == null) {
          fi = ci.getDeclaredStaticField(fname);
        }
    }
    
    if (fi == null) {      
      env.throwException("java.lang.NoSuchFieldException", fname);
      return MJIEnv.NULL;
      
    } else {
      ThreadInfo ti = env.getThreadInfo();
      Instruction insn = ti.getPC();
      ClassInfo fci = ClassInfo.getResolvedClassInfo("java.lang.reflect.Field");
      
      if (insn.requiresClinitCalls(ti, fci)) {
        env.repeatInvocation();
        return MJIEnv.NULL;
      }
      
      int regIdx = JPF_java_lang_reflect_Field.registerFieldInfo(fi);
      int eidx = env.newObject(fci);
      ElementInfo ei = env.getElementInfo(eidx);
      
      ei.setIntField("regIdx", regIdx);
      return eidx;
    }
  }
  
  public static int getDeclaredField__Ljava_lang_String_2__Ljava_lang_reflect_Field_2 (MJIEnv env, int clsRef, int nameRef) {
    return getField(env,clsRef,nameRef, false);
  }  
 
  public static int getField__Ljava_lang_String_2__Ljava_lang_reflect_Field_2 (MJIEnv env, int clsRef, int nameRef) {
    return getField(env,clsRef,nameRef, true);    
  }

  public static int getModifiers____I (MJIEnv env, int clsRef){
    ClassInfo ci = env.getReferredClassInfo(clsRef);
    return ci.getModifiers();
  }
  
  public static int getEnumConstants (MJIEnv env, int clsRef){
    ClassInfo ci = env.getReferredClassInfo(clsRef);
    if (ci.getSuperClass().getName().equals("java.lang.Enum")) {
      ArrayList<FieldInfo> list = new ArrayList<FieldInfo>();
      String cName = ci.getName();
      
      for (FieldInfo fi : ci.getDeclaredStaticFields()) {
        if (fi.isFinal() && cName.equals(fi.getType())){
          list.add(fi);
        }
      }
      
      int aRef = env.newObjectArray(cName, list.size());      
      StaticElementInfo sei = ci.getStaticElementInfo();
      int i=0;
      for (FieldInfo fi : list){
        env.setReferenceArrayElement( aRef, i++, sei.getReferenceField(fi));
      }
      return aRef;
    }
    
    return MJIEnv.NULL;
  }
    
  static public int getInterfaces_____3Ljava_lang_Class_2 (MJIEnv env, int clsRef){
    ClassInfo ci = env.getReferredClassInfo(clsRef);
    int aref = MJIEnv.NULL;
    ThreadInfo ti = env.getThreadInfo();
    
    // contrary to the API doc, this only returns the interfaces directly
    // implemented by this class, not it's bases
    // <2do> this is not exactly correct, since the interfaces should be ordered
    Set<String> ifcNames = ci.getInterfaces();
    aref = env.newObjectArray("Ljava/lang/Class;", ifcNames.size());
    
    int i=0;
    for (String ifc : ifcNames){
      ClassInfo ici = ClassInfo.getResolvedClassInfo(ifc);
      if (!ici.isRegistered()) {
        ici.registerClass(ti);
      }
      
      env.setReferenceArrayElement(aref, i++, ici.getClassObjectRef());
    }
    
    return aref;
  }


  /**
   * <2do> needs to load from the classfile location, NOT the MJIEnv (native) class
   *
   * @author Sebastian Gfeller (sebastian.gfeller@gmail.com)
   * @author Tihomir Gvero (tihomir.gvero@gmail.com)
   */
  public static int getByteArrayFromResourceStream(MJIEnv env, int clsRef, int nameRef) {
    String name = env.getStringObject(nameRef);

    // <2do> this is not loading from the classfile location! fix it
    InputStream is = env.getClass().getResourceAsStream(name);
    if (is == null){
      return MJIEnv.NULL;
    }
    // We assume that the entire input stream can be read at the moment,
    // although this could break.
    byte[] content = null;
    try {
      content = new byte[is.available()];
      is.read(content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // Now if everything worked, the content should be in the byte buffer.
    // We put this buffer into the JPF JVM.
    return env.newByteArray(content);
  }

  public static int getEnclosingClass(MJIEnv env, int robj) {
    ClassInfo enclosingClassInfo = env.getReferredClassInfo( robj).getEnclosingClassInfo();
    if (enclosingClassInfo == null)
      return MJIEnv.NULL;
    if (!enclosingClassInfo.isRegistered())
      enclosingClassInfo.registerClass(env.getThreadInfo());
    return enclosingClassInfo.getClassObjectRef();
  }

}
