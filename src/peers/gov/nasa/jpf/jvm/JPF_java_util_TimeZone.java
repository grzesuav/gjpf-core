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

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class JPF_java_util_TimeZone {

  static TimeZone getTimeZone(MJIEnv env, int tzRef) {
    int idRef = env.getReferenceField(tzRef, "ID");
    String id = env.getStringObject(idRef);
    
    return TimeZone.getTimeZone(id);
  }
  
  public static int getDisplayName__ZILjava_util_Locale_2__Ljava_lang_String_2 (MJIEnv env, int objRef,
                                       boolean daylight, int style, int localeRef) {
    TimeZone tz = getTimeZone(env, objRef);
    Locale displayLocale = JPF_java_util_Locale.getLocale(env, localeRef);
    String s = tz.getDisplayName(daylight, style, displayLocale);
    
    int sref = env.newString(s);
    return sref;
  }

  public static int getTimeZone__Ljava_lang_String_2__Ljava_util_TimeZone_2 (MJIEnv env, int clsRef,
                                                                              int idRef){
    String id = env.getStringObject(idRef);
    TimeZone tz = TimeZone.getTimeZone(id);
    
    id = tz.getID();
    idRef = env.newString(id);
    
    int tzRef = env.newObject("java.util.TimeZone");
    env.setReferenceField(tzRef, "ID", idRef);
    
    return tzRef;
  }
  
  public static boolean inDaylightTime__Ljava_util_Date_2__Z (MJIEnv env, int objRef, int dateRef){
    TimeZone tz = getTimeZone(env, objRef);
    Date date = JPF_java_util_Date.getDate(env, dateRef);
    return tz.inDaylightTime(date);
  }
  
  public static int getRawOffset____I (MJIEnv env, int objRef){
    TimeZone tz = getTimeZone(env, objRef);
    return tz.getRawOffset();
  }

  // used from within the private setDefaultZone()
  public static int getSystemGMTOffsetID____Ljava_lang_String_2 (MJIEnv env, int clsObjRef){
    // unfortunately this is private, so we have to use reflection
    try {
      Method m = TimeZone.class.getMethod("getSystemGMTOffsetID");
      m.setAccessible(true);
      String s = (String)m.invoke(null);
      
      return env.newString(s);
      
    } catch (Throwable t){
      return MJIEnv.NULL;
    }
  }
}

