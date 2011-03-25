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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * intercept and forward some of the filesystem access methods. This is very
 * slow, if a program uses this heavily we should keep the forwarding File
 * object around and modify the model class accordingly
 */
public class JPF_java_io_File {

  static File getFile(MJIEnv env, int objref) {
    int fnref = env.getReferenceField(objref, "filename");
    String fname = env.getStringObject(fnref);
    return new File(fname);
  }

  static int createJPFFile(MJIEnv env, File file) {
    int newFileRef = env.newObject("java.io.File");
    ElementInfo fileEI = env.getElementInfo(newFileRef);

    int fileNameRef = env.newString(file.getPath());
    fileEI.setReferenceField("filename", fileNameRef);

    return newFileRef;
  }

  public static int getParentFile____Ljava_io_File_2(MJIEnv env, int objref) {
    File thisFile = getFile(env, objref);
    File parent = thisFile.getParentFile();

    return createJPFFile(env, parent);
  }
  
  public static int getAbsolutePath____Ljava_lang_String_2 (MJIEnv env, int objref) {
    String pn = getFile(env,objref).getAbsolutePath();
    return env.newString(pn);
  }

  public static int getCanonicalPath____Ljava_lang_String_2 (MJIEnv env, int objref) {
    try {
      String pn = getFile(env,objref).getCanonicalPath();
      return env.newString(pn);
    } catch (IOException iox) {
      env.throwException("java.io.IOException", iox.getMessage());
      return MJIEnv.NULL;
    }
  }
  
  // internal helper
  @SuppressWarnings("deprecation")
  public static int getURLSpec____Ljava_lang_String_2 (MJIEnv env, int objref){
    try {
      File f = getFile(env,objref);
      URL url = f.toURL();
      return env.newString(url.toString());
    } catch (MalformedURLException mfux) {
      env.throwException("java.net.MalformedURLException", mfux.getMessage());
      return MJIEnv.NULL;
    }
  }

  public static int getURISpec____Ljava_lang_String_2 (MJIEnv env, int objref){
    File f = getFile(env, objref);
    URI uri = f.toURI();
    return env.newString(uri.toString());
  }

  public static boolean exists____Z (MJIEnv env, int objref) {
    return getFile(env,objref).exists();
  }

  public static boolean isDirectory____Z (MJIEnv env, int objref) {
    return getFile(env,objref).isDirectory();
  }

  public static boolean isFile____Z (MJIEnv env, int objref) {
    return getFile(env,objref).isFile();
  }
  
  public static boolean delete____Z (MJIEnv env, int objref) {
    return getFile(env,objref).delete();
  }
  
  public static long length____J (MJIEnv env, int objref) {
    return getFile(env,objref).length();
  }
  
  public static boolean canRead____Z (MJIEnv env, int objref) {
    return getFile(env,objref).canRead();
  }

  public static int list_____3Ljava_lang_String_2(MJIEnv env, int objref){
	  File f=getFile(env,objref);
    if (f.isDirectory()){
      String[] farr=f.list();
      return env.newStringArray(farr);
    } else {
      return MJIEnv.NULL;
    }
  }

  // <2do> ..and lots more
}
