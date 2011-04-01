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

import gov.nasa.jpf.*;
import gov.nasa.jpf.util.MethodInfoRegistry;
import gov.nasa.jpf.util.RunListener;
import gov.nasa.jpf.util.RunRegistry;
import java.lang.reflect.*;
import java.util.ArrayList;
import javax.naming.spi.DirStateFactory.Result;

public class JPF_java_lang_reflect_Method {

  static MethodInfoRegistry registry;

  // class init - this is called automatically from the NativePeer ctor
  public static void init (Config conf) {
    // this is an example of how to handle cross-initialization between
    // native peers - this might also get explicitly called by the java.lang.Class
    // peer, since it creates Method objects. Here we have to make sure
    // we only re-pushClinit between JPF runs

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
    int eidx = env.newObject(ClassInfo.getResolvedClassInfo("java.lang.reflect.Method"));
    ElementInfo ei = env.getElementInfo(eidx);
    
    ei.setIntField("regIdx", regIdx);
    ei.setBooleanField("isAccessible", mi.isPublic());
    
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
    Object objValue;
    long rawValue;
    int i, nArgs, passedCount, sourceRef;
    byte sourceType, destTypes[];

    if (argsRef == MJIEnv.NULL){
      return false;
    }

    destTypes     = mi.getArgumentTypes();
    destTypeNames = mi.getArgumentTypeNames();
    nArgs         = destTypeNames.length;
    passedCount   = env.getArrayLength(argsRef);
    
    if (nArgs != passedCount) {
      env.throwException(IllegalArgumentException.class.getName(), "Wrong number of arguments passed.  Actual = " + passedCount + ".  Expected = " + nArgs);
      return false;
    }
    
    for (i = 0; i < nArgs; i++) {
      
      sourceRef = env.getReferenceArrayElement(argsRef, i);

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
      sourceType  = getClassType(sourceClass, !isBoxedPrimitive(destTypeNames[i]));
       
      if (!isCompatible(sourceType, destTypes[i], sourceClass, destTypeNames[i])) {
        env.throwException(IllegalArgumentException.class.getName(), "Wrong argument type at index " + i + ".  Source Class = " + sourceClass.getName() + ".  Dest Class = " + destTypeNames[i]);
        return false;
      }
       
      objValue = readValue(source, sourceType);
      rawValue = convertValue(destTypes[i], objValue);
       
      pushValue(frame, destTypes[i], rawValue);
    }
    
    return true;
  }

 private static boolean isBoxedPrimitive(String className) {
    return (className.equals("java.lang.Byte")
        || className.equals("java.lang.Short")
        || className.equals("java.lang.Integer")
        || className.equals("java.lang.Long")
        || className.equals("java.lang.Float")
        || className.equals("java.lang.Double")
        || className.equals("java.lang.Boolean") || className.equals("java.lang.Character"));
  }

  private static byte getClassType(ClassInfo clazz, boolean unbox) {
    String className;
    
    className = clazz.getName();
    
    if (className.equals("java.lang.Byte") && unbox) {
      return Types.T_BYTE; 
    }

    if (className.equals("java.lang.Short") && unbox) {
      return Types.T_SHORT; 
    }

    if (className.equals("java.lang.Integer") && unbox) {
      return Types.T_INT; 
    }

    if (className.equals("java.lang.Long") && unbox) {
      return Types.T_LONG; 
    }

    if (className.equals("java.lang.Float") && unbox) {
      return Types.T_FLOAT; 
    }

    if (className.equals("java.lang.Double") && unbox) {
      return Types.T_DOUBLE; 
    }

    if (className.equals("java.lang.Boolean") && unbox) {
      return Types.T_BOOLEAN; 
    }

    if (className.equals("java.lang.Character") && unbox) {
      return Types.T_CHAR; 
    }

    if (className.equals("java.lang.Void")) {
      return Types.T_VOID; 
    }
    
    if (className.charAt(0) == '[') {
      return Types.T_ARRAY;
    }
    
    return Types.T_REFERENCE;
  }
  
