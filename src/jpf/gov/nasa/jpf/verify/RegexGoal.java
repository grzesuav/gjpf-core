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

/**
 * regular expression matcher goal
 * note that we match against ret.toString(), i.e. can check types, values whatever
 */
public class RegexGoal extends Goal {

  String regex;
  
  public RegexGoal (String regex){
    this.regex = regex;
  }
  
  public boolean postCheck (TestContext tctx, Method m, Object res, Throwable t) {
    
    if (res != null){
      String s = res.toString();
      return s.matches(regex);
    }
    
    return false;
  }

  public Class<?> getGoalType () {
    return Object.class;
  }

  public void printOn (PrintWriter pw) {
    pw.print("matches \"" + regex + "\"");
  }

}
