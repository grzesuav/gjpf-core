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

import java.lang.reflect.Modifier;
import gov.nasa.jpf.Config;

public class JPF_java_lang_reflect_Field {

  // the registry is rather braindead, let's hope we don't have many lookups - 
  // using Fields is fine, but creating them is not efficient until we fix this
  
  static final int NREG = 64;
  static FieldInfo[] registered;
  static int nRegistered;
  
  public static void init (Config conf){
    registered = new FieldInfo[NREG];
    nRegistered = 0;
  }
  
  static int registerFieldInfo (FieldInfo fi) {
    int idx;
    
    for (idx=0; idx < nRegistered; idx++) {
      if (registered[idx] == fi) {
        return idx;
      }
    }
    
    if (idx == registered.length) {
      FieldInfo[] newReg = new FieldInfo[registered.length+NREG];
      System.arraycopy(registered, 0, newReg, 0, registered.length);
      registered = newReg;
    }
    
    registered[idx] = fi;
    nRegistered++;
    return idx;
  }
  
  static FieldInfo getRegisteredFieldInfo (int idx) {
    return registered[idx];
  }
  
  /**
   * >2do> that doesn't take care of class init yet
   */
  public static int getType____Ljava_lang_Class_2 (MJIEnv env, int objRef) {
    ThreadInfo ti = env.getThreadInfo();
    FieldInfo fi = getFieldInfo(env, objRef);
    ClassInfo ci = fi.getTypeClassInfo();
    
    if (!ci.isInitialized()) {
      if (ci.loadAndInitialize(ti, ti.getPC()) > 0) {
        env.repeatInvocation();
      }
    }
    
    return ci.getClassObjectRef();
  }
  
  public static int getModifiers____I (MJIEnv env, int objRef){
    FieldInfo fi = getFieldInfo(env, objRef);
    return fi.getModifiers();
  }
  
  static int getIntField (MJIEnv env, int objRef, int fobjRef, Class<?> fiType, String type) {
    FieldInfo fi = getFieldInfo(env, objRef);
    ElementInfo ei;
    
    if (fobjRef == MJIEnv.NULL){ // static field
      ei = fi.getClassInfo().getStaticElementInfo();      
    } else { // instance field
      ei = env.getElementInfo(fobjRef);
    }

    // our guards (still need IllegalAccessException)
    if (ei == null) {
      env.throwException("java.lang.NullPointerException");
      return 0;      
    }
    if (!fiType.isInstance(fi)) {
      env.throwException("java.lang.IllegalArgumentException", "field type incompatible with " + type);
      return 0;
    }
    
    int val = ei.getIntField(fi);
    return val;    
  }

  static long getLongField (MJIEnv env, int objRef, int fobjRef, Class<?> fiType, String type) {
    FieldInfo fi = getFieldInfo(env, objRef);
    ElementInfo ei;
    
    if (fobjRef == MJIEnv.NULL){ // static field
      ei = fi.getClassInfo().getStaticElementInfo();      
    } else { // instance field
      ei = env.getElementInfo(fobjRef);
    }

    // our guards (still need IllegalAccessException)
    if (ei == null) {
      env.throwException("java.lang.NullPointerException");
      return 0;      
    }
    if (!fiType.isInstance(fi)) {
      env.throwException("java.lang.IllegalArgumentException", "field type incompatible with " + type);
      return 0;
    }
    
    long val = ei.getLongField(fi);
    return val;
  }

  static void setIntField (MJIEnv env, int objRef, int fobjRef,
                           Class<?> fiType, String type, int val) {
    FieldInfo fi = getFieldInfo(env, objRef);
    ElementInfo ei;
    
    if (fobjRef == MJIEnv.NULL){ // static field
      ei = fi.getClassInfo().getStaticElementInfo();      
    } else { // instance field
      ei = env.getElementInfo(fobjRef);
    }

    // our guards (still need IllegalAccessException)
    if (ei == null) {
      env.throwException("java.lang.NullPointerException");
      return;      
    }
    if (!fiType.isInstance(fi)) {
      env.throwException("java.lang.IllegalArgumentException", "field type incompatible with " + type);
      return;
    }
    
    ei.setIntField(fi, val);    
  }  

