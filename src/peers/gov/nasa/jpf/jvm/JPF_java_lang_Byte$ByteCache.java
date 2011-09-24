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
public class JPF_java_lang_Byte$ByteCache {
  
  public static void $clinit (MJIEnv env, int clsObjRef){
    byte low = -128;
    byte high = 127; // this is the fixed upper bound from Java 6 - we don't support the variable Java 7 bound yet
    
    int n = (high-low) + 1;
    int aRef = env.newObjectArray("java.lang.Byte", n);
    
    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Byte");
    FieldInfo fiValue = ci.getDeclaredInstanceField("value");
    
    // Byte$ByteCache is a private inner class, so there should be no way
    // java.lang.Byte isn't initialized yet
    
    byte val = low;
    for (int i=0; i<n; i++){
      int iRef = env.newObject( ci);
      ElementInfo eiElem = env.getElementInfo(iRef);
      eiElem.setByteField(fiValue, val++);
      env.setReferenceArrayElement(aRef, i, iRef);
    }
    
    ElementInfo sei = env.getClassElementInfo(clsObjRef);
    sei.setReferenceField("cache", aRef);
    
    // ByteCache doesn't have any other fields
  }

}
