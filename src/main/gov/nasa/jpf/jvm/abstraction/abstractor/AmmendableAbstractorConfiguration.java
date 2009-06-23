package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Config.Exception;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.ObjArray;

import java.util.LinkedList;

public class AmmendableAbstractorConfiguration extends FilterBasedAbstractorConfiguration {
  public static interface ObjectAbstractorAmmendment {
    ObjectAbstractor<?> getObjectAbstractor(ClassInfo ci, ObjectAbstractor<?> sofar);
  }
  
  public static interface StaticsAbstractorAmmendment {
    StaticsAbstractor getStaticsAbstractor(ClassInfo ci, StaticsAbstractor sofar);
  }
  
  protected final LinkedList<ObjectAbstractorAmmendment> objAmmendments =
    new LinkedList<ObjectAbstractorAmmendment>();
  protected final LinkedList<StaticsAbstractorAmmendment> stAmmendments =
    new LinkedList<StaticsAbstractorAmmendment>();

  public void appendObjectAmmendment(ObjectAbstractorAmmendment oaa) {
    objAmmendments.addLast(oaa);
  }
  
  public void appendStaticsAmmendment(StaticsAbstractorAmmendment saa) {
    stAmmendments.addLast(saa);
  }
  
  public void prependObjectAmmendment(ObjectAbstractorAmmendment oaa) {
    objAmmendments.addFirst(oaa);
  }
  
  public void prependStaticsAmmendment(StaticsAbstractorAmmendment saa) {
    stAmmendments.addFirst(saa);
  }
  
  @Override
  public void attach(JVM jvm) throws Exception {
    super.attach(jvm);
    appendConfiguredObjectAmmendments(jvm.getConfig());
    appendConfiguredStaticsAmmendments(jvm.getConfig());
  }

  protected void appendConfiguredObjectAmmendments(Config config)
  throws Config.Exception {
    ObjArray<ObjectAbstractorAmmendment> oaas =
      config.getInstances("abstraction.builder.object_ammendments", ObjectAbstractorAmmendment.class);
    if (oaas != null) Misc.addAll(objAmmendments,oaas);
  }

  protected void appendConfiguredStaticsAmmendments(Config config)
  throws Config.Exception {
    ObjArray<StaticsAbstractorAmmendment> saas =
      config.getInstances("abstraction.builder.statics_ammendments", StaticsAbstractorAmmendment.class);
    if (saas != null) Misc.addAll(stAmmendments,saas);
  }
  
  @Override
  public ObjectAbstractor<?> getObjectAbstractor(ClassInfo ci) {
    ObjectAbstractor<?> sofar = super.getObjectAbstractor(ci);
    for (ObjectAbstractorAmmendment oaa : objAmmendments) {
      sofar = oaa.getObjectAbstractor(ci, sofar);
      if (sofar == null) throw new IllegalStateException("Invalid null return from " + oaa);
    }
    return sofar;
  }

  @Override
  public StaticsAbstractor getStaticsAbstractor(ClassInfo ci) {
    StaticsAbstractor sofar = super.getStaticsAbstractor(ci);
    for (StaticsAbstractorAmmendment saa : stAmmendments) {
      sofar = saa.getStaticsAbstractor(ci, sofar);
      if (sofar == null) throw new IllegalStateException("Invalid null return from " + saa);
    }
    return sofar;
  }  
}
