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

import gov.nasa.jpf.jvm.abstraction.state.InstanceObject;
import gov.nasa.jpf.jvm.abstraction.state.NodeMetaData;
import gov.nasa.jpf.jvm.abstraction.state.ObjectNode;
import gov.nasa.jpf.jvm.abstraction.state.StaticsNode;
import gov.nasa.jpf.jvm.abstraction.state.ThreadObject;
import gov.nasa.jpf.jvm.DynamicElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.Fields;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;

/**
 * Associates the data in an abstract state node (with fields) with the VM
 * fields they came from.  Primitives that require more than 1 int take up the
 * appropriate number of slots by padding with nulls (the only case in which nulls
 * are allowed.
 * 
 * @author peterd
 */
public class FieldsMeta implements NodeMetaData {
  public final FieldInfo[] objFields;
  public final FieldInfo[] primFields;
  public final int nFields;
  
  public FieldsMeta(FieldsMetaBuilder builder) {
    this(builder.getObjs(),builder.getPrims(),builder.getNumberOfFields());
  }
  
  public FieldsMeta(FieldInfo[] objFields, FieldInfo[] primFields, int nFields) {
    this.objFields = objFields;
    this.primFields = primFields;
    this.nFields = nFields;
  }
  
  public int getNumberOfFields() {
    return nFields;
  }

  public int getNumberOfStorageInts() {
    return objFields.length + primFields.length;
  }
  
  
  // *********************** For Filling In Objects ******************** //
  public ObjVector<ObjectNode> getObjects(Fields values, AbstractorProcess procInfo) {
    int len = objFields.length;
    ObjVector<ObjectNode> refs = new ObjVector<ObjectNode>(len);
    for (int i = 0; i < len; i++) {
      int objref = values.getReferenceValue(objFields[i].getStorageOffset());
      refs.add(procInfo.mapOldHeapRef(objref));
    }
    return refs;
  }
  
  public IntVector getPrims(Fields values) {
    int len = primFields.length;
    IntVector prims = new IntVector(len);
    int offset = -2; // starts invalid
    for (int i = 0; i < len; i++) {
      FieldInfo fi = primFields[i]; 
      if (fi == null) {
        offset++; // add one to previous offset; > 1 int needed for field 
      } else {
        offset = fi.getStorageOffset();
      }
      prims.add(values.getIntValue(offset));
    }
    return prims;
  }

  
  // ************************* Retrievable abstractors ********************** //
  public ObjectAbstractor<InstanceObject> getInstanceAbstractor() {
    return new InstanceAbstractor();
  }
  
  public ObjectAbstractor<ThreadObject> getThreadAbstractor() {
    return new ThreadAbstractor();
  }
  
  public StaticsAbstractor getStaticsAbstractor() {
    return new Statics();
  }
  
  
  // ********************** Implementations w/ inner classes ******************** //
  protected abstract class AbstractInstanceAbstractor<O extends InstanceObject>
  implements ObjectAbstractor<O> {
    public void fillInstanceData(DynamicElementInfo dei, O skel, AbstractorProcess procInfo) {
      Fields fields = dei.getFields();
      skel.refs = getObjects(fields, procInfo);
      skel.prims = getPrims(fields);
      skel.classId = dei.getClassInfo().getUniqueId();
      skel.meta = FieldsMeta.this;
    }
  }
  
  protected class InstanceAbstractor extends AbstractInstanceAbstractor<InstanceObject> {
    public InstanceObject createInstanceSkeleton(DynamicElementInfo dei) {
      return new InstanceObject();
    }
  }
  
  protected class ThreadAbstractor extends AbstractInstanceAbstractor<ThreadObject> {
    public ThreadObject createInstanceSkeleton(DynamicElementInfo dei) {
      return new ThreadObject();
    }
  }
  
  protected class Statics implements StaticsAbstractor {
    public StaticsNode getStaticsNode(StaticElementInfo sei, AbstractorProcess procInfo) {
      if (nFields == 0) return null;
      StaticsNode ret = new StaticsNode();
      Fields fields = sei.getFields();
      ret.refs = getObjects(fields, procInfo);
      ret.prims = getPrims(fields);
      ret.classId = sei.getClassInfo().getUniqueId();
      ret.meta = FieldsMeta.this;
      return ret;
    }
  }
}
