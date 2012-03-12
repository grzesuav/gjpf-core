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
import gov.nasa.jpf.util.MethodInfoRegistry;
import gov.nasa.jpf.util.RunListener;
import gov.nasa.jpf.util.RunRegistry;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class JPF_java_lang_reflect_Method {

  static MethodInfoRegistry registry;

  // class init - this is called automatically from the NativePeer ctor
  public static void init (Config conf) {
    // this is an example of how to handle cross-initialization between
    // native peers - this might also get explicitly called by the java.lang.Class
    // peer, since it creates Method objects. Here we have to make sure
    // we only reset between JPF runs

    if (registry == null){
      registry = new MethodInfoRegistry();
      
      RunRegistry.getDefaultRegistry().addListener( new RunListener() {
        public void reset (RunRegistry reg){
          registry = null;
        }
      });
    }
  }

  static int createMethodObject (MJIEnv env, ClassInfo ciMth, MethodInfo mi){
    // note - it is the callers responsibility to ensure Method is properly initialized    
    int regIdx = registry.registerMethodInfo(mi);
    int eidx = env.newObject( ciMth);
    ElementInfo ei = env.getElementInfo(eidx);
    
    ei.setIntField("regIdx", regIdx);
    ei.setBooleanField("isAccessible", mi.isPublic());
    
    return eidx;
  }
  
  public static MethodInfo getMethodInfo (MJIEnv env, int objRef){
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
  
  static int getParameterTypes( MJIEnv env, MethodInfo mi) {
    try {
      ThreadInfo ti = env.getThreadInfo();
      String[] argTypeNames = mi.getArgumentTypeNames();
      int[] ar = new int[argTypeNames.length];

      for (int i = 0; i < argTypeNames.length; i++) {
        ClassInfo ci = ClassInfo.getResolvedClassInfo(argTypeNames[i]);
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
    return getParameterTypes(env, getMethodInfo(env, objRef));
  }
  
  static int getExceptionTypes(MJIEnv env, MethodInfo mi) {
    try {
      ThreadInfo ti = env.getThreadInfo();
      String[] exceptionNames = mi.getThrownExceptionClassNames();
       
      if (exceptionNames == null) {
        exceptionNames = new String[0];
      }
       
      int[] ar = new int[exceptionNames.length];
       
      for (int i = 0; i < exceptionNames.length; i++) {
        ClassInfo ci = ClassInfo.getResolvedClassInfo(exceptionNames[i]);
        if (!ci.isRegistered()) {
          ci.registerClass(ti);
        }
         
        ar[i] = ci.getClassObjectRef();
      }
       
      int aRef = env.newObjectArray("Ljava/lang/Class;", exceptionNames.length);
      for (int i = 0; i < exceptionNames.length; i++) {
        env.setReferenceArrayElement(aRef, i, ar[i]);
      }
       
      return aRef;
    } catch (NoClassInfoException cx) {
      env.throwException("java.lang.NoClassDefFoundError", cx.getMessage());
      return MJIEnv.NULL;
    }
  }
  
  public static int getExceptionTypes_____3Ljava_lang_Class_2 (MJIEnv env, int objRef) {
    return getExceptionTypes(env, getMethodInfo(env, objRef));
  }
  
  public static int getReturnType____Ljava_lang_Class_2 (MJIEnv env, int objRef){
    MethodInfo mi = getMethodInfo(env, objRef);
    ThreadInfo ti = env.getThreadInfo();

    try {
      ClassInfo ci = ClassInfo.getResolvedClassInfo(mi.getReturnTypeName());
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
    byte rt = mi.getReturnTypeCode();
    int ret = MJIEnv.NULL;
    ElementInfo rei;
    
    if (rt == Types.T_DOUBLE) {
      double v = frame.doublePop();
      ret = env.newObject(ClassInfo.getResolvedClassInfo("java.lang.Double"));
      rei = env.getElementInfo(ret);
      rei.setDoubleField("value", v);
    } else if (rt == Types.T_FLOAT) {
      int v = frame.pop();
      ret = env.newObject(ClassInfo.getResolvedClassInfo("java.lang.Float"));
      rei = env.getElementInfo(ret);
      rei.setIntField("value", v);
    } else if (rt == Types.T_LONG) {
      long v = frame.longPop();
      ret = env.newObject(ClassInfo.getResolvedClassInfo("java.lang.Long"));
      rei = env.getElementInfo(ret);
      rei.setLongField("value", v);
    } else if (rt == Types.T_BYTE) {
      int v = frame.pop(); 
      ret = env.newObject(ClassInfo.getResolvedClassInfo("java.lang.Byte"));
      rei = env.getElementInfo(ret);
      rei.setIntField("value", v);
    } else if (rt == Types.T_CHAR) {
      int v = frame.pop(); 
      ret = env.newObject(ClassInfo.getResolvedClassInfo("java.lang.Character"));
      rei = env.getElementInfo(ret);
      rei.setIntField("value", v);
    } else if (rt == Types.T_SHORT) {
      int v = frame.pop(); 
      ret = env.newObject(ClassInfo.getResolvedClassInfo("java.lang.Short"));
      rei = env.getElementInfo(ret);
      rei.setIntField("value", v);
    } else if (rt == Types.T_INT) {
      int v = frame.pop(); 
      ret = env.newObject(ClassInfo.getResolvedClassInfo("java.lang.Integer"));
      rei = env.getElementInfo(ret);
      rei.setIntField("value", v);
    } else if (rt == Types.T_BOOLEAN) {
      int v = frame.pop(); 
      ret = env.newObject(ClassInfo.getResolvedClassInfo("java.lang.Boolean"));
      rei = env.getElementInfo(ret);
      rei.setIntField("value", v);
    } else if (mi.isReferenceReturnType()){ 
      ret = frame.pop();
    }

    return ret;
  }

  static boolean pushUnboxedArguments (MJIEnv env, MethodInfo mi, StackFrame frame, int argsRef) {
    ElementInfo source;
    ClassInfo sourceClass;
    String destTypeNames[];
    int i, nArgs, passedCount, sourceRef;
    byte sourceType, destTypes[];

    destTypes     = mi.getArgumentTypes();
    destTypeNames = mi.getArgumentTypeNames();
    nArgs         = destTypeNames.length;
    
    // according to the API docs, passing null instead of an empty array is allowed for no args
    passedCount   = (argsRef != MJIEnv.NULL) ? env.getArrayLength(argsRef) : 0;
    
    if (nArgs != passedCount) {
      env.throwException(IllegalArgumentException.class.getName(), "Wrong number of arguments passed.  Actual = " + passedCount + ".  Expected = " + nArgs);
      return false;
    }
    
    for (i = 0; i < nArgs; i++) {
      
      sourceRef = env.getReferenceArrayElement(argsRef, i);

      // we have to handle null references explicitly
      if (sourceRef == MJIEnv.NULL) {
        if ((destTypes[i] != Types.T_REFERENCE) && (destTypes[i] != Types.T_ARRAY)) {
          env.throwException(IllegalArgumentException.class.getName(), "Wrong argument type at index " + i + ".  Actual = (null).  Expected = " + destTypeNames[i]);
          return false;
        } 
         
        frame.pushRef(MJIEnv.NULL);
        continue;
      }

      source      = env.getElementInfo(sourceRef);
      sourceClass = source.getClassInfo();   
      sourceType = getSourceType( sourceClass, destTypes[i], destTypeNames[i]);
             
      if ((sourceType == Types.T_NONE) || !pushArg(frame, source, sourceType, destTypes[i])){
        env.throwException(IllegalArgumentException.class.getName(), "Wrong argument type at index " + i + ".  Source Class = " + sourceClass.getName() + ".  Dest Class = " + destTypeNames[i]);
        return false;        
      }
    }
    
    return true;
  }

  // this returns the primitive type in case we have to unbox, and otherwise checks reference type compatibility
  private static byte getSourceType (ClassInfo ciArgVal, byte destType, String destTypeName){
    switch (destType){
    // the primitives
    case Types.T_BOOLEAN:
    case Types.T_BYTE:
    case Types.T_CHAR:
    case Types.T_SHORT:
    case Types.T_INT:
    case Types.T_LONG:
    case Types.T_FLOAT:
    case Types.T_DOUBLE:
      return Types.getUnboxedType(ciArgVal.getName());
      
    case Types.T_ARRAY:
    case Types.T_REFERENCE: // check if the source type is assignment compatible with the destType
      if (ciArgVal.isInstanceOf(destTypeName)){
        return destType;
      }
    }
    
    return Types.T_NONE;
  }
  
  // do the proper type conversion - Java is pretty forgiving here and does
  // not throw exceptions upon value truncation
  private static boolean pushArg( StackFrame frame, ElementInfo eiArg, byte srcType, byte destType){    
    switch (srcType) {
    case Types.T_DOUBLE:
    {
      double v = eiArg.getDoubleField("value");
      if (destType == Types.T_DOUBLE){      
        frame.longPush(Double.doubleToLongBits(v));
        return true;
      }
      return false;
    }
    case Types.T_FLOAT: // covers float, double
    {
      float v = eiArg.getFloatField("value");
      switch (destType){
      case Types.T_FLOAT:
        frame.push(Float.floatToIntBits(v));
        return true;
      case Types.T_DOUBLE:
        frame.longPush(Double.doubleToLongBits(v));
        return true;
      }
      return false;
    }
    case Types.T_LONG:
    {
      long v = eiArg.getLongField("value");
      switch (destType){
      case Types.T_LONG:
        frame.longPush(v);
        return true;
      case Types.T_FLOAT:
        frame.push(Float.floatToIntBits((float)v));
        return true;
      case Types.T_DOUBLE:
        frame.longPush( Double.doubleToLongBits((double)v));
        return true;
      }
      return false;
    }
    case Types.T_INT:
    { 
      int v = eiArg.getIntField("value");
      switch (destType){
      case Types.T_INT:
        frame.push(v);
        return true;
      case Types.T_LONG:
        frame.longPush(v);
        return true;        
      case Types.T_FLOAT:
        frame.push( Float.floatToIntBits((float)v));
        return true;
      case Types.T_DOUBLE:
        frame.longPush( Double.doubleToLongBits((double)v));
        return true;
      }
      return false;
    }
    case Types.T_SHORT:
    { 
      int v = eiArg.getShortField("value");
      switch (destType){
      case Types.T_SHORT:
      case Types.T_INT:
        frame.push(v);
        return true;
      case Types.T_LONG:
        frame.longPush(v);
        return true;        
      case Types.T_FLOAT:
        frame.push( Float.floatToIntBits((float)v));
        return true;
      case Types.T_DOUBLE:
        frame.longPush( Double.doubleToLongBits((double)v));
        return true;
      }
      return false;
    }
    case Types.T_BYTE:
    { 
      byte v = eiArg.getByteField("value");
      switch (destType){
      case Types.T_BYTE:
      case Types.T_SHORT:
      case Types.T_INT:
        frame.push(v);
        return true;
      case Types.T_LONG:
        frame.longPush(v);
        return true;
      case Types.T_FLOAT:
        frame.push( Float.floatToIntBits((float)v));
        return true;
      case Types.T_DOUBLE:
        frame.longPush( Double.doubleToLongBits((double)v));
        return true;
      }
      return false;
    }
    case Types.T_CHAR:
    {
      char v = eiArg.getCharField("value");
      switch (destType){
      case Types.T_CHAR:
      case Types.T_INT:
        frame.push(v);
        return true;
      case Types.T_LONG:
        frame.longPush(v);
        return true;        
      case Types.T_FLOAT:
        frame.push( Float.floatToIntBits((float)v));
        return true;
      case Types.T_DOUBLE:
        frame.longPush( Double.doubleToLongBits((double)v));
        return true;
      }
      return false;
    }
    case Types.T_BOOLEAN:
    {
      boolean v = eiArg.getBooleanField("value");
      if (destType == Types.T_BOOLEAN){
        frame.push(v ? 1 : 0);
        return true;
      }
      return false;
    }
    case Types.T_ARRAY:
    {
      int ref =  eiArg.getObjectRef();
      if (destType == Types.T_ARRAY){
        frame.pushRef(ref);
        return true;
      }
      return false;
    }
    case Types.T_REFERENCE:
    {
      int ref =  eiArg.getObjectRef();
      if (destType == Types.T_REFERENCE){
        frame.pushRef(ref);
        return true;
      }
      return false;
    }
    case Types.T_VOID:
    default:
      return false;
    }
  }

  
  public static int invoke__Ljava_lang_Object_2_3Ljava_lang_Object_2__Ljava_lang_Object_2 (MJIEnv env, int mthRef,
                                                                                           int objRef, int argsRef) {
    ThreadInfo ti = env.getThreadInfo();
    MethodInfo mi = getMethodInfo(env,mthRef);
    ClassInfo calleeClass = mi.getClassInfo();
    ElementInfo mth = ti.getElementInfo(mthRef);
    boolean accessible = (Boolean) mth.getFieldValueObject("isAccessible");
    
    if (!accessible) {
      StackFrame frame = ti.getTopFrame().getPrevious();
      ClassInfo callerClass = frame.getClassInfo();
      
      if (callerClass != calleeClass) {
        env.throwException(IllegalAccessException.class.getName(), "Class " + callerClass.getName() + " can not access a member of class " + calleeClass.getName() + " with modifiers \"" + Modifier.toString(mi.getModifiers()));
        return MJIEnv.NULL;
      }
    }
    
    StackFrame frame = ti.getReturnedDirectCall();

    if (frame != null){
      return createBoxedReturnValueObject( env, mi, frame);

    } else {
      MethodInfo stub = mi.createReflectionCallStub();
      frame = new DirectCallStackFrame(stub);

      if (!mi.isStatic()) {
        if (objRef == MJIEnv.NULL) {
          env.throwException(NullPointerException.class.getName(), "Expected an instance of " + calleeClass);
          return MJIEnv.NULL;
        }

        ElementInfo obj = ti.getElementInfo(objRef);
        ClassInfo objClass = obj.getClassInfo();
        
        if (!objClass.isInstanceOf(calleeClass)) {
          env.throwException(IllegalArgumentException.class.getName(), "Object is not an instance of declaring class.  Actual = " + objClass + ".  Expected = " + calleeClass);
          return MJIEnv.NULL;
        }
        
        frame.push(objRef, true);
      }

      if (!pushUnboxedArguments(env, mi, frame, argsRef)) {
        return MJIEnv.NULL;  
      }
       
      ti.pushFrame(frame);
      
      return MJIEnv.NULL;
    }
  }
  
  // this one has to collect annotations upwards in the inheritance chain
  static int getAnnotations (MJIEnv env, MethodInfo mi){
    String mname = mi.getName();
    String msig = mi.genericSignature;
    ArrayList<AnnotationInfo> aiList = new ArrayList<AnnotationInfo>();
    
    // our own annotations
    ClassInfo ci = mi.getClassInfo();
    for (AnnotationInfo ai : mi.getAnnotations()) {
      aiList.add(ai);
    }
    
    // our superclass annotations
    for (ci = ci.getSuperClass(); ci != null; ci = ci.getSuperClass()){
      mi = ci.getMethod(mname, msig, false);
      if (mi != null){
        for (AnnotationInfo ai: mi.getAnnotations()){
          aiList.add(ai);
        }        
      }
    }

    try {
      return env.newAnnotationProxies(aiList.toArray(new AnnotationInfo[aiList.size()]));
    } catch (ClinitRequired x){
      env.handleClinitRequest(x.getRequiredClassInfo());
      return MJIEnv.NULL;
    }    
  }
  public static int getAnnotations_____3Ljava_lang_annotation_Annotation_2 (MJIEnv env, int mthRef){
    return getAnnotations( env, getMethodInfo(env,mthRef));
  }
  
  // the following ones consist of a package default implementation that is shared with
  // the constructor peer, and a public model method
  static int getAnnotation (MJIEnv env, MethodInfo mi, int annotationClsRef){
    ClassInfo aci = env.getReferredClassInfo(annotationClsRef);
    
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
  public static int getAnnotation__Ljava_lang_Class_2__Ljava_lang_annotation_Annotation_2 (MJIEnv env, int mthRef, int annotationClsRef) {
    return getAnnotation(env, getMethodInfo(env,mthRef), annotationClsRef);
  }
  
  static int getDeclaredAnnotations (MJIEnv env, MethodInfo mi){
    AnnotationInfo[] ai = mi.getAnnotations();

    try {
      return env.newAnnotationProxies(ai);
    } catch (ClinitRequired x){
      env.handleClinitRequest(x.getRequiredClassInfo());
      return MJIEnv.NULL;
    }    
  }
  public static int getDeclaredAnnotations_____3Ljava_lang_annotation_Annotation_2 (MJIEnv env, int mthRef){
    return getDeclaredAnnotations( env, getMethodInfo(env,mthRef));
  }
  
  static int getParameterAnnotations (MJIEnv env, MethodInfo mi){
    AnnotationInfo[][] pa = mi.getParameterAnnotations();
    // this should always return an array object, even if the method has no arguments
    
    try {
      int paRef = env.newObjectArray("[Ljava/lang/annotation/Annotation;", pa.length);
      
      for (int i=0; i<pa.length; i++){
        int eRef = env.newAnnotationProxies(pa[i]);
        env.setReferenceArrayElement(paRef, i, eRef);
      }

      return paRef;
      
    } catch (ClinitRequired x){ // be prepared that we might have to initialize respective annotation classes
      env.handleClinitRequest(x.getRequiredClassInfo());
      return MJIEnv.NULL;
    }    
  }
  public static int getParameterAnnotations_____3_3Ljava_lang_annotation_Annotation_2 (MJIEnv env, int mthRef){
    return getParameterAnnotations( env, getMethodInfo(env,mthRef));
  }
  
  
  public static int toString____Ljava_lang_String_2 (MJIEnv env, int objRef){
    StringBuilder sb = new StringBuilder();
    
    MethodInfo mi = getMethodInfo(env, objRef);

    sb.append(Modifier.toString(mi.getModifiers()));
    sb.append(' ');

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
