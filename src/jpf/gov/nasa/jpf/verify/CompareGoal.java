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
 * check for no exception and compare result with value set
 */
public class CompareGoal extends Goal {

  public enum Operator {
    EQ("=="),
    NE("!="),
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">=");
    
    private String op;
    
    Operator(String s){
      op = s;
    }
    public String toString() {
      return op;
    }
  } 
  
  Operator op;
  ValSet values;
    
  CompareGoal (Operator compareOp, ValSet compareVal){
    op = compareOp;
    values = compareVal;
  }
  
  public Class<?> getGoalType () {
    if (op == op.EQ || op == op.NE){
      return Object.class; // doesn't really matter, we use equals()
    } else {
      return Number.class;
    }
  }
  
  public void printOn(PrintWriter pw){
    if (op != null){
      pw.print('"'); pw.print(op); pw.print('"');
    }
    
    if (values != null){
      if (op != null){
        pw.print(' ');
      }
      values.printOn(pw);
    }
  }
  
  void resolveFieldReferences (TestContext tctx) throws TestException {
    List<Object> v = values.getValues();
    int n = v.size();
    
    for (int i=0; i<n; i++){
      Object o = v.get(i);
      if (o instanceof FieldReference){
        Object fv = tctx.getFieldValue((FieldReference)o);
        if (fv == null){
          throw new TestException("cannot resolve compare value " + o);
        } else {
          v.set(i, fv);
        }
      }
    }
  }

  public boolean postCheck (TestContext tctx, Method m, Object res, Throwable t)
                   throws TestException {
    
    resolveFieldReferences(tctx);
    
    switch (op){
    case EQ:     // any of the values equals
      for (Object v : values){
        if (v.equals(res)){
          return true;
        }
      }
      break;
      
    case NE:     // none of the values equals
      for (Object v : values){
        if (v.equals(res)){
          return false;
        }
      }
      return true;
      
    case LT: 
    case LE:
    case GT:
    case GE:
      if (!(res instanceof Number)){
        throw new TestException("can't compare non-Number return type: " + res);
      }
      double r = ((Number)res).doubleValue();
      
      for (Object v : values){
        if (v instanceof Number){
          double d = ((Number)v).doubleValue();
          if (!compare(r,d)){
            return false;
          }
          
        } else {
          throw new TestException("can't compare to non-Number type: " + v);
        }
      }
      
      return true;
    }
        
    return false;
  }
  
  boolean compare (double r, double d) {
    switch (op){
    case LT: 
      if (!(r < d)){
        return false;
      }
      break;
    case LE:
      if (!(r <= d)){
        return false;
      }
      break;
    case GT:
      if (!(r > d)){
        return false;
      }
      break;
    case GE:
      if (!(r >= d)){
        return false;
      }
      break;
    default:
      return false;
    }
    
    return true;    
  }
}