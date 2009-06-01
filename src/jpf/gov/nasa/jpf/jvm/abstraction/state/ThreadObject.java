package gov.nasa.jpf.jvm.abstraction.state;

import gov.nasa.jpf.util.ObjVector;

public final class ThreadObject extends InstanceObject {
  public ThreadNode thread;

  @Override
  public void addRefs(ObjVector<StateNode> v) {
    v.add(thread);
    super.addRefs(v);
  }
}
