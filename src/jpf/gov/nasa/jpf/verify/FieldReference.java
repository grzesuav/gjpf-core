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

package gov.nasa.jpf.verify;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * represents a reference to a static or instance field (test object only).
 * Since it is evaluated AFTER invoking the test object ctor (because we
 * need a test object), i.e. lazily, we have to wrap this into a proxy
 * during parse time
 */
public class FieldReference {

  String id;
  boolean isStatic;
  
  public FieldReference (String id){
    this.id = id;
  }
  
  public static Field getField (Class<?> tgtClass,Object tgtObject,String id) {
    Class<?> cls;
    String fName;
  
    int idx = id.lastIndexOf('.');
    if (idx >=0){ // explicit class qualifier
      String clsName = id.substring(0,idx);
      fName = id.substring(idx+1);
      
      cls = TestContext.resolveClass(tgtClass.getPackage().getName(),clsName);
      if (cls == null){
        return null;
      }
      
    } else { // either instance or static field of the target class
      fName = id;
      if (tgtClass == null){ 
        if (tgtObject != null){
          cls = tgtObject.getClass();
        } else {
          return null; // don't know what to do
        }
      } else {
        cls = tgtClass;
      }
    }
    
    try {
      Field fi = cls.getDeclaredField(fName);
      
      return fi;
      
    } catch (NoSuchFieldException nsfx) {
      return null;
    } catch (SecurityException sx) {
      return null;
    }
  }
    
  public Object getValue (Class<?> tgtClass, Object tgtObject){
    
    Field fi = getField(tgtClass, tgtObject, id);
    if (fi != null){
      fi.setAccessible(true);
      
      if (Modifier.isStatic(fi.getModifiers()) || tgtObject != null){      
        try {
          Object v = fi.get(tgtObject);
          return v;

        } catch (IllegalAccessException iacx){
          return null; // should not happen
        } catch (IllegalArgumentException iarx) {
          return null; // should not happen either
        }
      }
    }
    
    return null;
  }
  
  public String getId() {
    return id;
  }
  
  public String toString() {
    return "field " + id;
  }
}
