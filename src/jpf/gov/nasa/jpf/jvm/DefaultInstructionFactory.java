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

import java.util.HashMap;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.bytecode.Instruction;

import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.InstructionHandle;

public class DefaultInstructionFactory implements InstructionFactory {
  static final String DEFAULT_CLASS_PREFIX = "gov.nasa.jpf.jvm.bytecode.";
  
  protected HashMap<Class<?>,Class<? extends Instruction>> bcel2jpf =
    new HashMap<Class<?>,Class<? extends Instruction>>(280);
  
  protected HashMap<String,Class<? extends Instruction>> map =
    new HashMap<String,Class<? extends Instruction>>(280);
  
  protected ClassLoader loader;
  
  public DefaultInstructionFactory (Config conf) {
    loader = conf.getCurrentClassLoader();
  }
  
  protected Class<? extends Instruction> mapBcel2Jpf(Class<?> bcelClass) throws ClassNotFoundException {
    Class<? extends Instruction> jpf = bcel2jpf.get(bcelClass);
    if (jpf == null) {
      String name = bcelClass.getName(); 
      if (!name.startsWith("org.apache.bcel.generic.")) {
        throw new JPFException("not a BCEL instruction type: " + name);
      }
      name = DEFAULT_CLASS_PREFIX + name.substring(24);
      jpf = loader.loadClass(name).asSubclass(Instruction.class);
      bcel2jpf.put(bcelClass, jpf);
    }
    return jpf;
  }
  
  public Instruction create (InstructionHandle h, int offset, MethodInfo m, ConstantPool cp) {
    try {
      Instruction insn = mapBcel2Jpf(h.getInstruction().getClass()).newInstance();
      insn.init(h, offset, m, cp);
      return insn;
    } catch (Throwable e) {
      throw new JPFException("creation of instruction " +
                             h.getInstruction() + " failed: " + e);
    }
  }
  
  public Instruction create (ClassInfo ciMth, String insnClsName){
    try {
      Class<? extends Instruction> cls = map.get(insnClsName);
      if (cls == null) {
         cls = (Class<? extends Instruction>)loader.loadClass(DEFAULT_CLASS_PREFIX + insnClsName);
         map.put(insnClsName, cls);
      }
      
      return cls.newInstance();
      
    } catch (Throwable e) {
      throw new JPFException("creation of instruction " +
                             insnClsName + " failed: " + e);
    }
  }
}