  static void setLongField (MJIEnv env, int objRef, int fobjRef,
                           Class<?> fiType, String type, long val) {
    FieldInfo fi = getFieldInfo(env, objRef);
    ElementInfo ei;
    
    if (fobjRef == MJIEnv.NULL){ // static field
      ei = fi.getClassInfo().getStaticElementInfo();      
    } else { // instance field
      ei = env.getElementInfo(fobjRef);
    }

    // our guards (still need IllegalAccessException)
    if (ei == null) {
      env.throwException("java.lang.NullPointerException");
      return;      
    }
    if (!fiType.isInstance(fi)) {
      env.throwException("java.lang.IllegalArgumentException", "field type incompatible with " + type);
      return;
    }
    
    ei.setLongField(fi, val);    
  }  
  
  public static boolean getBoolean__Ljava_lang_Object_2__Z (MJIEnv env, int objRef, int fobjRef) {
    int v = getIntField(env, objRef, fobjRef, IntegerFieldInfo.class, "boolean");
    return (v != 0) ? true : false;
  }
  public static byte getByte__Ljava_lang_Object_2__B (MJIEnv env, int objRef, int fobjRef) {
    int v = getIntField(env, objRef, fobjRef, IntegerFieldInfo.class, "byte");
    return (byte)v;
  }
  public static char getChar__Ljava_lang_Object_2__C (MJIEnv env, int objRef, int fobjRef) {
    int v = getIntField(env, objRef, fobjRef, IntegerFieldInfo.class, "char");
    return (char)v;
  }
  public static short getShort__Ljava_lang_Object_2__S (MJIEnv env, int objRef, int fobjRef) {
    int v = getIntField(env, objRef, fobjRef, IntegerFieldInfo.class, "short");
    return (short)v;
  }  
  public static int getInt__Ljava_lang_Object_2__I (MJIEnv env, int objRef, int fobjRef) {
    return getIntField(env, objRef, fobjRef, IntegerFieldInfo.class, "int");
  }
  public static long getLong__Ljava_lang_Object_2__J (MJIEnv env, int objRef, int fobjRef) {
    return getLongField(env, objRef, fobjRef, LongFieldInfo.class, "long");
  }
  public static float getFloat__Ljava_lang_Object_2__F (MJIEnv env, int objRef, int fobjRef) {
    int v = getIntField(env, objRef, fobjRef, FloatFieldInfo.class, "float");
    return Types.intToFloat(v);
  }
  public static double getDouble__Ljava_lang_Object_2__D (MJIEnv env, int objRef, int fobjRef) {
    long v = getLongField(env, objRef, fobjRef, DoubleFieldInfo.class, "double");
    return Types.longToDouble(v);
  }

  public static int getAnnotation__Ljava_lang_Class_2__Ljava_lang_annotation_Annotation_2 (MJIEnv env, int objRef, int annotationClsRef) {
    FieldInfo fi = getFieldInfo(env,objRef);
    ClassInfo aci = JPF_java_lang_Class.getReferredClassInfo(env,annotationClsRef);
    
    AnnotationInfo ai = fi.getAnnotation(aci.getName());
    if (ai != null){
      ClassInfo aciProxy = ClassInfo.getAnnotationProxy(aci);
      aciProxy.loadAndInitialize(env.getThreadInfo());      
      try {
        return env.newAnnotationProxy(aciProxy, ai);
      } catch (ClinitRequired x){
        env.handleClinitRequest(x.getRequiredClassInfo());
        return MJIEnv.NULL;
      }
    }
    
    return MJIEnv.NULL;
  }

