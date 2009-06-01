package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.jvm.abstraction.state.ObjectNode;

public interface AbstractorProcess extends AbstractorConfiguration {
  public ObjectNode mapOldHeapRef(int objref);
}
