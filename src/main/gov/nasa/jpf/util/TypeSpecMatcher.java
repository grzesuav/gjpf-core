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

import gov.nasa.jpf.vm.ClassInfo;

/**
 * a matcher for collections of TypeSpecs
 */
public class TypeSpecMatcher {

  protected TypeSpec[] mypeSpecs;
  
  public static TypeSpecMatcher create (String[] specs){
    if (specs != null && specs.length > 0){
      return new TypeSpecMatcher(specs);
    } else {
      return null;
    }
  }
  
  public TypeSpecMatcher(String[] specs){
    int len = specs.length;
    mypeSpecs = new TypeSpec[len];
    for (int i=0; i<len; i++){
      mypeSpecs[i] = TypeSpec.createTypeSpec(specs[i]);
    }
  }
  
  public boolean matches (ClassInfo ci){
    for (int i=0; i<mypeSpecs.length; i++){
      if (mypeSpecs[i].matches(ci)){
        return true;
      }
    }
    
    return false;
  }
}