  public static int getAnnotations_____3Ljava_lang_annotation_Annotation_2 (MJIEnv env, int objRef){
    FieldInfo fi = getFieldInfo(env,objRef);
    AnnotationInfo[] ai = fi.getAnnotations();
    
    try {
      return env.newAnnotationProxies(ai);
    } catch (ClinitRequired x){
      env.handleClinitRequest(x.getRequiredClassInfo());
      return MJIEnv.NULL;
    }
  }

  
  public static void setBoolean__Ljava_lang_Object_2Z__V (MJIEnv env, int objRef, int fobjRef,
                                                          boolean val) {
    setIntField( env, objRef, fobjRef, IntegerFieldInfo.class, "boolean", val ? 1 : 0);
  }
  public static void setByte__Ljava_lang_Object_2B__V (MJIEnv env, int objRef, int fobjRef,
                                                          byte val) {
    setIntField( env, objRef, fobjRef, IntegerFieldInfo.class, "byte", val);
  }
  public static void setChar__Ljava_lang_Object_2C__V (MJIEnv env, int objRef, int fobjRef,
                                                       char val) {
    setIntField( env, objRef, fobjRef, IntegerFieldInfo.class, "char", val);
  }
  public static void setShort__Ljava_lang_Object_2S__V (MJIEnv env, int objRef, int fobjRef,
                                                       short val) {
    setIntField( env, objRef, fobjRef, IntegerFieldInfo.class, "short", val);
  }  
  public static void setInt__Ljava_lang_Object_2I__V (MJIEnv env, int objRef, int fobjRef,
                                                      int val) {
    setIntField( env, objRef, fobjRef, IntegerFieldInfo.class, "int", val);
  }
  public static void setLong__Ljava_lang_Object_2J__V (MJIEnv env, int objRef, int fobjRef,
                                                       long val) {
    setLongField( env, objRef, fobjRef, LongFieldInfo.class, "long", val);
  }
  public static void setFloat__Ljava_lang_Object_2F__V (MJIEnv env, int objRef, int fobjRef,
                                                        float val) {
    setIntField( env, objRef, fobjRef, FloatFieldInfo.class, "float", Types.floatToInt(val));
  }
  public static void setDouble__Ljava_lang_Object_2D__V (MJIEnv env, int objRef, int fobjRef,
                                                         double val) {
    setLongField( env, objRef, fobjRef, DoubleFieldInfo.class, "double", Types.doubleToLong(val));
  }

  public static int get__Ljava_lang_Object_2__Ljava_lang_Object_2 (MJIEnv env, int objRef, int fobjRef) {
    FieldInfo fi = getFieldInfo(env, objRef);
    ElementInfo ei;

    if (fi.isStatic()){
      ClassInfo ci = fi.getClassInfo();
      ei = ci.getStaticElementInfo();
    } else {
      ei = env.getElementInfo(fobjRef);
    }

    // our guards (still need IllegalAccessException)
    if (ei == null) {
      env.throwException("java.lang.NullPointerException");
      return 0;      
    }
    
    if (!(fi instanceof ReferenceFieldInfo)) { // primitive type, we need to box it
      if (fi instanceof DoubleFieldInfo){
        double d = ei.getDoubleField(fi);
        return env.newDouble(d);
      } else if (fi instanceof FloatFieldInfo){
        float f = ei.getFloatField(fi);
        return env.newFloat(f);
      } else if (fi instanceof LongFieldInfo){
        long l = ei.getLongField(fi);
        return env.newLong(l);
      } else if (fi instanceof IntegerFieldInfo){
        // this might actually represent a plethora of types
        int i = ei.getIntField(fi);
        
        // <2do> this still sucks - we need to split teh IntegerFieldInfo up
        String cls = fi.getType();
        if (cls.equals("int")){
          return env.newInteger(i);
        } else if (cls.equals("boolean")){
          return env.newBoolean(i != 0);
        } else if (cls.equals("byte")){
          return env.newByte((byte)i);
        } else if (cls.equals("short")){
          return env.newShort((short)i);
        } else if (cls.equals("char")){
          return env.newCharacter((char)i);
        }
      }
      
    } else { // it's a reference
      int ref = ei.getIntField(fi); // we internally store it as int
      return ref;
    }
    
    env.throwException("java.lang.IllegalArgumentException", "unknown field type");
    return MJIEnv.NULL;
  }
  
  public static int getDeclaringClass____Ljava_lang_Class_2 (MJIEnv env, int objref){
    FieldInfo fi = getFieldInfo(env, objref);
    ClassInfo ci = fi.getClassInfo();
    return ci.getClassObjectRef();
  }
  
  public static boolean isSynthetic____Z (MJIEnv env, int objref){
    FieldInfo fi = getFieldInfo(env, objref);
    String fn = fi.getName();
    return (fn.startsWith("this$") || fn.startsWith("val$"));
  }
  
