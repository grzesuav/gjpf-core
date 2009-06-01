package gov.nasa.jpf.jvm.abstraction.linearization;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Config.Exception;
import gov.nasa.jpf.jvm.abstraction.state.StateNode;
import gov.nasa.jpf.util.ObjVector;
import gov.nasa.jpf.util.ReadOnlyObjList;

import java.util.LinkedList;
import java.util.Queue;

public class HeuristicStateGraphLinearizer extends StateGraphLinearizerSkeleton {
  protected NodeOrderingHeuristic heuristic;
  
  public void init(Config config) throws Exception {
    heuristic = config.getInstance("abstraction.linearization.heuristic.class", NodeOrderingHeuristic.class);
    if (heuristic == null) {
      heuristic = new DefaultNodeOrderingHeuristic();
    }
    heuristic.init(config);
  }
  
  protected ReadOnlyObjList<StateNode> orderedNodes;
  protected final Queue<StateNode> withUnorderedRefs = new LinkedList<StateNode>();
  private final ObjVector<StateNode> unordered = new ObjVector<StateNode>(); 
  
  // TODO: better would be to do some kind of partitioning instead of simple
  //  withUnorderedRefs structure
  protected void linearizeFrom(StateNode root) {
    StateNode cur;
    
    orderedNodes = getOrderedNodes();
    withUnorderedRefs.clear(); // just in case
    unordered.clear(); // just in case
    
    assert StateNode.linearIdOf(root) == StateNode.INVALID_LINEAR_ID;
    maybeAdd(root);
    
    int next = 0;
    for (;;) {
      // we give priority to breadth-first visitation & numbering of
      // ordered relationships
      if (next < orderedNodes.length()) {
        cur = orderedNodes.get(next);
        if (cur.refsOrdered()) {
          for (StateNode child : cur.getRefs()) {
            maybeAdd(child);
          }
        } else {
          withUnorderedRefs.add(cur);
        }
        next++;
        
      // each time we run out of ordered stuff, we consider the next
      // (possibly postponed) unordered successor list from a node
      } else if (!withUnorderedRefs.isEmpty()) {
        cur = withUnorderedRefs.remove();
        for (StateNode child : cur.getRefs()) {
          if (StateNode.linearIdOf(child) == StateNode.INVALID_LINEAR_ID) {
            unordered.add(child);
          }
        }
        if (unordered.size() > 0) {
          unordered.sort(StateNode.vmIdComparator);
          unordered.sort(heuristic);
          for (StateNode child : unordered) {
            maybeAdd(child);
          }
          unordered.clear();
        }
      
      // if neither ordered nor unordered left, we're done!
      } else {
        break; // fixed point
      }
    }
  }
}
