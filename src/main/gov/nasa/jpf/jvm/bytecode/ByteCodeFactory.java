//
// Copyright (C) 2010 United States Government as represented by the
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
import gov.nasa.jpf.classfile.ByteCodeReader;
import gov.nasa.jpf.classfile.ClassFile;
import gov.nasa.jpf.jvm.MethodInfo;
import java.util.ArrayList;

/**
 *
 */
public class ByteCodeFactory implements ByteCodeReader {

  ClassFile cf;
  MethodInfo mi;

  ArrayList<Instruction> code;
  int pc;
  int idx;
  
  public ByteCodeFactory (ClassFile cf, MethodInfo mi, int codeLength){
    this.cf = cf;
    this.mi = mi;
    
    // codeLength is always greater than the number of instructions
    code =  new ArrayList<Instruction>(codeLength);
    idx = 0;
  }
  
  protected void add(Instruction insn){
    insn.setMethodInfo(mi);
    insn.setLocation(idx++, pc);
    add(insn);
  }
  
  //--- the context
  public void startCode(Object tag) {
  }

  public void setPc(int pc) {
    this.pc = pc;
  }

  public void endCode(Object tag) {
  }


  //--- the factory methods
  public void aconst_null() {
    add( new ACONST_NULL());
  }

  public void aload(int localVarIndex) {
    add( new ALOAD(localVarIndex));
  }

  public void aload_0() {
    add( new ALOAD(0));
  }

  public void aload_1() {
    add( new ALOAD(1));
  }

  public void aload_2() {
    add( new ALOAD(2));
  }

  public void aload_3() {
    add( new ALOAD(3));
  }

  public void aaload() {
    add( new AALOAD());
  }

  public void astore(int localVarIndex) {
    add( new ASTORE(localVarIndex));
  }

  public void astore_0() {
    add( new ASTORE(0));
  }

  public void astore_1() {
    add( new ASTORE(1));
  }

  public void astore_2() {
    add( new ASTORE(2));
  }

  public void astore_3() {
    add( new ASTORE(3));
  }

  public void aastore() {
    add( new AASTORE());
  }

  public void areturn() {
    add( new ARETURN());
  }

  public void anewarray(int cpClassIndex) {
    add( new ANEWARRAY(cf.classNameAt(cpClassIndex)));
  }

  public void arraylength() {
    add( new ARRAYLENGTH());
  }

  public void athrow() {
    add( new ATHROW());
  }

  public void baload() {
    add( new BALOAD());
  }

  public void bastore() {
    add( new BASTORE());
  }

  public void bipush(int b) {
    add( new BIPUSH(b));
  }

  public void caload() {
    add( new CALOAD());
  }

  public void castore() {
    add( new CASTORE());
  }

  public void checkcast(int cpClassIndex) {
    add( new CHECKCAST(cf.classNameAt(cpClassIndex)));  // ??
  }

  public void d2f() {
    add( new D2F());
  }

  public void d2i() {
    add( new D2I());
  }

  public void d2l() {
    add( new D2L());
  }

  public void dadd() {
    add( new DADD());
  }

  public void daload() {
    add( new DALOAD());
  }

  public void dastore() {
    add( new DASTORE());
  }

  public void dcmpg() {
    add( new DCMPG());
  }

  public void dcmpl() {
    add( new DCMPL());
  }

  public void dconst_0() {
    add( new DCONST(0.0));
  }

  public void dconst_1() {
    add( new DCONST(1.0));
  }

  public void ddiv() {
    add( new DDIV());
  }

  public void dload(int localVarIndex) {
    add( new DLOAD(localVarIndex));
  }

  public void dload_0() {
    add( new DLOAD(0));
  }

  public void dload_1() {
    add( new DLOAD(1));
  }

  public void dload_2() {
    add( new DLOAD(2));
  }

  public void dload_3() {
    add( new DLOAD(3));
  }

