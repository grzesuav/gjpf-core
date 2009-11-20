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
package gov.nasa.jpf.jvm.bytecode;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.NoClassInfoException;
import gov.nasa.jpf.jvm.Types;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Type;


/**
 * Push item from runtime constant pool
 * ... => ..., value
 */
public class LDC extends Instruction {
  
  protected String  string;  // the string value if Type.STRING, classname if Type.CLASS
  protected int     value;
  protected Type    type;


  public void setPeer (org.apache.bcel.generic.Instruction insn, ConstantPool cp) {
    ConstantPoolGen cpg = ClassInfo.getConstantPoolGen(cp);
    org.apache.bcel.generic.LDC ldc = (org.apache.bcel.generic.LDC) insn;
    
    type = ldc.getType(cpg);
    int index = ldc.getIndex();

    if (type == Type.STRING) {
      string = cp.constantToString(cp.getConstant(
                                     ((ConstantString) cp.getConstant(index)).getStringIndex()));

    } else if (type == Type.INT) {
      value = ((ConstantInteger) cp.getConstant(index)).getBytes();

    } else if (type == Type.FLOAT) {
      value = Types.floatToInt( ((ConstantFloat) cp.getConstant(index)).getBytes());

    } else if (type == Type.CLASS) {
    //} else if (type.getType() == Constants.T_REFERENCE) {
      //type = Type.CLASS;
      /*
       * Java 1.5 silently introduced a class file change - LDCs can now directly reference class
       * constpool entries. To make it more interesting, BCEL 5.1 chokes on this with a hard exception.
       * As of Aug 2004, this was fixed in the BCEL Subversion repository, but there is no new
       * release out yet. In order to compile this code with BCEL 5.1, we can't even use Type.CLASS.
       * The current hack should compile with both BCEL 5.1 and svn, but only runs - when encountering
       * a Java 1.5 class file - if the BCEL svn jar is used
       */
      
      // that's kind of a hack - if this is a const class ref to the class that is
      // currently loaded, we don't have a corresponding object created yet, and
      // the StaticArea access methods might do a recursive class init. Our solution
      // is to cache the name, and resolve the reference when we get executed
      string = cp.constantToString(index, Constants.CONSTANT_Class);

    } else {
      throw new JPFException("invalid type of constant");
    }
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    if (type == Type.STRING) {
      // too bad we can't cache it, since location might change between different paths
      value = DynamicArea.getHeap().newInternString(string,ti);
      ti.push(value, true);

    } else if (type == Type.CLASS) {
      try {
        ClassInfo ci = ClassInfo.getClassInfo(string);

        // LDC doesn't cause a <clinit> - we only register all required classes
        // to make sure we have class objects. <clinit>s are called prior to
        // GET/PUT or INVOKE
        if (!ci.isRegistered()){
          ci.registerClass(ti);
        }

        ti.push(ci.getClassObjectRef(), true);

      } catch (NoClassInfoException cx){
        // can be any inherited class or required interface
        return ti.createAndThrowException("java.lang.NoClassDefFoundError", cx.getMessage());
      }

    } else {
      ti.push(value, false);
    }

    return getNext(ti);
  }

  public int getLength() {
    return 2; // opcode, index
  }

  public int getByteCode () {
    return 0x12;
  }
  
  public int getValue() {
    return value;
  }
  
  public Type getType() {
    return type;
  }
  
  public boolean isString() {
    return (type == Type.STRING);
  }
  
  public String getStringValue() { // if it is a String
    if (type == Type.STRING) {
      return string;
    } else {
      return null;
    }
  }
}
