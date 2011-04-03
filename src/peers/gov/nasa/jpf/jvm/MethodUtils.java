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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

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

  public static int toString(MJIEnv env, MethodInfo mi, String methodName, boolean appendReturnType){
    StringBuilder sb = new StringBuilder();
    
    appendMethodModifiers(mi, sb);

    if (appendReturnType) {
      sb.append(mi.getReturnTypeName());
      sb.append(' ');
    }

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

  public static int toGenericString(MJIEnv env, MethodInfo mi, String methodName, boolean appendReturnType) {
    StringBuilder sb = new StringBuilder();

    String signature = mi.getGenericSignature();
    // Not a generic method
    if (signature.isEmpty()) {
      return toString(env, mi, methodName, appendReturnType);
    }

    ArrayList<String> methodGenerics = getMethodGenerics(signature);
    ArrayList<String> parametrs = getMethodParametrs(signature);
    String returnType = getReturnType(signature);

    appendMethodModifiers(mi, sb);

    if (!methodGenerics.isEmpty()) {
      sb.append('<');
      for (int i = 0; i < methodGenerics.size(); i++) {
        sb.append(methodGenerics.get(i));
        if (i != methodGenerics.size() - 1) {
          sb.append(',');
        }
      }
      sb.append('>');
    }

    if (appendReturnType) {
      sb.append(returnType);
      sb.append(' ');
    }

    sb.append(mi.getClassName());
    sb.append('.');
    sb.append(mi.getName());

    sb.append('(');

    for (int i = 0; i < parametrs.size(); i++) {
      sb.append(parametrs.get(i));
      if (i != parametrs.size() - 1) {
        sb.append(',');
      }
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

    return env.newString(sb.toString());
  }

  /**
   * Method that parse method's generic types
   * Method example:
   *  public <MyNewType extends Pair<Long[], long[]>, MySuperNewType> Object foo()
   * Generic signature of this method:
   *  <MyNewType:Lgov/nasa/jpf/util/Pair<[Ljava/lang/Long;[J>;MySuperNewType:Ljava/lang/Object;>()Ljava/lang/Object;
   * Usual Method.toGenericString() omits " extends Y" and "super X" so do we.
   *
   * @param signature - method generic signature
   * @return - list of method's generic types
   */
  private static ArrayList<String> getMethodGenerics(String signature) {
    ArrayList<String> result = new ArrayList<String>();
    // If generic signature doesn't start with '<' no method has no own generic types
    if (signature.startsWith("<")) {
      // Methods generics ends with start of method parameters in generic signature
      String methodGenerics = signature.substring(1, signature.indexOf('(') - 1);

      int i = 0;
      int start = 0;
      int mgLen = methodGenerics.length();
      while (i < mgLen) {
        // ':' - splits method's generic type and it's class that it super/extends
        int columPos = methodGenerics.indexOf(':', i);
        if (columPos < 0) break;

        String genericType = methodGenerics.substring(start, columPos);
        result.add(genericType);

        i = columPos + 1;

        int genericLevel = 0;
        // Generic type can extends another generic type, so just avoid it
        while (methodGenerics.charAt(i) != ';' || genericLevel != 0) {
          if (methodGenerics.charAt(i) == '<') {
            genericLevel++;
          }
          else if (methodGenerics.charAt(i) == '>') {
            genericLevel--;
          }

          i++;
        }

        i++;
        start = i;
      }
    }

    return result;
  }

  /**
   * Return name of the return type
   */
  private static String getReturnType(String signature) {
    // Method's return type starts after method's parametrs in method's generic signature
    String returnSignature = signature.substring(signature.lastIndexOf(')') + 1);

    return getTypesList(returnSignature).get(0);

  }

  /**
   * Parse type list from generic signature.
   * Return type and parameters list has the same format in generic signature;
   * Methods example:
   *  public void foo(int i, long j, boolean b, Pair<Pair<long[], ArrayList<? extends Double>>, Long[]>[] p, ArrayList<? super Double> a, short s, byte bool, double d, float f);
   *  public Pair<? extends ArrayList<Integer>, Long[]> o4() {return null;}
   *  public Pair<long[], ? super ArrayList<Integer>> o5() {return null;}
   * Generic methods signatures
   *  foo -(IJZ[Lgov/nasa/jpf/util/Pair<Lgov/nasa/jpf/util/Pair<[JLjava/util/ArrayList<+Ljava/lang/Double;>;>;[Ljava/lang/Long;>;Ljava/util/ArrayList<-Ljava/lang/Double;>;SBDF)V
   *  o4 - Lgov/nasa/jpf/util/Pair<+Ljava/util/ArrayList<Ljava/lang/Integer;>;[Ljava/lang/Long;>;
   *  o5 - Lgov/nasa/jpf/util/Pair<[J-Ljava/util/ArrayList<Ljava/lang/Integer;>;>;
   * @param inputStr
   * @return
   */
  private static ArrayList<String> getTypesList(String inputStr) {
    // Special case. No parameters.
    if (inputStr.isEmpty()) {
      return new ArrayList<String>();
    }

    if (inputStr.equals("*")) {
      return new ArrayList<String>() {{
        add("?");
      }};
    }

    // Types list can consist of generic types wich generic parameters other generic types
    // so let's build list of signatures of generic types of first level
    ArrayList<String> splited = new ArrayList<String>();

    char inputStrChars[] = inputStr.toCharArray();
    int i = 0;
    int start = 0;
    while (i < inputStrChars.length) {
      // Found new generic type, add it to the current type signature for now
      if (inputStrChars[i] == '<') {

        int genericsLevel = 0;
        while (!(inputStrChars[i] == '>' && genericsLevel == 1)) {
          if (inputStrChars[i] == '<') {
            genericsLevel++;
          } else if (inputStrChars[i] == '>') {
            genericsLevel--;
          }

          i++;
        }
      }
      // Parameters are divided by ';' in a signature
      else if (inputStrChars[i] == ';') {
        String splitedType = inputStr.substring(start, i);
        splited.add(splitedType);
        start = i + 1;
      }

      i++;
    }

    // If signature consist of primitive types it doesn't end with ';' so some part
    // of the string left unsplited
    if (inputStrChars[i - 1] != ';') {
      splited.add(inputStr.substring(start, i));
    }

    ArrayList<String> result = new ArrayList<String>();
    for (String type : splited) {
      int j = 0;
      boolean isArray = false;
      while (j < type.length()) {
        // '[' is a sign of array after this symbol can be either character that represents
        // a primitive name or reference type definition
        if (type.charAt(j) == '[') {
          isArray = true;
          j++; continue;
        }
        // Check if next letter represents primitive type
        else if (isPrimitiveName(type.charAt(j))) {
          String s = getPrimitiveName(type.charAt(j));
          result.add(isArray? s + "[]" : s);
          isArray = false;

          j++; continue;
        }

        break;
      }

      // There were only primitives. Nothing more to parse
      if (j == type.length()) {
        return result;
      }

      String typeName = "";
      if (type.charAt(j) == '+') {
        typeName += "? extends ";
        j++;
      }
      else if (type.charAt(j) == '-') {
        typeName += "? super ";
        j++;
      }

      // Check if following type is generic
      int lessPos = type.indexOf('<', j);

      String preGeneric;
      String generic;
      // Not a generic, just remove leading 'L'
      if (lessPos < 0) {
        preGeneric = type.substring(j + 1);
        generic = "";
      }
      // Generic found
      else {
        // Cut type name and remove leading 'L'
        preGeneric = type.substring(j + 1, lessPos);
        // Cut generic parameters list
        String genericStr = type.substring(lessPos + 1, type.length() - 1);
        // Get list of generic parameters list for a generic type
        ArrayList<String> list = getTypesList(genericStr);

        generic = buildGenericType(list);
      }
      preGeneric = preGeneric.replace('/', '.');
      typeName += preGeneric + generic + ((isArray) ? "[]" : "");
      result.add(typeName);
    }

    return result;
  }

  private static String buildGenericType(ArrayList<String> list) {
    String result = "";

    for (int i = 0; i < list.size(); i++) {
      result += list.get(i);
      if (i != list.size() - 1) {
        result += ", ";
      }
    }

    return "<" + result + ">";
  }

  private static boolean isPrimitiveName(char c) {
    return "BSIJDFVZ".indexOf(c) >= 0;
  }

  private static String getPrimitiveName(char c) {
    switch(c) {
      case 'B':
        return "byte";

      case 'S':
        return "short";

      case 'I':
        return "int";

      case 'J':
        return "long";

      case 'F':
        return "float";

      case 'D':
        return "double";

      case 'Z':
        return "boolean";

      case 'V':
        return "void";

      default:
        throw new JPFException("Unknown primitive type name " + c);
    }
  }

  /**
   * Get list of method's input parameters names.
   */
  private static ArrayList<String> getMethodParametrs(String signature) {
    // Get parameter from signature
    int start = signature.indexOf('(') + 1;
    int end = signature.indexOf(')');
    String parametrs = signature.substring(start, end);

    return getTypesList(parametrs);
  }
}
