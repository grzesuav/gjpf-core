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

/**
 *
 * @author Ivan Mushketik
 */
public class JPF_java_util_StringTokenizer {

  private static final String standardDelims = "\t \b\f\n";

  public static void init__Ljava_lang_String_2Ljava_lang_String_2Z__V(MJIEnv env, int thisRef, int strRef, int delimRef, boolean returnDelims) {
    ElementInfo stEI = env.getElementInfo(thisRef);
    String str = env.getStringObject(strRef);

    stEI.setReferenceField("str", strRef);
    stEI.setIntField("strLength", str.length());
    stEI.setIntField("currentPos", 0);
    stEI.setBooleanField("returnDelims", returnDelims);

    String delims = env.getStringObject(delimRef);
    setDelimsArray(env, stEI, delims);
  }

  private static void setDelimsArray(MJIEnv env, ElementInfo stEI, String delims) {
    int delimsCodePoints[] = new int[delims.length()];

    int maxCodePoint = -1;
    int count = 0;
    int c;
    for (int i = 0; i < delims.length(); i += Character.charCount(i), count++) {
      c = delims.codePointAt(i);
      if (c > maxCodePoint) {
        maxCodePoint = c;
      }
      delimsCodePoints[count] = c;
    }
    
    int trimedDelims[];
    if (count < delims.length()) {
      trimedDelims = new int[count];
      System.arraycopy(delimsCodePoints, 0, trimedDelims, 0, count);
    }
    else {
      trimedDelims = delimsCodePoints;
    }

    int delimsRef = env.newIntArray(trimedDelims);
    stEI.setReferenceField("delims", delimsRef);
    stEI.setIntField("maxCodePoint", maxCodePoint);
  }

  public static void init__Ljava_lang_String_2Ljava_lang_String_2__V(MJIEnv env, int thisRef, int strRef, int delimRef) {
    init__Ljava_lang_String_2Ljava_lang_String_2Z__V(env, thisRef, strRef, delimRef, false);
  }

  public static void init__Ljava_lang_String_2__V(MJIEnv env, int thisRef, int strRef) {
    int delimRef = env.newString(standardDelims);
    init__Ljava_lang_String_2Ljava_lang_String_2Z__V(env, thisRef, strRef, delimRef, false);
  }

  public static boolean hasMoreElements____Z(MJIEnv env, int thisRef) {
    return hasMoreTokens____Z(env, thisRef);
  }

  public static boolean hasMoreTokens____Z(MJIEnv env, int thisRef) {
    ElementInfo stEI = env.getElementInfo(thisRef);
    int strLength = stEI.getIntField("strLength");
    int currentPos = stEI.getIntField("currentPos");

    currentPos = skipDelims(env, stEI, currentPos, strLength);
    stEI.setIntField("currentPos", currentPos);

    return (currentPos < strLength);
  }

  public static int nextElement____Ljava_lang_Object_2(MJIEnv env, int thisRef) {
    return nextToken____Ljava_lang_String_2(env, thisRef);
  }

  public static int nextToken____Ljava_lang_String_2(MJIEnv env, int thisRef) {
    ElementInfo stEI = env.getElementInfo(thisRef);
    int strLength = stEI.getIntField("strLength");
    int curPos = stEI.getIntField("currentPos");
    String str = stEI.getStringField("str");
    
    curPos = skipDelims(env, stEI, curPos, strLength);

    if (curPos < strLength) {
      int end = scanToken(env, stEI, curPos, strLength);
      String token = str.substring(curPos, end);

      stEI.setIntField("currentPos", end);
      return env.newString(token);
    }
    else {
      env.throwException("java.util.NoSuchElementException");
      return MJIEnv.NULL;
    }
  }

  public static int nextToken__Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int thisRef, int newDelimsRef) {
    ElementInfo stEI = env.getElementInfo(thisRef);


    String delims = env.getStringObject(newDelimsRef);
    setDelimsArray(env, stEI, delims);

    return nextToken____Ljava_lang_String_2(env, thisRef);
  }

  private static int skipDelims(MJIEnv env, ElementInfo stEI, int currentPos, int strLength) {
    int delimsRef = stEI.getReferenceField("delims");
    String str = stEI.getStringField("str");
    int maxCodePoint = stEI.getIntField("maxCodePoint");
    int delims[] = env.getHeap().get(delimsRef).asIntArray();
    boolean returnDelims = stEI.getBooleanField("returnDelims");

    int pos = currentPos;

    while (!returnDelims && pos < strLength) {
      int c = str.codePointAt(pos);

      if (c > maxCodePoint || !isDelimeter(delims, c)) {
        break;
      }

      pos += Character.charCount(c);
    }

    return pos;
  }

  private static int scanToken(MJIEnv env, ElementInfo stEI, int currentPos, int strLength) {
    int delimsRef = stEI.getReferenceField("delims");
    String str = stEI.getStringField("str");
    int maxCodePoint = stEI.getIntField("maxCodePoint");
    int delims[] = env.getHeap().get(delimsRef).asIntArray();
    boolean returnDelims = stEI.getBooleanField("returnDelims");

    int pos = currentPos;

    while (pos < strLength) {
      int c = str.codePointAt(pos);
      if (c <= maxCodePoint && isDelimeter(delims, c)) {
        break;
      }

      pos += Character.charCount(c);
    }

    if (returnDelims && pos == currentPos) {
      int c = str.codePointAt(pos);
      if (c <= maxCodePoint && isDelimeter(delims, c)) {
        pos += Character.charCount(c);
      }
    }

    return pos;
  }

  private static boolean isDelimeter(int[] delims, int c) {
    for (int i = 0; i < delims.length; i++) {
      if (delims[i] == c) {
        return true;
      }
    }

    return false;
  }

  public static int countTokens____I(MJIEnv env, int thisRef) {
    ElementInfo stEI = env.getElementInfo(thisRef);
    int currPos = stEI.getIntField("currentPos");
    int strLength = stEI.getIntField("strLength");

    int count = 0;
    while (currPos < strLength) {
      currPos = skipDelims(env, stEI, currPos, strLength);
      if (currPos > strLength) {
        break;
      }

      currPos = scanToken(env, stEI, currPos, strLength);
      count++;
    }

    return count;
  }
}