  public void dmul() {
    add( new DMUL());
  }

  public void dneg() {
    add( new DNEG());
  }

  public void drem() {
    add( new DREM());
  }

  public void dreturn() {
    add( new DRETURN());
  }

  public void dstore(int localVarIndex) {
    add( new DSTORE(localVarIndex));
  }

  public void dstore_0() {
    add( new DSTORE(0));
  }

  public void dstore_1() {
    add( new DSTORE(1));
  }

  public void dstore_2() {
    add( new DSTORE(2));
  }

  public void dstore_3() {
    add( new DSTORE(3));
  }

  public void dsub() {
    add( new DSUB());
  }

  public void dup() {
    add( new DUP());
  }

  public void dup_x1() {
    add( new DUP_X1());
  }

  public void dup_x2() {
    add( new DUP_X2());
  }

  public void dup2() {
    add( new DUP2());
  }

  public void dup2_x1() {
    add( new DUP2_X1());
  }

  public void dup2_x2() {
    add( new DUP2_X2());
  }

  public void f2d() {
    add( new F2D());
  }

  public void f2i() {
    add( new F2I());
  }

  public void f2l() {
    add( new F2L());
  }

  public void fadd() {
    add( new FADD());
  }

  public void faload() {
    add( new FALOAD());
  }

  public void fastore() {
    add( new FASTORE());
  }

  public void fcmpg() {
    add( new FCMPG());
  }

  public void fcmpl() {
    add( new FCMPL());
  }

  public void fconst_0() {
    add( new FCONST(0.0f));
  }

  public void fconst_1() {
    add( new FCONST(1.0f));
  }

  public void fconst_2() {
    add( new FCONST(2.0f));
  }

  public void fdiv() {
    add( new FDIV());
  }

  public void fload(int localVarIndex) {
    add( new FLOAD(localVarIndex));
  }

  public void fload_0() {
    add( new FLOAD(0));
  }

  public void fload_1() {
    add( new FLOAD(1));
  }

  public void fload_2() {
    add( new FLOAD(2));
  }

  public void fload_3() {
    add( new FLOAD(3));
  }

  public void fmul() {
    add( new FMUL());
  }

  public void fneg() {
    add( new FNEG());
  }

  public void frem() {
    add( new FREM());
  }

  public void freturn() {
    add( new FRETURN());
  }

  public void fstore(int localVarIndex) {
    add( new FSTORE(localVarIndex));
  }

  public void fstore_0() {
    add( new FSTORE(0));
  }

  public void fstore_1() {
    add( new FSTORE(1));
  }

  public void fstore_2() {
    add( new FSTORE(2));
  }

  public void fstore_3() {
    add( new FSTORE(3));
  }

  public void fsub() {
    add( new FSUB());
  }

  public void getfield(int cpFieldRefIndex) {
    String fieldName = cf.fieldNameAt(cpFieldRefIndex);
    String clsDescriptor = cf.fieldClassNameAt(cpFieldRefIndex);
    String fieldDescriptor = cf.fieldDescriptorAt(cpFieldRefIndex);

    add( new GETFIELD(fieldName, clsDescriptor, fieldDescriptor));
  }

  public void getstatic(int cpFieldRefIndex) {
    String fieldName = cf.fieldNameAt(cpFieldRefIndex);
    String clsDescriptor = cf.fieldClassNameAt(cpFieldRefIndex);
    String fieldDescriptor = cf.fieldDescriptorAt(cpFieldRefIndex);

    add( new GETSTATIC(fieldName, clsDescriptor, fieldDescriptor));
  }

  public void goto_(int pcOffset) {
    add( new ACONST_NULL());
  }

  public void goto_w(int pcOffset) {
    add( new ACONST_NULL());
  }

  public void i2b() {
    add( new I2B());
  }

  public void i2c() {
    add( new I2C());
  }

  public void i2d() {
    add( new I2D());
  }

