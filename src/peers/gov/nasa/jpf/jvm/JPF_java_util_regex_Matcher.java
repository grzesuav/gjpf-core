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

import gov.nasa.jpf.Config;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * native peer for a regex Matcher
 * this is just a delegatee peer
 */
public class JPF_java_util_regex_Matcher {

  static HashMap<Integer, Matcher> matchers;
 
  public static void init (Config conf){
    matchers = new HashMap<Integer,Matcher>();
  }

  static void putInstance (MJIEnv env, int objref, Matcher matcher) {
    int id = env.getIntField(objref,  "id");
    matchers.put(id, matcher);
  }

  static Matcher getInstance (MJIEnv env, int objref) {
    
    int id = env.getIntField(objref,  "id");
    return matchers.get(id);
  }
  
  public static void register____V (MJIEnv env, int objref){
    int patRef = env.getReferenceField(objref, "pattern");
    
    int regexRef = env.getReferenceField(patRef, "regex");
    String regex = env.getStringObject(regexRef);
    int flags = env.getIntField(patRef, "flags");
    
    Pattern pat = Pattern.compile(regex, flags);

    int inputRef = env.getReferenceField(objref, "input");
    String input = env.getStringObject(inputRef);
    
    Matcher matcher = pat.matcher(input);
    putInstance(env, objref, matcher);
  }
  
  public static boolean matches____Z (MJIEnv env, int objref){
    Matcher matcher = getInstance( env, objref);
    return matcher.matches();
  }
  
  public static boolean find____Z (MJIEnv env, int objref){
	Matcher matcher = getInstance( env, objref);
    return matcher.find();
  }
  
  public static int end____I (MJIEnv env, int objref){
	Matcher matcher = getInstance( env, objref);
	return matcher.end();
  }
  
  public static int reset____Ljava_util_regex_Matcher_2 (MJIEnv env, int objref){
    Matcher matcher = getInstance( env, objref);

    int inputRef = env.getReferenceField(objref, "input");
    String input = env.getStringObject(inputRef);
    
    matcher = matcher.reset(input);
    putInstance(env, objref, matcher);
    
    return objref;
  }
  
  public static int group__I__Ljava_lang_String_2 (MJIEnv env, int objref, int i){
    Matcher matcher = getInstance( env, objref);
    String grp = matcher.group(i);
    
    return env.newString(grp);
  }

  public static int quoteReplacement__Ljava_lang_String_2__Ljava_lang_String_2 (MJIEnv env, int clsObjref, int string) {
    String parm = env.getStringObject(string);
    String result = Matcher.quoteReplacement(parm);
    return env.newString(result);
  }

  public static int replaceAll__Ljava_lang_String_2__Ljava_lang_String_2 (MJIEnv env, int objref, int string) {
    Matcher matcher = getInstance(env, objref);
    String replacement = env.getStringObject(string);
    String result = matcher.replaceAll(replacement);

    int resultref = env.newString(result);
    return resultref;
  }
}
