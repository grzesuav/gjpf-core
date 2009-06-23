package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.jvm.abstraction.state.FrameNode;
import gov.nasa.jpf.jvm.StackFrame;

public interface FrameLocalAbstractor {
  FrameNode getFrameNode(StackFrame frame, AbstractorProcess procInfo);
}
