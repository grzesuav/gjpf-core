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

import gov.nasa.jpf.jvm.ModelAPI;

import java.util.Collection;

@ModelAPI
public interface SymEqCollection<T> {
  boolean add (T o);

  boolean addAll (Collection<? extends T> c);
  boolean addAll (SymEqCollection<? extends T> c);
  
  void clear ();

  boolean contains (Object o);

  boolean containsAll (Collection<?> c);
  boolean containsAll (SymEqCollection<?> c);

  boolean isEmpty ();

  boolean remove (Object o);

  boolean removeAll (Collection<?> c);
  boolean removeAll (SymEqCollection<?> c);

  boolean retainAll (Collection<?> c);
  boolean retainAll (SymEqCollection<?> c);

  int size ();
  
  SymEqCollection<T> clone();
}
