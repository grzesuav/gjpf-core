package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.jvm.abstraction.state.ThreadNode;
import gov.nasa.jpf.jvm.StackFrame;

public interface StackTailAbstractor {
  void addFrames(StackFrame[] frames, int fromIdx, ThreadNode threadNode, AbstractorProcess procInfo);
}
