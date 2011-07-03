//
// Copyright (C) 2011 United States Government as represented by the
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
package gov.nasa.jpf.jvm.serialize;

import gov.nasa.jpf.jvm.ArrayFields;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.Fields;
import gov.nasa.jpf.util.FinalBitSet;
import java.util.HashSet;

/**
 * a CFSerializer that uses Abstraction objects stored as field attributes to
 * obtain the values to hash. 
 * 
 * <2do> still needs to override serializeClass to support abstracted static fields
 */
public class DynamicAbstractionSerializer extends CFSerializer {
  
  HashSet<ClassInfo> monitoredClasses = new HashSet<ClassInfo>();
  
  protected void processAbstractedNamedFields(ClassInfo ci, Fields fields){
    FinalBitSet filtered = getInstanceFilterMask(ci);
    int nFields = ci.getNumberOfInstanceFields();
    
    for (int i = 0; i < nFields; i++) {
      FieldInfo fi = ci.getInstanceField(i);
      int off = fi.getStorageOffset();
      if (!filtered.get(off)) {
        Abstraction a = fi.getAttr(Abstraction.class);
        if (a != null) {
          if (fi.is1SlotField()) {
            if (fi.isReference()) {
              // <2do> we don't do any object abstraction yet, but could
              processReference(fields.getReferenceValue(off));
              
            } else if (fi.isFloatField()) {
              buf.add(a.getAbstractValue(fields.getFloatValue(off)));
            } else {
              buf.add(a.getAbstractValue(fields.getIntValue(off)));
            }
          } else { // double or long
            if (fi.isLongField()) {
              buf.add(a.getAbstractValue(fields.getLongValue(off)));
            } else { // got to be double
              buf.add(a.getAbstractValue(fields.getDoubleValue(off)));
            }
          }
        }
      }
    }
  }
  
  
  // <2do> this should also allow abstraction of whole objects, so that
  // we can hash combinations/relations of field values
  public void processElementInfo(ElementInfo ei) {
    Fields fields = ei.getFields();
    ClassInfo ci = ei.getClassInfo();
    buf.add(ci.getUniqueId());

    if (fields instanceof ArrayFields) { // not filtered
      processArrayFields( (ArrayFields)fields);

    } else { // named fields, filtered & abstracted via attributes
      if (isAbstractedClass(ci)){
        processAbstractedNamedFields(ci, fields);
      } else {
        processNamedFields(ci, fields);
      }
    }
  }

  protected boolean isAbstractedClass(ClassInfo ci){
    return ci.hasInstanceFieldInfoAttr(Abstraction.class);
  }
}
