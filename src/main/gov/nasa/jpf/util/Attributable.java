//
// Copyright (C) 2014 United States Government as represented by the
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

package gov.nasa.jpf.util;

/**
 * interface for types that support attributes
 */
public interface Attributable {

  boolean hasAttr ();
  boolean hasAttr (Class<?> attrType);
  Object getAttr();
  void setAttr (Object a);
  void addAttr (Object a);
  void removeAttr (Object a);
  void replaceAttr (Object oldAttr, Object newAttr);
  <T> T getAttr (Class<T> attrType);
  <T> T getNextAttr (Class<T> attrType, Object prev);
  ObjectList.Iterator attrIterator();
  <T> ObjectList.TypedIterator<T> attrIterator(Class<T> attrType);
  
}
