package gov.nasa.jpf.vm.choice;

import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * a scheduling point for exposure of objects that might become shared
 * by having their reference stored in a shared object
 */
public class ExposureCG extends ThreadChoiceFromSet {
  protected int exposedObjRef;
  
  public ExposureCG (String id, ThreadInfo[] set, ElementInfo eiExposed){
    super( id, set, true);
    
    exposedObjRef = eiExposed.getObjectRef();
  }
}
