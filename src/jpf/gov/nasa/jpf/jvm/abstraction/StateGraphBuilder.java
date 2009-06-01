package gov.nasa.jpf.jvm.abstraction;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.JVM;

public interface StateGraphBuilder {
  void attach(JVM jvm) throws Config.Exception;
  
  StateGraph buildStateGraph() throws JPFException;
}