  public static int getName____Ljava_lang_String_2 (MJIEnv env, int objRef) {
    FieldInfo fi = getFieldInfo(env, objRef);
    
    int nameRef = env.getReferenceField( objRef, "name");
    if (nameRef == -1) {
      nameRef = env.newString(fi.getName());
      env.setReferenceField(objRef, "name", nameRef);
    }
   
    return nameRef;
  }
  
  static FieldInfo getFieldInfo (MJIEnv env, int objRef) {
    int fidx = env.getIntField( objRef, "regIdx");
    assert ((fidx >= 0) || (fidx < nRegistered)) : "illegal FieldInfo request: " + fidx + ", " + nRegistered;
    
    return registered[fidx];
  }
  
  /**
  * Peer method for the <code>java.lang.reflect.Field.set</code> method.
  * 
  * @author Mirko Stojmenovic (mirko.stojmenovic@gmail.com)
  * @author Igor Andjelkovic (igor.andjelkovic@gmail.com)
  * @author Milos Gligoric (milos.gligoric@gmail.com)
  *  
  */
  public static void set__Ljava_lang_Object_2Ljava_lang_Object_2__V (MJIEnv env, int objRef, int fobjRef, int val) {
    FieldInfo fi = getFieldInfo(env, objRef);
    int modifiers = fi.getModifiers();

    if (fobjRef == MJIEnv.NULL && !Modifier.isStatic(modifiers)) {
      env.throwException("java.lang.NullPointerException");
      return;
    }
    if (Modifier.isFinal(modifiers)) {
      env.throwException("java.lang.IllegalAccessException", "field " + fi.getName() + " is final");
      return;
    }
    ClassInfo ci = fi.getClassInfo();
    ClassInfo cio = env.getClassInfo(fobjRef);

    if (!fi.isStatic() && !cio.isInstanceOf(ci)) {
      env.throwException("java.lang.IllegalArgumentException", 
                         fi.getType() + "field " + fi.getName() + " does not belong to this object");
      return;
    }
    
    if (!setValue(env, fi, fobjRef, val)) {
      env.throwException("java.lang.IllegalArgumentException",  
                         "Can not set " + fi.getType() + " field " + fi.getFullName() + " to " + ((MJIEnv.NULL != val) ? env.getClassInfo(val).getName() + " object " : "null"));
    }
  }

  private static boolean setValue(MJIEnv env, FieldInfo fi, int obj, int value) {
    ClassInfo fieldClassInfo = fi.getClassInfo();
    
    String className = fieldClassInfo.getName();
    String fieldType = fi.getType();
    ClassInfo ti = fi.getTypeClassInfo();
    
    if (ti.isPrimitive()) {
      
      if (value == MJIEnv.NULL) return false;  
    
      // checks whether unboxing can be done by accessing the field "value"
      final String fieldName = "value";
      FieldInfo finfo = env.getElementInfo(value).getFieldInfo(fieldName);
      if (finfo == null) return false;
      
      if (fieldType.equals("boolean") || fieldType.equals("byte") ||
          fieldType.equals("short") || fieldType.equals("char") ||
          fieldType.equals("int") || fieldType.equals("float")) {
    
        int val = env.getIntField(value, fieldName);
        if (fi.isStatic()) {
          ElementInfo ei = fi.getClassInfo().getStaticElementInfo();      
          ei.setIntField(fi, val);
        } else {
          env.setIntField(obj, fi.getName(), val);
        }
        return true;
      } else if (fieldType.equals("long") || fieldType.equals("double")) {
      
        long val = env.getLongField(value, fieldName);
        if (fi.isStatic()) {
          ElementInfo ei = fi.getClassInfo().getStaticElementInfo();      
          ei.setLongField(fi, val);
        } else {
          env.setLongField(obj, fi.getName(), val);
        }
        return true;
      } else {
        return false;
      }
    } else { // it's a reference
      if (value != MJIEnv.NULL) {
        String type = env.getTypeName(value);
        ClassInfo valueCI = ClassInfo.getClassInfo(Types.getTypeName(type));
        if (!valueCI.isInstanceOf(ti)) return false;
      }
      if (fi.isStatic()) { 
        env.setStaticReferenceField(className, fi.getName(), value);
      } else {
        env.setReferenceField(obj, fi.getName(), value);
      }
      return true;
    }
  }
}
