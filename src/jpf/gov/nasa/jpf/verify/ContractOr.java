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


/**
 * disjunction of two contracts
 */
public class ContractOr extends Contract {

  Contract c1, c2;
  
  public ContractOr (Contract c1, Contract c2){
    this.c1 = c1;
    this.c2 = c2;
  }
  
  public boolean holds (VarLookup lookupPolicy) {
    return c1.holds(lookupPolicy) || c2.holds(lookupPolicy);
  }

  protected void saveOldOperandValues (VarLookup lookupPolicy) {
    c1.saveOldOperandValues(lookupPolicy);
    c2.saveOldOperandValues(lookupPolicy);    
  }
  
  public String toString() {
    return "(" + c1 + " || " + c2 + ")";
  }

}
