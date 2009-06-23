//
// Copyright (C) 2006 United States Government as represented by the
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
package gov.nasa.jpf.jvm.abstraction.symmetry;


// right now it uses a unary representation.
// could be improved by adding multiplicity field.
class FastEqBag<T> extends FastEqSet<T> implements EqBag<T> {
  public FastEqBag(int pow) {
    super(pow);
  }

  public FastEqBag() {
    super();
  }

  // add even if there's already one like it
  public boolean add (T o) {
    strictAdd(o);
    return true;
  }
  
  // remove all in the bag
  public boolean remove (Object o) {
    boolean modified = false;
    while(delete(o) != null) modified = true;
    return modified;
  }

  public FastEqBag<T> clone() {
    FastEqBag<T> that = new FastEqBag<T>(tblPow);
    for (T o : this) {
      that.strictAdd(o);
    }
    return that;
  }
}
