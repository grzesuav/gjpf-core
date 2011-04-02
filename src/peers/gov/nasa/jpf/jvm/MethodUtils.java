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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *
 * @author proger
 */
public class MethodUtils {
  public static int getExceptionTypes(MJIEnv env, MethodInfo mi) {
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

  public static int getAnnotation(MJIEnv env, MethodInfo mi, int annotationClsRef) {
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

  public static int getAnnotations(MJIEnv env, MethodInfo mi){
    AnnotationInfo[] ai = mi.getAnnotations();

    try {
      return env.newAnnotationProxies(ai);
    } catch (ClinitRequired x){
      env.handleClinitRequest(x.getRequiredClassInfo());
      return MJIEnv.NULL;
    }
  }

  public static boolean isMethodInfoFlagSet(MethodInfo mi, String flagName) throws Exception {
    return (mi.getModifiers() & getModifierFlag(flagName)) != 0;
  }

  private static int getModifierFlag(String flagName) throws Exception {
    Field f = Modifier.class.getDeclaredField(flagName);
    f.setAccessible(true);

    return f.getInt(null);
  }

  private static void appendMethodModifiers(MethodInfo mi, StringBuilder sb) {
    if (mi.isPublic()) {
      sb.append("public ");
    } else if (mi.isProtected()) {
      sb.append("protected ");
    } else if (mi.isPrivate()) {
      sb.append("private ");
    }
    if (mi.isStatic()) {
      sb.append("static ");
    }
    if (mi.isSynchronized()) {
      sb.append("synchronized ");
    }
    if (mi.isNative()) {
      sb.append("native ");
    }
  }

  public static int toString(MJIEnv env, MethodInfo mi, String methodName){
    StringBuilder sb = new StringBuilder();
    
    appendMethodModifiers(mi, sb);

    sb.append(mi.getReturnTypeName());
    sb.append(' ');

    sb.append(mi.getClassName());
    sb.append('.');

    sb.append(methodName);

    sb.append('(');

    String[] at = mi.getArgumentTypeNames();
    for (int i=0; i<at.length; i++){
      if (i>0) sb.append(',');
      sb.append(at[i]);
    }

    sb.append(')');

    String[] exceptionNames = mi.getThrownExceptionClassNames();
    if (exceptionNames != null) {
      sb.append(" throws ");

      for (int i = 0; i < exceptionNames.length; i++) {
        sb.append(exceptionNames[i]);
        if (i != exceptionNames.length - 1)
          sb.append(',');
      }
    }

    int sref = env.newString(sb.toString());
    return sref;
  }
}
