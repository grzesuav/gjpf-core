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


import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.MethodInfoRegistry;
import gov.nasa.jpf.util.RunListener;
import gov.nasa.jpf.util.RunRegistry;


public class JPF_java_lang_reflect_Method {

  static MethodInfoRegistry registry;
    
  public static void init (Config conf) {
    // this is an example of how to handle cross-initialization between
    // native peers - this might also get explicitly called by the java.lang.Class
    // peer, since it creates Method objects. Here we have to make sure
    // we only re-initialize between JPF runs

    if (registry == null){
      registry = new MethodInfoRegistry();
      
      RunRegistry.getDefaultRegistry().addListener( new RunListener() {
        public void reset (RunRegistry reg){
          registry = null;
        }
      });
    }
  }

  static int createMethodObject (MJIEnv env, MethodInfo mi){
    int regIdx = registry.registerMethodInfo(mi);
    int eidx = env.newObject(ClassInfo.getClassInfo("java.lang.reflect.Method"));
    ElementInfo ei = env.getElementInfo(eidx);
    
    ei.setIntField("regIdx", regIdx);
    
    return eidx;
  }
  
  static MethodInfo getMethodInfo (MJIEnv env, int objRef){
    return registry.getMethodInfo(env,objRef, "regIdx");
  }
  
  public static int getName____Ljava_lang_String_2 (MJIEnv env, int objRef) {
    MethodInfo mi = getMethodInfo(env, objRef);
    
    int nameRef = env.getReferenceField( objRef, "name");
    if (nameRef == -1) {
      nameRef = env.newString(mi.getName());
      env.setReferenceField(objRef, "name", nameRef);
    }
   
    return nameRef;
  }

  public static int getModifiers____I (MJIEnv env, int objRef){
    MethodInfo mi = getMethodInfo(env, objRef);
    return mi.getModifiers();
  }
  
  static int getParameterTypes( MJIEnv env, int objRef, MethodInfo mi) {
    try {
      ThreadInfo ti = env.getThreadInfo();
      String[] argTypeNames = mi.getArgumentTypeNames();
      int[] ar = new int[argTypeNames.length];

      for (int i = 0; i < argTypeNames.length; i++) {
        ClassInfo ci = ClassInfo.getClassInfo(argTypeNames[i]);
        if (!ci.isRegistered()) {
          ci.registerClass(ti);
        }

        ar[i] = ci.getClassObjectRef();
      }

      int aRef = env.newObjectArray("Ljava/lang/Class;", argTypeNames.length);
      for (int i = 0; i < argTypeNames.length; i++) {
        env.setReferenceArrayElement(aRef, i, ar[i]);
      }

      return aRef;

    } catch (NoClassInfoException cx){
      env.throwException("java.lang.NoClassDefFoundError", cx.getMessage());
      return MJIEnv.NULL;
    }
  }
  
  public static int getParameterTypes_____3Ljava_lang_Class_2 (MJIEnv env, int objRef){
    return getParameterTypes(env, objRef, getMethodInfo(env, objRef));
  }
  
  public static int getReturnType____Ljava_lang_Class_2 (MJIEnv env, int objRef){
    MethodInfo mi = getMethodInfo(env, objRef);
    ThreadInfo ti = env.getThreadInfo();

    try {
      ClassInfo ci = ClassInfo.getClassInfo(mi.getReturnTypeName());
      if (!ci.isRegistered()) {
        ci.registerClass(ti);
      }

      return ci.getClassObjectRef();

    } catch (NoClassInfoException cx){
      env.throwException("java.lang.NoClassDefFoundError", cx.getMessage());
      return MJIEnv.NULL;
    }
  }
  
  public static int getDeclaringClass____Ljava_lang_Class_2 (MJIEnv env, int objRef){
    MethodInfo mi = getMethodInfo(env, objRef);    
    ClassInfo ci = mi.getClassInfo();
    // it's got to be registered, otherwise we wouldn't be able to acquire the Method object
    return ci.getClassObjectRef();
  }
    
  static int createBoxedReturnValueObject (MJIEnv env, MethodInfo mi, StackFrame frame) {
    byte rt = mi.getReturnType();
    int ret = MJIEnv.NULL;
    ElementInfo rei;
    
    if (rt == Types.T_DOUBLE) {
      double v = frame.doublePop();
      ret = env.newObject(ClassInfo.getClassInfo("java.lang.Double"));
      rei = env.getElementInfo(ret);
      rei.setDoubleField("value", v);
    } else if (rt == Types.T_LONG) {
      long v = frame.longPop();
      ret = env.newObject(ClassInfo.getClassInfo("java.lang.Long"));
      rei = env.getElementInfo(ret);
      rei.setLongField("value", v);
    } else if (rt == Types.T_BYTE) {
      int v = frame.pop(); 
      ret = env.newObject(ClassInfo.getClassInfo("java.lang.Byte"));
      rei = env.getElementInfo(ret);
      rei.setIntField("value", v);
    } else if (rt == Types.T_CHAR) {
      int v = frame.pop(); 
      ret = env.newObject(ClassInfo.getClassInfo("java.lang.Character"));
      rei = env.getElementInfo(ret);
      rei.setIntField("value", v);
    } else if (rt == Types.T_SHORT) {
      int v = frame.pop(); 
      ret = env.newObject(ClassInfo.getClassInfo("java.lang.Short"));
      rei = env.getElementInfo(ret);
      rei.setIntField("value", v);
    } else if (rt == Types.T_INT) {
      int v = frame.pop(); 
      ret = env.newObject(ClassInfo.getClassInfo("java.lang.Integer"));
      rei = env.getElementInfo(ret);
      rei.setIntField("value", v);
    } else if (rt == Types.T_BOOLEAN) {
      int v = frame.pop(); 
      ret = env.newObject(ClassInfo.getClassInfo("java.lang.Boolean"));
      rei = env.getElementInfo(ret);
      rei.setIntField("value", v);
    } else if (mi.isReferenceReturnType()){ 
      ret = frame.pop();
    }

    return ret;
  }

