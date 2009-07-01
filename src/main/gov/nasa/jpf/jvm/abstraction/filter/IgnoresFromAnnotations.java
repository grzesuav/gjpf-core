package gov.nasa.jpf.jvm.abstraction.filter;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.abstraction.filter.AmmendableFilterConfiguration.FieldAmmendment;
import gov.nasa.jpf.jvm.abstraction.filter.AmmendableFilterConfiguration.FrameAmmendment;
import gov.nasa.jpf.jvm.AnnotationInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.MethodInfo;

public class IgnoresFromAnnotations
implements FieldAmmendment, FrameAmmendment {
  protected Config config;
  
  public IgnoresFromAnnotations(Config config) {
    this.config = config;
  }
  
  public boolean ammendFieldInclusion(FieldInfo fi, boolean sofar) {
    AnnotationInfo ann = fi.getAnnotation("gov.nasa.jpf.annotations.FilterField");
    if (ann != null){
      String condition = ann.getValueAsString("condition");
      boolean invert = ann.getValueAsBoolean("invert");
      if ((condition == null) || (config.getBoolean(condition)) == !invert ) {
        return POLICY_IGNORE;
      }
    }
    
    return sofar;
  }

  public FramePolicy ammendFramePolicy(MethodInfo mi, FramePolicy sofar) {
    AnnotationInfo ann = mi.getAnnotation("gov.nasa.jpf.annotations.FilterFrame");
    if (ann != null) {
      if (ann.getValueAsBoolean("filterData")) {
        sofar.includeLocals = false;
        sofar.includeOps = false;
      }
      if (ann.getValueAsBoolean("filterPC")) {
        sofar.includePC = false;
      }
      if (ann.getValueAsBoolean("filterSubframes")) {
        sofar.recurse = false;
      }
    }
    return sofar;
  }

}
