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

import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.ArrayElementValue;
import org.apache.bcel.classfile.ClassElementValue;
import org.apache.bcel.classfile.ElementValue;
import org.apache.bcel.classfile.ElementValuePair;
import org.apache.bcel.classfile.EnumElementValue;
import org.apache.bcel.classfile.SimpleElementValue;

/**
 * the JPF counterpart for Java Annotations
 * 
 * we could just store the bcel constructs, but for consistencies sake, we
 * introduce our own type here (like we do for classes, methods, fields etc)
 */
public class AnnotationInfo {

  public static class Enum {
    String type, field;
    
    Enum (String t, String f){
      type = t;
      field = f;
    }
  }
    
  class Entry {
    String key;
    Object value;
    
    public String getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }
    
    Entry (String key, Object value){
      this.key = key;
      this.value = value;
    }
  }
  
  String name;
  Entry[] entries;
    
  public AnnotationInfo (String name, Entry[] entries){
    this.name = name;
    this.entries = entries;
  }
  
  public AnnotationInfo (AnnotationEntry ae){
    name = Types.getCanonicalTypeName(ae.getAnnotationType()); // it's in slash-notation
    
    ElementValuePair[] evp = ae.getElementValuePairs();
    entries = new Entry[evp.length];
    
    for (int i=0; i<evp.length; i++){
      entries[i] = new Entry(evp[i].getNameString(),
                             getValueObject(evp[i].getValue()));
    }
  }
  
  /**
   * get the ElementValue object, which is either a boxed type for a simple
   * value, a String, a static FieldInfo for enum constants, a ClassInfo
   * for classes, or arrays of all all of the above 
   */
  static Object getValueObject (ElementValue ev){
    switch (ev.getElementValueType()){
    case ElementValue.PRIMITIVE_INT:
      return new Integer(((SimpleElementValue)ev).getValueInt());
    case ElementValue.PRIMITIVE_LONG:
      return new Long(((SimpleElementValue)ev).getValueLong());
    case ElementValue.PRIMITIVE_DOUBLE:
      return new Double(((SimpleElementValue)ev).getValueDouble());
    case ElementValue.PRIMITIVE_FLOAT:
      return new Float(((SimpleElementValue)ev).getValueFloat());
    case ElementValue.PRIMITIVE_SHORT:
      return new Short(((SimpleElementValue)ev).getValueShort());
    case ElementValue.PRIMITIVE_CHAR:
      return new Character(((SimpleElementValue)ev).getValueChar());
    case ElementValue.PRIMITIVE_BYTE:
      return new Byte(((SimpleElementValue)ev).getValueByte());
    case ElementValue.PRIMITIVE_BOOLEAN:
      return Boolean.valueOf(((SimpleElementValue)ev).getValueBoolean());
    case ElementValue.STRING:
      return ((SimpleElementValue)ev).getValueString();
    case ElementValue.ARRAY:
      ElementValue[] a = ((ArrayElementValue)ev).getElementValuesArray();
      Object[] arr = new Object[a.length];
      for (int i=0; i<a.length; i++){
        arr[i] = getValueObject(a[i]);
      }
      return arr;
      
    case ElementValue.ENUM_CONSTANT:
      EnumElementValue eev = (EnumElementValue)ev;
      String etype = Types.getTypeName(eev.getEnumTypeString());
      String eval = eev.getEnumValueString();
      
      ClassInfo eci = ClassInfo.getClassInfo(etype);
      FieldInfo efi = eci.getStaticField(eval);
      
      return efi;
      
    case ElementValue.CLASS:
      ClassElementValue cev = (ClassElementValue)ev;
      String cname = Types.getTypeName(cev.getClassString());
      ClassInfo ci = ClassInfo.getClassInfo(cname);
      return ci;
      
    default:
      return ev.stringifyValue();        
    }
  }
  
  public String getName() {
    return name;
  }

  public Entry[] getEntries() {
    return entries;
  }
  
  // convenience method for single-attribute annotations
  public Object value() {
    return getValue("value");
  }
  
  public String valueAsString(){
    Object v = value();
    return (v != null) ? v.toString() : null;
  }
  
  public String getValueAsString (String key){
    Object v = getValue(key);
    return (v != null) ? v.toString() : null;
  }
  
  public String[] getValueAsStringArray() {
    Object v = value();
    if (v != null && v instanceof Object[]) {
      Object[] va = (Object[])v;
      String[] a = new String[va.length];
      for (int i=0; i<a.length; i++) {
        if (va[i] != null) {
          a[i] = va[i].toString();
        }
      }
      return a;
    }
    
    return null;    
  }
  
  public String[] getValueAsStringArray (String key) {
    // <2do> not very efficient
    Object v = getValue(key);
    if (v != null && v instanceof Object[]) {
      Object[] va = (Object[])v;
      String[] a = new String[va.length];
      for (int i=0; i<a.length; i++) {
        if (va[i] != null) {
          a[i] = va[i].toString();
        }
      }
      return a;
    }
    
    return null;
  }
  
  public boolean getValueAsBoolean (String key){
    Object v = getValue(key);
    return (v != null && v instanceof Boolean) ? ((Boolean)v).booleanValue() : false;
    
  }
  
  public Object getValue (String key){
    for (int i=0; i<entries.length; i++){
      if (entries[i].getKey().equals(key)){
        return entries[i].getValue();
      }
    }
    return null;
  }
  
  public String asString() {
    StringBuilder sb = new StringBuilder();
    sb.append('@');
    sb.append(name);
    sb.append('[');
    for (int i=0; i<entries.length; i++){
      if (i > 0){
        sb.append(',');
      }
      sb.append(entries[i].getKey());
      sb.append('=');
      sb.append(entries[i].getValue());
    }
    sb.append(']');
    
    return sb.toString();
  }
}