  public void i2f() {
    add( new I2F());
  }

  public void i2l() {
    add( new I2L());
  }

  public void i2s() {
    add( new I2S());
  }

  public void iadd() {
    add( new IADD());
  }

  public void iaload() {
    add( new IALOAD());
  }

  public void iand() {
    add( new IAND());
  }

  public void iastore() {
    add( new IASTORE());
  }

  public void iconst_m1() {
    add( new ICONST(-1));
  }

  public void iconst_0() {
    add( new ICONST(0));
  }

  public void iconst_1() {
    add( new ICONST(1));
  }

  public void iconst_2() {
    add( new ICONST(2));
  }

  public void iconst_3() {
    add( new ICONST(3));
  }

  public void iconst_4() {
    add( new ICONST(4));
  }

  public void iconst_5() {
    add( new ICONST(5));
  }

  public void idiv() {
    add( new IDIV());
  }

  public void if_acmpeq(int pcOffset) {
    add( new IF_ACMPEQ(pcOffset));
  }

  public void if_acmpne(int pcOffset) {
    add( new IF_ACMPNE(pcOffset));
  }

  public void if_icmpeq(int pcOffset) {
    add( new IF_ICMPEQ(pcOffset));
  }

  public void if_icmpne(int pcOffset) {
    add( new IF_ICMPNE(pcOffset));
  }

  public void if_icmplt(int pcOffset) {
    add( new IF_ICMPLT(pcOffset));
  }

  public void if_icmpge(int pcOffset) {
    add( new IF_ICMPGE(pcOffset));
  }

  public void if_icmpgt(int pcOffset) {
    add( new IF_ICMPGT(pcOffset));
  }

  public void if_icmple(int pcOffset) {
    add( new IF_ICMPLE(pcOffset));
  }

  public void ifeq(int pcOffset) {
    add( new IFEQ(pcOffset));
  }

  public void ifne(int pcOffset) {
    add( new IFNE(pcOffset));
  }

  public void iflt(int pcOffset) {
    add( new IFLT(pcOffset));
  }

  public void ifge(int pcOffset) {
    add( new IFGE(pcOffset));
  }

  public void ifgt(int pcOffset) {
    add( new IFGT(pcOffset));
  }

  public void ifle(int pcOffset) {
    add( new IFLE(pcOffset));
  }

  public void ifnonnull(int pcOffset) {
    add( new IFNONNULL(pcOffset));
  }

  public void ifnull(int pcOffset) {
    add( new IFNULL(pcOffset));
  }

  public void iinc(int localVarIndex, int incConstant) {
    add( new IINC(localVarIndex, incConstant));
  }

  public void iload(int localVarIndex) {
    add( new ILOAD(localVarIndex));
  }

  public void iload_0() {
    add( new ILOAD(0));
  }

  public void iload_1() {
    add( new ILOAD(1));
  }

  public void iload_2() {
    add( new ILOAD(2));
  }

  public void iload_3() {
    add( new ILOAD(3));
  }

  public void imul() {
    add( new IMUL());
  }

  public void ineg() {
    add( new INEG());
  }

  public void instanceof_(int cpClassIndex) {
    add( new INSTANCEOF(cf.classNameAt(cpClassIndex)));
  }

  public void invokeinterface(int cpInterfaceMethodRefIndex, int count, int zero) {
    String clsDescriptor = cf.interfaceMethodClassNameAt(cpInterfaceMethodRefIndex);
    String methodName = cf.interfaceMethodNameAt(cpInterfaceMethodRefIndex);
    String methodSignature = cf.interfaceMethodDescriptorAt(cpInterfaceMethodRefIndex);

    add( new INVOKEINTERFACE(clsDescriptor, methodName, methodSignature));
  }

