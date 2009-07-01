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

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;

// NOTE - this only works because DecimalFormat is a Format subclass, i.e.
// the java.text.Format native peer will be initialized first
// (otherwise we shouldn't depend on static data of other native peers)

public class JPF_java_text_DecimalFormat {

  static final int INTEGER_STYLE=0;
  static final int NUMBER_STYLE=1;
       
  static NumberFormat getInstance (MJIEnv env, int objref) {
    Format fmt = JPF_java_text_Format.getInstance(env,objref);
    assert fmt instanceof NumberFormat;
    
    return (NumberFormat)fmt;
  }
  
  /*
   * NOTE: if we would directly intercept the ctors, we would have to
   * explicitly call the superclass ctors here (the 'id' handle gets
   * initialized in the java.text.Format ctor) 
   */
  
  public static void init0____V (MJIEnv env, int objref) {
    DecimalFormat fmt = new DecimalFormat();
    JPF_java_text_Format.putInstance(env,objref,fmt);    
  }
  
  public static void init0__Ljava_lang_String_2__V (MJIEnv env, int objref, int patternref) {
    String pattern = env.getStringObject(patternref);
    
    DecimalFormat fmt = new DecimalFormat(pattern);
    JPF_java_text_Format.putInstance(env,objref,fmt);    
  }
  
  public static void init0__I__V (MJIEnv env, int objref, int style) {
    NumberFormat fmt = null;
    if (style == INTEGER_STYLE) {
      fmt = NumberFormat.getIntegerInstance();
    } else if (style == NUMBER_STYLE) {
      fmt = NumberFormat.getNumberInstance();
    } else {
      // unknown style
      fmt = new DecimalFormat();
    }
    
    JPF_java_text_Format.putInstance(env,objref,fmt);    
  }
  
  public static void setMaximumFractionDigits__I__V (MJIEnv env, int objref, int newValue){
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      fmt.setMaximumFractionDigits(newValue);
    }
  }
  public static void setMaximumIntegerDigits__I__V (MJIEnv env, int objref, int newValue){
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      fmt.setMaximumIntegerDigits(newValue);
    }
  }
  public static void setMinimumFractionDigits__I__V (MJIEnv env, int objref, int newValue){
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      fmt.setMinimumFractionDigits(newValue);
    }
  }
  public static void setMinimumIntegerDigits__I__V (MJIEnv env, int objref, int newValue){
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      fmt.setMinimumIntegerDigits(newValue);
    }
  }
  
  public static int format__J__Ljava_lang_String_2 (MJIEnv env, int objref, long number) {
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      String s = fmt.format(number);
      int sref = env.newString(s);
      return sref;
    }
    
    return MJIEnv.NULL;
  }
  
  public static int format__D__Ljava_lang_String_2 (MJIEnv env, int objref, double number) {
    NumberFormat fmt = getInstance(env,objref);
    if (fmt != null) {
      String s = fmt.format(number);
      int sref = env.newString(s);
      return sref;
    }
    
    return MJIEnv.NULL;
  }

}
