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
 * <2do> pcm - another superfluous type, we should reduce this to Fields. See
 * DynamicFields
 *
 * Represents the fields of an object or class.  Contains the values of the
 * fields, not their descriptors.  Descriptors are represented by
 * gov.nasa.jpf.jvm.FieldInfo objects, which are stored in the
 * ClassInfo structure.  Unlike DynamicFields, ONLY the values of fields of
 * a single class are present in a StaticFields.
 * @see gov.nasa.jpf.jvm.FieldInfo
 */
public class StaticFields extends Fields {
  /**
   * Creates a new field object.
   */
  public StaticFields (ClassInfo ci) {
    super(ci.getType(), ci, ci.getStaticDataSize());
  }

  public String getLogChar () {
    return "@";
  }

  public boolean equals (Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof StaticFields)) {
      return false;
    }

    return super.equals(o);
  }

  public FieldInfo getFieldInfo (int index) {
    return ci.getStaticField(index);
  }

  public int getNumberOfFields () {
    return ci.getNumberOfStaticFields();
  }

  protected FieldInfo findFieldInfo (int storageOffset) {
    int length = Math.min(storageOffset, ci.getNumberOfStaticFields() - 1);
    for (int i = length; i >= 0; i--) {
      FieldInfo fi = ci.getStaticField(i);
      if (fi.getStorageOffset() == storageOffset) return fi;
    }

    return null;
  }
}
