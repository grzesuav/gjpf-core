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

package gov.nasa.jpf.jvm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFNativePeerException;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.JPFLogger;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.bcel.Constants;

/**
 * a MethodInfo for a native peer executed method
 */
public class NativeMethodInfo extends MethodInfo {

  static JPFLogger logger = JPF.getLogger("gov.nasa.jpf.jvm.NativePeer");

  static final int  MAX_NARGS = 6;
  static Object[][]  argCache;

  static {
    argCache = new Object[MAX_NARGS][];

    for (int i = 0; i < MAX_NARGS; i++) {
      argCache[i] = new Object[i];
    }
  }


  Method mth; // the native method to execute in lieu

  NativePeer peer;



  public NativeMethodInfo (MethodInfo mi, Method mth, NativePeer peer){
    super(mi.globalId);

    uniqueName = mi.uniqueName;
    name = mi.name;
    signature = mi.signature;
    ci = mi.ci;
    modifiers = mi.modifiers;
    attrs = mi.attrs;
    thrownExceptionClassNames = mi.thrownExceptionClassNames;
    parameterAnnotations = mi.parameterAnnotations;

    // what about maxLocals and maxStack?

    // we just have one dummy instruction
    code = new Instruction[1];
    code[0] = insnFactory.create(ci, Constants.NOP); // maybe we should have a EXEC_NATIVE pseudo insn

    this.peer = peer;
    this.mth = mth;
  }

  public void replace( MethodInfo mi){
    mthTable.set(mi.globalId, this);
    mi.ci.putDeclaredMethod(this);
  }

  @Override
  public boolean isMJI () {
    return true;
  }

  public NativePeer getNativePeer() {
    return peer;
  }

  @Override
  public String getStackTraceSource() {
    return "native " + peer.getClass().getName();
  }

  @Override
  public int getLineNumber (Instruction pc) {
    return -1; // we have no line numbers
  }

  @Override
  protected StackFrame createStackFrame (ThreadInfo ti){
    return new NativeStackFrame(this, ti.getTopFrame());
  }


  @Override
  public Instruction execute (ThreadInfo ti) {
    Object   ret = null;
    Object[] args = null;
    String   exception;
    MJIEnv   env = ti.getMJIEnv();
    ElementInfo ei = null;
    JVM vm = ti.getVM();

    env.setCallEnvironment(this);

    if (mth == null) {
      return ti.createAndThrowException("java.lang.UnsatisfiedLinkError",
                                        "cannot find native " + ci.getName() + '.' + getName());
    }

    try {
      args = getArguments(env, ti, mth);

      enter(ti); // this does the locking, stackframe push and enter notification

      ret = mth.invoke(peer.getPeerClass(), args);

      // these are our non-standard returns
      if ((exception = env.getException()) != null) {
        String details = env.getExceptionDetails();

        // even though we should prefer throwing normal exceptions,
        // sometimes it might be better/required to explicitly throw
        // something that's not wrapped into a InvocationTargetException
        // (e.g. InterruptedException), which is why there still is a
        // MJIEnv.throwException()
        return ti.createAndThrowException(exception, details);
      }

      if (env.getRepeat()) {
        if (ti.getTopFrame().getMethodInfo() == this){
          ti.popFrame();
        }
        // don't pop arguments, we will re-execute
        return ti.getPC();
      }

      leave(ti); // this does unlocking and exit notification, the stackframe is still pushed

      // since there is no RETURN, we have to clean up the stack frames ourselves
      ti.popFrame();
      ti.removeArguments(this);  // watch out - that means the callers stack is modified during the INVOKE

      releaseArgArray(args);

      if (ret != null){
        pushReturnValue(ti, ret, env.getReturnAttribute());
      }

    } catch (IllegalArgumentException iax) {
      logger.warning(iax.toString());
      return ti.createAndThrowException("java.lang.IllegalArgumentException",
                                        "calling " + ci.getName() + '.' + getName());
    } catch (IllegalAccessException ilax) {
      logger.warning(ilax.toString());
      return ti.createAndThrowException("java.lang.IllegalAccessException",
                                        "calling " + ci.getName() + '.' + getName());
    } catch (InvocationTargetException itx) {
      // this will catch all exceptions thrown by the native method execution
      // we don't try to hand them back to the application
      throw new JPFNativePeerException("exception in native method "
          + ci.getName() + '.' + getName(), itx.getTargetException());

    } finally {
      // unlocking for exceptional exits would occur in ti.ceateAndThrowException()

      // bad native methods might keep references around
      env.clearCallEnvironment();
    }

    Instruction pc = ti.getPC();

    // System.exit() now creates a CG, i.e. there is a next pc, but it
    // will never be executed if the termination condition is recognized correctly
    // (if not, we get an AssertionError when the CG is used)

    // there is no RETURN for a native method, so we have to advance explicitly
    return pc.getNext();
  }


