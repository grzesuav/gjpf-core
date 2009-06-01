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

import gov.nasa.jpf.tools.MethodTester;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * represents a test goal
 */
public abstract class Goal {
    
  public boolean preCheck (TestContext tctx, Method m) throws TestException {
    return true;
  }
  
  public boolean postCheck (TestContext tctx, Method m, Object res, Throwable t)
            throws TestException {
    return true;
  }
  
  public Class<?> getGoalType () {
    return null;
  }
  
  public String toString() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    printOn(pw);
    return sw.toString();
  }
  
  public void printOn (PrintWriter pw) {
    pw.print("goal ");
    pw.print( getClass().getName());
  }
  
  // this is here so that goals can do their own execution (e.g. for concurrency tests)
  public Object execute (MethodTester tester, Method m, Object tgt, Object[] mthArgs)
                         throws InvocationTargetException, IllegalAccessException {
    return m.invoke(tgt,mthArgs);
  }
}


