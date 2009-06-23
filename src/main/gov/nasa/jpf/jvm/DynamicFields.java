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
 * <2do> pcm - another superfluous type, we should reduce this to Fields. Last
 * place the polymorphism is used (but after an 'instanceof') is FieldRaceInfo.
 * As soon as we cleaned up the race detection, scrub this class
 *
 * Represents the fields of an object or class.  Contains the values of the
 * fields, not their descriptors.  Descriptors are represented by
 * gov.nasa.jpf.jvm.FieldInfo objects, which are stored in the
 * ClassInfo structure.
 * @see gov.nasa.jpf.jvm.FieldInfo
 */
public class DynamicFields extends Fields {

  /**
   * Constructor
   */
  public DynamicFields (String t, ClassInfo ci) {
    super(t, ci, ci.getInstanceDataSize());
  }

  // the FieldInfo accessors are just here to hide the Dynamic/StaticFieldInfo
  // anomaly, remove once we have this solved
  public FieldInfo getDeclaredFieldInfo (String fname) {
    return ci.getDeclaredInstanceField(fname);
  }

  public FieldInfo getFieldInfo (String fname) {
    return ci.getInstanceField(fname);
  }

  public int getNumberOfFields() {
    return ci.getNumberOfInstanceFields();
  }

  public FieldInfo getFieldInfo (int fieldIndex) {
    return ci.getInstanceField( fieldIndex);
  }

  public String getLogChar () {
    return "#";
  }

  public boolean equals (Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof DynamicFields)) {
      return false;
    }

    return super.equals(o);
  }

  protected FieldInfo findFieldInfo (int storageOffset) {
    int length = Math.min(storageOffset, ci.getNumberOfInstanceFields() - 1);
    for (int i = length; i >= 0; i--) {
      FieldInfo fi = ci.getInstanceField(i);
      if (fi.getStorageOffset() == storageOffset) return fi;
    }

    return null;
  }
}
