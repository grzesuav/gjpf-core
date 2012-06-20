package gov.nasa.jpf.jvm;

import gov.nasa.jpf.JPFException;

/**
 * if any of the superclasses of the class, is the class itself, this
 * error is thrown
 */
public class ClassInfoCircularityError extends JPFException {

  public ClassInfoCircularityError (String details){
    super(details);
  }
}

