package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.jvm.abstraction.state.FrameNode;
import gov.nasa.jpf.jvm.abstraction.state.ObjectNode;
import gov.nasa.jpf.jvm.abstraction.filter.FramePolicy;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;

public final class FrameLocalAbstractors {
  private FrameLocalAbstractors() {}
  
  public static final FrameLocalAbstractor defaultInstance = new PolicyBased(new FramePolicy());
  
  public static FrameLocalAbstractor fromPolicy(FramePolicy policy) {
    if (policy.isDefaultPolicy()) {
      return defaultInstance;
    } else {
      return new PolicyBased(policy);
    }
  }
  
  public static class PolicyBased implements FrameLocalAbstractor {
    protected FramePolicy policy;
    
    public PolicyBased(FramePolicy policy) { this.policy = policy; }
    
    public FrameNode getFrameNode(StackFrame frame, AbstractorProcess procInfo) {
      FrameNode node = new FrameNode();
      MethodInfo mi = frame.getMethodInfo();
      node.methodId = mi.getGlobalId();
      if (policy.includePC) {
        node.instrOff = frame.getPC().getOffset();
      } else {
        node.instrOff = -1;
      }
      
      IntVector            prims = new IntVector();
      ObjVector<ObjectNode> refs = new ObjVector<ObjectNode>();

      if (policy.includeLocals) {
        int localsLen = frame.getLocalVariableCount();
        for (int i = 0; i < localsLen; i++) {
          int v = frame.getLocalVariable(i);
          if (frame.isLocalVariableRef(i)) {
            refs.add(procInfo.mapOldHeapRef(v));
          } else {
            prims.add(v);
          }
        }
      }
      
      if (policy.includeOps) {
        int operandsLen = frame.getTopPos() + 1;
        for (int i = 0; i < operandsLen; i++) {
          int v = frame.getAbsOperand(i);
          if (frame.isAbsOperandRef(i)) {
            refs.add(procInfo.mapOldHeapRef(v));
          } else {
            prims.add(v);
          }
        }
      }
      
      node.prims = prims;
      node.refs = refs;
      return node;
    }
  }
}
