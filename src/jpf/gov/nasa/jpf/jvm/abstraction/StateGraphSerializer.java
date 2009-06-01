package gov.nasa.jpf.jvm.abstraction;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;

public interface StateGraphSerializer {
  void init(Config config) throws Config.Exception;
  
  int[] serializeStateGraph(StateGraph graph) throws JPFException;
}
