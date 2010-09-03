//
// Copyright (C) 2006 United States Government as represented by the
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
package gov.nasa.jpf.jvm.choice;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.DoubleChoiceGenerator;

import java.util.logging.Logger;

/**
 * simple DoubleChoiceGenerator that takes it's values from a single
 * property "values" (comma or blank separated list)
 */
public class DoubleChoiceFromSet extends DoubleChoiceGenerator {
  
  static Logger log = JPF.getLogger("gov.nasa.jpf.jvm.choice");
  
  protected Object[] values;
  protected int count;
  
  public DoubleChoiceFromSet (Config conf, String id) {
    super(id);
    
    values = conf.getStringArray(id + ".values");
    if (values == null) {
      throw new JPFException("value set for <" + id + "> choice did not load");
    }
      
    count = -1;
  }

  public DoubleChoiceFromSet (double[] val){
    super(null);

    if (val != null){
      values = new Double[val.length];
      for (int i=0; i<val.length; i++){
        values[i] = new Double(val[i]);
      }
    } else {
      throw new JPFException("empty set for DoubleChoiceFromSet");
    }

    count = -1;
  }

  public void reset () {
    count = -1;
  }
  
  public Double getNextChoice () {
    if ((count >= 0) && (count < values.length)) {
      Object val = values[count];

      if (val instanceof String){
        return new Double( DoubleSpec.eval((String) val));
      } else if (val instanceof Double){
        return (Double)val;
      } else {
      throw new JPFException("unknown DoubleChoiceFromSet value spec: " + val);
      }
    }
    
    return Double.NaN;  // Hmm, maybe we should return the last value
  }
  
  public boolean hasMoreChoices () {
    return !isDone && (count < values.length-1);
  }
  
  public void advance () {
    if (count < values.length-1) count++;
  }

  public int getTotalNumberOfChoices () {
    return values.length;
  }

  public int getProcessedNumberOfChoices () {
    return count+1;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName());

    sb.append("[id=\"");
    sb.append(id);
    sb.append("\",");

    for (int i=0; i<values.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      if (i == count) {
        sb.append(MARKER);
      }
      sb.append(values[i]);
    }
    sb.append(']');
    return sb.toString();
  }

  public DoubleChoiceFromSet randomize () {
    for (int i = values.length - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      Object tmp = values[i];
      values[i] = values[j];
      values[j] = tmp;
    }
    return this;
  }
  
}
