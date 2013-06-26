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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.vm.ClassFileContainer;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ClassParseException;
import gov.nasa.jpf.vm.ClassParser;
import java.io.File;
import java.io.IOException;

/**
 * default implementation of a JVM specific ClassFactory
 */
public class DefaultJVMClassFactory implements JVMClassFactory {
  
  protected JVMInstructionFactory jvmInsnFactory;
  
  public DefaultJVMClassFactory (Config config) {
    jvmInsnFactory = config.getEssentialInstance("jvm.insn_factory.class", JVMInstructionFactory.class);
    
    if (!JVMCodeBuilder.init(this)){
      throw new JPFConfigException("JVMCodeBuilder failed to initialize");
    }
  }
  
  public DefaultJVMClassFactory (){
    jvmInsnFactory = new gov.nasa.jpf.jvm.bytecode.InstructionFactory();
    
    if (!JVMCodeBuilder.init(this)){
      throw new JPFConfigException("JVMCodeBuilder failed to initialize");
    }
  }
  
  // for testing only
  protected DefaultJVMClassFactory (JVMInstructionFactory jvmInsnFactory){
    this.jvmInsnFactory = jvmInsnFactory;
  }
  
  @Override
  public JVMInstructionFactory getJVMInstructionFactory (){
    return jvmInsnFactory;
  }
  
  @Override
  public ClassFileContainer createClassFileContainer (String spec) {
    int i = spec.indexOf(".jar");
    if (i > 0) {
      // its a jar
      int j = i + 4;
      int len = spec.length();
      String jarPath;
      String pathPrefix = null;
      File jarFile;
      if (j == len) {
        // no path prefix, plain jar
        jarPath = spec;
      } else {
        if (spec.charAt(j) == '/') {
          pathPrefix = spec.substring(j);
          jarPath = spec.substring(0, j);
        } else {
          return null;
        }
      }
      jarFile = new File(jarPath);
      if (jarFile.isFile()) {
        try {
          return new JarClassFileContainer(jarFile, pathPrefix);
        } catch (IOException ix) {
          return null;
        }
      } else {
        return null;
      }
    } else {
      // a dir
      File dir = new File(spec);
      if (dir.isDirectory()) {
        return new DirClassFileContainer(dir);
      } else {
        return null;
      }
    }
  }

  @Override
  public ClassParser createClassParser (byte[] data, int offset){
    ClassFile cf = new ClassFile( data, offset);
    return new ClassFileParser(cf);
  }

  @Override
  public ClassInfo createClassInfo (String typeName, ClassLoaderInfo classLoader, String url, byte[] data, int offset, int length) throws ClassParseException {
    ClassParser parser = createClassParser( data, offset);
    JVMClassInfo ci = new JVMClassInfo(typeName, classLoader, parser, url);
    return ci;
  }

}
