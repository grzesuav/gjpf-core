package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.jvm.abstraction.state.FrameNode;
import gov.nasa.jpf.jvm.abstraction.state.ThreadNode;
import gov.nasa.jpf.jvm.abstraction.filter.FramePolicy;
import gov.nasa.jpf.jvm.StackFrame;

public final class StackTailAbstractors {
  private StackTailAbstractors() {}
  
  public static final Default   defaultInstance   = new Default();
  public static final NoRecurse noRecurseInstance = new NoRecurse();
  
  public static StackTailAbstractor fromPolicy(FramePolicy policy) {
    if (policy.recurse) {
      return defaultInstance;
    } else {
      return noRecurseInstance;
    }
  }
  
  public static class Default implements StackTailAbstractor {
    public void addFrames(StackFrame[] frames, int fromIdx,
                          ThreadNode threadNode, AbstractorProcess procInfo) {
      if (fromIdx < frames.length) {
        StackFrame frame = frames[fromIdx];
        FrameNode node = procInfo.getFrameLocalAbstractor(frame.getMethodInfo()).getFrameNode(frame, procInfo); 
        threadNode.frames.add(node);
        recurse(frames, fromIdx + 1, threadNode, procInfo);
      }
    }
    
    protected void recurse(StackFrame[] frames, int fromIdx,
                          ThreadNode threadNode, AbstractorProcess procInfo) {
      if (fromIdx < frames.length) {
        procInfo.getStackTailAbstractor(frames[fromIdx].getMethodInfo()).
                               addFrames(frames, fromIdx, threadNode, procInfo);
      }
    }
  }
  
  public static class NoRecurse extends Default {
    @Override
    protected void recurse(StackFrame[] frames, int fromIdx, ThreadNode threadNode, AbstractorProcess procInfo) {
      // do nothing!!!
    }
  }
  
}
