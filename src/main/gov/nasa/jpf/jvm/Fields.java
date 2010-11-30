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
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.Misc;


/**
 * Represents the variable, hash-collapsed pooled data associated with an object
 * that is related to the object values (as opposed to synchronization ->Monitor).
 * Contains the values of the fields, not their descriptors.  Descriptors are represented
 * by gov.nasa.jpf.jvm.FieldInfo objects, which are stored in the ClassInfo structure.
 *
 * @see gov.nasa.jpf.jvm.FieldInfo
 * @see gov.nasa.jpf.jvm.Monitor
 */
public abstract class Fields implements Cloneable {

  /**
   * we use this to store arbitrary field attributes (like symbolic values),
   * but only pushClinit this on demand
   */
  protected Object[] fieldAttrs;

  /**
   * attribute attached to the object as a whole
   */
  protected Object objectAttr;


  protected Fields() {}

  public boolean hasFieldAttrs() {
    return fieldAttrs != null;
  }

  public boolean hasFieldAttrs (Class<?> attrType){
    Object[] fa = fieldAttrs;
    if (fa != null){
      for (int i=0; i<fa.length; i++){
        Object a = fa[i];
        if (a != null && attrType.isAssignableFrom(a.getClass())){
          return true;
        }
      }
    }
    return false;
  }

  /**
   * set the (optional) attribute for a field
   *
   * note that the provided fieldIndex is the ordinal of the field, not
   * an index into values (a long field occupies two values slots)
   */
  public void setFieldAttr (int nFieldsOrElements, int fieldOrElementIndex, Object attr){
    if (fieldAttrs == null){
      if (attr == null){
        return; // no need to waste an array object for storing null
      }
      fieldAttrs = new Object[nFieldsOrElements];
    }
    fieldAttrs[fieldOrElementIndex] = attr;
  }

  public <T> T getFieldAttr (Class<T> attrType, int fieldOrElementIndex){
    if (fieldAttrs != null){
      Object a = fieldAttrs[fieldOrElementIndex];
      if (a != null && attrType.isAssignableFrom(a.getClass())){
        return (T) a;
      }
    }

    return null;
  }

  // supposed to return all
  public Object getFieldAttr (int fieldOrElementIndex){
    if (fieldAttrs != null){
      return fieldAttrs[fieldOrElementIndex];
    }
    return null;
  }

  public boolean hasObjectAttr () {
    return (objectAttr != null);
  }

  public boolean hasObjectAttr (Class<?> attrType){
    return objectAttr != null && attrType.isAssignableFrom(objectAttr.getClass());
  }

  public void setObjectAttr (Object attr){
    objectAttr = attr;
  }

  public <T> T getObjectAttr (Class<T> attrType) {
    if (objectAttr != null && attrType.isAssignableFrom(objectAttr.getClass())){
      return (T)objectAttr;
    }
    return null;
  }

  // supposed to return all
  public Object getObjectAttr () {
    return objectAttr;
  }

  public abstract int[] asFieldSlots();

  /**
   * give an approximation of the heap size in bytes - we assume fields are word
   * aligned, hence the number of values*4 should be good. Note that this is
   * overridden by ArrayFields (arrays would be packed)
   */
  public abstract int getHeapSize ();


  public boolean isReferenceArray () {
    return false;
  }

  // our low level getters and setters
  public abstract int getIntValue (int index);

  // same as getIntValue(), just here to make intentions clear
  public abstract int getReferenceValue (int index);

  public abstract long getLongValue (int index);

  public abstract boolean getBooleanValue (int index);

  public abstract byte getByteValue (int index);

  public abstract char getCharValue (int index);

  public abstract short getShortValue (int index);

  public abstract float getFloatValue (int index);

  public abstract double getDoubleValue (int index);

  //--- the field modifier methods (both instance and static)

  public abstract void setReferenceValue (int index, int newValue);

  public abstract void setBooleanValue (int index, boolean newValue);

  public abstract void setByteValue (int index, byte newValue);

  public abstract void setCharValue (int index, char newValue);

  public abstract void setShortValue (int index, short newValue);

  public abstract void setFloatValue (int index, float newValue);

  public abstract void setIntValue (int index, int newValue);

  public abstract void setLongValue (int index, long newValue);

  public abstract void setDoubleValue (int index, double newValue);

  public abstract Fields clone ();

  protected Fields cloneFields() {
    try {
      Fields f = (Fields)super.clone();

      if (fieldAttrs != null) {
        f.fieldAttrs = fieldAttrs.clone();
      }

      if (objectAttr != null) {
        f.objectAttr = objectAttr; //
      }

      return f;
    } catch (CloneNotSupportedException cnsx){
      return null;
    }
  }

  public abstract boolean equals(Object o);

  protected boolean compareAttrs(Fields f) {
    if (fieldAttrs != null || f.fieldAttrs != null) {
      if (!Misc.compare(fieldAttrs, f.fieldAttrs)) {
        return false;
      }
    }

    if (objectAttr != null) {
      if (!objectAttr.equals(f.objectAttr)) {
        return false;
      }
    } else if (f.objectAttr != null) {
      return false;
    }

    return true;
  }

  // serialization interface
  public abstract void appendTo(IntVector v);


  public int hashCode () {
    HashData hd = new HashData();
    hash(hd);
    hashAttrs(hd);
    return hd.getValue();
  }

  public abstract void hash(HashData hd);

  /**
   * Adds some data to the computation of an hashcode.
   */
  public void hashAttrs (HashData hd) {

    // it's debatable if we add the attributes to the state, but whatever it
    // is, it should be kept consistent with the StackFrame.hash()
    Object[] a = fieldAttrs;
    if (a != null) {
      for (int i=0, l=a.length; i < l; i++) {
        hd.add(a[i]);
      }
    }

    if (objectAttr != null){
      hd.add(objectAttr);
    }
  }


  public void copyAttrs(Fields other) {
    if (other.fieldAttrs != null){
      if (fieldAttrs == null){
        fieldAttrs = other.fieldAttrs.clone();
      } else {
        System.arraycopy(other.fieldAttrs, 0, fieldAttrs, 0, fieldAttrs.length);
      }
    }

    objectAttr = other.objectAttr;
  }
}
