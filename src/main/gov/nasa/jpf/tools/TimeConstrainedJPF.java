//
// Copyright (C) 2007 United States Government as represented by the
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

package gov.nasa.jpf.tools;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.search.SearchListener;


/**
 * a listener to gracefully stop JPF after a specified amount of time 
 * and yet allow for the printing of statistics at the end of the search
 * use the property jpf.time_limit <time_in_seconds> to configure
 */
public class TimeConstrainedJPF implements SearchListener{
	
	static long maxTime = 0;
	static long startTime = 0;
	
	public void searchStarted(Search search){
		JVM vm = search.getVM();
		Config config = vm.getConfig();
		this.startTime = System.currentTimeMillis();
		this.maxTime = config.getInt("jpf.time_limit", -1);
		System.out.println("****TIME BOUNDED SEARCH - LIMIT SET TO (SECONDS): " + 
				maxTime + " ****");
		this.maxTime = this.maxTime * 1000; //convert to milli
	}
	
	public void stateAdvanced(Search search){
		long duration = System.currentTimeMillis() - this.startTime;
		if (duration >= maxTime){
			duration = duration/1000;
			System.out.println("****TIME BOUNDED SEARCH - TOTAL TIME (SECONDS): " + 
					duration + " ****");
			search.terminate();
		}
	}
	
	public void searchFinished(Search search){
	}
	
	public void searchConstraintHit(Search search){
	}
	
	public void stateBacktracked(Search search){
	}
	
	public void stateProcessed(Search search){
	}
	
	public void stateStored(Search search){
	}
	
	public void stateRestored(Search search){
	}
	
	public void propertyViolated(Search search){
	}
}
