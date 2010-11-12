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
  static int FATTR_MASK = 0xffff; // pass all propagated attributes

  /** Type of the object or class */
  protected final String type;

  /** the class of this object */
  protected final ClassInfo ci;

  /** this is where we store the instance data. should be final but we set it
   * during a clone
   */
  protected int[] values;

  /**
   * we use this to store arbitrary field attributes (like symbolic values),
   * but only pushClinit this on demand
   */
  protected Object[] fieldAttrs;

  /**
   * attribute attached to the object as a whole
   */
  protected Object objectAttr;

  protected Fields (String type, ClassInfo ci, int dataSize) {
    this.type = type;
    this.ci = ci;

    values = new int[dataSize];
  }

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
  public void setFieldAttr (int fieldOrElementIndex, Object attr){
    if (fieldAttrs == null){
      if (attr == null){
        return; // no need to waste an array object for storing null
      }
      fieldAttrs = new Object[getNumberOfFieldsOrElements()];
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

  int getNumberOfFieldsOrElements () {
    return getNumberOfFields();
  }

  /**
   * give an approximation of the heap size in bytes - we assume fields a word
   * aligned, hence the number of fields*4 should be good. Note that this is
   * overridden by ArrayFields (arrays would be packed)
   */
  public int getHeapSize () {
    return values.length * 4;
  }

  /**
   * do we have a reference field with value objRef? This is used by
   * the reachability analysis
   */
  public boolean hasRefField (int objRef) {
    return ci.hasRefField( objRef, this);
  }

  /**
   * Returns true if the fields belong to an array.
   */
  public boolean isArray () {
    return Types.getBaseType(type) == Types.T_ARRAY;
  }

  public boolean isReferenceArray () {
    return false;
  }

  /**
   * Returns a reference to the class information.
   */
  public ClassInfo getClassInfo () {
    return ci;
  }

  public abstract int getNumberOfFields ();
  // NOTE - fieldIndex (ClassInfo) != storageOffset (Fields). We *don't pad anymore!
  public abstract FieldInfo getFieldInfo (int fieldIndex);

  // our low level getters and setters
  public int getIntValue (int index) {
    return values[index];
  }

  public boolean isEqual (Fields other, int off, int len, int otherOff) {
    int iEnd = off + len;
    int jEnd = otherOff + len;
    int[] v = other.values;

    if ((iEnd > values.length) || (jEnd > v.length)) {
      return false;
    }

    for (int i=off, j=otherOff; i<iEnd; i++, j++) {
      if (values[i] != v[j]) {
        return false;
      }
    }

    return true;
  }

  // same as above, just here to make intentions clear
  public int getReferenceValue (int index) {
    return values[index];
  }

  public long getLongValue (int index) {
    return Types.intsToLong(values[index + 1], values[index]);
  }

  public boolean getBooleanValue (int index) {
    return Types.intToBoolean(values[index]);
  }

  public byte getByteValue (int index) {
    return (byte) values[index];
  }

  public char getCharValue (int index) {
    return (char) values[index];
  }

  public short getShortValue (int index) {
    return (short) values[index];
  }

  // beware, this is only for internal use, to increase efficiency
  protected int[] getValues() {
    return values;
  }

  //--- the field modifier methods (both instance and static)

  public void setReferenceValue (ElementInfo ei, int index, int newValue) {
    values[index] = newValue;
  }

  public void setBooleanValue (ElementInfo ei, int index, boolean newValue) {
    values[index] = newValue ? 1 : 0;
  }

  public void setByteValue (ElementInfo ei, int index, byte newValue) {
    values[index] = newValue;
  }

  public void setCharValue (ElementInfo ei, int index, char newValue) {
    values[index] = (int)newValue;
  }

  public void setShortValue (ElementInfo ei, int index, short newValue) {
    values[index] = newValue;
  }

  public void setFloatValue (ElementInfo ei, int index, float newValue) {
    values[index] = Types.floatToInt(newValue);
  }

  public void setIntValue (ElementInfo ei, int index, int newValue) {
    values[index] = newValue;
  }

  public void setLongValue (ElementInfo ei, int index, long newValue) {
		values[index++] = Types.hiLong(newValue);
    values[index] = Types.loLong(newValue);
  }

  public void setDoubleValue (ElementInfo ei, int index, double newValue) {
    values[index++] = Types.hiDouble(newValue);
    values[index] = Types.loDouble(newValue);
  }


  public float getFloatValue (int index) {
    return Types.intToFloat(values[index]);
  }

  public double getDoubleValue (int index) {
    return Types.intsToDouble( values[index+1], values[index]);
  }




  /**
   * Returns the type of the object or class associated with the fields.
   */
  public String getType () {
    return type;
  }

  /**
   * Creates a clone.
   */
  public Fields clone () {
    Fields f;

    try {
      f = (Fields) super.clone();
      f.values = values.clone();

      // NOTE - attribute objects are not cloned automatically - if you
      // change them and they need to preserve state, you have to do this explicitly

      if (fieldAttrs != null){
        f.fieldAttrs = fieldAttrs.clone();
      }

      if (objectAttr != null){
        f.objectAttr = objectAttr; //
      }
    } catch (CloneNotSupportedException e) {
      throw new InternalError(e.getMessage());
    }

    return f;
  }

  /**
   * Checks for equality.
   */
  public boolean equals (Object o) {
    if (o == null || !(o instanceof Fields)) {
      return false;
    }

    Fields f = (Fields) o;

    if (!type.equals(f.type) || (ci != f.ci)) {
      return false;
    }

    //--- check values
    int[] v1 = values;
    int[] v2 = f.values;
    int   l = v1.length;
    if (l != v2.length) {
      return false;
    }
    for (int i = 0; i < l; i++) {
      if (v1[i] != v2[i]) {
        return false;
      }
    }

    if (fieldAttrs != null || f.fieldAttrs != null){
      if (!Misc.compare(getNumberOfFieldsOrElements(), fieldAttrs, f.fieldAttrs)) {
        return false;
      }
    }
    
    if (objectAttr != null){
      if (!objectAttr.equals(f.objectAttr)){
        return false;
      }
    } else if (f.objectAttr != null){
      return false;
    }

    return true;
  }

  public void copyTo(IntVector v) {
    v.append(values);
  }



  public int arrayLength () {
    // re-implemented by ArrayFields
    throw new JPFException ("attempt to get length of non-array: " + ci.getName());
  }

  public boolean[] asBooleanArray () {
    throw new JPFException( "not an array object: " + ci.getName());
  }
  public byte[] asByteArray () {
    throw new JPFException( "not an array object: " + ci.getName());
  }
  public char[] asCharArray () {
    throw new JPFException( "not an array object: " + ci.getName());
  }
  public short[] asShortArray () {
    throw new JPFException( "not an array object: " + ci.getName());
  }
  public int[] asIntArray () {
    throw new JPFException( "not an array object: " + ci.getName());
  }
  public long[] asLongArray () {
    throw new JPFException( "not an array object: " + ci.getName());
  }
  public float[] asFloatArray () {
    throw new JPFException( "not an array object: " + ci.getName());
  }
  public double[] asDoubleArray () {
    throw new JPFException( "not an array object: " + ci.getName());
  }

  /**
   * Computes an hash code.
   */
  public int hashCode () {
    HashData hd = new HashData();

    hash(hd);

    return hd.getValue();
  }

  /**
   * Adds some data to the computation of an hashcode.
   */
  public void hash (HashData hd) {
    int[] v = values;
    for (int i=0, l=v.length; i < l; i++) {
      hd.add(v[i]);
    }

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

  /**
   * Size of the fields.
   */
  public int size () {
    return values.length;
  }

  public String toString () {
    StringBuilder sb = new StringBuilder("Fields(type=");

    sb.append(type);
    sb.append(',');

    sb.append("ci=");
    sb.append(ci.getName());
    sb.append(',');

    sb.append("values=");
    sb.append('[');

    for (int i = 0; i < values.length; i++) {
      if (i != 0) {
        sb.append(',');
      }

      sb.append(values[i]);
    }

    sb.append(']');
    sb.append(',');

    sb.append(')');

    return sb.toString();
  }

  protected abstract String getLogChar ();

  // do not modify result!
  public int[] dumpRawValues() {
    return values;
  }

  public void copyFrom(Fields other) {
    //assert (other.values.length == this.values.length);
    assert (other.ci == this.ci);
    System.arraycopy(other.values, 0, this.values, 0, values.length);

    if (other.fieldAttrs != null){
      if (fieldAttrs == null){
        fieldAttrs = other.fieldAttrs.clone();
      } else {
        System.arraycopy(other.fieldAttrs, 0, fieldAttrs, 0, fieldAttrs.length);
      }
    }

    objectAttr = other.objectAttr;
  }

  // Method that finds the FieldInfo for the given storrageOffset
  // of some field, overriden in StaticFields and DynamicFields.
  protected FieldInfo findFieldInfo (int storageOffset) {
    return null;
  }
}
