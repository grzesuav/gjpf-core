//
// Copyright (C) 2011 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package gov.nasa.jpf.util.json;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.BooleanChoiceGenerator;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.choice.DoubleChoiceFromSet;
import gov.nasa.jpf.jvm.choice.IntChoiceFromSet;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;
import java.util.HashMap;

/**
 * Singleton factory for creating CGCreators.
 * @author Ivan Mushketik
 */
public class CGCreatorFactory {

  private static CGCreatorFactory factory;

  // Hash where key is a name that user can use in JSON document to set a
  // ChoiceGenerator and value is creator of ChoiceGenerator that uses Values[]
  // from JSON to creat CG
  private HashMap<String, CGCreator> cgTable = new HashMap<String, CGCreator>() {{
    put("TrueFalse", new BoolCGCreator());
    put("IntSet", new IntFromSetCGCreator());
    put("IntInterval", new IntIntervalCGCreator());
    put("DoubleSet", new DoubleFromSetCGCreator());
  }};

  private CGCreatorFactory() {
  }

  public static CGCreatorFactory getFactory() {
    if (factory == null) {
      factory = new CGCreatorFactory();
    }

    return factory;
  }

  public CGCreator getCGCreator(String key) {
    return cgTable.get(key);
  }
}

/**
 * CGCreator that creates instance of BooleanChoiceGenerator
 */
class BoolCGCreator implements CGCreator {

  public ChoiceGenerator createCG(String id, Value[] params) {
    return new BooleanChoiceGenerator(id);
  }
}


/**
 * CGCreator that creates IntChoiceFromSet CG 
 */
class IntFromSetCGCreator implements CGCreator {

  // <2do> add support from ctor with no params
  public ChoiceGenerator createCG(String id, Value[] params) {
    int[] intSet = new int[params.length];

    for (int i = 0; i < intSet.length; i++) {
      intSet[i] = params[i].getDouble().intValue();
    }

    return new IntChoiceFromSet(id, intSet);
  }
}

class IntIntervalCGCreator implements CGCreator {

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

class DoubleFromSetCGCreator implements CGCreator {

  public ChoiceGenerator createCG(String id, Value[] params) {
    double[] doubleSet = new double[params.length];

    for (int i = 0; i < doubleSet.length; i++) {
      doubleSet[i] = params[i].getDouble().doubleValue();
    }

    return new DoubleChoiceFromSet(id, doubleSet);
  }

}