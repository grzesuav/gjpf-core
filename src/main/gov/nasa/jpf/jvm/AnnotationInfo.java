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

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.classfile.ClassFile;
import gov.nasa.jpf.classfile.ClassFileException;
import gov.nasa.jpf.classfile.ClassFileReaderAdapter;
import gov.nasa.jpf.classfile.ClassPath;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.bcel.classfile.AnnotationDefault;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.ArrayElementValue;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassElementValue;
import org.apache.bcel.classfile.ElementValue;
import org.apache.bcel.classfile.ElementValuePair;
import org.apache.bcel.classfile.EnumElementValue;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.SimpleElementValue;

/**
 * the JPF counterpart for Java Annotations
 * 
 * we could just store the bcel constructs, but for consistencies sake, we
 * introduce our own enumClassName here (like we do for classes, methods, fields etc)
 */
public class AnnotationInfo {

  static final Entry[] NONE = new Entry[0];
  
  // we have to jump through a lot of hoops to handle default annotation parameter values
  // this is not ideal, since it causes the classfile to be re-read if the SUT
  // uses annotation reflection (which creates a ClassInfo), but this is rather
  // exotic, so we save some time by not creating a ClassInfo (which would hold
  // the default vals as method annotations) and directly store the default values here
  static HashMap<String,Entry[]> defaultEntries = new HashMap<String,Entry[]>();

  static class DefaultValueCollector extends ClassFileReaderAdapter {
    String annotationName;

    String key;
    Object[] valElements;
    ArrayList<Entry> entries;

    Entry[] getDefaultValueEntries() {
      if (entries == null){
        return NONE;
      } else {
        return entries.toArray(new Entry[entries.size()]);
      }
    }

    public void setClass(ClassFile cf, String clsName, String superClsName, int flags, int cpCount) {
      entries = null;
      annotationName = Types.getClassNameFromTypeName(clsName);
      if (!"java/lang/Object".equals(superClsName)){
        throw new JPFException("illegal annotation superclass of: " + annotationName + " is " + superClsName);
      }
    }

    public void setInterface(ClassFile cf, int ifcIndex, String ifcName) {
      if (!"java/lang/annotation/Annotation".equals(ifcName)){
        throw new JPFException("illegal annotation interface of: " + annotationName + " is " + ifcName);
      }
    }

    public void setMethod(ClassFile cf, int methodIndex, int accessFlags, String name, String descriptor) {
      key = name;
    }

    public void setMethodAttribute(ClassFile cf, int methodIndex, int attrIndex, String name, int attrLength) {
      if (name == ClassFile.ANNOTATIONDEFAULT_ATTR){
        if (entries == null){
          entries = new ArrayList<Entry>();
        }
        cf.parseAnnotationDefaultAttr(this, key);
      }
    }

    public void setMethodsDone(ClassFile cf) {
      cf.stopParsing();
    }

    public void setPrimitiveAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int arrayIndex, Object val) {
      if (arrayIndex >=0){
        valElements[arrayIndex] = val;
      } else {
        entries.add(new Entry(key,val));
      }
    }

