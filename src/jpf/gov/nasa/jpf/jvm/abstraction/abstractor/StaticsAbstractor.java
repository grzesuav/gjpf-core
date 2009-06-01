package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.jvm.abstraction.state.StaticsNode;
import gov.nasa.jpf.jvm.StaticElementInfo;

public interface StaticsAbstractor {
  /**
   * (may return null if no static state)
   */
  StaticsNode getStaticsNode(StaticElementInfo sei, AbstractorProcess procInfo);
}
