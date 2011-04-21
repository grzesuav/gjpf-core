/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.nasa.jpf.util.json;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;

/**
 *
 * @author IvanMushketik
 */
public class IntIntervalCGCreator implements CGCreator {

  public ChoiceGenerator createCG(String id, Value[] params) {
    int min = params[0].getDouble().intValue();
    int max = params[1].getDouble().intValue();
    if (params.length == 2) {
      return new IntIntervalGenerator(id, min, max);
    }
    else if (params.length == 3) {
      int delta = params[2].getDouble().intValue();
      return new IntIntervalGenerator(id, min, max, delta);
    }

    throw new JPFException("Can't create IntIntevalChoiceGenerator with id " + id);
  }

}