    public void setStringAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int arrayIndex, String val) {
      if (arrayIndex >=0){
        valElements[arrayIndex] = val;
      } else {
        entries.add(new Entry(key,val));
      }
    }

    public void setClassAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int arrayIndex, String typeName) {
    }

    public void setEnumAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int arrayIndex, String enumType, String enumValue) {
    }

    public void setAnnotationValueElementCount(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int elementCount) {
      valElements = new Object[elementCount];
    }

    public void setAnnotationValueElementsDone(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName) {
      entries.add( new Entry(key, valElements));
    }

  }

  static DefaultValueCollector valueCollector = new DefaultValueCollector();

  public static Entry[] getDefaultEntries (ClassPath locator, String annotationType){

    Entry[] def = defaultEntries.get(annotationType);

    if (def == null){ // Annotation not seen yet - we have to dig it out from the classfile
      try {
        byte[] data = locator.getClassData(annotationType);
        ClassFile cf = new ClassFile(data);
        cf.parse(valueCollector);

        def = valueCollector.getDefaultValueEntries();
        defaultEntries.put(annotationType, def);

      } catch (ClassFileException cfx){
        throw new JPFException("malformed annotation classfile");
      }

    }

    return def;
  }



  static Entry[] getDefaultEntries (String annotationType){

    Entry[] def = defaultEntries.get(annotationType);

    if (def == null){ // Annotation not seen yet - we have to dig it out from the classfile
      JavaClass acls = ClassInfo.getJavaClass(annotationType);
      if (acls != null){
        ArrayList<Entry> list = null;
        for (Method m : acls.getMethods()) {
          for (Attribute a : m.getAttributes()) {
            if ("AnnotationDefault".equals(a.getName())) {
              if (list == null) {
                list = new ArrayList<Entry>();
              }

              AnnotationDefault ad = (AnnotationDefault) a;
              ElementValue val = ad.getDefaultValue();

              list.add(new Entry(m.getName(), getValueObject(val)));
            }
          }
        }
        if (list != null && !list.isEmpty()) {
          def = list.toArray(new Entry[list.size()]);
        } else {
          def = NONE;
        }
        defaultEntries.put(annotationType, def);

      } else {
        // <2do> maybe we should raise an exception, but apparently simple
        // occurrence of an annotation doesn't load the class in a normal VM
        ClassInfo.logger.warning("annotation type not found: " + annotationType);
        def = NONE;
      }
    }

    return def;
  }



  public static class EnumValue {
    String eClassName;
    String eConst;
    
    EnumValue (String clsName, String constName){
      eClassName = clsName;
      eConst = constName;
    }
    public String getEnumClassName(){
      return eClassName;
    }
    public String getEnumConstName(){
      return eConst;
    }
    public String toString(){
      return eClassName + '.' + eConst;
    }
  }

  public static class ClassValue {
    String name;

    ClassValue (String cn){
      name = cn;
    }

    public String getName(){
      return name;
    }
    public String toString(){
      return name;
    }
  }

  public static Object getEnumValue(String eType, String eConst){
    return new EnumValue( Types.getClassNameFromTypeName(eType), eConst);
  }

  public static Object getClassValue(String type){
    return new ClassValue( Types.getClassNameFromTypeName(type));
  }

  
  static class Entry {
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


  protected AnnotationInfo(String name){
    this.name = name;
    // entries will follow later, so this object is only partially initialized
  }

  protected void startEntries (int count){
    entries = new Entry[count];
  }

  protected void setValue(int index, String key, Object value){
    entries[index] = new Entry(key,value);
  }



  public AnnotationInfo (String name, Entry[] entries){
    this.name = name;
    this.entries = entries;
  }
  
  public AnnotationInfo (AnnotationEntry ae){
    name = Types.getClassNameFromTypeName(ae.getAnnotationType()); // it's in slash-notation
    ArrayList<Entry> list = new ArrayList<Entry>();

    // those are only the explicitly provided parameters
    ElementValuePair[] evp = ae.getElementValuePairs();
    
    for (int i = 0; i < evp.length; i++) {
      String key = evp[i].getNameString();
      ElementValue eval = evp[i].getValue();
      Object val = getValueObject(eval);

      list.add(new Entry(key, val));
    }

    // now we have to check if there are any default parameters that are not overridden
    for (Entry d : getDefaultEntries(name)){
      boolean overridden = false;
      int n = list.size();
      for (int j=0; j<n; j++){
        Entry e = list.get(j);
        if (e.key.equals(d.key)){
          overridden = true;
          break;
        }
      }
      if (!overridden){
        list.add(d);
      }
    }
    
    entries = list.toArray(new Entry[list.size()]);
  }
  
  /**
   * get the ElementValue object, which is either a boxed enumClassName for a simple
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
      String etype = Types.getClassNameFromTypeName(eev.getEnumTypeString());
      String eval = eev.getEnumValueString();
      return getEnumValue(etype, eval);
      
    case ElementValue.CLASS:
      ClassElementValue cev = (ClassElementValue)ev;
      String cname = Types.getClassNameFromTypeName(cev.getClassString());
      return getClassValue(cname);
      
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
    String a[] = null; 
    Object v = value();
    if (v != null && v instanceof Object[]) {
      Object[] va = (Object[])v;
      a = new String[va.length];
      for (int i=0; i<a.length; i++) {
        if (va[i] != null) {
          a[i] = va[i].toString();
        }
      }
    }
    
    return a;    
  }
  
  public String[] getValueAsStringArray (String key) {
    // <2do> not very efficient
    String a[] = null; 
    Object v = getValue(key);
    if (v != null && v instanceof Object[]) {
      Object[] va = (Object[])v;
      a = new String[va.length];
      for (int i=0; i<a.length; i++) {
        if (va[i] != null) {
          a[i] = va[i].toString();
        }
      }
    }
    
    return a;
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
