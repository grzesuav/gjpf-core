//
// Copyright (C) 2007 United States Government as represented by the
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

import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.InstructionHandle;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DefaultInstructionFactory;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;

/**
 * generic InstructionFactory that defaults to our standard, concrete
 * execution bytecodes, but can be configured in terms of overridden bytecodes
 * and instrumented target classes (i.e. the classes that should use the
 * overridden bytecodes).
 * All overridden bytecode classes have to share a common prefix (package name
 * or enclosing class)
 */
public abstract class GenericInstructionFactory extends DefaultInstructionFactory {

  protected static final String BCEL_PACKAGE = "org.apache.bcel.generic.";
  
  static Logger log = JPF.getLogger("gov.nasa.jpf.bytecode");
  
  protected String[] bcNames;  // which bytecodes do we replace
  protected String   bcPrefix; // where do their classes reside
  
  // filter using an explicit set of class names (can be used for one-pass load)
  protected StringSetMatcher includes;  // included classes that should use them
  protected StringSetMatcher excludes;  // excluded classes (that should NOT use them)

  // filter using base/derived class sets (only useful in subsequent pass)
  ClassInfo ciLeaf;
  ClassInfo ciRoot;
  
  protected HashMap<Class<?>,Class<? extends Instruction>> ownBcel2jpf;  
  protected HashMap<String,Class<? extends Instruction>> ownMap;

  
  protected GenericInstructionFactory (Config conf, String bcPrefix, String[] bcNames,
                             String[] includeCls, String[] excludeCls){
    super(conf);
    
    this.bcPrefix = bcPrefix;
    this.bcNames = bcNames;

    includes = StringSetMatcher.getNonEmpty(includeCls);    
    excludes = StringSetMatcher.getNonEmpty(excludeCls);

    createMaps();
  }

  public void setLeafClassInfo (ClassInfo ciLeaf){
    this.ciLeaf = ciLeaf;
  }
  
  public void setRootClassInfo (ClassInfo ciRoot){
    this.ciRoot = ciRoot;
  }
  
  protected void createMaps() {
    
    ownBcel2jpf = new HashMap<Class<?>,Class<? extends Instruction>>(bcNames.length);
    ownMap = new HashMap<String,Class<? extends Instruction>>(bcNames.length);
    
    for (String cls : bcNames) {
      Class<?> bcelCls = null;
      Class<? extends gov.nasa.jpf.jvm.bytecode.Instruction> extCls = null;

      try {
        extCls = 
          (Class<? extends gov.nasa.jpf.jvm.bytecode.Instruction>)loader.loadClass(bcPrefix + cls);
      } catch (ClassNotFoundException cnfx) {
        log.warning("could not load overridden instruction class: " + cnfx.getMessage());
      }

      try {
        bcelCls = loader.loadClass(BCEL_PACKAGE + cls); // <?> do we actually want this to be overridable?
      } catch (ClassNotFoundException x1){
        // possible if this is one of our artificial BCs
        // <2do> we could check for extension BCs here (via instance)
      }

      ownBcel2jpf.put(bcelCls,extCls);
      ownMap.put(cls,extCls);
    }
  }
  
  public boolean isInstrumentedClass (ClassInfo ci){
    if (ci == null){
      
      // <??> not clear what to do in this case, since we have nothing to
      // filter on. Since all reflection calls come in here, it's probably
      // better to instrument by default (until we have a better mechanism)
      return true;
      
      /**
      if (includes != null || excludes != null || ciLeaf != null || ciRoot != null){
        return false;
      } else {
        return true;
      }
      **/
      
    } else {
      String clsName = ci.getName();
    
      if (StringSetMatcher.isMatch(clsName, includes, excludes)){
        if (ciLeaf == null || ciLeaf.isInstanceOf(ci)){
          if (ciRoot == null || ci.isInstanceOf(ciRoot)){
            return true;
          }
        }
      }
    }
    
    return false;
  }
  
  // the bcel factory method (including initialization)
  public Instruction create (InstructionHandle h, int offset, MethodInfo m, ConstantPool cp) {
    
    try {
      if (isInstrumentedClass(m.getClassInfo())) {
        Class<? extends Instruction> insnCls = ownBcel2jpf.get(h.getInstruction().getClass());
        if (insnCls != null){
          Instruction insn = insnCls.newInstance();
          insn.init(h, offset, m, cp);
          return insn;
        }
      }
      
      return super.create(h,offset,m,cp); // our fallback
        
    } catch (Throwable e) {
      throw new JPFException("creation of instruction " +
                             h.getInstruction() + " failed: " + e);
    }
  }

  // the explicit factory method (user has to initialize)
  public Instruction create (ClassInfo ciMth, String insnClsName){
        
    try {
      if (isInstrumentedClass(ciMth)){
        Class<? extends gov.nasa.jpf.jvm.bytecode.Instruction> insnCls = ownMap.get(insnClsName);
        if (insnCls != null){
          return insnCls.newInstance();
        }
      }
      
      return super.create(ciMth, insnClsName); // our fallback
      
    } catch (Throwable e){
      throw new JPFException("creation of instruction " +
                             insnClsName + " failed: " + e);
    }
  }
}