  private Object[] getArgArray (int n) {
    Object[] a;
    if (n < MAX_NARGS) {
      a = argCache[n];
      if (a != null){
        argCache[n] = null;
      } else {
        a = new Object[n];
      }
    } else {
      a = new Object[n];
    }

    return a;
  }

  private void releaseArgArray (Object[] a){
    int n = a.length;
    if (n < MAX_NARGS){
      if (argCache[n] == null){
        argCache[n] = a;
      }
    }

    // can't reset lastArgs because we haven't notified listeners yet
  }


  /**
   * Get and convert the native method parameters off the ThreadInfo stack.
   * Use the MethodInfo parameter type info for this (not the reflect.Method
   * type array), or otherwise we won't have any type check
   */
  private Object[] getArguments (MJIEnv env, ThreadInfo ti, Method mth) {
    int      nArgs = getNumberOfArguments();
    Object[] a = getArgArray(nArgs + 2);
    byte[]   argTypes = getArgumentTypes();
    int      stackOffset;
    int      i, j, k;
    int      ival;
    long     lval;
    StackFrame caller = ti.getTopFrame();


    for (i = 0, stackOffset = 0, j = nArgs + 1, k = nArgs - 1;
         i < nArgs;
         i++, j--, k--) {
      switch (argTypes[k]) {
      case Types.T_BOOLEAN:
        ival = caller.peek(stackOffset);
        a[j] = Boolean.valueOf(Types.intToBoolean(ival));

        break;

      case Types.T_BYTE:
        ival = caller.peek(stackOffset);
        a[j] = Byte.valueOf((byte) ival);

        break;

      case Types.T_CHAR:
        ival = caller.peek(stackOffset);
        a[j] = Character.valueOf((char) ival);

        break;

      case Types.T_SHORT:
        ival = caller.peek(stackOffset);
        a[j] = new Short((short) ival);

        break;

      case Types.T_INT:
        ival = caller.peek(stackOffset);
        a[j] = new Integer(ival);

        break;

      case Types.T_LONG:
        lval = caller.longPeek(stackOffset);
        stackOffset++; // 2 stack words
        a[j] = new Long(lval);

        break;

      case Types.T_FLOAT:
        ival = caller.peek(stackOffset);
        a[j] = new Float(Types.intToFloat(ival));

        break;

      case Types.T_DOUBLE:
        lval = caller.longPeek(stackOffset);
        stackOffset++; // 2 stack words
        a[j] = new Double(Types.longToDouble(lval));

        break;

      default:
        // NOTE - we have to store T_REFERENCE as an Integer, because
        // it shows up in our native method as an 'int'
        ival = caller.peek(stackOffset);
        a[j] = new Integer(ival);
      }

      stackOffset++;
    }

    if (isStatic()) {
      a[1] = new Integer(ci.getClassObjectRef());
    } else {
      a[1] = new Integer(ti.getCalleeThis(this));
    }

    a[0] = env;

    return a;
  }


  private void pushReturnValue (ThreadInfo ti, Object ret, Object retAttr) {
    int  ival;
    long lval;
    int  retSize = 1;

    // in case of a return type mismatch, we get a ClassCastException, which
    // is handled in executeMethod() and reported as a InvocationTargetException
    // (not completely accurate, but we rather go with safety)
    if (ret != null) {
      switch (getReturnType()) {
      case Types.T_BOOLEAN:
        ival = Types.booleanToInt(((Boolean) ret).booleanValue());
        ti.push(ival, false);
        break;

      case Types.T_BYTE:
        ti.push(((Byte) ret).byteValue(), false);
        break;

      case Types.T_CHAR:
        ti.push(((Character) ret).charValue(), false);
        break;

      case Types.T_SHORT:
        ti.push(((Short) ret).shortValue(), false);
        break;

      case Types.T_INT:
        ti.push(((Integer) ret).intValue(), false);
        break;

      case Types.T_LONG:
        ti.longPush(((Long) ret).longValue());
        retSize=2;
        break;

      case Types.T_FLOAT:
        ival = Types.floatToInt(((Float) ret).floatValue());
        ti.push(ival, false);
        break;

      case Types.T_DOUBLE:
        lval = Types.doubleToLong(((Double) ret).doubleValue());
        ti.longPush(lval);
        retSize=2;
        break;

      default:
        // everything else is supposed to be a reference
        ti.push(((Integer) ret).intValue(), true);
      }

      if (retAttr != null) {
        StackFrame frame = ti.getTopFrame(); // no need to clone anymore
        if (retSize == 1) {
          frame.setOperandAttr(retAttr);
        } else {
          frame.setLongOperandAttr(retAttr);
        }
      }
    }
  }

}
