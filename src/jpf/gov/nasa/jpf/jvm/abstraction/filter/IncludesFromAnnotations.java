package gov.nasa.jpf.jvm.abstraction.filter;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.abstraction.filter.AmmendableFilterConfiguration.FieldAmmendment;
import gov.nasa.jpf.jvm.AnnotationInfo;
import gov.nasa.jpf.jvm.FieldInfo;

public class IncludesFromAnnotations
implements FieldAmmendment {
  protected Config config;
  
  public IncludesFromAnnotations(Config config) throws Config.Exception {
    this.config = config;
  }
  
  public boolean ammendFieldInclusion(FieldInfo fi, boolean sofar) {
    AnnotationInfo ann = fi.getAnnotation("gov.nasa.jpf.jvm.abstraction.filter.UnfilterField");
    if (ann != null) {
      return POLICY_INCLUDE;
    }
    return sofar;
  }
}
