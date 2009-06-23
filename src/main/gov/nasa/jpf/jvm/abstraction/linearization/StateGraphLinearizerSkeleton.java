package gov.nasa.jpf.jvm.abstraction.linearization;

import gov.nasa.jpf.jvm.abstraction.StateGraph;
import gov.nasa.jpf.jvm.abstraction.StateGraphLinearizer;
import gov.nasa.jpf.jvm.abstraction.state.StateNode;
import gov.nasa.jpf.util.ObjVector;
import gov.nasa.jpf.util.ReadOnlyObjList;

public abstract class StateGraphLinearizerSkeleton
implements StateGraphLinearizer {
  private final ObjVector<StateNode> orderedNodes = new ObjVector<StateNode>();
  
  public final void invalidateLinearization(StateGraph graph,
      ReadOnlyObjList<StateNode> nodeList) {
    for (StateNode node : nodeList) {
      node.linearNodeId = StateNode.INVALID_LINEAR_ID;
    }
  }
  
  public final ReadOnlyObjList<StateNode> linearizeStateGraph(StateGraph graph) {
    //Precondition:
    //orderedNodes.clear();
    
    linearizeFrom(graph.root);
    
    ReadOnlyObjList<StateNode> ret = orderedNodes.toObjArray();
    orderedNodes.clear();
    return ret;
  }

  /**
   * Subclass uses getOrderedNodes and addNextOrderedNode to perform
   * linearization. 
   */
  protected abstract void linearizeFrom(StateNode root);
  
  /**
   * Returns a read-only view of the linearization so far.  Same object is
   * returned each time.
   */
  protected final ReadOnlyObjList<StateNode> getOrderedNodes() {
    return orderedNodes;
  }
  
  /**
   * Takes care of adding to ordered nodes and setting linearid of node.
   */
  protected final void maybeAdd(StateNode node) {
    if (node != null && node.linearNodeId == StateNode.INVALID_LINEAR_ID) {
      node.linearNodeId = orderedNodes.length();
      orderedNodes.add(node);
    }
  }
}