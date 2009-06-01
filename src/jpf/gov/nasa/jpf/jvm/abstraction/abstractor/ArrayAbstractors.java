package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.jvm.abstraction.state.ObjectNode;
import gov.nasa.jpf.jvm.abstraction.state.PrimArrayObject;
import gov.nasa.jpf.jvm.abstraction.state.RefArrayObject;
import gov.nasa.jpf.jvm.ArrayFields;
import gov.nasa.jpf.jvm.DynamicElementInfo;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;

public final class ArrayAbstractors {
  private ArrayAbstractors() {}
  
  public static final DefaultPrims defaultPrimsInstance = new DefaultPrims();
  public static final DefaultRefs  defaultRefsInstance  = new DefaultRefs();
  
  public static class DefaultPrims implements ObjectAbstractor<PrimArrayObject> {
    public PrimArrayObject createInstanceSkeleton(DynamicElementInfo dei) {
      return new PrimArrayObject();
    }

    public void fillInstanceData(DynamicElementInfo dei, PrimArrayObject skel, AbstractorProcess procInfo) {
      ArrayFields fields = (ArrayFields) dei.getFields();
      skel.classId = dei.getClassInfo().getUniqueId();
      skel.prims = new IntVector(fields.dumpRawValues());
    }
  }
  
  public static class DefaultRefs implements ObjectAbstractor<RefArrayObject> {
    public RefArrayObject createInstanceSkeleton(DynamicElementInfo dei) {
      return new RefArrayObject();
    }

    public void fillInstanceData(DynamicElementInfo dei, RefArrayObject skel, AbstractorProcess procInfo) {
      ArrayFields fields = (ArrayFields) dei.getFields();
      skel.classId = dei.getClassInfo().getUniqueId();
      int[] oldRefs = fields.dumpRawValues();
      ObjVector<ObjectNode> newRefs = new ObjVector<ObjectNode>(oldRefs.length);
      for (int i = 0; i < oldRefs.length; i++) {
        newRefs.add(procInfo.mapOldHeapRef(oldRefs[i]));
      }
      skel.refs = newRefs;
    }
  }
  
}
