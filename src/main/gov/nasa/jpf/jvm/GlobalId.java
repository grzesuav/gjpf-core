//
// Copyright (C) 2012 United States Government as represented by the
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

package gov.nasa.jpf.jvm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.IntTable;

/**
 * utility class to compute search global id's based on per-thread and
 * execution context counts
 */
public class GlobalId extends ExecutionCount {

  private static int maxId = 0;
  private static IntTable<IntTable.Entry<ExecutionContext>> globalIdMap;
  
  public static boolean init (Config conf){
    int tblPow = conf.getInt("vm.id_size", 12);
    globalIdMap = new IntTable<IntTable.Entry<ExecutionContext>>(tblPow);
    
    return true;
  }
  
  
  public GlobalId (String name){
    super(name);
  }
  
  public int computeId (ThreadInfo ti){
    int id = 0;
    
    IntTable.Entry<ExecutionContext> ee = getExecEntry(ti);
    ee.val ++;
    
    IntTable.Entry<IntTable.Entry<ExecutionContext>> eGid = globalIdMap.get(ee); 
    if (eGid == null){
      id = maxId++;
      // NOTE - we have to clone ee since it can get modified
      globalIdMap.put(ee.clone(), id);
    } else {
      id = eGid.val;
    }
    
    //System.out.println("@@ " + name + "(" + Integer.toHexString(ee.hashCode()) + ") = " + id);
    
    return id;
  }
}
