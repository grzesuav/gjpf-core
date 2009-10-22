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

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.NativePeer;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Types;

import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.ConstantPoolGen;


/**
 * abstraction for all invoke instructions
 */
public abstract class InvokeInstruction extends Instruction {
  /* Those are all from the BCEL class, i.e. straight from the class file.
   * Note that we can't directly resolve to MethodInfo objects because
   * the corresponding class might not be loaded yet (has to be done
   * on execution)
   */
  protected String cname;
  protected String mname;
  protected String signature;

  protected int argSize = -1;

  /** to cache the last callee object */
  protected int lastObj = Integer.MIN_VALUE;

  /**
   * watch out - this is only const for static and special invocation
   * all virtuals will use it only as a cache
   */
  protected MethodInfo invokedMethod;

  protected Object[] arguments; // temporary cache for arg values (for listeners)

  protected InvokeInstruction () {}

  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
    org.apache.bcel.generic.InvokeInstruction ii;
    ConstantPoolGen cpg;

    cpg = ClassInfo.getConstantPoolGen(cp);
    ii = (org.apache.bcel.generic.InvokeInstruction) i;

    cname = ii.getReferenceType(cpg).toString();
    signature = ii.getSignature(cpg);
    mname = MethodInfo.getUniqueName(ii.getMethodName(cpg), signature);
  }

  public int getLength() {
    return 3; // opcode, index1, index2
  }

  /**
   * this is for explicit initialization (not BCEL)
   */
  public void setInvokedMethod (String clsName, String mthName, String sig) {
    cname = clsName;
    mname = mthName + sig;
    signature = sig;
  }

  /**
   * be aware of that this might differ from getInvokedMethod(), since it only
   * denotes the target type info we have at the static point of the call, i.e.
   * before dynamic dispatching
   */
  public String getInvokedMethodClassName() {
    return cname;
  }

  public String getInvokedMethodSignature() {
    return signature;
  }

  public String getInvokedMethodName () {
    return mname;
  }

  public abstract MethodInfo getInvokedMethod (ThreadInfo ti);

  public MethodInfo getInvokedMethod () {
    if (invokedMethod == null){
      invokedMethod = getInvokedMethod(ThreadInfo.getCurrentThread());
    }

    return invokedMethod;
  }

  public boolean isCompleted(ThreadInfo ti) {
    Instruction nextPc = ti.getNextPC();

    if (nextPc == null || nextPc == this){
      return false;
    }

    if (invokedMethod != null){
      MethodInfo topMethod = ti.getTopFrame().getMethodInfo();
      if (invokedMethod.isMJI() && (topMethod == mi)) {
        // same frame -> this was a native method that already returned
        return true;
      }

      if (topMethod == invokedMethod){
        return true;
      }
    }

    // <2do> how do we account for exceptions?

    return false;
  }

  StackFrame getCallerFrame (ThreadInfo ti, MethodInfo callee) {
    StackFrame frame = null;

    if (isCompleted(ti) && callee.isMJI()){
      // note that args are already from the caller stack (native methods
      // are executed synchronously, and push the return value BEFORE returning
      // from the invoke insn)
      frame = NativePeer.getLastCaller();
    } else {
      // locate the caller stack and get it from there. Note this might be
      // further down the stack, since we might already have pushed the
      // callee frame (or a direct call overlay like clinit)
      frame = ti.getStackFrameExecuting(this, 0);
    }

    return frame;
  }

  /**
   * this is a little helper to find out about call argument values from listeners that
   * don't want to dig through MethodInfos and Types. Reference arguments are returned as
   * either ElementInfos or 'null', all others are boxed (i.e. a 'double' is returned as
   * a 'Double' object).
   * It goes without saying that this method can only be called during an executeInstruction()
   * or instructionExecuted() notification for the corresponding InvokeInstruction
   */
  public Object[] getArgumentValues (ThreadInfo ti) {
    MethodInfo callee = getInvokedMethod(ti);
    StackFrame frame = getCallerFrame(ti, callee);

    assert frame != null : "can't find caller stackframe for: " + this;

    return getArgsFromCaller(frame, callee);
  }

  public Object[] getArgumentAttrs (ThreadInfo ti) {
    MethodInfo callee = getInvokedMethod(ti);
    StackFrame frame = getCallerFrame(ti, callee);

    assert frame != null : "can't find caller stackframe for: " + this;
    return frame.getArgumentAttrs(callee);
  }

  Object[] getArgsFromCaller (StackFrame frame, MethodInfo callee){
    int n = callee.getNumberOfArguments();
    Object[] args = new Object[n];
    byte[] at = callee.getArgumentTypes();

    for (int i=n-1, off=0; i>=0; i--) {
      switch (at[i]) {
      case Types.T_ARRAY:
      //case Types.T_OBJECT:
      case Types.T_REFERENCE:
        int ref = frame.peek(off);
        if (ref >=0) {
          args[i] = DynamicArea.getHeap().get(ref);
        } else {
          args[i] = null;
        }
        off++;
        break;

      case Types.T_LONG:
        args[i] = new Long(frame.longPeek(off));
        off+=2;
        break;
      case Types.T_DOUBLE:
        args[i] = new Double(Types.longToDouble(frame.longPeek(off)));
        off+=2;
        break;

      case Types.T_BOOLEAN:
        args[i] = new Boolean(frame.peek(off) != 0);
        off++;
        break;
      case Types.T_BYTE:
        args[i] = new Byte((byte)frame.peek(off));
        off++;
        break;
      case Types.T_CHAR:
        args[i] = new Character((char)frame.peek(off));
        off++;
        break;
      case Types.T_SHORT:
        args[i] = new Short((short)frame.peek(off));
        off++;
        break;
      case Types.T_INT:
        args[i] = new Integer((int)frame.peek(off));
        off++;
        break;
      case Types.T_FLOAT:
        args[i] = new Float(Types.intToFloat(frame.peek(off)));
        off++;
        break;
      default:
        // error, unknown argument type
      }
    }

    return args;
  }

  public int getArgSize () {
    if (argSize < 0) {
      argSize = Types.getArgumentsSize(signature) + 1; // 'this'
    }

    return argSize;
  }

  public int getReturnType() {
    return Types.getReturnType(signature);
  }

  public boolean isReferenceReturnType() {
    int r = Types.getReturnType(signature);
    return ((r == Types.T_REFERENCE) || (r == Types.T_ARRAY));
  }

  public String getReturnTypeName() {
    return Types.getReturnTypeName(signature);
  }

  public Object getFieldOrArgumentValue (String id, ThreadInfo ti){
    Object v = null;

    if ((v = getArgumentValue(id,ti)) == null){
      v = getFieldValue(id, ti);
    }

    return v;
  }

  public abstract Object getFieldValue (String id, ThreadInfo ti);

  public Object getArgumentValue (String id, ThreadInfo ti){
    MethodInfo mi = getInvokedMethod();
    String[] localNames = mi.getLocalVariableNames();
    Object[] args = getArgumentValues(ti);

    if (localNames != null){
      int j = mi.isStatic() ? 0 : 1;

      for (int i=0; i<args.length; i++, j++){
        Object a = args[i];
        if (localNames[j].equals(id)){
          return a;
        } else {
          if (a instanceof Long || a instanceof Double){
            j++;
          }
        }
      }
    }

    return null;
  }

  /**
   * NOTE this makes only sense for synchronized methods, don't call it otherwise
   */
  protected ChoiceGenerator<?> getSyncCG (int objRef, MethodInfo mi,
                                          SystemState ss, KernelState ks, ThreadInfo ti) {
    ElementInfo ei = ks.da.get(objRef);

    if (ei.getLockingThread() == ti) {
      assert ei.getLockCount() > 0;
      // a little optimization - recursive locks are always left movers
      return  null;
    }

    // first time around - reexecute if the scheduling policy gives us a choice point
    if (!ti.isFirstStepInsn()) {

      if (!ei.canLock(ti)) {
        // block first, so that we don't get this thread in the list of CGs
        ei.block(ti);
      }

      ChoiceGenerator<?> cg = ss.getSchedulerFactory().createSyncMethodEnterCG(ei, ti);
      if (cg != null) { // Ok, break here
        if (!ti.isBlocked()) {
          // record that this thread would lock the object upon next execution
          ei.registerLockContender(ti);
        }
        return cg;
      }

      assert !ti.isBlocked() : "scheduling policy did not return ChoiceGenerator for blocking INVOKE";
    }

    return null;
  }
}
