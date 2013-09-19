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

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ClassParseException;
import gov.nasa.jpf.vm.ClassParser;
import gov.nasa.jpf.vm.DirectCallStackFrame;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.NativeMethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * a ClassInfo that was created from a Java classfile
 */
public class JVMClassInfo extends ClassInfo {
  
  public JVMClassInfo (String name, ClassLoaderInfo classLoader, ClassParser parser, String classFileUrl) throws ClassParseException {
    super( name, classLoader, parser, classFileUrl);
  }
  
  //--- for annotation classinfos
  
  // called on the annotation classinfo
  @Override
  protected ClassInfo createAnnotationProxy (String proxyName){
    return new JVMClassInfo (this, proxyName, classLoader, null);
  }
  
  // concrete proxy ctor
  protected JVMClassInfo (ClassInfo ciAnnotation, String proxyName, ClassLoaderInfo cli, String url) {
    super( ciAnnotation, proxyName, cli, url);
  }

  //--- call processing
  
  protected JVMCodeBuilder getSystemCodeBuilder (ClassFile cf, MethodInfo mi){
    JVMSystemClassLoaderInfo sysCl = (JVMSystemClassLoaderInfo) ClassLoaderInfo.getCurrentSystemClassLoader();
    JVMCodeBuilder cb = sysCl.getSystemCodeBuilder(cf, mi);
    
    return cb;
  }
  
  /**
   * to be called from super proxy ctor
   * this needs to be in the VM specific ClassInfo because we need to create code
   */
  @Override
  protected void setAnnotationValueGetterCode (MethodInfo pmi, FieldInfo fi){
    JVMCodeBuilder cb = getSystemCodeBuilder(null, pmi);

    cb.aload(0);
    cb.getfield( pmi.getName(), name, pmi.getReturnType());
    if (fi.isReference()) {
      cb.areturn();
    } else {
      if (fi.getStorageSize() == 1) {
        cb.ireturn();
      } else {
        cb.lreturn();
      }
    }

    cb.installCode();
  }
  
  @Override
  protected void setDirectCallCode (MethodInfo miDirectCall, MethodInfo miCallee){
    JVMCodeBuilder cb = getSystemCodeBuilder(null, miDirectCall);
    
    String calleeName = miCallee.getName();
    String calleeSig = miCallee.getSignature();

    if (miCallee.isStatic()){
      if (miCallee.isClinit()) {
        cb.invokeclinit(this);
      } else {
        cb.invokestatic( name, calleeName, calleeSig);
      }
    } else if (name.equals("<init>") || miCallee.isPrivate()){
      cb.invokespecial( name, calleeName, calleeSig);
    } else {
      cb.invokevirtual( name, calleeName, calleeSig);
    }

    cb.directcallreturn();
    
    cb.installCode();
  }
  
  @Override
  protected void setNativeCallCode (NativeMethodInfo miNative){
    JVMCodeBuilder cb = getSystemCodeBuilder(null, miNative);
    
    cb.executenative(miNative);
    cb.nativereturn();
    
    cb.installCode();
  }
  
  @Override
  protected void setRunStartCode (MethodInfo miStub, MethodInfo miRun){
    JVMCodeBuilder cb = getSystemCodeBuilder(null, miStub);
    
    cb.runStart( miStub);
    cb.invokevirtual( name, miRun.getName(), miRun.getSignature());
    cb.directcallreturn();
    
    cb.installCode();    
  }
  

  // create a stack frame that has properly initialized arguments
  @Override
  public StackFrame createStackFrame (ThreadInfo ti, MethodInfo callee){
    
    if (callee.isMJI()){
      NativeMethodInfo nativeCallee = (NativeMethodInfo) callee;
      JVMNativeStackFrame calleeFrame = new JVMNativeStackFrame( nativeCallee);
      calleeFrame.setArguments( ti);
      return calleeFrame; 
      
    } else {
      JVMStackFrame calleeFrame = new JVMStackFrame( callee);
      calleeFrame.setCallArguments( ti);
      return calleeFrame;      
    }
  }
  
  @Override
  public DirectCallStackFrame createDirectCallStackFrame (ThreadInfo ti, MethodInfo miCallee, int nLocals){
    int nOperands = miCallee.getNumberOfCallerStackSlots();
    
    MethodInfo miDirect = new MethodInfo(miCallee, nLocals, nOperands);
    setDirectCallCode( miDirect, miCallee);
    
    return new JVMDirectCallStackFrame( miDirect, miCallee);
  }
  
  /**
   * while this is a normal DirectCallStackFrame, it has different code which has to be created here 
   */
  @Override
  public DirectCallStackFrame createRunStartStackFrame (ThreadInfo ti, MethodInfo miRun){
    MethodInfo miDirect = new MethodInfo( miRun, 0, 1);
    setRunStartCode( miDirect, miRun);
    
    return new JVMDirectCallStackFrame( miDirect, miRun);
  }
  
  
  //--- for testing
  protected JVMClassInfo (ClassParser parser) throws ClassParseException {
    super( parser);
  }
  
}
