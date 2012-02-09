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
 * this is a workaround  for static Integer.IntegerCache initialization, which
 * in Java 7 gets its upper bound from a sun.misc.VM method that is not in 
 * Java 6.
 * As an added benefit, we avoid executing a couple hundred instructions that are of
 * lesser interest for verification
 */
public class JPF_java_lang_Integer$IntegerCache {
  
  public static void $clinit (MJIEnv env, int clsObjRef){
    int low = -128;
    int high = 127; // this is the fixed upper bound from Java 6 - we don't support the variable Java 7 bounds yet
    
    int n = (high-low) + 1;
    int aRef = env.newObjectArray("java.lang.Integer", n);
    
    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Integer");
    FieldInfo fiValue = ci.getDeclaredInstanceField("value");
    
    // Integer$IntegerCache is a private inner class, so there should be no way
    // java.lang.Integer isn't initialized yet
    
    int val = low;
    for (int i=0; i<n; i++){
      int eRef = env.newObject( ci);
      ElementInfo eiElem = env.getElementInfo(eRef);
      eiElem.setIntField(fiValue, val++);
      env.setReferenceArrayElement(aRef, i, eRef);
    }
    
    ElementInfo sei = env.getClassElementInfo(clsObjRef);
    sei.setReferenceField("cache", aRef);
    
    FieldInfo fiLow = sei.getFieldInfo("low");
    if (fiLow != null){ // grrr, it's not in Java 6
      sei.setIntField( fiLow, low);
    }
    
    FieldInfo fiHigh = sei.getFieldInfo("high");
    if (fiHigh != null){ // apparently, IcedTea 1.6 doesn't have that one either
      sei.setIntField( fiHigh, high);
    }
  }
}
