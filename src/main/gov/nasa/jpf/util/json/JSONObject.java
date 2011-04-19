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

package gov.nasa.jpf.util.json;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.MJIEnv;
import java.util.HashMap;

/**
 * Object parsed from JSON document.
 * @author Ivan Mushketik
 */
public class JSONObject{

  private HashMap<String, Value> keyValues = new HashMap<String, Value>();
  private HashMap<String, CGCall> cgCalls = new HashMap<String, CGCall>();

  void addValue(String key, Value value) {
    if (keyValues.containsKey(key)) {
      throw new JPFException("Attempt to add two nodes with the same key in JSON object");
    }

    keyValues.put(key, value);
  }

  /**
   * Get value read from JSON document with specified key.
   * @param key - value's key.
   * @return read value.
   */
  public Value getValue(String key) {
    return keyValues.get(key);
  }

  public void addCGCall(String key, CGCall cgCall) {
    if (cgCalls.containsKey(key)) {
      throw new JPFException("Attempt to add two CG with the same key in JSON object");
    }

    cgCalls.put(key, cgCall);
  }

  public CGCall getCGCall(String key) {
    return cgCalls.get(key);
  }


  //--- the fillers
  public int fillObject(MJIEnv env, String typeName) {

    int newObjRef = env.newObject(typeName);
    ElementInfo ei = env.getHeap().get(newObjRef);

    if (ei == null) {
      throw new JPFException("No such class " + typeName);
    }

    ClassInfo ci = ei.getClassInfo();

    // Fill all fields for this class until it has a super class
    while (ci != null) {
      FieldInfo[] fields = ci.getDeclaredInstanceFields();

      for (FieldInfo fi : fields) {
        String fieldName = fi.getName();
        Value val = getValue(fieldName);

        // If a value was defined in JSON document
        if (val != null) {
          // Handle primitive types
          if (!fi.isReference()) {
            fillPrimitive(ei, fi, val);
          }
          else {
            // Field is not of the primitive type, try to handle special classes
            Creator creator = CreatorsFactory.getCreator(fi.getType());

            if (creator != null) {
              int newSubObjRef = creator.create(env, fi.getType(), val);
              ei.setReferenceField(fi, newSubObjRef);
            // Not a special case. Fill it recursively
            } else {
              String subTypeName = fi.getType();
              JSONObject jsonObj = val.getObject();
              int fieldRef = MJIEnv.NULL;
              if (jsonObj != null){
                fieldRef = jsonObj.fillObject(env, subTypeName);
              }

              ei.setReferenceField(fi.getName(), fieldRef);
            }
          }
        }
      }

      ci = ci.getSuperClass();
    }

    return newObjRef;
  }

  private static void fillPrimitive(ElementInfo ei, FieldInfo fi, Value val) {
    String primitiveName = fi.getType();

    if (primitiveName.equals("boolean")) {
      ei.setBooleanField(fi, val.getBoolean());
    }
    else if (primitiveName.equals("byte")) {
      ei.setByteField(fi, val.getDouble().byteValue());
    }
    else if (primitiveName.equals("short")) {
      ei.setShortField(fi, val.getDouble().shortValue());
    }
    else if (primitiveName.equals("int")) {
      ei.setIntField(fi, val.getDouble().intValue());
    }
    else if (primitiveName.equals("long")) {
      ei.setLongField(fi, val.getDouble().longValue());
    }
    else if (primitiveName.equals("float")) {
      ei.setFloatField(fi, val.getDouble().floatValue());
    }
    else if (primitiveName.equals("double")) {
      ei.setDoubleField(fi, val.getDouble());
    }
  }

}
