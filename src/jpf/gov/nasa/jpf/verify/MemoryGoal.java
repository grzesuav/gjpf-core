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

package gov.nasa.jpf.verify;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

/**
 * test allocation constraints
 */
public class MemoryGoal extends Goal {

  TestContext tctx;
  
  long maxGrowth;
  long freeMem1, freeMem2;
  
  public MemoryGoal (TestContext tctx, ArgList args) throws TestException {
    this.tctx = tctx;
    
    if (args != null){
      List<Object[]> al = args.getArgCombinations();
      if (al.size() != 1 ||
          al.get(0).length != 1 || !(al.get(0)[0] instanceof Number)){
        throw new TestException("MemoryGoal only accepts one number parameter");
      }
      maxGrowth = ((Number)al.get(0)[0]).longValue();
      
    } else {
      maxGrowth = 0;
    }
  }
  
  public MemoryGoal (long maxGrowth){
    this.maxGrowth = maxGrowth;
  }
  
  public boolean preCheck (TestContext tctx, Method m) {
    Runtime rt = Runtime.getRuntime();
    rt.gc();
    freeMem1 = rt.freeMemory();
    
    return true;
  }
  
  public boolean postCheck (TestContext tctx, Method m, Object res, Throwable t){
    Runtime rt = Runtime.getRuntime();
    rt.gc();
    freeMem2 = rt.freeMemory();
    long diff = freeMem1 - freeMem2;

    tctx.log("memory growth: " + diff + " bytes");
    
    return (diff <= maxGrowth);
  }
    
  public void printOn (PrintWriter pw) {
    pw.print("alloc limit <= ");
    pw.print( maxGrowth);
  }

}
