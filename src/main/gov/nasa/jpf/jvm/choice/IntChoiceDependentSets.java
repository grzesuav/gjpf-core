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
/*
 * Created on Sep 8, 2005
 */
package gov.nasa.jpf.jvm.choice;

import gov.nasa.jpf.Config;
import java.util.HashMap;
import java.util.Map;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.JPFException;
/**
 * @author jpenix
 *
 * choose from a set of values provided in configuration as
 *		m.class = gov.nasa.jpf.jvm.choice.IntChoiceDependentSets
 *		m.constrainedBy = myVariable
 *		m.value0 = SamfSequenceMgr.SAFM_OPS_1: 101, 102, 103, 104
 *		m.value1 = SamfSequenceMgr.SAFM_OPS_6: 601, 602
 *		m.value2 = SamfSequenceMgr.SAFM_OPS_3: 304, 303
 *		m.label = major mode
 * 
 * choices can then made using: getInt("xxx");
 */
public class IntChoiceDependentSets extends IntChoiceFromSet {

	/** variable providing constraint value */
	String constraint_var;

	/** map from constraint values to choice value sets */
	Map<String, String[]> value_map = new HashMap<String, String[]>();

	/**
	 * @param id
	 */
	public IntChoiceDependentSets(Config conf, String id) {
	  super(id);

	  constraint_var = conf.getString(id + ".constrainedBy");

	  int i = 0;
	  String v = conf.getString(id+".value"+i);
	  while (v != null){
	    int colon = v.indexOf(':');
	    if (v.length() > colon){ //TODO better validity check goes here
	      String[] set = v.substring(colon+1).trim().split("[, ]+");
	      //System.out.println(v.substring(0,colon)+" -> "+set);
	      value_map.put(v.substring(0,colon),set);
	    }
	    else {
	      throw new JPFException("IntChoiceDependentSets parse error on :"
	          +id+".value"+i+ "="+v);
	    }
	    // get next
	    i++;
	    v = conf.getString(id+".value"+i);
	  }
	  v = conf.getString(id+".default");
  //^^^^ dead assignment.  what's up?  -peterd
	}

	/* (non-Javadoc)
	 * @see gov.nasa.jpf.jvm.IntChoiceGenerator#getNextChoice()
	 */
	public int getNextChoice(JVM vm) {
		
		if(count==0){ //initialize set.  
			//System.out.println("count is 0");
			// would like to do this in constr but we don't have 
			// a handle on the JVM there must be a better way... TODO
			int i = 0;
			int len = value_map.size(); // calc outside of loop
			int con_val = IntSpec.eval(constraint_var); 
			Object[] keys = value_map.keySet().toArray();

			// find first key that matches constraint value
			while((i<len) && (con_val != IntSpec.eval((String)keys[i]))) {
				i++;
			}
			if (i<len) {
				values = value_map.get(keys[i]); 
			}
			else {
				// what to do if there is no match?
				values = value_map.get("default");
				if (values==null) {
					throw new JPFException("no value match, no default found");
				}
			}
		}

		int ret;
		ret = IntSpec.eval(values[count]);

		// print "Choice: bob = MyClass.ONE(1)"
		vm.println("Choice: "+id + " = " + values[count] + "("+ret+")");
		return ret;
	}
}
