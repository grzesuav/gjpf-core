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

import java.lang.reflect.Modifier;

import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Signature;
import org.apache.bcel.classfile.Field;


/**
 * type, name and attribute information of a field.
 */
public abstract class FieldInfo extends InfoObject {

  //--- FieldInfo attributes
  // don't break transitions on get/putXX insns of this field, even if shared
  static final int NEVER_BREAK = 0x10000;
  
  // always break on this field's access if object is shared
  // (ignored if NEVER_BREAK is set)
  static final int BREAK_SHARED = 0x20000;

  
  protected final String name;
  protected final String type;
  protected final String genericSignature;
  protected int storageSize;

  protected final ClassInfo ci; // class this field belongs to
  protected final ConstantValue cv; // the optional initializer value for this field.
  final int fieldIndex; // declaration ordinal

  // where in the corresponding Fields object do we store the value
  // (note this works because of the wonderful single inheritance)
  final int storageOffset;

  protected int modifiers;

  // high 16 bit: non-propagation relevant field attributes
  // low 16 bit: object attribute propagation mask  (non-final!)
  int attributes = ElementInfo.ATTR_PROP_MASK;

  protected static String computeGenericSignature(Field f) {
    Attribute attribs[] = f.getAttributes();
    for (int i = attribs.length; --i >= 0; ) {
      if (attribs[i] instanceof Signature) {
        Signature signature = (Signature) attribs[i];
        return signature.getSignature();
      }
    }
     
    return "";
  }

  /**
    * factory method for the various concrete FieldInfos
    */
  public static FieldInfo create (Field f, ClassInfo ci, int idx, int off) {
    String name = f.getName();
    String type = Types.getCanonicalTypeName(f.getType().toString());
    ConstantValue cv = f.getConstantValue();
    int modifiers = f.getModifiers();
    String genericSignature = computeGenericSignature(f);
    FieldInfo ret = create(name, type, genericSignature, modifiers, cv, ci, idx, off);
    ret.loadAnnotations(f.getAnnotationEntries());

    return ret;
  }

  @Deprecated
  public static FieldInfo create (String name, String type, int modifiers,
                                  ConstantValue cv, ClassInfo ci, int idx, int off){
    return create(name, type, "", modifiers, cv, ci, idx, off);
  }
  
  public static FieldInfo create (String name, String type, String genericSignature, int modifiers,
                                  ConstantValue cv, ClassInfo ci, int idx, int off){
     
    FieldInfo ret;

    if ("boolean".equals(type) ||
        "byte".equals(type) ||
        "char".equals(type) ||
        "short".equals(type) ||
        "int".equals(type)){
      ret = new IntegerFieldInfo(name, type, genericSignature, modifiers, cv, ci, idx, off);
    } else if ("long".equals(type)){
      ret = new LongFieldInfo(name, type, genericSignature, modifiers, cv, ci, idx, off);
    } else if ("double".equals(type)){
      ret = new DoubleFieldInfo(name, type, genericSignature, modifiers, cv, ci, idx, off);
    } else if ("float".equals(type)){
      ret = new FloatFieldInfo(name, type, genericSignature, modifiers, cv, ci, idx, off);
    } else {
      ret = new ReferenceFieldInfo(name, type, genericSignature, modifiers, cv, ci, idx, off);
    }

    return ret;
  }

  /*
  @Deprecated
  protected FieldInfo (String name, String type, int modifiers,
                       ConstantValue cv, ClassInfo ci, int idx, int off) {
    this(name, type, "", modifiers, cv, ci, idx, off); 
  }
  */

  protected FieldInfo(String name, String type, String genericSignature, int modifiers, 
                       ConstantValue cv, ClassInfo ci, int idx, int off) {
    this.name = name;
    this.type = type;
    this.ci = ci;
    this.cv = cv;
    this.fieldIndex = idx;
    this.storageOffset = off;
    this.modifiers = modifiers;
    this.genericSignature = genericSignature;
  }


  public abstract String valueToString (Fields f);


  /**
   * Returns the class that this field is associated with.
   */
  public ClassInfo getClassInfo () {
    return ci;
  }

  public ConstantValue getConstantValue () {
    return cv;
  }

  public abstract Object getValueObject (Fields data);

  public int getModifiers() {
    return modifiers;
  }

  public int getFieldIndex () {
    return fieldIndex;
  }

  public boolean isReference () {
    return false;
  }

  public boolean isArrayField () {
    return false;
  }

  /**
   * is this a static field? Counter productive to the current class struct,
   * but at some point we want to get rid of the Dynamic/Static branch (it's
   * really just a field attribute)
   */
  public boolean isStatic () {
    return (modifiers & Modifier.STATIC) != 0;
  }

  /**
   * is this field declared `final'?
   */
  public boolean isFinal () {
    return (modifiers & Modifier.FINAL) != 0;
  }

  public boolean isVolatile () {
    return (modifiers & Modifier.VOLATILE) != 0;
  }

  public boolean isTransient () {
    return (modifiers & Modifier.TRANSIENT) != 0;
  }

  public boolean isPublic () {
    return (modifiers & Modifier.PUBLIC) != 0;
  }


  /**
   * Returns the name of the field.
   */
  public String getName () {
    return name;
  }

  /**
   * @return the storage size of this field, @see Types.getTypeSize
   */
  public int getStorageSize () {
    return 1;
  }

  /**
   * Returns the type of the field.
   */
  public String getType () {
    return type;
  }
  
  public String getGenericSignature() {
    return genericSignature; 
  }
  
  public ClassInfo getTypeClassInfo () {
    return ClassInfo.getResolvedClassInfo(type);
  }

  public Class<? extends ChoiceGenerator<?>> getChoiceGeneratorType (){
    return null;
  }

  /**
   * pushClinit the corresponding data in the provided Fields instance
   */
  public abstract void initialize (ElementInfo ei);


  /**
   * Returns a string representation of the field.
   */
  public String toString () {
    StringBuilder sb = new StringBuilder();

    if (isStatic()) {
      sb.append("static ");
    }
    if (isFinal()) {
      sb.append("final ");
    }

    //sb.append(Types.getTypeName(type));
    sb.append(type);
    sb.append(' ');
    sb.append(ci.getName());
    sb.append('.');
    sb.append(name);

    return sb.toString();
  }

  void setAttributes (int a) {
    attributes = a;
  }

  public int getAttributes () {
    return attributes;
  }

  public boolean breakShared() {
    return ((attributes & BREAK_SHARED) != 0);
  }
  
  public boolean neverBreak() {
    return ((attributes & NEVER_BREAK) != 0);    
  }
  
  public int getStorageOffset () {
    return storageOffset;
  }

  public String getFullName() {
    return ci.getName() + '.' + name;
  }

  public boolean isUntracked () {
    return getAnnotation("gov.nasa.jpf.jvm.untracked.UntrackedField") != null;
  }
}
