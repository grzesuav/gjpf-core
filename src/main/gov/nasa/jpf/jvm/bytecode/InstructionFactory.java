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
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.NativeMethodInfo;
import gov.nasa.jpf.util.Invocation;
import java.util.List;

/**
 * this is the new InstructionFactory
 */
public class InstructionFactory implements Cloneable {

  public InstructionFactory(){
    // nothing here
  }

  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException cnsx){
      throw new JPFException("InstructionFactory " + this.getClass().getName() + " does not support cloning");
    }
  }

  //--- the factory methods
  public ACONST_NULL aconst_null() {
    return new ACONST_NULL();
  }

  public ALOAD aload(int localVarIndex) {
    return new ALOAD(localVarIndex);
  }

  public ALOAD aload_0() {
    return new ALOAD(0);
  }

  public ALOAD aload_1() {
    return new ALOAD(1);
  }

  public ALOAD aload_2() {
    return new ALOAD(2);
  }

  public ALOAD aload_3() {
    return new ALOAD(3);
  }

  public AALOAD aaload() {
    return new AALOAD();
  }

  public ASTORE astore(int localVarIndex) {
    return new ASTORE(localVarIndex);
  }

  public ASTORE astore_0() {
    return new ASTORE(0);
  }

  public ASTORE astore_1() {
    return new ASTORE(1);
  }

  public ASTORE astore_2() {
    return new ASTORE(2);
  }

  public ASTORE astore_3() {
    return new ASTORE(3);
  }

  public AASTORE aastore() {
    return new AASTORE();
  }

  public ARETURN areturn() {
    return new ARETURN();
  }

  public ANEWARRAY anewarray(String clsName){
    return new ANEWARRAY(clsName);
  }

  public ARRAYLENGTH arraylength() {
    return new ARRAYLENGTH();
  }

  public ATHROW athrow() {
    return new ATHROW();
  }

  public BALOAD baload() {
    return new BALOAD();
  }

  public BASTORE bastore() {
    return new BASTORE();
  }

  public BIPUSH bipush(int b) {
    return new BIPUSH(b);
  }

  public CALOAD caload() {
    return new CALOAD();
  }

  public CASTORE castore() {
    return new CASTORE();
  }

  public CHECKCAST checkcast(String clsName){
    return new CHECKCAST(clsName);
  }

  public D2F d2f() {
    return new D2F();
  }

  public D2I d2i() {
    return new D2I();
  }

  public D2L d2l() {
    return new D2L();
  }

  public DADD dadd() {
    return new DADD();
  }

  public DALOAD daload() {
    return new DALOAD();
  }

  public DASTORE dastore() {
    return new DASTORE();
  }

  public DCMPG dcmpg() {
    return new DCMPG();
  }

  public DCMPL dcmpl() {
    return new DCMPL();
  }

  public DCONST dconst_0() {
    return new DCONST(0.0);
  }

  public DCONST dconst_1() {
    return new DCONST(1.0);
  }

  public DDIV ddiv() {
    return new DDIV();
  }

  public DLOAD dload(int localVarIndex) {
    return new DLOAD(localVarIndex);
  }

  public DLOAD dload_0() {
    return new DLOAD(0);
  }

  public DLOAD dload_1() {
    return new DLOAD(1);
  }

  public DLOAD dload_2() {
    return new DLOAD(2);
  }

  public DLOAD dload_3() {
    return new DLOAD(3);
  }

  public DMUL dmul() {
    return new DMUL();
  }

  public DNEG dneg() {
    return new DNEG();
  }

  public DREM drem() {
    return new DREM();
  }

  public DRETURN dreturn() {
    return new DRETURN();
  }

  public DSTORE dstore(int localVarIndex) {
    return new DSTORE(localVarIndex);
  }

  public DSTORE dstore_0() {
    return new DSTORE(0);
  }

  public DSTORE dstore_1() {
    return new DSTORE(1);
  }

  public DSTORE dstore_2() {
    return new DSTORE(2);
  }

  public DSTORE dstore_3() {
    return new DSTORE(3);
  }

  public DSUB dsub() {
    return new DSUB();
  }

  public DUP dup() {
    return new DUP();
  }

  public DUP_X1 dup_x1() {
    return new DUP_X1();
  }

  public DUP_X2 dup_x2() {
    return new DUP_X2();
  }

  public DUP2 dup2() {
    return new DUP2();
  }

  public DUP2_X1 dup2_x1() {
    return new DUP2_X1();
  }

  public DUP2_X2 dup2_x2() {
    return new DUP2_X2();
  }

  public F2D f2d() {
    return new F2D();
  }

  public F2I f2i() {
    return new F2I();
  }

  public F2L f2l() {
    return new F2L();
  }

  public FADD fadd() {
    return new FADD();
  }

  public FALOAD faload() {
    return new FALOAD();
  }

  public FASTORE fastore() {
    return new FASTORE();
  }

  public FCMPG fcmpg() {
    return new FCMPG();
  }

  public FCMPL fcmpl() {
    return new FCMPL();
  }

  public FCONST fconst_0() {
    return new FCONST(0.0f);
  }

  public FCONST fconst_1() {
    return new FCONST(1.0f);
  }

  public FCONST fconst_2() {
    return new FCONST(2.0f);
  }

  public FDIV fdiv() {
    return new FDIV();
  }

  public FLOAD fload(int localVarIndex) {
    return new FLOAD(localVarIndex);
  }

  public FLOAD fload_0() {
    return new FLOAD(0);
  }

  public FLOAD fload_1() {
    return new FLOAD(1);
  }

  public FLOAD fload_2() {
    return new FLOAD(2);
  }

  public FLOAD fload_3() {
    return new FLOAD(3);
  }

  public FMUL fmul() {
    return new FMUL();
  }

  public FNEG fneg() {
    return new FNEG();
  }

  public FREM frem() {
    return new FREM();
  }

  public FRETURN freturn() {
    return new FRETURN();
  }

  public FSTORE fstore(int localVarIndex) {
    return new FSTORE(localVarIndex);
  }

  public FSTORE fstore_0() {
    return new FSTORE(0);
  }

  public FSTORE fstore_1() {
    return new FSTORE(1);
  }

  public FSTORE fstore_2() {
    return new FSTORE(2);
  }

  public FSTORE fstore_3() {
    return new FSTORE(3);
  }

  public FSUB fsub() {
    return new FSUB();
  }

  public GETFIELD getfield(String fieldName, String clsName, String fieldDescriptor){
    return new GETFIELD(fieldName, clsName, fieldDescriptor);
  }

  public GETSTATIC getstatic(String fieldName, String clsName, String fieldDescriptor){
    return new GETSTATIC(fieldName, clsName, fieldDescriptor);
  }


  public GOTO goto_(int targetPc) {
    return new GOTO(targetPc);
  }

  public GOTO_W goto_w(int targetPc) {
    return new GOTO_W(targetPc);
  }

  public I2B i2b() {
    return new I2B();
  }

  public I2C i2c() {
    return new I2C();
  }

  public I2D i2d() {
    return new I2D();
  }

  public I2F i2f() {
    return new I2F();
  }

  public I2L i2l() {
    return new I2L();
  }

  public I2S i2s() {
    return new I2S();
  }

  public IADD iadd() {
    return new IADD();
  }

  public IALOAD iaload() {
    return new IALOAD();
  }

  public IAND iand() {
    return new IAND();
  }

  public IASTORE iastore() {
    return new IASTORE();
  }

  public ICONST iconst_m1() {
    return new ICONST(-1);
  }

  public ICONST iconst_0() {
    return new ICONST(0);
  }

  public ICONST iconst_1() {
    return new ICONST(1);
  }

  public ICONST iconst_2() {
    return new ICONST(2);
  }

  public ICONST iconst_3() {
    return new ICONST(3);
  }

  public ICONST iconst_4() {
    return new ICONST(4);
  }

  public ICONST iconst_5() {
    return new ICONST(5);
  }

  public IDIV idiv() {
    return new IDIV();
  }

  public IF_ACMPEQ if_acmpeq(int targetPc) {
    return new IF_ACMPEQ(targetPc);
  }

  public IF_ACMPNE if_acmpne(int targetPc) {
    return new IF_ACMPNE(targetPc);
  }

  public IF_ICMPEQ if_icmpeq(int targetPc) {
    return new IF_ICMPEQ(targetPc);
  }

  public IF_ICMPNE if_icmpne(int targetPc) {
    return new IF_ICMPNE(targetPc);
  }

  public IF_ICMPLT if_icmplt(int targetPc) {
    return new IF_ICMPLT(targetPc);
  }

  public IF_ICMPGE if_icmpge(int targetPc) {
    return new IF_ICMPGE(targetPc);
  }

  public IF_ICMPGT if_icmpgt(int targetPc) {
    return new IF_ICMPGT(targetPc);
  }

  public IF_ICMPLE if_icmple(int targetPc) {
    return new IF_ICMPLE(targetPc);
  }

  public IFEQ ifeq(int targetPc) {
    return new IFEQ(targetPc);
  }

  public IFNE ifne(int targetPc) {
    return new IFNE(targetPc);
  }

  public IFLT iflt(int targetPc) {
    return new IFLT(targetPc);
  }

  public IFGE ifge(int targetPc) {
    return new IFGE(targetPc);
  }

  public IFGT ifgt(int targetPc) {
    return new IFGT(targetPc);
  }

  public IFLE ifle(int targetPc) {
    return new IFLE(targetPc);
  }

  public IFNONNULL ifnonnull(int targetPc) {
    return new IFNONNULL(targetPc);
  }

  public IFNULL ifnull(int targetPc) {
    return new IFNULL(targetPc);
  }

  public IINC iinc(int localVarIndex, int incConstant) {
    return new IINC(localVarIndex, incConstant);
  }

  public ILOAD iload(int localVarIndex) {
    return new ILOAD(localVarIndex);
  }

  public ILOAD iload_0() {
    return new ILOAD(0);
  }

  public ILOAD iload_1() {
    return new ILOAD(1);
  }

  public ILOAD iload_2() {
    return new ILOAD(2);
  }

  public ILOAD iload_3() {
    return new ILOAD(3);
  }

  public IMUL imul() {
    return new IMUL();
  }

  public INEG ineg() {
    return new INEG();
  }

  public INSTANCEOF instanceof_(String clsName){
    return new INSTANCEOF(clsName);
  }

  public INVOKEINTERFACE invokeinterface(String clsName, String methodName, String methodSignature){
    return new INVOKEINTERFACE(clsName, methodName, methodSignature);
  }

  public INVOKESPECIAL invokespecial(String clsName, String methodName, String methodSignature){
    return new INVOKESPECIAL(clsName, methodName, methodSignature);
  }

  public INVOKESTATIC invokestatic(String clsName, String methodName, String methodSignature){
    return new INVOKESTATIC(clsName, methodName, methodSignature);
  }

  public INVOKEVIRTUAL invokevirtual(String clsName, String methodName, String methodSignature){
    return new INVOKEVIRTUAL(clsName, methodName, methodSignature);
  }


  public IOR ior() {
    return new IOR();
  }

  public IREM irem() {
    return new IREM();
  }

  public IRETURN ireturn() {
    return new IRETURN();
  }

  public ISHL ishl() {
    return new ISHL();
  }

  public ISHR ishr() {
    return new ISHR();
  }

  public ISTORE istore(int localVarIndex) {
    return new ISTORE(localVarIndex);
  }

  public ISTORE istore_0() {
    return new ISTORE(0);
  }

  public ISTORE istore_1() {
    return new ISTORE(1);
  }

  public ISTORE istore_2() {
    return new ISTORE(2);
  }

  public ISTORE istore_3() {
    return new ISTORE(3);
  }

  public ISUB isub() {
    return new ISUB();
  }

  public IUSHR iushr() {
    return new IUSHR();
  }

  public IXOR ixor() {
    return new IXOR();
  }

  public JSR jsr(int targetPc) {
    return new JSR(targetPc);
  }

  public JSR_W jsr_w(int targetPc) {
    return new JSR_W(targetPc);
  }

  public L2D l2d() {
    return new L2D();
  }

  public L2F l2f() {
    return new L2F();
  }

  public L2I l2i() {
    return new L2I();
  }

  public LADD ladd() {
    return new LADD();
  }

  public LALOAD laload() {
    return new LALOAD();
  }

  public LAND land() {
    return new LAND();
  }

  public LASTORE lastore() {
    return new LASTORE();
  }

  public LCMP lcmp() {
    return new LCMP();
  }

  public LCONST lconst_0() {
    return new LCONST(0);
  }

  public LCONST lconst_1() {
    return new LCONST(1L);
  }

  public LDC ldc(int v){
    return new LDC(v);
  }
  public LDC ldc(float v){
    return new LDC(v);
  }
  public LDC ldc(String v, boolean isClass){
    return new LDC(v, isClass);
  }


  public LDC_W ldc_w(int v){
    return new LDC_W(v);
  }
  public LDC_W ldc_w(float v){
    return new LDC_W(v);
  }
  public LDC_W ldc_w(String v, boolean isClass){
    return new LDC_W(v, isClass);
  }

  public LDC2_W ldc2_w(long v){
    return new LDC2_W(v);
  }
  public LDC2_W ldc2_w(double v){
    return new LDC2_W(v);
  }

  public LDIV ldiv() {
    return new LDIV();
  }

  public LLOAD lload(int localVarIndex) {
    return new LLOAD(localVarIndex);
  }

  public LLOAD lload_0() {
    return new LLOAD(0);
  }

  public LLOAD lload_1() {
    return new LLOAD(1);
  }

  public LLOAD lload_2() {
    return new LLOAD(2);
  }

  public LLOAD lload_3() {
    return new LLOAD(3);
  }

  public LMUL lmul() {
    return new LMUL();
  }

  public LNEG lneg() {
    return new LNEG();
  }

  public LOOKUPSWITCH lookupswitch(int defaultTargetPc, int nEntries) {
    return new LOOKUPSWITCH(defaultTargetPc, nEntries);
  }

  public LOR lor() {
    return new LOR();
  }

  public LREM lrem() {
    return new LREM();
  }

  public LRETURN lreturn() {
    return new LRETURN();
  }

  public LSHL lshl() {
    return new LSHL();
  }

  public LSHR lshr() {
    return new LSHR();
  }

  public LSTORE lstore(int localVarIndex) {
    return new LSTORE(localVarIndex);
  }

  public LSTORE lstore_0() {
    return new LSTORE(0);
  }

  public LSTORE lstore_1() {
    return new LSTORE(1);
  }

  public LSTORE lstore_2() {
    return new LSTORE(2);
  }

  public LSTORE lstore_3() {
    return new LSTORE(3);
  }

  public LSUB lsub() {
    return new LSUB();
  }

  public LUSHR lushr() {
    return new LUSHR();
  }

  public LXOR lxor() {
    return new LXOR();
  }

  public MONITORENTER monitorenter() {
    return new MONITORENTER();
  }

  public MONITOREXIT monitorexit() {
    return new MONITOREXIT();
  }

  public MULTIANEWARRAY multianewarray(String clsName, int dimensions){
    return new MULTIANEWARRAY(clsName, dimensions);
  }

  public NEW new_(String clsName) {
    return new NEW(clsName);
  }

  public NEWARRAY newarray(int typeCode) {
    return new NEWARRAY(typeCode);
  }

  public NOP nop() {
    return new NOP();
  }

  public POP pop() {
    return new POP();
  }

  public POP2 pop2() {
    return new POP2();
  }

  public PUTFIELD putfield(String fieldName, String clsName, String fieldDescriptor){
    return new PUTFIELD(fieldName, clsName, fieldDescriptor);
  }

  public PUTSTATIC putstatic(String fieldName, String clsName, String fieldDescriptor){
    return new PUTSTATIC(fieldName, clsName, fieldDescriptor);
  }

  public RET ret(int localVarIndex) {
    return new RET(localVarIndex);
  }

  public RETURN return_() {
    return new RETURN();
  }

  public SALOAD saload() {
    return new SALOAD();
  }

  public SASTORE sastore() {
    return new SASTORE();
  }

  public SIPUSH sipush(int val) {
    return new SIPUSH(val);
  }

  public SWAP swap() {
    return new SWAP();
  }

  public TABLESWITCH tableswitch(int defaultTargetPc, int low, int high) {
    return new TABLESWITCH(defaultTargetPc, low, high);
  }

  public WIDE wide() {
    return new WIDE();
  }

  
  //--- the JPF specific ones (only used in synthetic methods)
  public INVOKECG invokecg(List<Invocation> invokes) {
    return new INVOKECG(invokes);
  }

  public INVOKECLINIT invokeclinit(ClassInfo ci) {
    return new INVOKECLINIT(ci);
  }

  public DIRECTCALLRETURN directcallreturn(){
    return new DIRECTCALLRETURN();
  }

  public EXECUTENATIVE executenative(NativeMethodInfo mi){
    return new EXECUTENATIVE(mi);
  }

  public NATIVERETURN nativereturn(){
    return new NATIVERETURN();
  }

  // this is never part of MethodInfo stored code
  public RUNSTART runstart(MethodInfo miRun){
    return new RUNSTART(miRun);
  }


}
