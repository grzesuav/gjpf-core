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

import gov.nasa.jpf.vm.FieldInfo;

/**
 * a matcher for a collection of FieldSpecs
 */
public class FieldSpecMatcher {

  protected FieldSpec[] fieldSpecs;
  
  public static FieldSpecMatcher create (String[] specs){
    if (specs != null && specs.length > 0){
      return new FieldSpecMatcher(specs);
    } else {
      return null;
    }
  }
  
  public FieldSpecMatcher(String[] specs){
    int len = specs.length;
    fieldSpecs = new FieldSpec[len];
    for (int i=0; i<len; i++){
      fieldSpecs[i] = FieldSpec.createFieldSpec(specs[i]);
    }
  }
  
  public boolean matches (FieldInfo fi){
    for (int i=0; i<fieldSpecs.length; i++){
      if (fieldSpecs[i].matches(fi)){
        return true;
      }
    }
    
    return false;
  }
}
