package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.jvm.abstraction.state.ObjectNode;
import gov.nasa.jpf.jvm.DynamicElementInfo;

public interface ObjectAbstractor<T extends ObjectNode> {
  T createInstanceSkeleton(DynamicElementInfo dei);
  
  void fillInstanceData(DynamicElementInfo dei, T skel, AbstractorProcess procInfo);
}