  static void pushUnboxedArguments (MJIEnv env, MethodInfo mi, StackFrame frame, int argsRef) {
    byte[] at = mi.getArgumentTypes();
    int nArgs = at.length;
        
    for (int i=0; i<nArgs; i++) {
      int argRef = env.getReferenceArrayElement(argsRef, i);
      if (argRef != MJIEnv.NULL){
        ElementInfo aei = env.getElementInfo(argRef);
        ClassInfo aci = aei.getClassInfo();
        
        // of course, we should only unbox if the argument type is a builtin
        if (aci.isBoxClass() && (at[i] != Types.T_OBJECT)) { // unbox
          String cname = aci.getName();
          if (cname.equals("java.lang.Long")) {
            long l = aei.getLongField("value");
            frame.longPush(l);
          } else if (cname.equals("java.lang.Double")) {
            double d = aei.getDoubleField("value");
            frame.push(Types.hiDouble(d), false);
            frame.push(Types.loDouble(d), false);
          } else {
            int v = aei.getIntField("value");
            frame.push(v, false);
          }
        } else { // otherwise it's a reference
          frame.push(argRef, true);
        }
      } else {
        frame.push(MJIEnv.NULL,true);
      }
    }
  }
  
  public static int invoke__Ljava_lang_Object_2_3Ljava_lang_Object_2__Ljava_lang_Object_2 (MJIEnv env, int mthRef,
                                                                                           int objRef, int argsRef) {
    ThreadInfo ti = env.getThreadInfo();
    Instruction insn = ti.getPC();
        
    MethodInfo mi = getMethodInfo(env,mthRef);
    DirectCallStackFrame frame;
    
    if (!ti.isResumedInstruction(insn)) { // make a direct call
      MethodInfo stub = mi.createReflectionCallStub();
      frame = new DirectCallStackFrame(stub, insn);
        
      if (!mi.isStatic()) {
        frame.push(objRef, true);
      }
      
      pushUnboxedArguments(env, mi, frame, argsRef);
      
      ti.pushFrame(frame);
      env.repeatInvocation();
      
      return MJIEnv.NULL;
    } else { // direct call returned, unbox return type (if any)      
      return createBoxedReturnValueObject(env, mi, ti.getReturnedDirectCall());
    }
  }
  
  public static int getAnnotation__Ljava_lang_Class_2__Ljava_lang_annotation_Annotation_2 (MJIEnv env, int mthRef, int annotationClsRef) {
    MethodInfo mi = getMethodInfo(env,mthRef);
    ClassInfo aci = JPF_java_lang_Class.getReferredClassInfo(env,annotationClsRef);
    
    AnnotationInfo ai = mi.getAnnotation(aci.getName());
    if (ai != null){
      ClassInfo aciProxy = ClassInfo.getAnnotationProxy(aci);
      try {
        return env.newAnnotationProxy(aciProxy, ai);
      } catch (ClinitRequired x){
        env.handleClinitRequest(x.getRequiredClassInfo());
        return MJIEnv.NULL;
      }
    }
    
    return MJIEnv.NULL;
  }
  
  public static int getAnnotations_____3Ljava_lang_annotation_Annotation_2 (MJIEnv env, int mthRef){
    MethodInfo mi = getMethodInfo(env,mthRef);
    AnnotationInfo[] ai = mi.getAnnotations();

    try {
      return env.newAnnotationProxies(ai);
    } catch (ClinitRequired x){
      env.handleClinitRequest(x.getRequiredClassInfo());
      return MJIEnv.NULL;
    }
  }
  
  public static int toString____Ljava_lang_String_2 (MJIEnv env, int objRef){
    StringBuilder sb = new StringBuilder();
    
    MethodInfo mi = getMethodInfo(env, objRef);

    if (mi.isPublic()){
      sb.append("public ");
    } else if (mi.isProtected()){
      sb.append("protected ");
    } else if (mi.isPrivate()){
      sb.append("private ");
    }

    if (mi.isStatic()){
      sb.append("static ");
    }
    if (mi.isSynchronized()){
      sb.append("synchronized ");
    }
    if (mi.isNative()){
      sb.append("native ");
    }

    sb.append(mi.getReturnTypeName());
    sb.append(' ');

    sb.append(mi.getClassName());
    sb.append('.');

    sb.append(mi.getName());

    sb.append('(');
    
    String[] at = mi.getArgumentTypeNames();
    for (int i=0; i<at.length; i++){
      if (i>0) sb.append(',');
      sb.append(at[i]);
    }
    
    sb.append(')');
    
    int sref = env.newString(sb.toString());
    return sref;
  }
}
