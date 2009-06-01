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

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.ThreadInfo;

/**
 * @author jpenix
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IntSpec {

	/**
	 * Allow integers to be specified as literals or identifiers
	 * @param spec int literal or variable identifier
	 * @return value of literal or value of variable in current vm state
	 */
	public static int eval (String spec) {
		int ret;
        
        char c = spec.charAt(0);
        if (Character.isDigit(c) || (c == '+') || (c=='-')) {    
          try {
			ret = Integer.parseInt(spec); 
          } 
          catch (NumberFormatException nfx) {
            throw new JPFException("int literal did not parse: " + spec);
          }
        } else {
          ret = resolveVar(spec);
		}
		return ret;
	}

	/**
	 * Look up value of variable identifer in jvm state
	 * @param name variable name
	 * @return value of variable
	 */
	public static int resolveVar(String name){
      JVM vm = JVM.getVM();
		//System.out.println("resolving "+name);
	  	// split out (potential) class name and variable name
		String[] varId = name.split("[.]+");

		int ret;
		switch (varId.length){
		case 1: { // variable name
			ThreadInfo ti = ThreadInfo.getCurrentThread();
			try {
				ret = ti.getIntLocal(varId[0]);
				// that throws an exception (a few calls down) if  
				// the name is not found...
			}
			catch (JPFException e){ //not local? try a field!
				int id = ti.getThis();
				if(id>=0){  // in a normal (non-static) method
				  ElementInfo ei = DynamicArea.getHeap().get(id);
				  ret = ei.getIntField(varId[0]);
				}
				else { // static method (no this)- must be static var
			      ClassInfo ci = ti.getMethod().getClassInfo();
			      StaticElementInfo ei = vm.getKernelState().sa.get(ci.getName());
			      ret = ei.getIntField(varId[0]);
				}
			}
			break;
		}
		case 2: { // static variable name TODO other cases here...
			ClassInfo ci = ClassInfo.getClassInfo(varId[0]);
			StaticElementInfo ei = vm.getKernelState().sa.get(ci.getName());
			ret = ei.getIntField(varId[1]);
			break;
		}
		default: 
	        throw new JPFException("Choice value format error parsing \"" + name +"\"");
		}
		return ret;
	}
	
}
