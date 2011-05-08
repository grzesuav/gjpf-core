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

import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * MJI NativePeer class for java.lang.String library abstraction
 */
public class JPF_java_lang_String {
  
  public static int intern____Ljava_lang_String_2 (MJIEnv env, int robj) {
    // <2do> Replace this with a JPF space HashSet once we have a String model
    Heap   heap = env.getHeap();

    String s = env.getStringObject(robj);
    robj = heap.newInternString(s, env.getThreadInfo());

    return robj;
  }
  
  public static boolean equals__Ljava_lang_Object_2__Z (MJIEnv env, int objRef, int argRef) {
    if (argRef == MJIEnv.NULL){
      return false;
      
    }

    Heap   heap = env.getHeap();
    ElementInfo   s1 = heap.get(objRef);
    ElementInfo   s2 = heap.get(argRef);

    if (!env.isInstanceOf(argRef,"java.lang.String")) {
      return false;
    }
    
    Fields f1 = heap.get( s1.getReferenceField("value")).getFields();
    int o1 = s1.getIntField("offset");
    int l1 = s1.getIntField("count");
    
    Fields f2 = heap.get( s2.getReferenceField("value")).getFields();
    int o2 = s2.getIntField("offset");
    int l2 = s2.getIntField("count");
    
    if (l1 != l2) {
      return false;
    }

    char[] c1 = ((CharArrayFields)f1).asCharArray();
    char[] c2 = ((CharArrayFields)f2).asCharArray();

    for (int j=o1, k=o2, max=o1+l1; j<max; j++, k++){
      if (c1[j] != c2[k]){
        return false;
      }
    }

    return true;
  }
  
  public static int toCharArray_____3C (MJIEnv env, int objref){
    int vref = env.getReferenceField(objref, "value");
    int off = env.getIntField(objref, "offset");
    int len = env.getIntField(objref, "count");
    
    int cref = env.newCharArray(len);
    
    for (int i=0, j=off; i<len; i++, j++){
      env.setCharArrayElement(cref, i, env.getCharArrayElement(vref, j));
    }
    
    return cref;
  }
  
  public static int indexOf__I__I (MJIEnv env, int objref, int c) {
    int vref = env.getReferenceField(objref, "value");
    int off = env.getIntField(objref, "offset");
    int len = env.getIntField(objref, "count");
    
    for (int i=0, j=off; i<len; i++, j++){
      if ((int)env.getCharArrayElement(vref, j) == c) {
        return i;
      }
    }
    
    return -1;
  }
  
  public static int hashCode____I (MJIEnv env, int objref) {
    int h = env.getIntField(objref, "hash");
    
    if (h == 0){
      int vref = env.getReferenceField(objref, "value");
      int off = env.getIntField(objref, "offset");
      int len = env.getIntField(objref, "count");

      // now get the char array data, but be aware they are stored as ints
      ElementInfo ei = env.getElementInfo(vref);
      char[] values = ((CharArrayFields)ei.getFields()).asCharArray();

      for (int max=off+len; off<max; off++) {
        h = 31*h + values[off];
      }
      env.setIntField(objref, "hash", h);
      
    }    
    
    return h;
  }
  
  public static boolean matches__Ljava_lang_String_2__Z (MJIEnv env, int objRef, int regexRef){
    String s = env.getStringObject(objRef);
    String r = env.getStringObject(regexRef);
    
    return s.matches(r);
  }
  
  // <2do> we also need startsWith, endsWith, indexOf etc. - all not relevant from
  // a model checking perspective (unless we want to compute execution budgets)
  
    
  public static int format__Ljava_lang_String_2_3Ljava_lang_Object_2__Ljava_lang_String_2 (MJIEnv env, int clsObjRef,
                                                                                           int fmtRef, int argRef){
    return env.newString(env.format(fmtRef,argRef));
  }
  
  //
  public static int getBytes__Ljava_lang_String_2___3B(MJIEnv env, int objRef, int charSetRef){
	  String string = env.getStringObject(objRef);
    String charset = env.getStringObject(charSetRef);

    try {
      byte[] b=string.getBytes(charset);
  	  return env.newByteArray(b);

    } catch (UnsupportedEncodingException uex){
      env.throwException(uex.getClass().getName(), uex.getMessage());
      return MJIEnv.NULL;
    }
  }
  
  public static int split__Ljava_lang_String_2___3Ljava_lang_String_2(MJIEnv env,int clsObjRef,int strRef){
    String s=env.getStringObject(strRef);
    String obj=env.getStringObject(clsObjRef);

    String[] result=obj.split(s);

    return env.newStringArray(result);
  }


  public static int toUpperCase____Ljava_lang_String_2 (MJIEnv env, int objRef){
    String s = env.getStringObject(objRef);
    String upper = s.toUpperCase();

    return (s == upper) ? objRef : env.newString(upper);
  }

  public static int toLowerCase____Ljava_lang_String_2 (MJIEnv env, int objRef){
    String s = env.getStringObject(objRef);
    String lower = s.toLowerCase();

    return (s == lower) ? objRef : env.newString(lower);
  }


  public static int toUpperCase__Ljava_util_Locale_2__Ljava_lang_String_2 (MJIEnv env, int objRef, int locRef){
    String s = env.getStringObject(objRef);
    Locale loc = JPF_java_util_Locale.getLocale(env, locRef);

    String upper = s.toUpperCase(loc);

    return (s == upper) ? objRef : env.newString(upper);
  }

  public static int toLowerCase__Ljava_util_Locale_2__Ljava_lang_String_2 (MJIEnv env, int objRef, int locRef){
    String s = env.getStringObject(objRef);
    Locale loc = JPF_java_util_Locale.getLocale(env, locRef);

    String lower = s.toLowerCase(loc);

    return (s == lower) ? objRef : env.newString(lower);
  }


}
