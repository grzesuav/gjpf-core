//
// Copyright (C) 2008 United States Government as represented by the
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

package gov.nasa.jpf.listener;



import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;

/** 
 * A lightweight listener to generate the error trace by printing
 * the program instructions at POR boundaries (where there is more than
 * one choice to explore)
 **/
public class ErrorTraceGenerator extends PropertyListenerAdapter 
implements PublisherExtension {


	public ErrorTraceGenerator(Config conf, JPF jpf) {
		jpf.addPublisherExtension(ConsolePublisher.class, this);
	}

	public void propertyViolated(Search search) {
		JVM vm = search.getVM();
		System.out.println("======================= Lightweight Error Trace ==========================\n\n");
		System.out.println("Length of Error Trace: " + vm.getPathLength());

		ChoiceGenerator<?> cg = vm.getChoiceGenerator();
		while(cg != null) {
			printStateInfo(cg);
			cg = cg.getPreviousChoiceGenerator();
		}

		System.out.println("==============================================================\n\n\n");
	}

	private void printStateInfo(ChoiceGenerator<?> cg){
		try{
			String sourceLoc;
			if(cg.getSourceLocation() == null) {
				sourceLoc = "Source Not Available"+ "\n" 
				+ "CName: " + cg.getInsn().getMethodInfo().getClassName() + " " 
				+ "MName: " + cg.getInsn().getMethodInfo().getName() + " "
				+ "Loc: " + cg.getInsn().getPosition();
			} else {
				sourceLoc = cg.getInsn().getSourceLocation() + "\n" 
				+ cg.getInsn().getSourceLine();
			}
			System.out.println( "--------------------------------------------------- Thread"
					+ cg.getThreadInfo().getIndex() 
					+ "\n "+ sourceLoc  
			);
		}catch(NullPointerException e){
		}

	}

}