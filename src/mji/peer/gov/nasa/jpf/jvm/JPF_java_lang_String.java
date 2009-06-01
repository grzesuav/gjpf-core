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

/**
 * MJI NativePeer class for java.lang.String library abstraction
 */
public class JPF_java_lang_String {
  
  public static int intern____Ljava_lang_String_2 (MJIEnv env, int robj) {
    // now this is REALLY a naive implementation of 'intern', but
    // at least we don't burn state for a Hashtable.
    // Replace this once we have a real String abstraction
    DynamicArea   da = env.getDynamicArea();
    ElementInfo   sei = da.get(robj);

    // <2do> really? with heap symmetry, can't a corresponding one be at > robj?
    //*reply:
    // this picks a canonical one, the one with the lowest objref.  if a match
    // is at a point > robj (or there is no other matching string), robj is
    // the canonical. -peterd 
    for (int i = 0; i < robj; i++) {
      ElementInfo ei = da.get(i);

      if (ei != null) {
        if (ei.equals(sei)) {
          return i;
        }
      }
    }

    return robj;
  }
  
  public static boolean equals__Ljava_lang_Object_2__Z (MJIEnv env, int objRef, int argRef) {
    if (argRef == MJIEnv.NULL){
      return false;
      
    }

    DynamicArea   da = env.getDynamicArea();
    ElementInfo   s1 = da.get(objRef);
    ElementInfo   s2 = da.get(argRef);

    if (!env.isInstanceOf(argRef,"java.lang.String")) {
      return false;
    }
    
    Fields f1 = da.get( s1.getReferenceField("value")).getFields();
    int o1 = s1.getIntField("offset");
    int l1 = s1.getIntField("count");
    
    Fields f2 = da.get( s2.getReferenceField("value")).getFields();
    int o2 = s2.getIntField("offset");
    int l2 = s2.getIntField("count");
    
    if (l1 != l2) {
      return false;
    }
    
    return f1.isEqual(f2, o1, l1, o2);
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
      int[] values = ei.getFields().getValues();

      for (int i = 0; i < len; i++) {
        h = 31*h + (char)values[off++];
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
  public static int getBytes__Ljava_lang_String_2___3B(MJIEnv env, int ObjRef, int str){
	  String string=env.getStringObject(str);
	  byte[] b=string.getBytes();
	  return env.newByteArray(b);
  }
  
  public static int split__Ljava_lang_String_2___3Ljava_lang_String_2(MJIEnv env,int clsObjRef,int strRef){
    String s=env.getStringObject(strRef);
    String obj=env.getStringObject(clsObjRef);

    String[] result=obj.split(s);

    return env.newStringArray(result);
  }

}
