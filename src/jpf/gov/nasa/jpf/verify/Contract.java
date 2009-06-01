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

import gov.nasa.jpf.JPF;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;


/**
 * the base class for all our Contracts
 */
public abstract class Contract {

  protected static Logger log = JPF.getLogger("gov.nasa.jpf.test");

  Contract superContract;

  //--- the two methods to override
  public abstract boolean holds (VarLookup lookupPolicy);
  protected abstract void saveOldOperandValues (VarLookup lookup);


  //--- those encapsulate walking through our inherited contracts
  public void saveOldValues (VarLookup lookup) {
    for (Contract c = this; c != null; c = c.superContract) {
      if (!c.isEmpty()) {
        saveOldOperandValues(lookup);
      }
    }
  }

  public boolean holdsAny (VarLookup lookup) {
    for (Contract c=this; c != null; c = c.superContract){
      if (!c.isEmpty()) { // EmptyContract is always fulfilled
        if (c.holds(lookup)) {
          return true;
        }
      }
    }

    return false;
  }

  public boolean holdsAll (VarLookup lookup) {
    for (Contract c=this; c != null; c = c.superContract){
      if (!c.isEmpty()) { // EmptyContract is always fulfilled
        if (!c.holds(lookup)) {
          return false;
        }
      }
    }

    return true;
  }

  //--- auxiliary methods
  public void setSuperContract(Contract c){
    superContract = c;
  }

  public Contract getSuperContract(){
    return superContract;
  }

  public boolean isEmpty() {
    return false;
  }

  public String getErrorMessage(VarLookup lookupPolicy, String combinator) {

    StringBuilder sb = new StringBuilder();
    sb.append("\"");

    for (Contract c = this; c != null; c = c.superContract) {
      if (!c.isEmpty()) {
        if (c != this) {
          sb.append(' ');
          sb.append(combinator);
          sb.append(' ');
        }
        sb.append(c.toString());
      }
    }
    sb.append("\"");

    return sb.toString();
  }

  public boolean hasNonEmptyContracts() {
    if (isEmpty()) {
      if (superContract != null) {
        return superContract.hasNonEmptyContracts();
      } else {
        return false;
      }
    } else {
      return true;
    }
  }

  static double EPS = 1.0e-10;

  public int compareNumericValues (Object v1, Object v2) {

    if ( (v1 == null) || (v2 == null) ||
        !(v1 instanceof Number) || !(v2 instanceof Number)){
      throw new ContractException("numeric comparison for non numbers: " + v1 + ',' + v2);
    }

    if (v1 instanceof Double || v1 instanceof Float){
      double r = ((Number)v1).doubleValue() - ((Number)v2).doubleValue();

      if (Math.abs(r) < EPS ) {
        return 0;
      } else if (r < 0.0) {
        return -1;
      } else {
        return 1;
      }

    } else {
      int r = ((Number)v1).intValue() - ((Number)v2).intValue();

      if (r < 0) {
        return -1;
      } else if (r == 0) {
        return 0;
      } else {
        return 1;
      }
    }
  }



  //--- abstract bases for unary, binary and tertiary contracts

  abstract static class TertiaryContract extends Contract {
    Operand o1, o2, o3;

    protected TertiaryContract (Operand o1, Operand o2, Operand o3) {
      this.o1 = o1;
      this.o2 = o2;
      this.o3 = o3;
    }

    protected void saveOldOperandValues (VarLookup lookup) {
      o1.saveOldOperandValue(lookup);
      o2.saveOldOperandValue(lookup);
      o3.saveOldOperandValue(lookup);
    }
  }


  abstract static class BinaryContract extends Contract {
    Operand o1, o2;

    protected BinaryContract (Operand o1, Operand o2) {
      this.o1 = o1;
      this.o2 = o2;
    }

    protected void saveOldOperandValues (VarLookup lookup) {
      o1.saveOldOperandValue(lookup);
      o2.saveOldOperandValue(lookup);
    }
  }

  abstract static class UnaryContract extends Contract {
    Operand o1;

    protected UnaryContract (Operand o) {
      this.o1 = o;
    }

    protected void saveOldOperandValues (VarLookup lookup) {
      o1.saveOldOperandValue(lookup);
    }
  }


  //---- out concrete subclasses

  public static class EQ extends BinaryContract {
    public EQ (Operand o1, Operand o2) {
      super(o1,o2);
    }

    public boolean holds (VarLookup lookup) {
      Object v1 = o1.getValue(lookup);
      Object v2 = o2.getValue(lookup);

      if (v1 == v2) { // covers both 'null'
        return true;
      }
      if (v1 == null || v2 == null) {
        return false;
      }

      return v1.equals(v2);
    }

    public String toString() {
      return "(" + o1 + " == " + o2 + ")";
    }
  }


  public static class NE extends BinaryContract {
    public NE (Operand o1, Operand o2) {
      super(o1,o2);
    }

    public boolean holds (VarLookup lookup) {
      Object v1 = o1.getValue(lookup);
      Object v2 = o2.getValue(lookup);

      if (v1 == v2) { // covers both 'null'
        return false;
      }
      if (v1 == null || v2 == null) {
        return true;
      }

      return !v1.equals(v2);
    }

