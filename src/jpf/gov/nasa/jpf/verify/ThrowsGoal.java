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

import gov.nasa.jpf.util.StringSetMatcher;

import java.io.PrintWriter;
import java.lang.reflect.Method;

/**
 * check for a thrown exception that matches the given class name values
 */
public class ThrowsGoal extends Goal {

  String clsName;
  
  public ThrowsGoal (String clsName){
    this.clsName = clsName;
  }
  
  public boolean postCheck (TestContext tctx, Method m, Object res, Throwable t) {
    if (t != null){
      String tClsName = t.getClass().getName();
      
      StringSetMatcher matcher = new StringSetMatcher(clsName);
      boolean ret = matcher.matchesAny(tClsName);
      
      if (!ret){ // check if it's a java.lang.* exception
        if (clsName.indexOf('.') < 0 && tClsName.startsWith("java.lang.")){
          return matcher.matchesAny(tClsName.substring(10));
        }
        
        // <2do> we should check for same package here
      }
      
      return ret;
    }
    
    return false;
  }
  
  public void printOn (PrintWriter pw) {
    pw.print( "throws " + clsName);
  }

}
