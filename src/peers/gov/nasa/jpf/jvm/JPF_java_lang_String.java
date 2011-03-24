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

  public static boolean equalsIgnoreCase__Ljava_lang_String_2__Z(MJIEnv env, int objref, int anotherString) {
    String thisString = env.getStringObject(objref);
    return thisString.equalsIgnoreCase(env.getStringObject(anotherString));
  }

  public static int compareTo__Ljava_lang_String_2__I(MJIEnv env, int objref, int anotherString) {
    Heap heap = env.getHeap();

    ElementInfo thisStr = heap.get(objref);
    ElementInfo otherStr = heap.get(anotherString);

    CharArrayFields thisFields = (CharArrayFields) heap.get(thisStr.getReferenceField("value")).getFields();
    int thisOffset = thisStr.getIntField("offset");
    int thisLength = thisStr.getIntField("count");
    char[] thisChars = thisFields.asCharArray();

    CharArrayFields otherFields = (CharArrayFields) heap.get(otherStr.getReferenceField("value")).getFields();
    int otherOffset = otherStr.getIntField("offset");
    int otherLength = otherStr.getIntField("count");
    char[] otherChars = otherFields.asCharArray();

    int minLength = Math.min(thisLength, otherLength);

    for (int i = thisOffset, j = otherOffset; i < thisOffset + minLength; i++, j++){
      if (thisChars[i] != otherChars[j]){
        return thisChars[i] - otherChars[j];
      }
    }

    return thisLength - otherLength;
  }

  public static boolean startsWith__Ljava_lang_String_2I__Z(MJIEnv env, int objref, int stringPrefixRef, int toffset) {

    if (toffset < 0)
      return false;

    Heap heap = env.getHeap();

    ElementInfo thisStr = heap.get(objref);
    ElementInfo prefix = heap.get(stringPrefixRef);

    CharArrayFields thisFields = (CharArrayFields) heap.get(thisStr.getReferenceField("value")).getFields();
    int thisLength = thisStr.getIntField("count");
    int thisOffset = thisStr.getIntField("offset");
    char thisChars[] = thisFields.asCharArray();

    CharArrayFields prefixFields = (CharArrayFields) heap.get(prefix.getReferenceField("value")).getFields();
    int prefixLength = prefix.getIntField("count");
    int prefixOffset = prefix.getIntField("offset");
    char prefixChars[] = prefixFields.asCharArray();

    return compareCharArraysFromOffset(thisChars, thisOffset, thisLength, prefixChars, prefixOffset, prefixLength, toffset);
  }

  private static boolean compareCharArraysFromOffset(char[] thisChars, int thisOffset, int thisLength, char[] otherChars, int otherOffset, int otherLength, int toffset) {
    if (thisLength < otherLength + toffset) {
      return false;
    }
    for (int t = thisOffset + toffset, p = otherOffset; p < otherOffset + otherLength; t++, p++) {
      if (thisChars[t] != otherChars[p]) {
        return false;
      }
    }
    return true;
  }

  public static boolean startsWith__Ljava_lang_String_2__Z(MJIEnv env, int objRef, int stringPrefixRef) {
    return startsWith__Ljava_lang_String_2I__Z(env, objRef, stringPrefixRef, 0);
  }

  public static boolean endsWith__Ljava_lang_String_2__Z(MJIEnv env, int objRef, int stringSuffixRef) {
     Heap heap = env.getHeap();

    ElementInfo thisStr = heap.get(objRef);
    ElementInfo prefix = heap.get(stringSuffixRef);

    CharArrayFields thisFields = (CharArrayFields) heap.get(thisStr.getReferenceField("value")).getFields();
    int thisLength = thisStr.getIntField("count");
    int thisOffset = thisStr.getIntField("offset");
    char thisChars[] = thisFields.asCharArray();

    CharArrayFields prefixFields = (CharArrayFields) heap.get(prefix.getReferenceField("value")).getFields();
    int suffixLength = prefix.getIntField("count");
    int suffixOffset = prefix.getIntField("offset");
    char suffixChars[] = prefixFields.asCharArray();

    return compareCharArraysFromOffset(thisChars, thisOffset, thisLength, suffixChars, suffixOffset, suffixLength,
            thisLength - suffixLength);
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
    String thisStr = env.getStringObject(objref);

    return thisStr.indexOf(c);
  }

  public static int indexOf__Ljava_lang_String_2__I(MJIEnv env, int objref, int str) {
    String thisStr = env.getStringObject(objref);
    String indexStr = env.getStringObject(str);

    return thisStr.indexOf(indexStr);
  }

  public static int indexOf__Ljava_lang_String_2I__I(MJIEnv env, int objref, int str, int fromIndex) {
    String thisStr = env.getStringObject(objref);
    String indexStr = env.getStringObject(str);

    return thisStr.indexOf(indexStr, fromIndex);
  }

  public static int lastIndexOf__I__I(MJIEnv env, int objref, int ch) {
    String thisStr = env.getStringObject(objref);

    return thisStr.lastIndexOf(ch);
  }

  public static int lastIndexOf__II__I(MJIEnv env, int objref, int ch, int fromIndex) {
    String thisStr = env.getStringObject(objref);

    return thisStr.lastIndexOf(ch, fromIndex);
  }

  public static int lastIndexOf__Ljava_lang_String_2__I(MJIEnv env, int objref, int strRef) {
    String thisStr = env.getStringObject(objref);
    String str = env.getStringObject(strRef);

    return thisStr.lastIndexOf(str);
  }

  public static int lastIndexOf__Ljava_lang_String_2I__I(MJIEnv env, int objref, int strRef, int fromIndex) {
    String thisStr = env.getStringObject(objref);
    String str = env.getStringObject(strRef);

    return thisStr.lastIndexOf(str, fromIndex);
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

  public static int concat__Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef, int strRef) {
    Heap heap = env.getHeap();

    ElementInfo thisStr = heap.get(objRef);
    ElementInfo otherStr = heap.get(strRef);

    int thisLength = thisStr.getIntField("count");
    int otherLength = otherStr.getIntField("count");

    if (otherLength == 0)
      return objRef;

    int thisOffset = thisStr.getIntField("offset");
    CharArrayFields thisFields = (CharArrayFields) heap.get(thisStr.getReferenceField("value")).getFields();
    char thisChars[] = thisFields.asCharArray();
    
    int otherOffset = otherStr.getIntField("offset");
    CharArrayFields otherFields = (CharArrayFields) heap.get(otherStr.getReferenceField("value")).getFields();
    char otherChars[] = otherFields.asCharArray();

    char resultChars[] = new char[thisLength + otherLength];
    System.arraycopy(thisChars, thisOffset, resultChars, 0, thisLength);
    System.arraycopy(otherChars, otherOffset, resultChars, thisLength, otherLength);

    return env.newString(new String(resultChars));
  }

  public static int replace__CC__Ljava_lang_String_2(MJIEnv env, int objRef, char oldChar, char newChar) {
    if (oldChar != newChar) {
      Heap heap = env.getHeap();

      ElementInfo thisStr = heap.get(objRef);
      int thisOffset = thisStr.getIntField("offset");
      int thisLength = thisStr.getIntField("count");
      CharArrayFields thisFields = (CharArrayFields) env.getHeap().get(thisStr.getReferenceField("value")).getFields();
      char newChars[] = thisFields.asCharArray(thisOffset, thisLength);

      for (int i = 0; i < newChars.length; i++)
        if (newChars[i] == oldChar)
          newChars[i] = newChar;

      return env.newString(new String(newChars));

    }
    return objRef;

  }

  public static boolean matches__Ljava_lang_String_2__Z(MJIEnv env, int objRef, int regexRef) {
    String thisStr = env.getStringObject(objRef);
    String regexStr = env.getStringObject(regexRef);

    return thisStr.matches(regexStr);
  }

  public static int replaceFirst__Ljava_lang_String_2Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef, int regexRef, int replacementRef) {
    String thisStr = env.getStringObject(objRef);
    String regexStr = env.getStringObject(regexRef);
    String replacementStr = env.getStringObject(replacementRef);

    String result = thisStr.replaceFirst(regexStr, replacementStr);
    return env.newString(result);
  }

  public static int replaceAll__Ljava_lang_String_2Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef, int regexRef, int replacementRef) {
    String thisStr = env.getStringObject(objRef);
    String regexStr = env.getStringObject(regexRef);
    String replacementStr = env.getStringObject(replacementRef);

    String result = thisStr.replaceAll(regexStr, replacementStr);
    return env.newString(result);
  }

  // <2do> we also need startsWith, endsWith, indexOf etc. - all not relevant from
  // a model checking perspective (unless we want to compute execution budgets)
  
    
  public static int format__Ljava_lang_String_2_3Ljava_lang_Object_2__Ljava_lang_String_2 (MJIEnv env, int clsObjRef,
                                                                                           int fmtRef, int argRef){
    return env.newString(env.format(fmtRef,argRef));
  }
  
  //
  public static int getBytes__Ljava_lang_String_2___3B(MJIEnv env, int ObjRef, int str) {
    String string = env.getStringObject(str);
    byte[] b = string.getBytes();
    return env.newByteArray(b);
  }
  
  public static int split__Ljava_lang_String_2I___3Ljava_lang_String_2(MJIEnv env, int clsObjRef, int strRef, int limit) {
    String s = env.getStringObject(strRef);
    String obj = env.getStringObject(clsObjRef);

    String[] result = obj.split(s, limit);

    return env.newStringArray(result);
  }

  public static int split__Ljava_lang_String_2___3Ljava_lang_String_2(MJIEnv env,int clsObjRef,int strRef){
    String s=env.getStringObject(strRef);
    String obj=env.getStringObject(clsObjRef);

    String[] result=obj.split(s);

    return env.newStringArray(result);
  }

  public static int trim____Ljava_lang_String_2(MJIEnv env,int objRef) {
    Heap heap = env.getHeap();
    ElementInfo thisStr = heap.get(objRef);

    CharArrayFields thisFields = (CharArrayFields) heap.get(thisStr.getReferenceField("value")).getFields();
    int thisLength = thisStr.getIntField("count");
    int thisOffset = thisStr.getIntField("offset");
    char thisChars[] = thisFields.asCharArray();

    int start = thisOffset;
    int end = thisOffset + thisLength;

    while ((start < end) && (thisChars[start] == ' '))
      start++;

    while ((start < end) && (thisChars[end - 1] == ' '))
      end--;

    if (start == thisOffset &&
        end == thisOffset + thisLength)
      return objRef;

    String result = new String(thisChars, start, end - start);
    return env.newString(result);
  }

}
