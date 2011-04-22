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

package gov.nasa.jpf.util;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.MJIEnv;
import java.lang.reflect.Field;

/**
 * Object transformer from Java objects to JPF objects
 * @author Ivan Mushketik
 */
public class ObjectTransformer {
  // <2do> add support for arrays, and fields of reference types
  public static int JPFObjectFromJavaObject(MJIEnv env, Object javaObject) {
    try {
      Class<?> javaClass = javaObject.getClass();
      String typeName = javaClass.getCanonicalName();
      int newObjRef = env.newObject(typeName);
      ElementInfo newObjEI = env.getElementInfo(newObjRef);

      ClassInfo ci = env.getClassInfo(newObjRef);
      while (ci != null) {
        for (FieldInfo fi : ci.getDeclaredInstanceFields()) {
          if (!fi.isReference()) {
            setJPFPrimitive(newObjEI, fi, javaClass.getDeclaredField(fi.getName()), javaObject);
          }
        }

        ci = ci.getSuperClass();
      }

      return newObjRef;
    }
    catch (Exception ex) {
      throw new JPFException(ex);
    }
  }

  private static void setJPFPrimitive(ElementInfo newObjEI, FieldInfo fi, Field javaField, Object javaObject) {
    try {
      String jpfTypeName = fi.getType();
      javaField.setAccessible(true);

      if (jpfTypeName.equals("char")) {
        newObjEI.setCharField(fi, javaField.getChar(javaObject));
      }
      else if (jpfTypeName.equals("byte")) {
        newObjEI.setByteField(fi, javaField.getByte(javaObject));
      }
      else if (jpfTypeName.equals("short")) {
        newObjEI.setShortField(fi, javaField.getShort(javaObject));
      }
      else if (jpfTypeName.equals("int")) {
        newObjEI.setIntField(fi, javaField.getInt(javaObject));
      }
      else if (jpfTypeName.equals("long")) {
        newObjEI.setLongField(fi, javaField.getLong(javaObject));
      }
      else if (jpfTypeName.equals("float")) {
        newObjEI.setFloatField(fi, javaField.getFloat(javaObject));
      }
      else if (jpfTypeName.equals("double")) {
        newObjEI.setDoubleField(fi, javaField.getDouble(javaObject));
      }
    }
    catch (Exception ex) {
      throw new JPFException(ex);
    }
  }
}
