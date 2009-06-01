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



/**
 *a Field (data value) store for array objects
 */
public class ArrayFields extends Fields {

  private int    elementStorageSize;
  private int    length;
  private String elementType;
  private boolean isReference;

  public ArrayFields (String type, ClassInfo ci, int storageSize, int length,
                      boolean isReference) {
    super(type, ci, storageSize);
    this.length = length;
    this.isReference = isReference;
    elementType = type.substring(1);
    elementStorageSize = Types.getTypeSize(type);

    // Ok, this seems to be a design anomaly, but array elements are not fields,
    // hence the only non-0 element initialization is for reference arrays, and
    // only with const, default 'null' values. All other inits (even the
    // static init "a = {...}:") is compiled into explicit NEWARRAY, ASTORE..
    // sequences, and does not need any special, automatic init code
    if (isReference) {
      for (int i=0; i<length; i++) {
        values[i] = -1;
      }
    }
  }

  int getNumberOfFieldsOrElements () {
    return length; // we have no fields
  }

  public int arrayLength () {
    return length;
  }

  public int getHeapSize () {
    return Types.getTypeSizeInBytes(elementType) * length;
  }

  public FieldInfo getFieldInfo(String clsBase, String fname) {
    // has none
    return null;
  }

  public int getNumberOfFields() {
    // has none
    return 0;
  }

  public FieldInfo getFieldInfo(int fieldIndex) {
    // has none
    return null;
  }

  /**
   * @see gov.nasa.jpf.jvm.FieldInfo#getFieldIndex()
   */
  public int getFieldIndex (String name, String referenceType) {
    // will this ever happen?
    throw new NoSuchFieldError("array does not have any fields!" +
                               getClassInfo().getName() + "." + name);
  }

  public String getFieldName (int index) {
    return Integer.toString(index / elementStorageSize);
  }

  public String getFieldType (String name, String referenceType) {
    // will this ever happen?
    throw new NoSuchFieldError("array does not have any fields!" +
                               getClassInfo().getName() + "." + name);
  }

  public String getFieldType (int findex) {
    if (elementType == null) {
      elementType = getType().substring(1);
    }

    return elementType;
  }

  public String getLogChar () {
    return "*";
  }

  public void setLongField (String name, String referenceType, long value) {
    throw new NoSuchFieldError("array does not have any fields!" +
                               getClassInfo().getName() + "." + name);
  }

  public long getLongField (String name, String referenceType) {
    throw new NoSuchFieldError("array does not have any fields!" +
                               getClassInfo().getName() + "." + name);
  }

  public boolean isReferenceArray() {
    return isReference;
  }

  public boolean isRef (String name, String referenceType) {
    throw new NoSuchFieldError("array does not have any fields!" +
                               getClassInfo().getName() + "." + name);
  }

  public boolean[] asBooleanArray () {
    // <2do> we probably should check the type first
    int       length = values.length;
    boolean[] result = new boolean[length];

    for (int i = 0; i < length; i++) {
      result[i] = Types.intToBoolean(values[i]);
    }

    return result;
  }

  public byte[] asByteArray () {
    // <2do> we probably should check the type first
    int       length = values.length;
    byte[] result = new byte[length];

    for (int i = 0; i < length; i++) {
      result[i] = (byte) values[i];
    }

    return result;
  }

  public char[] asCharArray () {
    // <2do> we probably should check the type first
    int       length = values.length;
    char[] result = new char[length];

    for (int i = 0; i < length; i++) {
      result[i] = (char)values[i];
    }

    return result;
  }

  public char[] asCharArray (int offset, int length) {
    char[] result = new char[length];
    int max = offset+length;

    for (int i = offset, j=0; i < max; i++, j++) {
      result[j] = (char)values[i];
    }

    return result;
  }

  public boolean equals (int offset, int length, String s) {
    if (offset+length > values.length) {
      return false;
    }

    for (int i=offset, j=0; j<length; i++, j++) {
      if ((char)values[i] != s.charAt(j)) {
        return false;
      }
    }

    return true;
  }

  public short[] asShortArray () {
    // <2do> we probably should check the type first
    int       length = values.length;
    short[] result = new short[length];

    for (int i = 0; i < length; i++) {
      result[i] = (short) values[i];
    }

    return result;
  }

  public int[] asIntArray () {
    // <2do> we probably should check the type first
    return values.clone();
  }

  public long[] asLongArray () {
    // <2do> we probably should check the type first
    int       length = values.length / 2;
    long[] result = new long[length];

    for (int i = 0, j=0; i < length; i++, j+=2) {
      result[i] = Types.intsToLong(values[j + 1], values[j]);
    }

    return result;
  }

  public float[] asFloatArray () {
    // <2do> we probably should check the type first
    int       length = values.length;
    float[] result = new float[length];

    for (int i = 0; i < length; i++) {
      result[i] = Types.intToFloat(values[i]);
    }

    return result;
  }

  public double[] asDoubleArray () {
    // <2do> we probably should check the type first
    int       length = values.length / 2;
    double[] result = new double[length];

    for (int i = 0, j=0; i < length; i++, j+=2) {
      result[i] = Types.intsToDouble(values[j + 1], values[j]);
    }

    return result;
  }

}
