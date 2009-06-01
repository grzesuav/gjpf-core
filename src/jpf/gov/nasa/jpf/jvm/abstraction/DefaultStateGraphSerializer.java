package gov.nasa.jpf.jvm.abstraction;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.Config.Exception;
import gov.nasa.jpf.jvm.abstraction.linearization.HeuristicStateGraphLinearizer;
import gov.nasa.jpf.jvm.abstraction.state.StateNode;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;
import gov.nasa.jpf.util.ReadOnlyObjList;

public class DefaultStateGraphSerializer implements StateGraphSerializer {
  protected StateGraphLinearizer linearizer;
  
  public void init(Config config) throws Exception {
    linearizer = config.getInstance("abstraction.linearizer.class", StateGraphLinearizer.class);
    if (linearizer == null) {
      linearizer = new HeuristicStateGraphLinearizer();
    }
    linearizer.init(config);
  }

  protected final IntVector buf = new IntVector();  
  
  public int[] serializeStateGraph(StateGraph graph) throws JPFException {
    buf.clear();
    ReadOnlyObjList<StateNode> nodeList = linearizer.linearizeStateGraph(graph);
    for (StateNode n : nodeList) {
      serializeNodeTo(n, buf);
    }
    return buf.toArray();
  }

  protected final ObjVector<StateNode> nbuf = new ObjVector<StateNode>(); 
  
  protected void serializeNodeTo(StateNode n, IntVector v) {
    v.add(n.getNodeTypeId());
    
    n.addRefs(nbuf);
    if (!n.refsOrdered()) {
      nbuf.sort(StateNode.linearIdComparator);
    }
    v.add(nbuf.size());
    for (StateNode nn : nbuf) {
      v.add(StateNode.linearIdOf(nn));
    }
    nbuf.clear();
    
    int szIdx = v.size();
    v.add(0); // placeholder for size
    n.addPrimData(v);
    v.set(szIdx, v.size() - szIdx - 1); // replace size
  }
}
