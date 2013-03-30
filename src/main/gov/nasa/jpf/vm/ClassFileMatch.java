//
// Copyright (C) 2013 United States Government as represented by the
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

package gov.nasa.jpf.vm;

/**
 * a lookup match for a given typename in a ClassFileContainer
 */
public class ClassFileMatch {
  public final byte[] data;
  public final String typeName;
  public final String url;
  public final ClassFileContainer container;

  ClassFileMatch (String typeName, ClassFileContainer container, byte[] data) {
    this.typeName = typeName;
    this.container = container;
    this.data = data;
    
    this.url = container.getClassURL(typeName);
  }

  public byte[] getBytes () {
    return data;
  }

  public String getClassURL () {
    return url;
  }

  public ClassInfo createClassInfo (ClassLoaderInfo classLoader) throws ClassParseException {
    return container.createClassInfo( typeName, classLoader, url, data);
  }
  
  public AnnotationInfo createAnnotationInfo (ClassLoaderInfo classLoader) throws ClassParseException {
    return container.createAnnotationInfo( typeName, classLoader, data);
  }
}
