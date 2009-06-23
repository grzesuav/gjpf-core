package gov.nasa.jpf.jvm.abstraction;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;

public interface StateGraphTransform {
  void init(Config config) throws Config.Exception;
  
  void transformStateGraph(StateGraph graph) throws JPFException;
}
