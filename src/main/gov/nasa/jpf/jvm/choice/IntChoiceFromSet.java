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
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.IntChoiceGenerator;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.ThreadInfo;
/**
 * @author jpenix
 *
 * choose from a set of values provided in configuration as
 * xxx.class = IntChoiceFromSet
 * xxx.values = {1, 2, 3, 400}
 * where "xxx" is the choice id.
 * 
 * choices can then made using: getInt("xxx");
 */
public class IntChoiceFromSet extends IntChoiceGenerator {

	// int values to choose from stored as Strings or Integers
	protected Integer[] values;
	protected int count = -1;
	
	/**
	 * @param conf JPF configuration object
	 * @param id name used in choice config
	 */
	public IntChoiceFromSet(Config conf, String id) {
		super(id);

		String[] vals = conf.getCompactStringArray(id + ".values");
		if (vals == null || vals.length == 0) {
			throw new JPFException("no value specs for IntChoiceFromSet " + id);
		}

    // get the choice values here because otherwise successive getNextChoice()
    // calls within the same transition could see different values when looking
    // up fields and locals
    values = new Integer[vals.length];
    StackFrame resolveFrame = ThreadInfo.getCurrentThread().getLastNonSyntheticStackFrame();
    for (int i=0; i<vals.length; i++){
      values[i] = parse(vals[i], resolveFrame);
    }
	}

  protected Integer parse (String varId, StackFrame resolveFrame){
    int sign = 1;

    char c = varId.charAt(0);
    if (c == '+'){
      varId = varId.substring(1);
    } else if (c == '-'){
      sign = -1;
      varId = varId.substring(1);
    }

    if (varId.isEmpty()){
      throw new JPFException("illegal value spec for IntChoiceFromSet " + id);
    }

    c = varId.charAt(0);
    if (Character.isDigit(c)){ // its an integer literal
      return Integer.parseInt(varId) * sign;

    } else { // a variable or field name
      Object o = resolveFrame.getLocalOrFieldValue(varId);
      if (o == null){
        throw new JPFException("no local or field '" + varId + "' value found for IntChoiceFromSet " + id);
      }
      if (o instanceof Number){
        return new Integer( ((Number)o).intValue() * sign);
      } else {
        throw new JPFException("non-integer local or field '" + varId + "' value for IntChoiceFromSet " + id);
      }
    }
  }

  public IntChoiceFromSet(String id, int... val){
    super(id);

    if (val != null){
      values = new Integer[val.length];
      for (int i=0; i<val.length; i++){
        values[i] = new Integer(val[i]);
      }
    } else {
      throw new JPFException("empty set for IntChoiceFromSet");
    }

    count = -1;
  }


	/** super constructor for subclasses that want to configure themselves
	 * 
	 * @param id name used in choice config
	 */
	protected IntChoiceFromSet(String id){
		super(id);
	}

  public void reset () {
    count = -1;

    isDone = false;
  }
    
	/** 
	 * @see gov.nasa.jpf.jvm.IntChoiceGenerator#getNextChoice()
	 **/
	public Integer getNextChoice() {

    if ((count >= 0) && (count < values.length)) {
      return values[count];
    }

    return 0;
	}

	/**
	 * @see gov.nasa.jpf.jvm.ChoiceGenerator#hasMoreChoices()
	 **/
	public boolean hasMoreChoices() {
		if (!isDone && (count < values.length-1))  
			return true;
		else
			return false;
	}

	/**
	 * @see gov.nasa.jpf.jvm.ChoiceGenerator#advance()
	 **/
	public void advance() {
		if (count < values.length-1) count++;
	}

	/**
	 * get String label of current value, as specified in config file
	 **/
	public String getValueLabel(){
		return values[count].toString();
	}

  public int getTotalNumberOfChoices () {
    return values.length;
  }

  public int getProcessedNumberOfChoices () {
    return count+1;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getName());
    sb.append("[id=\"");
    sb.append(id);
    sb.append('"');

    sb.append(",isCascaded:");
    sb.append(isCascaded);

    sb.append(",");
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
  
  public IntChoiceFromSet randomize () {
    for (int i = values.length - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      Integer tmp = values[i];
      values[i] = values[j];
      values[j] = tmp;
    }
    return this;
  }

}
