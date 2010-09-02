//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.jvm;

import gov.nasa.jpf.jvm.choice.RandomOrderLongCG;


/**
 * base type for 'long' choice generator types
 */
public abstract class LongChoiceGenerator extends ChoiceGenerator<Long> {

  protected LongChoiceGenerator (String id) {
    super(id);
  }

  public abstract Long getNextChoice ();

  public Class<Long> getChoiceType() {
    return Long.class;
  }

  public String toString () {
    return (super.toString() + " => " + getNextChoice());
  }

  /**
   * this is just our generic Decorator - if a concrete instance has a better
   * way of handling this w/o changing , it is free to override this method
   */
  public LongChoiceGenerator randomize () {
    return new RandomOrderLongCG(this);
  }
}
