package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;

public interface AbstractorConfiguration {
  void attach(JVM jvm) throws Config.Exception;
  
  ObjectAbstractor<?>  getObjectAbstractor    (ClassInfo  ci);
  StaticsAbstractor    getStaticsAbstractor   (ClassInfo  ci);
  StackTailAbstractor  getStackTailAbstractor (MethodInfo mi);
  FrameLocalAbstractor getFrameLocalAbstractor(MethodInfo mi);    
}
