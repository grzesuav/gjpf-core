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
package gov.nasa.jpf.jvm;


/**
 * helper class to store reference objects
 * <2do> check how 'isClass' correlates with ClassRef
 * 
 * <2do> - I'm a bit suspicious of that this is just a workaround for the
 * Static/DynamicElementInfo thing, and would go away if we get rid of
 * the statics
 */
public class ObjRef {
  public static final ObjRef NULL = new ObjRef(-1);
  
  int reference;
  boolean isClass;

  protected ObjRef (int ref, boolean isCls) {
    reference = ref;
    isClass = isCls;
  }

  public ObjRef (int r) {
    this(r, false);
  }

  public boolean isClass () {
    return isClass;
  }

  public boolean isNull () {
    return reference == -1;
  }

  public int getReference () {
    return reference;
  }

  public Object clone () {
    return new ObjRef(reference);
  }

  public boolean equals (Object o) {
    return (reference == ((ObjRef) o).reference) && 
           (isClass == ((ObjRef) o).isClass);
  }

  public int hashCode () {
    return reference;
  }

  public String toString () {
    JVM vm = JVM.getVM();

    if (isClass) { // StaticElementInfo
      return vm.getCurrentStaticArea().get(reference).toString(); // this is SO ugly, remove this
    } else {       // DynamicElementInfo
      return (vm.getHeap().get(reference)).toString();
    }
  }
}