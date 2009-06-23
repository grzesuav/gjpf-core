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


/**
 * A specialized version of ElementInfo for use in the DynamicArea.
 * DynamicElementInfo objects represent heap objects
 * @see gov.nasa.jpf.jvm.ElementInfo
 */
public final class DynamicElementInfo extends ElementInfo {
  public DynamicElementInfo () {
  }

  public DynamicElementInfo (Fields f, Monitor m) {
    super(f, m);
  }

  public void setIntField(FieldInfo fi, int value) {
    //checkFieldInfo(fi); // in case somebody caches and uses the wrong FieldInfo

    if (!fi.isReference()) {
      cloneFields().setIntValue(this, fi.getStorageOffset(), value);
    } else {
      throw new JPFException("reference field: " + fi.getName());
    }
  }


  public int getNumberOfFields () {
    return getClassInfo().getNumberOfInstanceFields();
  }

  public FieldInfo getFieldInfo (int fieldIndex) {
    return getClassInfo().getInstanceField(fieldIndex);
  }

  public DynamicElementInfo getFieldDereference (FieldInfo fi) {
    assert fi.isReference();
    return (DynamicElementInfo) area.get(getIntField(fi));
  }

  public FieldInfo getFieldInfo (String fname) {
    return getClassInfo().getInstanceField(fname);
  }
  protected FieldInfo getDeclaredFieldInfo (String clsBase, String fname) {
    return ClassInfo.getClassInfo(clsBase).getDeclaredInstanceField(fname);
  }

  protected ElementInfo getElementInfo (ClassInfo ci) {
    // DynamicElementInfo fields are always flattened, so there is no need to
    // look up a Fields container
    return this;
  }

  protected Ref getRef () {
    return new ObjRef(getIndex());
  }
}
