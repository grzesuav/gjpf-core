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

package gov.nasa.jpf.jvm;

import gov.nasa.jpf.vm.AnnotationInfo;
import gov.nasa.jpf.vm.AnnotationParser;
import gov.nasa.jpf.vm.ClassFileContainer;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ClassParseException;
import gov.nasa.jpf.vm.ClassParser;

/**
 * ClassFileContainer that holds Java classfiles
 */
public abstract class JVMClassFileContainer extends ClassFileContainer {
  
  protected JVMClassFileContainer (String name) {
    super(name);
  }
  
  @Override
  public String getClassURL (String typeName){
    return getURL() + typeName.replace('.', '/') + ".class";
  }
  
  @Override
  public ClassInfo createClassInfo (String typeName, ClassLoaderInfo classLoader, String url, byte[] data) throws ClassParseException {
    ClassFile cf = new ClassFile( data);
    ClassParser parser = new ClassFileParser(cf);
    
    return new JVMClassInfo( typeName, classLoader, parser, url);
  }
  
  @Override
  public AnnotationInfo createAnnotationInfo (String typeName, ClassLoaderInfo classLoader, byte[] data) throws ClassParseException {
    ClassFile cf = new ClassFile( data);
    JVMAnnotationParser parser = new JVMAnnotationParser(cf);
    
    return new AnnotationInfo( typeName, classLoader, parser);
  }
}
