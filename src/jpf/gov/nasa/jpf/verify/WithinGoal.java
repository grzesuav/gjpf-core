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
 * check value interval
 */
public class WithinGoal extends Goal {

  Object v0,v1;
  boolean isDelta;
  
  public WithinGoal (Object v0, Object v1, boolean isDelta){
    this.v0 = v0;
    this.v1 = v1;
    this.isDelta = isDelta;
  }

  void resolveFieldReferences (TestContext tctx) throws TestException {

    if (v0 instanceof FieldReference){
      v0 = tctx.getFieldValue((FieldReference)v0);
      if (v0 == null){
        throw new TestException("cannot resolve boundary " + v0);
      }
    }
    
    if (v1 instanceof FieldReference){
      v1 = tctx.getFieldValue((FieldReference)v1);
      if (v1 == null){
        throw new TestException("cannot resolve boundary " + v1);
      }
    }
  }
    
  void normalizeBoundaries () throws TestException {
    if (v0 instanceof Double){
      if (!(v1 instanceof Double)){
        v1 = new Double(((Number)v1).doubleValue());
      }
    } else if (v1 instanceof Double){
      if (!(v0 instanceof Double)){
        v0 = new Double(((Number)v0).doubleValue());
      }      
    } else {
      if (!(v0 instanceof Integer)){
        v0 = new Integer(((Number)v0).intValue());
      }
      if (!(v1 instanceof Integer)){
        v1 = new Integer(((Number)v1).intValue());
      }
    }
    
    if ((v0.getClass() != v1.getClass()) || !(v0 instanceof Number)){
      throw new TestException("incompatible boundary types: " + v0 + "," + v1);
    }
  }
  
  void promoteToDoubleBoundaries (TestContext tctx, Class<?> realCls){
    v0 = new Double(((Number)v0).doubleValue());
    v1 = new Double(((Number)v1).doubleValue());    
  }
  
  Object promoteResult (Number res){
    if (v0 instanceof Double){
      if (!(res instanceof Double)){
        return new Double(res.doubleValue());
      }
    } else if (v0 instanceof Integer){
      if (!(res instanceof Integer)){
        return new Integer(res.intValue());
      }
    }
    
    return res;
  }
  
  public boolean postCheck (TestContext tctx, Method m, Object res, Throwable t) 
                    throws TestException {
    
    if (!(res instanceof Number)){
      throw new TestException("result type not a number: " + res);
    }
  
    resolveFieldReferences(tctx);     // first, resolve field refs
    normalizeBoundaries();            // second, make boundaries same type
    
    // now normalize to the maximum precision used in either the result or the boundaries
    if (res instanceof Double){
      // if res is a real, our boundaries have to be, too
      promoteToDoubleBoundaries(tctx, res.getClass());
    } else {
      // promote res to whatever our boundaries are
      res = promoteResult((Number)res);
    }
    
    if (v0 instanceof Double){
      double d0 = (Double)v0;
      double d1 = (Double)v1;
      double r = (Double)res;
      
      if (isDelta){
        return (r >= (d0-d1) && r <= (d0+d1));
      } else {
        return (r >= d0 && r <= d1);
      }
      
    } else if (v0 instanceof Integer){
      double i0 = (Integer)v0;
      double i1 = (Integer)v1;
      double r = (Integer)res;
      
      if (isDelta){
        return (r >= (i0-i1) && r <= (i0+i1));
      } else {
        return (r >= i0 && r <= i1);
      }
    }
    
    return false;
  }

  public Class<?> getGoalType () {
    return Number.class;
  }

  public void printOn (PrintWriter pw) {
    if (isDelta){
      pw.print(v0);
      pw.print("+-");
      pw.print(v1);
    } else {
      pw.print(v0);
      pw.print(",");
      pw.print(v1);
    }
  }

}
