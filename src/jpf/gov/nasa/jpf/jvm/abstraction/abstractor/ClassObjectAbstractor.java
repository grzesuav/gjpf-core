package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.jvm.abstraction.state.ClassObject;
import gov.nasa.jpf.jvm.DynamicElementInfo;

public class ClassObjectAbstractor implements ObjectAbstractor<ClassObject> {

  public ClassObject createInstanceSkeleton(DynamicElementInfo dei) {
    return new ClassObject();
  }

  public void fillInstanceData(DynamicElementInfo dei, ClassObject skel, AbstractorProcess procInfo) {
    skel.classId = dei.getClassInfo().getUniqueId(); // id for java.lang.Class
    skel.id = dei.getIntField("cref"); // id for represented class
    // "cref" now same as uniqueId
  }

}
