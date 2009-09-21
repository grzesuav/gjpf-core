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
	Object[] values;
	
	int count = -1;
	
	/**
	 * @param conf JPF configuration object
	 * @param id name used in choice config
	 */
	public IntChoiceFromSet(Config conf, String id) {
		super(id);
		values = conf.getStringArray(id + ".values");
		if (values == null) {
			throw new JPFException("value set for <" + id + "> choice did not load");
		}
	}

  public IntChoiceFromSet(int[] val){
    super(null);

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
    }
    
	/** 
	 * @see gov.nasa.jpf.jvm.IntChoiceGenerator#getNextChoice()
	 **/
	public Integer getNextChoice() {

    if ((count >= 0) && (count < values.length)) {
      Object val = values[count];

      if (val instanceof String){
        return new Integer( IntSpec.eval((String) val));
      } else if (val instanceof Integer){
        return (Integer)val;
      } else {
        throw new JPFException("unknown IntChoiceFromSet value spec: " + val);
      }
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
  
  public IntChoiceFromSet randomize () {
    for (int i = values.length - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      Object tmp = values[i];
      values[i] = values[j];
      values[j] = tmp;
    }
    return this;
  }

}
