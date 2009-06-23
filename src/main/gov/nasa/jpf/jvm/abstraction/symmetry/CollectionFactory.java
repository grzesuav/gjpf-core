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

import gov.nasa.jpf.jvm.Verify;

public abstract class CollectionFactory {
  /* ============== Public Interface ============ */
  
  public static <T> EqBag<T> newEqBag() {
    return deflt.createEqBag();
  }
  public static <T> EqSet<T> newEqSet() {
    return deflt.createEqSet();
  }
  public static <T> SymEqSet<T> newSymEqSet() {
    return deflt.createSymEqSet();
  }
  public static <T> SymEqBag<T> newSymEqBag() {
    return deflt.createSymEqBag();
  }
  
  
  
  /* ================== IMPLEMENTATION ================ */
  
  static final CollectionFactory deflt; // default
  static final CollectionFactory canonical;
  static final CollectionFactory fast;
  
  abstract <T> EqBag<T> createEqBag();
  abstract <T> EqSet<T> createEqSet();
  abstract <T> SymEqSet<T> createSymEqSet();
  abstract <T> SymEqBag<T> createSymEqBag();

  static {
    canonical = new CanonicalFactory();
    fast      = new FastFactory();
    deflt     = Verify.vmIsMatchingStates() ? canonical : fast;
  }

  static class CanonicalFactory extends CollectionFactory {
    public <T> EqBag<T> createEqBag () {
      return new CanonicalEqBag<T>();
    }

    public <T> EqSet<T> createEqSet () {
      return new CanonicalEqSet<T>();
    }

    public <T> SymEqBag<T> createSymEqBag () {
      return new CanonicalEqBag<T>();
    }

    public <T> SymEqSet<T> createSymEqSet () {
      return new CanonicalEqSet<T>();
    }
  }
  
  static class FastFactory extends CollectionFactory {
    public <T> EqBag<T> createEqBag () {
      return new FastEqBag<T>();
    }

    public <T> EqSet<T> createEqSet () {
      return new FastEqSet<T>();
    }

    public <T> SymEqBag<T> createSymEqBag () {
      return new FastEqBag<T>();
    }

    public <T> SymEqSet<T> createSymEqSet () {
      return new FastEqSet<T>();
    }
  }
}
