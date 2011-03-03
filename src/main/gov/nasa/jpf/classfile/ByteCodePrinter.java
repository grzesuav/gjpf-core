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

package gov.nasa.jpf.classfile;

import java.io.PrintWriter;

/**
 * utility class that prints out bytecode in readable form
 */
public class ByteCodePrinter implements ByteCodeReader {

  PrintWriter pw;
  ClassFile cf; // need this to get the constpool entries

  int pc; // code index
  String prefix;

  public ByteCodePrinter (PrintWriter pw, ClassFile cf, String prefix){
    this.pw = pw;
    this.cf = cf;
    this.prefix = prefix;
  }

  public void setPc (int pc){
    this.pc = pc;
  }

  public void aconst_null() {
    pw.printf("%s%3d: %s\n", prefix, pc, "aconst_null");
  }

  public void aload(int localVarIndex) {
    pw.printf("%s%3d: %s [%d]\n", prefix, pc, "aload", localVarIndex);
  }

  public void aload_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "aload_0");
  }

  public void aload_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "aload_1");
  }

  public void aload_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "aload_2");
  }

  public void aload_3() {
    pw.printf("%s%3d: %s\n", prefix, pc, "aload_3");
  }

  public void aaload() {
    pw.printf("%s%3d: %s\n", prefix, pc, "aaload");
  }

  public void astore(int localVarIndex) {
    pw.printf("%s%3d: %s [%d]\n", prefix, pc, "astore", localVarIndex);
  }

  public void astore_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "astore_0");
  }

  public void astore_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "astore_1");
  }

  public void astore_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "astore_2");
  }

  public void astore_3() {
    pw.printf("%s%3d: %s\n", prefix, pc, "astore_3");
  }

  public void aastore() {
    pw.printf("%s%3d: %s\n", prefix, pc, "aastore");
  }

  public void areturn() {
    pw.printf("%s%3d: %s\n", prefix, pc, "areturn");
  }

  public void anewarray(int cpClassIndex) {
    pw.printf("%s%3d: %s @%d(\"%s\")\n", prefix, pc, "anewarray", cpClassIndex, cf.classNameAt(cpClassIndex));
  }

  public void arraylength() {
    pw.printf("%s%3d: %s\n", prefix, pc, "arraylength");
  }

  public void athrow() {
    pw.printf("%s%3d: %s\n", prefix, pc, "athrow");
  }

  public void baload() {
    pw.printf("%s%3d: %s\n", prefix, pc, "baload");
  }

  public void bastore() {
    pw.printf("%s%3d: %s\n", prefix, pc, "bastore");
  }

  public void bipush(int b) {
    pw.printf("%s%3d: %s %d\n", prefix, pc, "bipush", b);
  }

  public void caload() {
    pw.printf("%s%3d: %s\n", prefix, pc, "caload");
  }

  public void castore() {
    pw.printf("%s%3d: %s\n", prefix, pc, "castore");
  }

  public void checkcast(int cpClassIndex) {
    pw.printf("%s%3d: %s @%d(\"%s\")\n", prefix, pc, "checkcast", cpClassIndex, cf.classNameAt(cpClassIndex));
  }

  public void d2f() {
    pw.printf("%s%3d: %s\n", prefix, pc, "d2f");
  }

  public void d2i() {
    pw.printf("%s%3d: %s\n", prefix, pc, "d2i");
  }

  public void d2l() {
    pw.printf("%s%3d: %s\n", prefix, pc, "d2l");
  }

  public void dadd() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dadd");
  }

  public void daload() {
    pw.printf("%s%3d: %s\n", prefix, pc, "daload");
  }

  public void dastore() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dastore");
  }

  public void dcmpg() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dcmpg");
  }

  public void dcmpl() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dcmpl");
  }

  public void dconst_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dconst_0");
  }

  public void dconst_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dcont_1");
  }

  public void ddiv() {
    pw.printf("%s%3d: %s\n", prefix, pc, "ddiv");
  }

  public void dload(int localVarIndex) {
    pw.printf("%s%3d: %s [%d]\n", prefix, pc, "dload", localVarIndex);
  }

  public void dload_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dload_0");
  }

  public void dload_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dload_1");
  }

  public void dload_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dload_2");
  }

  public void dload_3() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dload_3");
  }

  public void dmul() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dmul");
  }

  public void dneg() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dneg");
  }

  public void drem() {
    pw.printf("%s%3d: %s\n", prefix, pc, "drem");
  }

  public void dreturn() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dreturn");
  }

  public void dstore(int localVarIndex) {
    pw.printf("%s%3d: %s [%d]\n", prefix, pc, "dstore", localVarIndex);
  }

  public void dstore_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dstore_0");
  }

  public void dstore_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dstore_1");
  }

  public void dstore_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dstore_2");
  }

  public void dstore_3() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dstore_3");
  }

  public void dsub() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dsub");
  }

  public void dup() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dup");
  }

  public void dup_x1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dup_x1");
  }

  public void dup_x2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dup_x2");
  }

  public void dup2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dup2");
  }

  public void dup2_x1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dup2_x1");
  }

  public void dup2_x2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "dup2_x2");
  }

  public void f2d() {
    pw.printf("%s%3d: %s\n", prefix, pc, "f2d");
  }

  public void f2i() {
    pw.printf("%s%3d: %s\n", prefix, pc, "f2i");
  }

  public void f2l() {
    pw.printf("%s%3d: %s\n", prefix, pc, "f2l");
  }

  public void fadd() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fadd");
  }

  public void faload() {
    pw.printf("%s%3d: %s\n", prefix, pc, "faload");
  }

  public void fastore() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fastore");
  }

  public void fcmpg() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fcmpg");
  }

  public void fcmpl() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fcmpl");
  }

  public void fconst_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fconst_0");
  }

  public void fconst_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fconst_1");
  }

  public void fconst_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fconst_2");
  }

  public void fdiv() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fdiv");
  }

  public void fload(int localVarIndex) {
    pw.printf("%s%3d: %s [%d]\n", prefix, pc, "fload", localVarIndex);
  }

  public void fload_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fload_0");
  }

  public void fload_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fload_1");
  }

  public void fload_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fload_2");
  }

  public void fload_3() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fload_3");
  }

  public void fmul() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fmul");
  }

  public void fneg() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fneg");
  }

  public void frem() {
    pw.printf("%s%3d: %s\n", prefix, pc, "frem");
  }

  public void freturn() {
    pw.printf("%s%3d: %s\n", prefix, pc, "freturn");
  }

  public void fstore(int localVarIndex) {
    pw.printf("%s%3d: %s [%d]\n", prefix, pc, "fstore", localVarIndex);
  }

  public void fstore_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fstore_0");
  }

  public void fstore_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fstore_1");
  }

  public void fstore_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fstore_2");
  }

  public void fstore_3() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fstore_3");
  }

  public void fsub() {
    pw.printf("%s%3d: %s\n", prefix, pc, "fsub");
  }

  public void getfield(int cpFieldRefIndex) {
    pw.printf("%s%3d: %s @%d(\"%s\",\"%s\",\"%s\")\n", prefix, pc, "getfield", cpFieldRefIndex,
            cf.fieldClassNameAt(cpFieldRefIndex),
            cf.fieldNameAt(cpFieldRefIndex),
            cf.fieldDescriptorAt(cpFieldRefIndex));
  }

  public void getstatic(int cpFieldRefIndex) {
    pw.printf("%s%3d: %s @%d(\"%s\",\"%s\",\"%s\")\n", prefix, pc, "getstatic", cpFieldRefIndex,
            cf.fieldClassNameAt(cpFieldRefIndex),
            cf.fieldNameAt(cpFieldRefIndex),
            cf.fieldDescriptorAt(cpFieldRefIndex));
  }

  public void goto_(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "goto", pcOffset, (pc + pcOffset));
  }

  public void goto_w(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "goto_w", pcOffset, (pc + pcOffset));
  }

  public void i2b() {
    pw.printf("%s%3d: %s\n", prefix, pc, "i2b");
  }

  public void i2c() {
    pw.printf("%s%3d: %s\n", prefix, pc, "i2c");
  }

  public void i2d() {
    pw.printf("%s%3d: %s\n", prefix, pc, "i2d");
  }

  public void i2f() {
    pw.printf("%s%3d: %s\n", prefix, pc, "i2f");
  }

  public void i2l() {
    pw.printf("%s%3d: %s\n", prefix, pc, "i2l");
  }

  public void i2s() {
    pw.printf("%s%3d: %s\n", prefix, pc, "i2s");
  }

  public void iadd() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iadd");
  }

  public void iaload() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iaload");
  }

  public void iand() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iand");
  }

  public void iastore() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iastore");
  }

  public void iconst_m1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iconst_m1");
  }

  public void iconst_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iconst_0");
  }

  public void iconst_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iconst_1");
  }

  public void iconst_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iconst_2");
  }

  public void iconst_3() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iconst_3");
  }

  public void iconst_4() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iconst_4");
  }

  public void iconst_5() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iconst_5");
  }

  public void idiv() {
    pw.printf("%s%3d: %s\n", prefix, pc, "idiv");
  }

  public void if_acmpeq(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "if_acmpeq", pcOffset, (pc + pcOffset));
  }

  public void if_acmpne(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "if_acmpne", pcOffset, (pc + pcOffset));
  }

  public void if_icmpeq(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "if_icmpeq", pcOffset, (pc + pcOffset));
  }

  public void if_icmpne(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "if_icmpne", pcOffset, (pc + pcOffset));
  }

  public void if_icmplt(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "if_icmplt", pcOffset, (pc + pcOffset));
  }

  public void if_icmpge(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "if_icmpge", pcOffset, (pc + pcOffset));
  }

  public void if_icmpgt(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "if_icmpgt", pcOffset, (pc + pcOffset));
  }

  public void if_icmple(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "if_icmple", pcOffset, (pc + pcOffset));
  }

  public void ifeq(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "ifeq", pcOffset, (pc + pcOffset));
  }

  public void ifne(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "ifne", pcOffset, (pc + pcOffset));
  }

  public void iflt(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "iflt", pcOffset, (pc + pcOffset));
  }

  public void ifge(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "ifge", pcOffset, (pc + pcOffset));
  }

  public void ifgt(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "ifgt", pcOffset, (pc + pcOffset));
  }

  public void ifle(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "ifle", pcOffset, (pc + pcOffset));
  }

  public void ifnonnull(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "ifnonnull", pcOffset, (pc + pcOffset));
  }

  public void ifnull(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "ifnull", pcOffset, (pc + pcOffset));
  }

  public void iinc(int localVarIndex, int incConstant) {
    pw.printf("%s%3d: %s [%d] %+d\n", prefix, pc, "iinc", localVarIndex, incConstant);
  }

  public void iload(int localVarIndex) {
    pw.printf("%s%3d: %s [%d]\n", prefix, pc, "iload", localVarIndex);
  }

  public void iload_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iload_0");
  }

  public void iload_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iload_1");
  }

  public void iload_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iload_2");
  }

  public void iload_3() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iload_3");
  }

  public void imul() {
    pw.printf("%s%3d: %s\n", prefix, pc, "imul");
  }

  public void ineg() {
    pw.printf("%s%3d: %s\n", prefix, pc, "ineg");
  }

  public void instanceof_(int cpClassIndex) {
    pw.printf("%s%3d: %s @%d(\"%s\")\n", prefix, pc, "instanceof", cpClassIndex, cf.classNameAt(cpClassIndex));
  }

  public void invokeinterface(int cpInterfaceMethodRefIndex, int count, int zero) {
    pw.printf("%s%3d: %s @%d(\"%s\",\"%s\",\"%s\") %d\n", prefix, pc, "invokeinterface", cpInterfaceMethodRefIndex,
            cf.methodClassNameAt(cpInterfaceMethodRefIndex),
            cf.methodNameAt(cpInterfaceMethodRefIndex),
            cf.methodDescriptorAt(cpInterfaceMethodRefIndex),
            count);
  }

  public void invokespecial(int cpMethodRefIndex) {
    pw.printf("%s%3d: %s @%d(\"%s\",\"%s\",\"%s\")\n", prefix, pc, "invokespecial", cpMethodRefIndex,
            cf.methodClassNameAt(cpMethodRefIndex),
            cf.methodNameAt(cpMethodRefIndex),
            cf.methodDescriptorAt(cpMethodRefIndex));
  }

  public void invokestatic(int cpMethodRefIndex) {
    pw.printf("%s%3d: %s @%d(\"%s\",\"%s\",\"%s\")\n", prefix, pc, "invokestatic", cpMethodRefIndex,
            cf.methodClassNameAt(cpMethodRefIndex),
            cf.methodNameAt(cpMethodRefIndex),
            cf.methodDescriptorAt(cpMethodRefIndex));
  }

  public void invokevirtual(int cpMethodRefIndex) {
    pw.printf("%s%3d: %s @%d(\"%s\",\"%s\",\"%s\")\n", prefix, pc, "invokevirtual", cpMethodRefIndex,
            cf.methodClassNameAt(cpMethodRefIndex),
            cf.methodNameAt(cpMethodRefIndex),
            cf.methodDescriptorAt(cpMethodRefIndex));
  }

  public void ior() {
    pw.printf("%s%3d: %s\n", prefix, pc, "ior");
  }

  public void irem() {
    pw.printf("%s%3d: %s\n", prefix, pc, "irem");
  }

  public void ireturn() {
    pw.printf("%s%3d: %s\n", prefix, pc, "ireturn");
  }

  public void ishl() {
    pw.printf("%s%3d: %s\n", prefix, pc, "ishl");
  }

  public void ishr() {
    pw.printf("%s%3d: %s\n", prefix, pc, "ishr");
  }

  public void istore(int localVarIndex) {
    pw.printf("%s%3d: %s [%d]\n", prefix, pc, "istore", localVarIndex);
  }

  public void istore_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "istore_0");
  }

  public void istore_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "istore_1");
  }

  public void istore_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "istore_2");
  }

  public void istore_3() {
    pw.printf("%s%3d: %s\n", prefix, pc, "istore_3");
  }

  public void isub() {
    pw.printf("%s%3d: %s\n", prefix, pc, "isub");
  }

  public void iushr() {
    pw.printf("%s%3d: %s\n", prefix, pc, "iushr");
  }

  public void ixor() {
    pw.printf("%s%3d: %s\n", prefix, pc, "ixor");
  }

  public void jsr(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "jsr", pcOffset, (pc + pcOffset));
  }

  public void jsr_w(int pcOffset) {
    pw.printf("%s%3d: %s %+d (%d)\n", prefix, pc, "jsr_w", pcOffset, (pc + pcOffset));
  }

  public void l2d() {
    pw.printf("%s%3d: %s\n", prefix, pc, "l2d");
  }

  public void l2f() {
    pw.printf("%s%3d: %s\n", prefix, pc, "l2f");
  }

  public void l2i() {
    pw.printf("%s%3d: %s\n", prefix, pc, "l2i");
  }

  public void ladd() {
    pw.printf("%s%3d: %s\n", prefix, pc, "ladd");
  }

  public void laload() {
    pw.printf("%s%3d: %s\n", prefix, pc, "laload");
  }

  public void land() {
    pw.printf("%s%3d: %s\n", prefix, pc, "land");
  }

  public void lastore() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lastore");
  }

  public void lcmp() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lcmp");
  }

  public void lconst_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lconst_0");
  }

  public void lconst_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lconst_1");
  }

  public void ldc(int cpIntOrFloatOrStringIndex) {
    pw.printf("%s%3d: %s @%d(%s)\n", prefix, pc, "ldc", cpIntOrFloatOrStringIndex,
            cf.getCpValue(cpIntOrFloatOrStringIndex));
  }

  public void ldc_w(int cpIntOrFloatOrStringIndex) {
    pw.printf("%s%3d: %s @%d(%s)\n", prefix, pc, "ldc_w", cpIntOrFloatOrStringIndex,
            cf.getCpValue(cpIntOrFloatOrStringIndex));
  }

  public void ldc2_w(int cpLongOrDoubleIndex) {
    pw.printf("%s%3d: %s @%d(%s)\n", prefix, pc, "ldc2_w", cpLongOrDoubleIndex,
            cf.getCpValue(cpLongOrDoubleIndex));
  }

  public void ldiv() {
    pw.printf("%s%3d: %s\n", prefix, pc, "ldiv");
  }

  public void lload(int localVarIndex) {
    pw.printf("%s%3d: %s [%d]\n", prefix, pc, "lload", localVarIndex);
  }

  public void lload_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lload_0");
  }

  public void lload_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lload_1");
  }

  public void lload_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lload_2");
  }

  public void lload_3() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lload_3");
  }

  public void lmul() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lmul");
  }

  public void lneg() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lneg");
  }

  public void lookupswitch(int defaultPcOffset, int nEntries) {
    pw.printf("%s%3d: %s default:%+d\n", prefix, pc, "lookupswitch", defaultPcOffset);
    cf.parseLookupSwitchEntries(this, nEntries);
  }
  public void lookupswitchEntry(int index, int match, int pcOffset){
    pw.printf("%s      %d : %+d (%d)\n", prefix, match, pcOffset, (pc + pcOffset));
  }


  public void lor() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lor");
  }

  public void lrem() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lrem");
  }

  public void lreturn() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lreturn");
  }

  public void lshl() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lshl");
  }

  public void lshr() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lshr");
  }

  public void lstore(int localVarIndex) {
    pw.printf("%s%3d: %s [%d]\n", prefix, pc, "lstore", localVarIndex);
  }

  public void lstore_0() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lstore_0");
  }

  public void lstore_1() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lstore_1");
  }

  public void lstore_2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lstore_2");
  }

  public void lstore_3() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lstore_3");
  }

  public void lsub() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lsub");
  }

  public void lushr() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lushr");
  }

  public void lxor() {
    pw.printf("%s%3d: %s\n", prefix, pc, "lxor");
  }

  public void monitorenter() {
    pw.printf("%s%3d: %s\n", prefix, pc, "monitorenter");
  }

  public void monitorexit() {
    pw.printf("%s%3d: %s\n", prefix, pc, "monitorexit");
  }

  public void multianewarray(int cpClassIndex, int dimensions) {
    pw.printf("%s%3d: %s @%d(\"%s\") dim: %d\n", prefix, pc, "multianewarray",
            cf.classNameAt(cpClassIndex), dimensions);
  }

  public void new_(int cpClassIndex) {
    pw.printf("%s%3d: %s @%d(\"%s\")\n", prefix, pc, "new",
            cpClassIndex, cf.classNameAt(cpClassIndex));
  }

  public void newarray(int typeCode) {
    pw.printf("%s%3d: %s %s[]\n", prefix, pc, "newarray", cf.getTypeName(typeCode));
  }

  public void nop() {
    pw.printf("%s%3d: %s\n", prefix, pc, "nop");
  }

  public void pop() {
    pw.printf("%s%3d: %s\n", prefix, pc, "pop");
  }

  public void pop2() {
    pw.printf("%s%3d: %s\n", prefix, pc, "pop2");
  }

  public void putfield(int cpFieldRefIndex) {
    pw.printf("%s%3d: %s @%d(\"%s\",\"%s\",\"%s\")\n", prefix, pc, "putfield", cpFieldRefIndex,
            cf.fieldClassNameAt(cpFieldRefIndex),
            cf.fieldNameAt(cpFieldRefIndex),
            cf.fieldDescriptorAt(cpFieldRefIndex));
  }

  public void putstatic(int cpFieldRefIndex) {
    pw.printf("%s%3d: %s @%d(\"%s\",\"%s\",\"%s\")\n", prefix, pc, "putstatic", cpFieldRefIndex,
            cf.fieldClassNameAt(cpFieldRefIndex),
            cf.fieldNameAt(cpFieldRefIndex),
            cf.fieldDescriptorAt(cpFieldRefIndex));
  }

  public void ret(int localVarIndex) {
    pw.printf("%s%3d: %s [%d]\n", prefix, pc, "ret", localVarIndex);
  }

  public void return_() {
    pw.printf("%s%3d: %s\n", prefix, pc, "return");
  }

  public void saload() {
    pw.printf("%s%3d: %s\n", prefix, pc, "saload");
  }

  public void sastore() {
    pw.printf("%s%3d: %s\n", prefix, pc, "sastore");
  }

  public void sipush(int val) {
    pw.printf("%s%3d: %s %d\n", prefix, pc, "sipush", val);
  }

  public void swap() {
    pw.printf("%s%3d: %s\n", prefix, pc, "swap");
  }

  public void tableswitch(int defaultPcOffset, int low, int high) {
    pw.printf("%s%3d: %s [%d..%d] default: %+d\n", prefix, pc, "tableswitch", low, high, defaultPcOffset);
    cf.parseTableSwitchEntries(this, low, high);
  }
  public void tableswitchEntry(int val, int pcOffset){
    pw.printf("%s      %d: %+d (%d)\n", prefix, val, pcOffset, (pc + pcOffset));
  }

  public void wide() {
    pw.printf("%s%3d: %s\n", prefix, pc, "wide");
  }

  public void unknown(int bytecode) {
    pw.printf("%s%3d: %s\n", prefix, pc, "");
  }


}
