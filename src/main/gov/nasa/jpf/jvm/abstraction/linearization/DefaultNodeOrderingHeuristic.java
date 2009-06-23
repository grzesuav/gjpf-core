package gov.nasa.jpf.jvm.abstraction.linearization;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Config.Exception;
import gov.nasa.jpf.jvm.abstraction.state.StateNode;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;

public class DefaultNodeOrderingHeuristic
implements NodeOrderingHeuristic {
  protected int heuristicDepth;
  
  public void init(Config config) throws Exception {
    heuristicDepth = config.getInt("abstraction.linearization.heuristic_depth", 3);
  }

  public final int compare(StateNode o1, StateNode o2) {
    return compare(o1, o2, heuristicDepth);
  }
  
  public int compare(StateNode o1, StateNode o2, int remainingDepth) {
    int diff;
    
    if (o1 == o2) return 0; // we'll never find a difference if they're the same!
    
    diff = StateNode.linearIdOf(o1) - StateNode.linearIdOf(o2);
    if (diff != 0) return diff;

    // getting here means neither is null
    // getting here means both have "invalid" linearid
    
    diff = o1.getNodeTypeId() - o2.getNodeTypeId();
    if (diff != 0) return diff;
    
    // getting here means they have the same runtime type

    // depth 0 gets us here
    if (remainingDepth <= 0) { // don't go into costly comparisons
      return 0;
    }

    IntVector prims1 = o1.getPrimData();
    IntVector prims2 = o2.getPrimData();
    
    diff = prims1.size() - prims2.size();
    if (diff != 0) return diff;
    
    diff = prims1.compareTo(prims2);
    if (diff != 0) return diff;

    // usually this is a consequence of the type, but just in case...
    diff = (o1.refsOrdered() ? 1 : 0) - (o2.refsOrdered() ? 1 : 0);
    if (diff != 0) return diff;

    ObjVector<StateNode> refs1 = o1.getRefs();
    ObjVector<StateNode> refs2 = o2.getRefs();

    diff = refs1.size() - refs2.size();
    if (diff != 0) return diff;

    if (o1.refsOrdered()) {
      // recursive comparison
      int len = refs1.size();
      for (int i = 0; i < len; i++) {
        diff = compare(refs1.get(i), refs2.get(i), remainingDepth - 1);
        if (diff != 0) return diff;
      }
    } else {
      diff = compareUnordered(refs1,refs2);
      if (diff != 0) return diff;
    }

    // got nuthin' else
    return 0;
  }
  
  //precondition: equal length
  protected int compareUnordered(ObjVector<StateNode> refs1, ObjVector<StateNode> refs2) {
    int diff;
    int len = refs1.size();
    int sum1, sum2;

    // small, order-independent heuristics:
    
    sum1 = 0; sum2 = 0;
    for (int i = 0; i < len; i++) {
      sum1 += StateNode.linearIdOf(refs1.get(i));
      sum2 += StateNode.linearIdOf(refs2.get(i));
    }
    diff = sum1 - sum2;
    if (diff != 0) return diff;

    sum1 = 0; sum2 = 0;
    for (int i = 0; i < len; i++) {
      sum1 += StateNode.typeIdOf(refs1.get(i));
      sum2 += StateNode.typeIdOf(refs2.get(i));
    }
    diff = sum1 - sum2;
    if (diff != 0) return diff;

    // oh well; good enough for now.
    
    return 0;
  }
}
