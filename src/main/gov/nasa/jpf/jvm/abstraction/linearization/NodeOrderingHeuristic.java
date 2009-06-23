package gov.nasa.jpf.jvm.abstraction.linearization;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.abstraction.state.StateNode;

import java.util.Comparator;

public interface NodeOrderingHeuristic extends Comparator<StateNode> {
  void init(Config config) throws Config.Exception;
  
  /*
   * Calling conventions:
   * 
   * n1 and n2 could be ==, either could be null, could have different runtime
   * type, etc.
   * 
   * a node's linearid is invalid before it is put in the ordering and set
   * as it is put in the ordering.  this can be taken into account in the
   * comparison.
   * 
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  int compare(StateNode n1, StateNode n2);
}