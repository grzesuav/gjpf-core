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

import java.util.Date;

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JVM;

/**
 * represents contract operands
 */
public abstract class Operand {

  /**
   * values are returned either as ElementInfos or null (for references), or as
   * Numbers (for builtin numeric types).
   */
  public abstract Object getValue(VarLookup lookup);

  public void saveOldOperandValue(VarLookup lookup) {
    // only for 'Old' expressions
  }


  /****************************************************************************
   * since predicates are evaluated at the JVM level, we have to translate
   * values from JPF into VM objects
   */

  public Number getNumberValue (VarLookup lookup) {
    Object v = getValue(lookup);

    if ((v != null) && (v instanceof Number)) {
      return (Number)v;
    } else {
      throw new ContractException("number expected: " + this);
    }
  }

  public boolean isReferenceValue (VarLookup lookup) {
    Object v = getValue(lookup);
    return (v != null) && (v instanceof ElementInfo);
  }


  /****************************************************************************
   * an operand representing a field reference (variable)
   */
  public static class VarRef extends Operand {

    String id;

    public VarRef(String id){
      this.id = id;
    }

    public Object getValue (VarLookup vl) {
      // <2do> array subscripts??
      return vl.lookup(id);
    }

    public String toString() {
      return id;
    }
  }


  /****************************************************************************
   * an operand representing a class reference (const value)
   */
  public static class ClassRef extends Operand {
    String clsName;

    public ClassRef (String clsName){
      this.clsName = clsName;
    }

    public Object getValue (VarLookup vl){
      return ClassInfo.getClassInfo(clsName);
    }

    public String toString() {
      return "class " + clsName;
    }
  }


  /****************************************************************************
   * an operand representing a const value (given as literal)
   */
  public static class Const extends Operand {
    Object value;

    public Const (Object val){
      value = val;
    }

    public Object getValue (VarLookup vl){
      return value;
    }

    public String toString() {
      if (value == null) {
        return "null";
      } else {
        return value.toString();
      }
    }
  }

  //--- our standard const values
  static final public Const NULL = new Const(null);
  static final public Const EPS = new Const(1.0e-6); // should be configurable


  /****************************************************************************
   * an operand representing a method return value
   */
  public static final String RESULT = "Result";

  public static class Result extends Operand {
    public Result() {
      // nothing
    }

    public Object getValue (VarLookup lookup) {
      return lookup.getValue(RESULT);
    }

    public String toString() {
      return RESULT;
    }
  }
}
