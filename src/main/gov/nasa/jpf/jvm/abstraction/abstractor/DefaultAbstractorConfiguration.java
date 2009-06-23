package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.Config.Exception;
import gov.nasa.jpf.jvm.abstraction.symmetry.SymmetricCollectionsAbstractorAmmendment;
import gov.nasa.jpf.jvm.JVM;

public class DefaultAbstractorConfiguration extends AmmendableAbstractorConfiguration {
  @Override
  public void attach(JVM jvm) throws Exception {
    super.attach(jvm);
    prependObjectAmmendment(new SymmetricCollectionsAbstractorAmmendment());
  }
}
