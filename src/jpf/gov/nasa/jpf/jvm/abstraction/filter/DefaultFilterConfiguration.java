package gov.nasa.jpf.jvm.abstraction.filter;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Config.Exception;

public class DefaultFilterConfiguration extends AmmendableFilterConfiguration {
  @Override
  public void init(Config config) throws Exception {
    // these built-in come first
    appendStaticAmmendment(IgnoreConstants.instance);
    appendInstanceAmmendment(IgnoreReflectiveNames.instance);
    appendFieldAmmendment(IgnoreThreadNastiness.instance);
    appendFieldAmmendment(IgnoreUtilSilliness.instance);
    
    // ignores (e.g. NoMatch) from annotations
    IgnoresFromAnnotations ignores = new IgnoresFromAnnotations(config); 
    appendFieldAmmendment(ignores);
    appendFrameAmmendment(ignores);
    
    // configured via properties
    super.init(config);
    
    // includes (e.g. ForceMatch) from annotations
    IncludesFromAnnotations includes = new IncludesFromAnnotations(config); 
    appendFieldAmmendment(includes);
    //appendFrameAmmendment(includes);
  }
}
