package gov.nasa.jpf.jvm.abstraction.symmetry;

import gov.nasa.jpf.jvm.abstraction.abstractor.AbstractorProcess;
import gov.nasa.jpf.jvm.abstraction.abstractor.ObjectAbstractor;
import gov.nasa.jpf.jvm.abstraction.abstractor.AmmendableAbstractorConfiguration.ObjectAbstractorAmmendment;
import gov.nasa.jpf.jvm.abstraction.state.BagObject;
import gov.nasa.jpf.jvm.abstraction.state.ObjectNode;
import gov.nasa.jpf.jvm.abstraction.state.SetObject;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DynamicElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;

public class SymmetricCollectionsAbstractorAmmendment implements ObjectAbstractorAmmendment {
  protected static final String eqSetName = CanonicalEqSet.class.getName();
  protected static final String eqBagName = CanonicalEqBag.class.getName();

  protected final SetAbstractor setAbs = new SetAbstractor();
  protected final BagAbstractor bagAbs = new BagAbstractor();
  
  public ObjectAbstractor<?> getObjectAbstractor(ClassInfo ci, ObjectAbstractor<?> sofar) {
    String cname = ci.getName();
    if (cname.equals(eqSetName)) {
      setAbs.dataField = ci.getInstanceField("data");
      return setAbs;
    } else if (cname.equals(eqBagName)) {
      bagAbs.dataField = ci.getInstanceField("data");
      return bagAbs;
    } else {
      return sofar;
    }
  }

  protected static class BagAbstractor implements ObjectAbstractor<BagObject> { 
    public FieldInfo dataField = null;
    
    public BagObject createInstanceSkeleton(DynamicElementInfo dei) {
      return new BagObject();
    }

    public void fillInstanceData(DynamicElementInfo dei, BagObject skel, AbstractorProcess procInfo) {
      DynamicElementInfo aei = dei.getFieldDereference(dataField);
      skel.classId = dei.getClassInfo().getUniqueId();
      int[] oldRefs = aei.getFields().dumpRawValues();
      EqBag<ObjectNode> newRefs = CollectionFactory.newEqBag();
      for (int i = 0; i < oldRefs.length; i++) {
        newRefs.add(procInfo.mapOldHeapRef(oldRefs[i]));
      }
      skel.refs = newRefs;
    }
  }
  
  protected static class SetAbstractor implements ObjectAbstractor<SetObject> { 
    public FieldInfo dataField = null;
    
    public SetObject createInstanceSkeleton(DynamicElementInfo dei) {
      return new SetObject();
    }

    public void fillInstanceData(DynamicElementInfo dei, SetObject skel, AbstractorProcess procInfo) {
      DynamicElementInfo aei = dei.getFieldDereference(dataField);
      skel.classId = dei.getClassInfo().getUniqueId();
      int[] oldRefs = aei.getFields().dumpRawValues();
      EqSet<ObjectNode> newRefs = CollectionFactory.newEqSet();
      for (int i = 0; i < oldRefs.length; i++) {
        newRefs.add(procInfo.mapOldHeapRef(oldRefs[i]));
      }
      skel.refs = newRefs;
    }
  }
}
