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
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.ThreadInfo;

import java.util.logging.Logger;

/**
 * simple DoubleChoiceGenerator that takes it's values from a single
 * property "values" (comma or blank separated list)
 */
public class DoubleChoiceFromSet extends DoubleChoiceGenerator {
  
  static Logger log = JPF.getLogger("gov.nasa.jpf.jvm.choice");
  
  protected Double[] values;
  protected int count = -1;
  
  public DoubleChoiceFromSet (Config conf, String id) {
    super(id);

		String[] vals = conf.getCompactStringArray(id + ".values");
		if (vals == null || vals.length == 0) {
			throw new JPFException("no value specs for IntChoiceFromSet " + id);
		}

    // get the choice values here because otherwise successive getNextChoice()
    // calls within the same transition could see different values when looking
    // up fields and locals
    values = new Double[vals.length];
    StackFrame resolveFrame = ThreadInfo.getCurrentThread().getLastNonSyntheticStackFrame();
    for (int i=0; i<vals.length; i++){
      values[i] = parse(vals[i], resolveFrame);
    }
  }

  protected Double parse (String varId, StackFrame resolveFrame){
    int sign = 1;

    char c = varId.charAt(0);
    if (c == '+'){
      varId = varId.substring(1);
    } else if (c == '-'){
      sign = -1;
      varId = varId.substring(1);
    }

    if (varId.isEmpty()){
      throw new JPFException("illegal value spec for DoubleChoiceFromSet " + id);
    }

    c = varId.charAt(0);
    if (Character.isDigit(c)){ // its an integer literal
      return Double.parseDouble(varId) * sign;

    } else { // a variable or field name
      Object o = resolveFrame.getLocalOrFieldValue(varId);
      if (o == null){
        throw new JPFException("no local or field '" + varId + "' value found for IntChoiceFromSet " + id);
      }
      if (o instanceof Number){
        return new Double( ((Double)o).doubleValue() * sign);
      } else {
        throw new JPFException("non-floating point local or field '" + varId + "' value for IntChoiceFromSet " + id);
      }
    }
  }


  public DoubleChoiceFromSet (String id, double... val){
    super(id);

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
    isDone = false;
  }
  
  public Double getNextChoice () {
    if ((count >= 0) && (count < values.length)) {
      return values[count];
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
      Double tmp = values[i];
      values[i] = values[j];
      values[j] = tmp;
    }
    return this;
  }
  
}