  public void invokespecial(int cpMethodRefIndex) {
    String clsDescriptor = cf.methodClassNameAt(cpMethodRefIndex);
    String methodName = cf.methodNameAt(cpMethodRefIndex);
    String methodSignature = cf.methodDescriptorAt(cpMethodRefIndex);

    add( new INVOKEINTERFACE(clsDescriptor, methodName, methodSignature));
  }

  public void invokestatic(int cpMethodRefIndex) {
    String clsDescriptor = cf.methodClassNameAt(cpMethodRefIndex);
    String methodName = cf.methodNameAt(cpMethodRefIndex);
    String methodSignature = cf.methodDescriptorAt(cpMethodRefIndex);

    add( new INVOKESTATIC(clsDescriptor, methodName, methodSignature));
  }

  public void invokevirtual(int cpMethodRefIndex) {
    String clsDescriptor = cf.methodClassNameAt(cpMethodRefIndex);
    String methodName = cf.methodNameAt(cpMethodRefIndex);
    String methodSignature = cf.methodDescriptorAt(cpMethodRefIndex);

    add( new INVOKEVIRTUAL(clsDescriptor, methodName, methodSignature));
  }

  public void ior() {
    add( new IOR());
  }

  public void irem() {
    add( new IREM());
  }

  public void ireturn() {
    add( new IRETURN());
  }

  public void ishl() {
    add( new ISHL());
  }

  public void ishr() {
    add( new ISHR());
  }

  public void istore(int localVarIndex) {
    add( new ISTORE(localVarIndex));
  }

  public void istore_0() {
    add( new ISTORE(0));
  }

  public void istore_1() {
    add( new ISTORE(1));
  }

  public void istore_2() {
    add( new ISTORE(2));
  }

  public void istore_3() {
    add( new ISTORE(3));
  }

  public void isub() {
    add( new ISUB());
  }

  public void iushr() {
    add( new IUSHR());
  }

  public void ixor() {
    add( new IXOR());
  }

  public void jsr(int pcOffset) {
    add( new JSR(pcOffset));
  }

  public void jsr_w(int pcOffset) {
    add( new JSR_W(pcOffset));
  }

  public void l2d() {
    add( new L2D());
  }

  public void l2f() {
    add( new L2F());
  }

  public void l2i() {
    add( new L2I());
  }

  public void ladd() {
    add( new LADD());
  }

  public void laload() {
    add( new LALOAD());
  }

  public void land() {
    add( new LAND());
  }

  public void lastore() {
    add( new LASTORE());
  }

  public void lcmp() {
    add( new LCMP());
  }

  public void lconst_0() {
    add( new LCONST(0));
  }

  public void lconst_1() {
    add( new LCONST(1L));
  }

  public void ldc(int cpIntOrFloatOrStringIndex) {
    Instruction insn;
    Object v = cf.getCpValue(cpIntOrFloatOrStringIndex);
    if (v instanceof String){
      insn = new LDC((String)v, false);
    } else if (v instanceof Integer){
      insn = new LDC((Integer)v);
    } else if (v instanceof Float){
      insn = new LDC((Float)v);
    } else {
      insn = new LDC(cf.classNameAt(cpIntOrFloatOrStringIndex), true);
    }
    add( insn);
  }

  public void ldc_w(int cpIntOrFloatOrStringIndex) {
    Instruction insn;
    Object v = cf.getCpValue(cpIntOrFloatOrStringIndex);
    if (v instanceof String){
      insn = new LDC_W((String)v, false);
    } else if (v instanceof Integer){
      insn = new LDC_W((Integer)v);
    } else if (v instanceof Float){
      insn = new LDC_W((Float)v);
    } else {
      insn = new LDC_W(cf.classNameAt(cpIntOrFloatOrStringIndex), true);
    }
    add( insn);
  }

  public void ldc2_w(int cpLongOrDoubleIndex) {
    Instruction insn;
    Object v = cf.getCpValue(cpLongOrDoubleIndex);
    if (v instanceof Long){
      insn = new LDC2_W((Long)v);
    } else {
      insn = new LDC2_W((Integer)v);
    }
    add( insn);
  }

