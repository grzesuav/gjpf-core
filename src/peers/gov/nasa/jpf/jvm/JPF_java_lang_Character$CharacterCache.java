//
// Copyright (C) 2011 United States Government as represented by the
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
 * not strictly required since cache size is fixed, but since we have to override
 * the Integer cache initialization (Java 6/7 incompatibility), we can do the
 * same here for optimization purposes
 */
public class JPF_java_lang_Character$CharacterCache {
  
  public static void $clinit (MJIEnv env, int clsObjRef){
    
    short max = 128;
    
    int aRef = env.newObjectArray("java.lang.Character", max);
    
    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Character");
    FieldInfo fiValue = ci.getDeclaredInstanceField("value");
    
    // CharacterCache is a private inner class, so there should be no way
    // java.lang.Character isn't initialized yet
    
    for (short i=0; i<max; i++){
      int eRef = env.newObject( ci);
      ElementInfo eiElem = env.getElementInfo(eRef);
      eiElem.setCharField(fiValue, (char)i);
      env.setReferenceArrayElement(aRef, i, eRef);
    }
    
    ElementInfo sei = env.getClassElementInfo(clsObjRef);
    sei.setReferenceField("cache", aRef);
  }
}