    public String toString() {
      return "(" + o1 + " != " + o2 + ")";
    }
  }


  public static class LT extends BinaryContract {
    public LT (Operand o1, Operand o2) {
      super(o1,o2);
    }

    public boolean holds (VarLookup lookup) {
      Object v1 = o1.getValue(lookup);
      Object v2 = o2.getValue(lookup);

      return compareNumericValues(v1,v2) < 0;
    }

    public String toString() {
      return "(" + o1 + " < " + o2 + ")";
    }
  }


  public static class LE  extends BinaryContract {
    public LE (Operand o1, Operand o2) {
      super(o1,o2);
    }

    public boolean holds (VarLookup lookup) {
      Object v1 = o1.getValue(lookup);
      Object v2 = o2.getValue(lookup);

      return compareNumericValues(v1,v2) <= 0;
    }

    public String toString() {
      return "(" + o1 + " <= " + o2 + ")";
    }
  }


  public static class GT extends BinaryContract {
    public GT (Operand o1, Operand o2) {
      super(o1,o2);
    }

    public boolean holds (VarLookup lookup) {
      Object v1 = o1.getValue(lookup);
      Object v2 = o2.getValue(lookup);

      return compareNumericValues(v1,v2) > 0;
    }

    public String toString() {
      return "(" + o1 + " > " + o2 + ")";
    }
  }


  public static class GE extends BinaryContract {
    public GE (Operand o1, Operand o2) {
      super(o1,o2);
    }

    public boolean holds (VarLookup lookup) {
      Object v1 = o1.getValue(lookup);
      Object v2 = o2.getValue(lookup);

      return compareNumericValues(v1,v2) >= 0;
    }

    public String toString() {
      return "(" + o1 + " >= " + o2 + ")";
    }
  }


  public static class IsEmpty extends UnaryContract {
    public IsEmpty (Operand o) {
      super(o);
    }

    public boolean holds (VarLookup lookup) {
      Object v = o1.getValue(lookup);
      return true;  // <2do>
    }

    public String toString() {
      return "(" + o1 + " isEmpty)";
    }
  }


  public static class NotEmpty extends UnaryContract {
    public NotEmpty (Operand o) {
      super(o);
    }

    public boolean holds (VarLookup lookup) {
      Object v = o1.getValue(lookup);
      return true;  // <2do>
    }

    public String toString() {
      return "(" + o1 + " notEmpty)";
    }
  }


  public static class InstanceOf extends UnaryContract {
    String type;

    public InstanceOf (Operand o, String type) {
      super(o);
      this.type = type;
    }

    public boolean holds (VarLookup lookup) {
      Object v = o1.getValue(lookup);
      return true;  // <2do>
    }

    public String toString() {
      return "(" + o1 + " instanceOf " + type + ")";
    }
  }


  public static class Within extends TertiaryContract {

    public Within (Operand testObj, Operand lowerBoundary, Operand upperBoundary) {
      super(testObj, lowerBoundary, upperBoundary);
    }

    public boolean holds (VarLookup lookup) {
      Object v1 = o1.getValue(lookup);
      Object v2 = o2.getValue(lookup);
      Object v3 = o3.getValue(lookup);

      return ((compareNumericValues(v1,v2) >= 0) && (compareNumericValues(v1,v3) <= 0));
    }

    public String toString() {
      return "(" + o1 + " within " + o2 + ',' + o3 + ")";
    }
  }


  public static class WithinCenter extends TertiaryContract {

    public WithinCenter (Operand testObj, Operand center, Operand delta) {
      super(testObj, center, delta);
    }

    boolean isFP(Number n) {
      return (n != null && (n instanceof Double || n instanceof Float));
    }

    public boolean holds (VarLookup lookup) {
      Number n1 = o1.getNumberValue(lookup);
      Number n2 = o2.getNumberValue(lookup);
      Number n3 = o3.getNumberValue(lookup);

      // if any of the operands is a floating point, convert all to double
      if (isFP(n1) || isFP(n2) || isFP(3)) {
        double d1 = n1.doubleValue();
        double d2 = n2.doubleValue();
        double d3 = n3.doubleValue();

        // <2do> we need open and closed intervals !!
        return ((d1 - EPS) >= (d2 - d3) && (d1 + EPS) <= (d2 + d3));

      } else {
        int i1 = n1.intValue();
        int i2 = n2.intValue();
        int i3 = n3.intValue();
        return (i1 >= (i2 - i3) && i1 <= (i2 + i3));
      }
    }

    public String toString() {
      return "(" + o1 + " within " + o2 + "+-" + o3 + ")";
    }
  }


  public static class Matches extends UnaryContract {
    String regex;

    public Matches (Operand o, String regex) {
      super(o);
      this.regex = regex;
    }

    public boolean holds (VarLookup lookup) {
      Object v1 = o1.getValue(lookup);

      if (v1 != null) {
        return regex.matches(v1.toString());
      } else {
        return false;
      }
    }

    public String toString() {
      return "(" + o1 + " matches \"" + regex + "\")";
    }
  }

}