  private static boolean isCompatible(byte sourceType, byte destType, ClassInfo sourceClass, String destClassName) {
    switch (destType) {
      case Types.T_DOUBLE:
        if (sourceType == Types.T_DOUBLE)
          return true;
        //break;
        
      case Types.T_FLOAT:
        if (sourceType == Types.T_FLOAT)
          return true;
        //break;

      case Types.T_LONG:
        if (sourceType == Types.T_LONG)
          return true; 
        //break;

      case Types.T_INT:
        if (sourceType == Types.T_CHAR)   // HotSpot's Method.invoke() will convert char to double/float/long/int
          return true;

        if (sourceType == Types.T_INT)
          return true;
        //break;
       
      case Types.T_SHORT:
        if (sourceType == Types.T_SHORT)
          return true;
        //break;

      case Types.T_BYTE:
        return sourceType == Types.T_BYTE;

      case Types.T_BOOLEAN:
        return sourceType == Types.T_BOOLEAN;

      case Types.T_CHAR:
        return sourceType == Types.T_CHAR;

      case Types.T_ARRAY:
      case Types.T_REFERENCE:
        return sourceClass.isInstanceOf(destClassName);

      case Types.T_VOID:
      default:
        throw new JPFException("Can not convert " + sourceClass.getName() + " to " + destClassName);
    }
  }
  
  private static Object readValue(ElementInfo value, byte type) {
    switch (type) {
      case Types.T_DOUBLE:  return value.getDoubleField("value");
      case Types.T_LONG:    return value.getLongField("value");
      case Types.T_FLOAT:   return value.getFloatField("value");
      case Types.T_INT:     return value.getIntField("value");
      case Types.T_SHORT:   return value.getShortField("value");
      case Types.T_BYTE:    return value.getByteField("value");
      case Types.T_CHAR:    return value.getCharField("value");
      case Types.T_BOOLEAN: return value.getBooleanField("value");
       
      case Types.T_ARRAY:
      case Types.T_REFERENCE:
        return value.getIndex();

      case Types.T_VOID:
      default:
        throw new JPFException("Unhandled type: " + type);
    }
  }
  
  private static long convertValue(byte type, Object value) {
    if ((type != Types.T_CHAR) && (value instanceof Character)) {  // HotSpot's Method.invoke() will convert char into double/float/long/int
      value = Integer.valueOf(((Character) value).charValue());
    } 
     
    switch (type) {
      case Types.T_DOUBLE:  return Double.doubleToLongBits(((Number) value).doubleValue());
      case Types.T_FLOAT:   return Float.floatToIntBits(((Number) value).floatValue());
      case Types.T_LONG:    return ((Number) value).longValue();
      case Types.T_INT:     return ((Number) value).intValue();
      case Types.T_SHORT:   return ((Number) value).shortValue();
      case Types.T_BYTE:    return ((Number) value).byteValue();
      case Types.T_CHAR:    return ((Character) value).charValue();
      case Types.T_BOOLEAN: return ((Boolean) value).booleanValue() ? 1 : 0;

      case Types.T_ARRAY:
      case Types.T_REFERENCE:
         return ((Integer) value).intValue();

      case Types.T_VOID:
      default:
         throw new JPFException("Unhandled type: " + type);
    }
  }
  
  private static void pushValue(StackFrame frame, byte type, long value) {
    switch (type) {
      case Types.T_DOUBLE:
      case Types.T_LONG:
        frame.longPush(value);
        break;
              
      case Types.T_FLOAT:
      case Types.T_INT:
      case Types.T_SHORT:
      case Types.T_BYTE:
      case Types.T_CHAR:
      case Types.T_BOOLEAN:
        frame.push((int) value);
        break;

      case Types.T_ARRAY:
      case Types.T_REFERENCE:
        frame.pushRef((int) value);
        break;

      case Types.T_VOID:
      default:
        throw new JPFException("Unhandled type: " + type);
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
    
    DirectCallStackFrame frame = ti.getReturnedDirectCall();

    if (frame != null){
      return createBoxedReturnValueObject( env, mi, frame);

    } else {
      MethodInfo stub = mi.createReflectionCallStub();
      frame = new DirectCallStackFrame(stub, stub.getMaxStack(), stub.getMaxLocals());

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

  public static int toString____Ljava_lang_String_2 (MJIEnv env, int objRef){
    StringBuilder sb = new StringBuilder();
    
    MethodInfo mi = getMethodInfo(env, objRef);
    appendMethodModifiers(mi, sb);

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

  public static int toGenericString____Ljava_lang_String_2(MJIEnv env, int objRef) {
    StringBuilder sb = new StringBuilder();
    MethodInfo mi = getMethodInfo(env, objRef);

    String signature = mi.getGenericSignature();
    // Not a generic method
    if (signature.isEmpty()) {
      return toString____Ljava_lang_String_2(env, objRef);
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
          sb.append(", ");
        }
      }
      sb.append('>');
    }

    sb.append(returnType);
    sb.append(' ');

    sb.append(mi.getClassName());
    sb.append('.');
    sb.append(mi.getName());
    
    sb.append('(');

    for (int i = 0; i < parametrs.size(); i++) {
      sb.append(parametrs.get(i));
      if (i != parametrs.size() - 1) {
        sb.append(", ");
      }
    }

    sb.append(");");

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
