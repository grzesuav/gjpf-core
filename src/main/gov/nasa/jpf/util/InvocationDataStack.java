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

package gov.nasa.jpf.util;

import gov.nasa.jpf.JPFException;

/**
 * Stack that is used to save native data methods data between repetetive calls.
 * Native method can call non-native methods during it's execution. In this case
 * native method would be called again after non-native method finished. But non-native
 * method can call another methods and one of them can be native previously called
 * native method.
 * @author Ivan Mushketik
 */
public class InvocationDataStack {

  InvocationData top;

  public InvocationData get() {
    return top;
  }

  public void remove() {
    if (top == null) {
      throw new JPFException("Removing from empty InvocationDataStack");
    }

    top = top.prev;
  }

  public void add(InvocationData id) {
    id.prev = top;
    top = id;
  }
}