  public void ldiv() {
    add( new LDIV());
  }

  public void lload(int localVarIndex) {
    add( new LLOAD(localVarIndex));
  }

  public void lload_0() {
    add( new LLOAD(0));
  }

  public void lload_1() {
    add( new LLOAD(1));
  }

  public void lload_2() {
    add( new LLOAD(2));
  }

  public void lload_3() {
    add( new LLOAD(3));
  }

  public void lmul() {
    add( new LMUL());
  }

  public void lneg() {
    add( new LNEG());
  }

  public void lookupswitch(int defaultPcOffset, int nEntries) {
    add( new ACONST_NULL());
  }

  public void lookupswitchEntry(int index, int match, int pcOffset) {
    add( new ACONST_NULL());
  }

  public void lor() {
    add( new LOR());
  }

  public void lrem() {
    add( new LREM());
  }

  public void lreturn() {
    add( new LRETURN());
  }

  public void lshl() {
    add( new LSHL());
  }

  public void lshr() {
    add( new LSHR());
  }

  public void lstore(int localVarIndex) {
    add( new LSTORE(localVarIndex));
  }

  public void lstore_0() {
    add( new LSTORE(0));
  }

  public void lstore_1() {
    add( new LSTORE(1));
  }

  public void lstore_2() {
    add( new LSTORE(2));
  }

  public void lstore_3() {
    add( new LSTORE(3));
  }

  public void lsub() {
    add( new LSUB());
  }

  public void lushr() {
    add( new LUSHR());
  }

  public void lxor() {
    add( new LXOR());
  }

  public void monitorenter() {
    add( new MONITORENTER());
  }

  public void monitorexit() {
    add( new MONITOREXIT());
  }

  public void multianewarray(int cpClassIndex, int dimensions) {
    add( new ACONST_NULL());
  }

  public void new_(int cpClassIndex) {
    add( new ACONST_NULL());
  }

  public void newarray(int typeCode) {
    add( new ACONST_NULL());
  }

  public void nop() {
    add( new NOP());
  }

  public void pop() {
    add( new POP());
  }

  public void pop2() {
    add( new POP2());
  }

  public void putfield(int cpFieldRefIndex) {
    String fieldName = cf.fieldNameAt(cpFieldRefIndex);
    String clsDescriptor = cf.fieldClassNameAt(cpFieldRefIndex);
    String fieldDescriptor = cf.fieldDescriptorAt(cpFieldRefIndex);

    add( new PUTFIELD(fieldName, clsDescriptor, fieldDescriptor));
  }

  public void putstatic(int cpFieldRefIndex) {
    String fieldName = cf.fieldNameAt(cpFieldRefIndex);
    String clsDescriptor = cf.fieldClassNameAt(cpFieldRefIndex);
    String fieldDescriptor = cf.fieldDescriptorAt(cpFieldRefIndex);

    add( new PUTSTATIC(fieldName, clsDescriptor, fieldDescriptor));
  }

  public void ret(int localVarIndex) {
    add( new ACONST_NULL());
  }

  public void return_() {
    add( new RETURN());
  }

  public void saload() {
    add( new SALOAD());
  }

  public void sastore() {
    add( new SASTORE());
  }

  public void sipush(int val) {
    add( new ACONST_NULL());
  }

  public void swap() {
    add( new SWAP());
  }

  public void tableswitch(int defaultPcOffset, int low, int high) {
    add( new ACONST_NULL());
  }

  public void tableswitchEntry(int index, int pcOffset) {
    add( new ACONST_NULL());
  }

  public void wide() {
    add( new WIDE());
  }

  public void unknown(int bytecode) {
    throw new JPFException("unknown bytecode: " + Integer.toHexString(bytecode));
  }

}
