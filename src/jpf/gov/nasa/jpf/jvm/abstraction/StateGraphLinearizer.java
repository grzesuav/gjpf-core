package gov.nasa.jpf.jvm.abstraction;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.abstraction.state.StateNode;
import gov.nasa.jpf.util.ReadOnlyObjList;

public interface StateGraphLinearizer {
  void init(Config config) throws Config.Exception;
  
  /**
   * Returns not only linearized list, but assigns linearNodeIds in
   * StateNodes also.
   * 
   * Precondition: all reachable nodes in StateGraph have INVALID linearId 
   */
  ReadOnlyObjList<StateNode> linearizeStateGraph(StateGraph graph);

  /**
   * Used to return a StateGraph to pre-linearization state, so that
   * it can be linearized again after changes. 
   */
  void invalidateLinearization(StateGraph graph, ReadOnlyObjList<StateNode> nodeList);
}
