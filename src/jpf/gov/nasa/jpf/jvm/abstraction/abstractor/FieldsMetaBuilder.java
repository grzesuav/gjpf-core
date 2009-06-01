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
package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.jvm.FieldInfo;

import java.util.ArrayList;

public class FieldsMetaBuilder {
  protected final ArrayList<FieldInfo> relevantObj = new ArrayList<FieldInfo>();
  protected final ArrayList<FieldInfo> relevantPrim = new ArrayList<FieldInfo>();
  protected int nFields = 0;

  public FieldsMetaBuilder() {}
  
  public FieldsMetaBuilder(Iterable<FieldInfo> infos) {
    addAll(infos);
  }
  
  public void reset() {
    relevantObj.clear();
    relevantPrim.clear();
    nFields = 0;
  }
  
  public void addAll(Iterable<FieldInfo> infos) {
    for (FieldInfo fi : infos) {
      add(fi);
    }
  }
  
  public void add(FieldInfo fi) {
    nFields++;
    if (fi.isReference()) {
      relevantObj.add(fi);
    } else {
      relevantPrim.add(fi);
      for (int j = 1; j < fi.getStorageSize(); j++) {
        relevantPrim.add(null);
      }
    }
  }
  
  public FieldInfo[] getObjs() {
    return relevantObj.toArray(new FieldInfo[relevantObj.size()]);
  }
  
  public FieldInfo[] getPrims() {
    return relevantPrim.toArray(new FieldInfo[relevantPrim.size()]);
  }
  
  public int getNumberOfFields() {
    return nFields;
  }
}
