//
// Copyright (C) 2009 United States Government as represented by the
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

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.InstructionHandle;

/**
 * default InstructionFactory implementation for concrete value execution
 */
public class DefaultInstructionFactory implements InstructionFactory {

  static Class<? extends Instruction>[] insnClass;

  @SuppressWarnings("unchecked")
  static public Class<? extends Instruction>[] createInsnClassArray(int n) {
    return (Class<? extends Instruction>[]) new Class<?>[n];
  }

  static {
    insnClass = createInsnClassArray(260);

    // note that bcel doesn't have different instruction classes for
    // immediate operand variants

    insnClass[NOP]             = gov.nasa.jpf.jvm.bytecode.NOP.class;
    insnClass[ACONST_NULL]     = gov.nasa.jpf.jvm.bytecode.ACONST_NULL.class;
    insnClass[ICONST_M1]       = gov.nasa.jpf.jvm.bytecode.ICONST.class;
    insnClass[ICONST_0]        = gov.nasa.jpf.jvm.bytecode.ICONST.class;
    insnClass[ICONST_1]        = gov.nasa.jpf.jvm.bytecode.ICONST.class;
    insnClass[ICONST_2]        = gov.nasa.jpf.jvm.bytecode.ICONST.class;
    insnClass[ICONST_3]        = gov.nasa.jpf.jvm.bytecode.ICONST.class;
    insnClass[ICONST_4]        = gov.nasa.jpf.jvm.bytecode.ICONST.class;
    insnClass[ICONST_5]        = gov.nasa.jpf.jvm.bytecode.ICONST.class;
    insnClass[LCONST_0]        = gov.nasa.jpf.jvm.bytecode.LCONST.class;
    insnClass[LCONST_1]        = gov.nasa.jpf.jvm.bytecode.LCONST.class;
    insnClass[FCONST_0]        = gov.nasa.jpf.jvm.bytecode.FCONST.class;
    insnClass[FCONST_1]        = gov.nasa.jpf.jvm.bytecode.FCONST.class;
    insnClass[FCONST_2]        = gov.nasa.jpf.jvm.bytecode.FCONST.class;
    insnClass[DCONST_0]        = gov.nasa.jpf.jvm.bytecode.DCONST.class;
    insnClass[DCONST_1]        = gov.nasa.jpf.jvm.bytecode.DCONST.class;

    insnClass[BIPUSH]          = gov.nasa.jpf.jvm.bytecode.BIPUSH.class;
    insnClass[SIPUSH]          = gov.nasa.jpf.jvm.bytecode.SIPUSH.class;
    insnClass[LDC]             = gov.nasa.jpf.jvm.bytecode.LDC.class;
    insnClass[LDC_W]           = gov.nasa.jpf.jvm.bytecode.LDC_W.class;
    insnClass[LDC2_W]          = gov.nasa.jpf.jvm.bytecode.LDC2_W.class;
    insnClass[ILOAD]           = gov.nasa.jpf.jvm.bytecode.ILOAD.class;
    insnClass[LLOAD]           = gov.nasa.jpf.jvm.bytecode.LLOAD.class;
    insnClass[FLOAD]           = gov.nasa.jpf.jvm.bytecode.FLOAD.class;
    insnClass[DLOAD]           = gov.nasa.jpf.jvm.bytecode.DLOAD.class;
    insnClass[ALOAD]           = gov.nasa.jpf.jvm.bytecode.ALOAD.class;
    insnClass[ILOAD_0]         = gov.nasa.jpf.jvm.bytecode.ILOAD.class;
    insnClass[ILOAD_1]         = gov.nasa.jpf.jvm.bytecode.ILOAD.class;
    insnClass[ILOAD_2]         = gov.nasa.jpf.jvm.bytecode.ILOAD.class;
    insnClass[ILOAD_3]         = gov.nasa.jpf.jvm.bytecode.ILOAD.class;
    insnClass[LLOAD_0]         = gov.nasa.jpf.jvm.bytecode.LLOAD.class;
    insnClass[LLOAD_1]         = gov.nasa.jpf.jvm.bytecode.LLOAD.class;
    insnClass[LLOAD_2]         = gov.nasa.jpf.jvm.bytecode.LLOAD.class;
    insnClass[LLOAD_3]         = gov.nasa.jpf.jvm.bytecode.LLOAD.class;
    insnClass[FLOAD_0]         = gov.nasa.jpf.jvm.bytecode.FLOAD.class;
    insnClass[FLOAD_1]         = gov.nasa.jpf.jvm.bytecode.FLOAD.class;
    insnClass[FLOAD_2]         = gov.nasa.jpf.jvm.bytecode.FLOAD.class;
    insnClass[FLOAD_3]         = gov.nasa.jpf.jvm.bytecode.FLOAD.class;
    insnClass[DLOAD_0]         = gov.nasa.jpf.jvm.bytecode.DLOAD.class;
    insnClass[DLOAD_1]         = gov.nasa.jpf.jvm.bytecode.DLOAD.class;
    insnClass[DLOAD_2]         = gov.nasa.jpf.jvm.bytecode.DLOAD.class;
    insnClass[DLOAD_3]         = gov.nasa.jpf.jvm.bytecode.DLOAD.class;
    insnClass[ALOAD_0]         = gov.nasa.jpf.jvm.bytecode.ALOAD.class;
    insnClass[ALOAD_1]         = gov.nasa.jpf.jvm.bytecode.ALOAD.class;
    insnClass[ALOAD_2]         = gov.nasa.jpf.jvm.bytecode.ALOAD.class;
    insnClass[ALOAD_3]         = gov.nasa.jpf.jvm.bytecode.ALOAD.class;
    insnClass[IALOAD]          = gov.nasa.jpf.jvm.bytecode.IALOAD.class;
    insnClass[LALOAD]          = gov.nasa.jpf.jvm.bytecode.LALOAD.class;
    insnClass[FALOAD]          = gov.nasa.jpf.jvm.bytecode.FALOAD.class;
    insnClass[DALOAD]          = gov.nasa.jpf.jvm.bytecode.DALOAD.class;
    insnClass[AALOAD]          = gov.nasa.jpf.jvm.bytecode.AALOAD.class;
    insnClass[BALOAD]          = gov.nasa.jpf.jvm.bytecode.BALOAD.class;
    insnClass[CALOAD]          = gov.nasa.jpf.jvm.bytecode.CALOAD.class;
    insnClass[SALOAD]          = gov.nasa.jpf.jvm.bytecode.SALOAD.class;
    insnClass[ISTORE]          = gov.nasa.jpf.jvm.bytecode.ISTORE.class;
    insnClass[LSTORE]          = gov.nasa.jpf.jvm.bytecode.LSTORE.class;
    insnClass[FSTORE]          = gov.nasa.jpf.jvm.bytecode.FSTORE.class;
    insnClass[DSTORE]          = gov.nasa.jpf.jvm.bytecode.DSTORE.class;
    insnClass[ASTORE]          = gov.nasa.jpf.jvm.bytecode.ASTORE.class;
    insnClass[ISTORE_0]        = gov.nasa.jpf.jvm.bytecode.ISTORE.class;
    insnClass[ISTORE_1]        = gov.nasa.jpf.jvm.bytecode.ISTORE.class;
    insnClass[ISTORE_2]        = gov.nasa.jpf.jvm.bytecode.ISTORE.class;
    insnClass[ISTORE_3]        = gov.nasa.jpf.jvm.bytecode.ISTORE.class;
    insnClass[LSTORE_0]        = gov.nasa.jpf.jvm.bytecode.LSTORE.class;
    insnClass[LSTORE_1]        = gov.nasa.jpf.jvm.bytecode.LSTORE.class;
    insnClass[LSTORE_2]        = gov.nasa.jpf.jvm.bytecode.LSTORE.class;
    insnClass[LSTORE_3]        = gov.nasa.jpf.jvm.bytecode.LSTORE.class;
    insnClass[FSTORE_0]        = gov.nasa.jpf.jvm.bytecode.FSTORE.class;
    insnClass[FSTORE_1]        = gov.nasa.jpf.jvm.bytecode.FSTORE.class;
    insnClass[FSTORE_2]        = gov.nasa.jpf.jvm.bytecode.FSTORE.class;
    insnClass[FSTORE_3]        = gov.nasa.jpf.jvm.bytecode.FSTORE.class;
    insnClass[DSTORE_0]        = gov.nasa.jpf.jvm.bytecode.DSTORE.class;
    insnClass[DSTORE_1]        = gov.nasa.jpf.jvm.bytecode.DSTORE.class;
    insnClass[DSTORE_2]        = gov.nasa.jpf.jvm.bytecode.DSTORE.class;
    insnClass[DSTORE_3]        = gov.nasa.jpf.jvm.bytecode.DSTORE.class;

    insnClass[ASTORE_0]        = gov.nasa.jpf.jvm.bytecode.ASTORE.class;
    insnClass[ASTORE_1]        = gov.nasa.jpf.jvm.bytecode.ASTORE.class;
    insnClass[ASTORE_2]        = gov.nasa.jpf.jvm.bytecode.ASTORE.class;
    insnClass[ASTORE_3]        = gov.nasa.jpf.jvm.bytecode.ASTORE.class;
    insnClass[IASTORE]         = gov.nasa.jpf.jvm.bytecode.IASTORE.class;
    insnClass[LASTORE]         = gov.nasa.jpf.jvm.bytecode.LASTORE.class;
    insnClass[FASTORE]         = gov.nasa.jpf.jvm.bytecode.FASTORE.class;
    insnClass[DASTORE]         = gov.nasa.jpf.jvm.bytecode.DASTORE.class;
    insnClass[AASTORE]         = gov.nasa.jpf.jvm.bytecode.AASTORE.class;
    insnClass[BASTORE]         = gov.nasa.jpf.jvm.bytecode.BASTORE.class;
    insnClass[CASTORE]         = gov.nasa.jpf.jvm.bytecode.CASTORE.class;
    insnClass[SASTORE]         = gov.nasa.jpf.jvm.bytecode.SASTORE.class;

    insnClass[POP]             = gov.nasa.jpf.jvm.bytecode.POP.class;
    insnClass[POP2]            = gov.nasa.jpf.jvm.bytecode.POP2.class;

    insnClass[DUP]             = gov.nasa.jpf.jvm.bytecode.DUP.class;
    insnClass[DUP_X1]          = gov.nasa.jpf.jvm.bytecode.DUP_X1.class;
    insnClass[DUP_X2]          = gov.nasa.jpf.jvm.bytecode.DUP_X2.class;
    insnClass[DUP2]            = gov.nasa.jpf.jvm.bytecode.DUP2.class;
    insnClass[DUP2_X1]         = gov.nasa.jpf.jvm.bytecode.DUP2_X1.class;
    insnClass[DUP2_X2]         = gov.nasa.jpf.jvm.bytecode.DUP2_X2.class;
    insnClass[SWAP]            = gov.nasa.jpf.jvm.bytecode.SWAP.class;

    insnClass[IADD]            = gov.nasa.jpf.jvm.bytecode.IADD.class;
    insnClass[LADD]            = gov.nasa.jpf.jvm.bytecode.LADD.class;
    insnClass[FADD]            = gov.nasa.jpf.jvm.bytecode.FADD.class;
    insnClass[DADD]            = gov.nasa.jpf.jvm.bytecode.DADD.class;
    insnClass[ISUB]            = gov.nasa.jpf.jvm.bytecode.ISUB.class;
    insnClass[LSUB]            = gov.nasa.jpf.jvm.bytecode.LSUB.class;
    insnClass[FSUB]            = gov.nasa.jpf.jvm.bytecode.FSUB.class;
    insnClass[DSUB]            = gov.nasa.jpf.jvm.bytecode.DSUB.class;
    insnClass[IMUL]            = gov.nasa.jpf.jvm.bytecode.IMUL.class;
    insnClass[LMUL]            = gov.nasa.jpf.jvm.bytecode.LMUL.class;
    insnClass[FMUL]            = gov.nasa.jpf.jvm.bytecode.FMUL.class;
    insnClass[DMUL]            = gov.nasa.jpf.jvm.bytecode.DMUL.class;
    insnClass[IDIV]            = gov.nasa.jpf.jvm.bytecode.IDIV.class;
    insnClass[LDIV]            = gov.nasa.jpf.jvm.bytecode.LDIV.class;
    insnClass[FDIV]            = gov.nasa.jpf.jvm.bytecode.FDIV.class;
    insnClass[DDIV]            = gov.nasa.jpf.jvm.bytecode.DDIV.class;

    insnClass[IREM]            = gov.nasa.jpf.jvm.bytecode.IREM.class;
    insnClass[LREM]            = gov.nasa.jpf.jvm.bytecode.LREM.class;
    insnClass[FREM]            = gov.nasa.jpf.jvm.bytecode.FREM.class;
    insnClass[DREM]            = gov.nasa.jpf.jvm.bytecode.DREM.class;

    insnClass[INEG]            = gov.nasa.jpf.jvm.bytecode.INEG.class;
    insnClass[LNEG]            = gov.nasa.jpf.jvm.bytecode.LNEG.class;
    insnClass[FNEG]            = gov.nasa.jpf.jvm.bytecode.FNEG.class;
    insnClass[DNEG]            = gov.nasa.jpf.jvm.bytecode.DNEG.class;

    insnClass[ISHL]            = gov.nasa.jpf.jvm.bytecode.ISHL.class;
    insnClass[LSHL]            = gov.nasa.jpf.jvm.bytecode.LSHL.class;
    insnClass[ISHR]            = gov.nasa.jpf.jvm.bytecode.ISHR.class;
    insnClass[LSHR]            = gov.nasa.jpf.jvm.bytecode.LSHR.class;
    insnClass[IUSHR]           = gov.nasa.jpf.jvm.bytecode.IUSHR.class;
    insnClass[LUSHR]           = gov.nasa.jpf.jvm.bytecode.LUSHR.class;

    insnClass[IAND]            = gov.nasa.jpf.jvm.bytecode.IAND.class;
    insnClass[LAND]            = gov.nasa.jpf.jvm.bytecode.LAND.class;
    insnClass[IOR]             = gov.nasa.jpf.jvm.bytecode.IOR.class;
    insnClass[LOR]             = gov.nasa.jpf.jvm.bytecode.LOR.class;
    insnClass[IXOR]            = gov.nasa.jpf.jvm.bytecode.IXOR.class;
    insnClass[LXOR]            = gov.nasa.jpf.jvm.bytecode.LXOR.class;
    insnClass[IINC]            = gov.nasa.jpf.jvm.bytecode.IINC.class;

    insnClass[I2L]             = gov.nasa.jpf.jvm.bytecode.I2L.class;
    insnClass[I2F]             = gov.nasa.jpf.jvm.bytecode.I2F.class;
    insnClass[I2D]             = gov.nasa.jpf.jvm.bytecode.I2D.class;
    insnClass[L2I]             = gov.nasa.jpf.jvm.bytecode.L2I.class;
    insnClass[L2F]             = gov.nasa.jpf.jvm.bytecode.L2F.class;
    insnClass[L2D]             = gov.nasa.jpf.jvm.bytecode.L2D.class;
    insnClass[F2I]             = gov.nasa.jpf.jvm.bytecode.F2I.class;
    insnClass[F2L]             = gov.nasa.jpf.jvm.bytecode.F2L.class;
    insnClass[F2D]             = gov.nasa.jpf.jvm.bytecode.F2D.class;
    insnClass[D2I]             = gov.nasa.jpf.jvm.bytecode.D2I.class;
    insnClass[D2L]             = gov.nasa.jpf.jvm.bytecode.D2L.class;
    insnClass[D2F]             = gov.nasa.jpf.jvm.bytecode.D2F.class;
    insnClass[I2B]             = gov.nasa.jpf.jvm.bytecode.I2B.class;
    insnClass[I2C]             = gov.nasa.jpf.jvm.bytecode.I2C.class;
    insnClass[I2S]             = gov.nasa.jpf.jvm.bytecode.I2S.class;

    insnClass[LCMP]            = gov.nasa.jpf.jvm.bytecode.LCMP.class;
    insnClass[FCMPL]           = gov.nasa.jpf.jvm.bytecode.FCMPL.class;
    insnClass[FCMPG]           = gov.nasa.jpf.jvm.bytecode.FCMPG.class;
    insnClass[DCMPL]           = gov.nasa.jpf.jvm.bytecode.DCMPL.class;
    insnClass[DCMPG]           = gov.nasa.jpf.jvm.bytecode.DCMPG.class;
    insnClass[IFEQ]            = gov.nasa.jpf.jvm.bytecode.IFEQ.class;
    insnClass[IFNE]            = gov.nasa.jpf.jvm.bytecode.IFNE.class;
    insnClass[IFLT]            = gov.nasa.jpf.jvm.bytecode.IFLT.class;
    insnClass[IFGE]            = gov.nasa.jpf.jvm.bytecode.IFGE.class;
    insnClass[IFGT]            = gov.nasa.jpf.jvm.bytecode.IFGT.class;
    insnClass[IFLE]            = gov.nasa.jpf.jvm.bytecode.IFLE.class;

    insnClass[IF_ICMPEQ]       = gov.nasa.jpf.jvm.bytecode.IF_ICMPEQ.class;
    insnClass[IF_ICMPNE]       = gov.nasa.jpf.jvm.bytecode.IF_ICMPNE.class;
    insnClass[IF_ICMPLT]       = gov.nasa.jpf.jvm.bytecode.IF_ICMPLT.class;
    insnClass[IF_ICMPGE]       = gov.nasa.jpf.jvm.bytecode.IF_ICMPGE.class;
    insnClass[IF_ICMPGT]       = gov.nasa.jpf.jvm.bytecode.IF_ICMPGT.class;
    insnClass[IF_ICMPLE]       = gov.nasa.jpf.jvm.bytecode.IF_ICMPLE.class;

    insnClass[GOTO]            = gov.nasa.jpf.jvm.bytecode.GOTO.class;
    insnClass[IF_ACMPEQ]       = gov.nasa.jpf.jvm.bytecode.IF_ACMPEQ.class;
    insnClass[IF_ACMPNE]       = gov.nasa.jpf.jvm.bytecode.IF_ACMPNE.class;
    insnClass[JSR]             = gov.nasa.jpf.jvm.bytecode.JSR.class;
    insnClass[RET]             = gov.nasa.jpf.jvm.bytecode.RET.class;
    insnClass[TABLESWITCH]     = gov.nasa.jpf.jvm.bytecode.TABLESWITCH.class;
    insnClass[LOOKUPSWITCH]    = gov.nasa.jpf.jvm.bytecode.LOOKUPSWITCH.class;
    insnClass[IRETURN]         = gov.nasa.jpf.jvm.bytecode.IRETURN.class;
    insnClass[LRETURN]         = gov.nasa.jpf.jvm.bytecode.LRETURN.class;
    insnClass[FRETURN]         = gov.nasa.jpf.jvm.bytecode.FRETURN.class;
    insnClass[DRETURN]         = gov.nasa.jpf.jvm.bytecode.DRETURN.class;

    insnClass[ARETURN]         = gov.nasa.jpf.jvm.bytecode.ARETURN.class;
    insnClass[RETURN]          = gov.nasa.jpf.jvm.bytecode.RETURN.class;

    insnClass[GETSTATIC]       = gov.nasa.jpf.jvm.bytecode.GETSTATIC.class;
    insnClass[PUTSTATIC]       = gov.nasa.jpf.jvm.bytecode.PUTSTATIC.class;
    insnClass[GETFIELD]        = gov.nasa.jpf.jvm.bytecode.GETFIELD.class;
    insnClass[PUTFIELD]        = gov.nasa.jpf.jvm.bytecode.PUTFIELD.class;

    insnClass[INVOKEVIRTUAL]   = gov.nasa.jpf.jvm.bytecode.INVOKEVIRTUAL.class;
    insnClass[INVOKESPECIAL]   = gov.nasa.jpf.jvm.bytecode.INVOKESPECIAL.class;
    insnClass[INVOKESTATIC]    = gov.nasa.jpf.jvm.bytecode.INVOKESTATIC.class;
    insnClass[INVOKEINTERFACE] = gov.nasa.jpf.jvm.bytecode.INVOKEINTERFACE.class;

    insnClass[NEW]             = gov.nasa.jpf.jvm.bytecode.NEW.class;
    insnClass[NEWARRAY]        = gov.nasa.jpf.jvm.bytecode.NEWARRAY.class;
    insnClass[ANEWARRAY]       = gov.nasa.jpf.jvm.bytecode.ANEWARRAY.class;
    insnClass[ARRAYLENGTH]     = gov.nasa.jpf.jvm.bytecode.ARRAYLENGTH.class;

    insnClass[ATHROW]          = gov.nasa.jpf.jvm.bytecode.ATHROW.class;

    insnClass[CHECKCAST]       = gov.nasa.jpf.jvm.bytecode.CHECKCAST.class;
    insnClass[INSTANCEOF]      = gov.nasa.jpf.jvm.bytecode.INSTANCEOF.class;

    insnClass[MONITORENTER]    = gov.nasa.jpf.jvm.bytecode.MONITORENTER.class;
    insnClass[MONITOREXIT]     = gov.nasa.jpf.jvm.bytecode.MONITOREXIT.class;

    insnClass[WIDE]            = gov.nasa.jpf.jvm.bytecode.WIDE.class;
    insnClass[MULTIANEWARRAY]  = gov.nasa.jpf.jvm.bytecode.MULTIANEWARRAY.class;
    insnClass[IFNULL]          = gov.nasa.jpf.jvm.bytecode.IFNULL.class;
    insnClass[IFNONNULL]       = gov.nasa.jpf.jvm.bytecode.IFNONNULL.class;

    insnClass[GOTO_W]          = gov.nasa.jpf.jvm.bytecode.GOTO_W.class;
    insnClass[JSR_W]           = gov.nasa.jpf.jvm.bytecode.JSR_W.class;

    // our artificial bytecodes - very unlikely to ever be overridden
    insnClass[gov.nasa.jpf.jvm.bytecode.RUNSTART.OPCODE] = gov.nasa.jpf.jvm.bytecode.RUNSTART.class;
    insnClass[gov.nasa.jpf.jvm.bytecode.INVOKECLINIT.OPCODE] = gov.nasa.jpf.jvm.bytecode.INVOKECLINIT.class;
  }


  /**
   * this is used to createAndInitialize Instruction objects from corresponding bcel
   * instructions when loading classfiles
   * NOTE: this usually does not need to be overridden unless the concrete
   * factory requires special initialization
   */
  public Instruction createAndInitialize(MethodInfo mi, InstructionHandle h, int offset, ConstantPool cp) {
    Instruction insn = create(mi.getClassInfo(),h.getInstruction().getOpcode());
    insn.init(h, offset, mi, cp);
    return insn;
  }

  /**
   * this is used for explicit method construction
   * NOTE: Instruction instances are created with default ctor and might require
   * further intialization
   */
  public Instruction create(ClassInfo ciMth, int opCode) {
    Class<?> cls = insnClass[opCode];
    if (cls != null) {
      try {
        Instruction insn = (Instruction) cls.newInstance();
        return insn;

      } catch (Throwable e) {
        throw new JPFException("creation of JPF Instruction object for opCode "
                + opCode + " failed: " + e);
      }

    } else {
      throw new JPFException("no JPF Instruction class found for opCode " + opCode);
    }
  }

}
