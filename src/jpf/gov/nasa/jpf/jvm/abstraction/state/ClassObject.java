package gov.nasa.jpf.jvm.abstraction.state;

import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.ObjVector;

public final class ClassObject extends ObjectNode {
  public int id;

  @Override
  public void addPrimData(IntVector v) {
    v.add2(id,classId);
  }

  @Override
  public void addRefs(ObjVector<StateNode> v) { }

  public Iterable<? extends ObjectNode> getHeapRefs() {
    return Misc.emptyIterable();
  }

  public boolean refsOrdered() {
    return true;
  }
}
