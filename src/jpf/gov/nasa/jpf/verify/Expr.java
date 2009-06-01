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

import java.util.List;

public abstract class Expr extends Operand {
      
  static abstract class UnaryExpr extends Expr {
    Operand o;
    
    UnaryExpr (Operand o){
      this.o = o;
    }

    public void saveOldOperandValue(VarLookup lookup) {
      o.saveOldOperandValue(lookup);
    }
  }
  
  static abstract class BinaryExpr extends Expr {
    Operand a, b;
    
    BinaryExpr (Operand a, Operand b){
      this.a = a;
      this.b = b;
    }
    
    public void saveOldOperandValue(VarLookup lookup) {
      a.saveOldOperandValue(lookup);
      b.saveOldOperandValue(lookup);
    }

  }
  
  public static class Mult extends BinaryExpr {
    
    public Mult (Operand factor1, Operand factor2) {
      super(factor1, factor2);
    }
  
    public Object getValue (VarLookup lookup) {
      Number na = a.getNumberValue(lookup);
      Number nb = b.getNumberValue(lookup);
      
      if (na instanceof Double || nb instanceof Double ||
          na instanceof Float || nb instanceof Float) {
        return new Double( na.doubleValue() * nb.doubleValue());
      } else {
        return new Integer(na.intValue() * nb.intValue());
      }
    }
    
    public String toString() {
      return "(" + a + "*" + b + ")";
    }
  }
  
  public static class Div extends BinaryExpr {

    public Div (Operand divident, Operand divisor) {
      super(divident,divisor);
    }
    
    public Object getValue (VarLookup lookup) {
      Number na = a.getNumberValue(lookup);
      Number nb = b.getNumberValue(lookup);
      
      if (na instanceof Double || nb instanceof Double ||
          na instanceof Float || nb instanceof Float) {
        return new Double( na.doubleValue() / nb.doubleValue());
      } else {
        return new Integer(na.intValue() / nb.intValue());
      }
    }

    public String toString() {
      return "(" + a + "/" + b + ")";
    }
  }  
  
  public static class Plus extends BinaryExpr {
    public Plus (Operand a, Operand b) {
      super(a,b);
    }

    public Object getValue (VarLookup lookup) {
      Number na = a.getNumberValue(lookup);
      Number nb = b.getNumberValue(lookup);
      
      if (na instanceof Double || nb instanceof Double ||
          na instanceof Float || nb instanceof Float) {
        return new Double( na.doubleValue() + nb.doubleValue());
      } else {
        return new Integer(na.intValue() + nb.intValue());
      }
    }

    public String toString() {
      return "(" + a + "+" + b + ")";
    }
  }
  
  public static class Minus extends BinaryExpr {
    public Minus (Operand a, Operand b) {
      super(a,b);      
    }
    
    public Object getValue (VarLookup lookup) {
      Number na = a.getNumberValue(lookup);
      Number nb = b.getNumberValue(lookup);
      
      if (na instanceof Double || nb instanceof Double ||
          na instanceof Float || nb instanceof Float) {
        return new Double( na.doubleValue() - nb.doubleValue());
      } else {
        return new Integer(na.intValue() - nb.intValue());
      }
    }
    
    public String toString() {
      return "(" + a + "-" + b + ")";
    }
  }

  public static class Log extends UnaryExpr {
    public Log (Operand x) {
      super(x);
    }
    
    public Object getValue (VarLookup lookup) {
      Number no = o.getNumberValue(lookup);

      return new Double(Math.log(no.doubleValue()));
    }
    
    public String toString() {
      return "log(" + o + ")";
    }
  }
  
  public static class Log10 extends UnaryExpr {
    public Log10 (Operand x) {
      super(x);
    }
    
    public Object getValue (VarLookup lookup) {
      Number no = o.getNumberValue(lookup);
      
      return new Double(Math.log10(no.doubleValue()));
    }

    public String toString() {
      return "log10(" + o + ")";
    }
  }
  
  
  public static class Pow extends BinaryExpr {
    public Pow (Operand base, Operand exponent) {
      super(base,exponent);
    }

    public Object getValue (VarLookup lookup) {
      Number na = a.getNumberValue(lookup);
      Number nb = b.getNumberValue(lookup);
      
      if (na instanceof Double || nb instanceof Double ||
          na instanceof Float || nb instanceof Float) {
        return new Double( Math.pow(na.doubleValue(), nb.doubleValue()));
      } else {
        return new Integer( (int)Math.pow(na.doubleValue(), nb.doubleValue()));
      }
    }

    public String toString() {
      return "pow(" + a + "," + b + ")";
    }
  }

  public static class Old extends Expr {
    Operand o;
    
    public Old (Operand o) {
      this.o = o;
    }
  
    public Object getValue(VarLookup lookup) {
      return lookup.lookup(this);
    }
    
    public void saveOldOperandValue(VarLookup lookup) {
      if (!lookup.containsKey(this)) {
        Object v = o.getValue(lookup);
        lookup.put(this, v);
      }
    }
    
    public String toString() {
      return "old(" + o + ")";
    }
  }
  
  public static class Func extends Expr {
    String id;
    List<Operand> args;
    
    public Func (String id, List<Operand> args) {
      this.id = id;
      this.args = args;
    }
    
    public Object getValue(VarLookup lookup) {
      return null; // <2do>
    }
    
    public void saveOldOperandValue(VarLookup lookup) {
      for (Operand a : args) {
        a.saveOldOperandValue(lookup);
      }
    }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      
      sb.append(id);
      sb.append('(');

      for (Operand a : args) {
        if (!first) {
          sb.append(',');
        } else {
          first = false;
        }
        sb.append(a);
      }

      sb.append(')');
      return sb.toString();
    }

  }
}
