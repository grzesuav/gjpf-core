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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.JPFNativePeerException;
import gov.nasa.jpf.jvm.bytecode.Instruction;

import java.lang.reflect.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * native peer classes are part of MJI and contain the code that is
 * executed by the host VM (i.e. outside the state-tracked JPF JVM). Each
 * class executed by JPF that has native mehods must have a native peer class
 * (which is looked up and associated at class loadtime)
 */
public class NativePeer {

  static final String MODEL_PACKAGE = "<model>";
  static final String DEFAULT_PACKAGE = "<default>";

  static Logger logger = JPF.getLogger("gov.nasa.jpf.jvm.NativePeer");

  static ClassLoader loader;
  static HashMap<String, NativePeer> peers;
  static final int  MAX = 6;
  static Object[][]  argCache;
  static Config config;

  static String[] peerPackages;

  ClassInfo ci;
  Class<?> peerClass;
  HashMap<String, Method> methods;


  public static void init (Config conf) {
    loader = conf.getClass().getClassLoader();
    peers = new HashMap<String, NativePeer>();
    argCache = new Object[MAX][];

    for (int i = 0; i < MAX; i++) {
      argCache[i] = new Object[i];
    }

    peerPackages = getPeerPackages(conf);

    config = conf;
  }

  static String[] getPeerPackages (Config conf) {
    String[] defPeerPackages = { MODEL_PACKAGE, "gov.nasa.jpf.jvm.", DEFAULT_PACKAGE };
    String[] packages = conf.getStringArray("vm.peer.packages", defPeerPackages);

    // internalize
    for (int i=0; i<packages.length; i++) {
      if (packages[i].equals(MODEL_PACKAGE)) {
        packages[i] = MODEL_PACKAGE;
      } else if (packages[i].equals(DEFAULT_PACKAGE)) {
        packages[i] = DEFAULT_PACKAGE;
      }
    }

    return packages;
  }

  NativePeer () {
    // just here for our derived classes
  }

  NativePeer (Class<?> peerClass, ClassInfo ci) {
    initialize(peerClass, ci, true);
  }

  static Class<?> locatePeerCls (String clsName) {
    String cn = "JPF_" + clsName.replace('.', '_');

    for (int i=0; i<peerPackages.length; i++) {
      String pcn;
      String pkg = peerPackages[i];

      if (pkg == MODEL_PACKAGE) {
        int j = clsName.lastIndexOf('.');
        pcn = clsName.substring(0, j+1) + cn;
      } else if (pkg == DEFAULT_PACKAGE) {
        pcn = cn;
      } else {
        pcn = pkg + '.' + cn;
      }

      try {
        Class<?> peerCls = loader.loadClass(pcn);
        
        if ((peerCls.getModifiers() & Modifier.PUBLIC) == 0) {
          logger.warning("non-public peer class: " + peerCls.getName());
          continue; // pointless to use this one, it would just create IllegalAccessExceptions
        }
        
        return peerCls;
      } catch (ClassNotFoundException cnfx) {
        // try next one
      }
    }

    return null; // nothing found
  }

  /**
   * this becomes the factory method to load either a plain (slow)
   * reflection-based peer (a NativePeer object), or some speed optimized
   * derived class object.
   * Watch out - this gets called before the ClassInfo is fully initialized
   * (we shouldn't rely on more than just its name here)
   */
  static NativePeer getNativePeer (ClassInfo ci) {
    String     clsName = ci.getName();
    NativePeer peer = peers.get(clsName);
    Class<?>      peerCls = null;

    if (peer == null) {
      peerCls = locatePeerCls(clsName);

      if (peerCls != null) {

        // Method.invoke() got fast enough so that we don't need a peer dispatcher anymore
                
        if (logger.isLoggable(Level.INFO)) {
          logger.info("load peer: " + peerCls.getName());
        }

        peer = new NativePeer(peerCls, ci);
        peers.put(clsName, peer);
      }
    }

    return peer;
  }

  static String getPeerDispatcherClassName (String clsName) {
    return (clsName + '$');
  }

  /**
   * this is the real work horse - it takes parameters of the JPF operand stack,
   * converts them into host VM types, and then does a reflection call
   */
  Instruction executeMethod (ThreadInfo ti, MethodInfo mi) {
    Object   ret = null;
    Object[] args = null;
    Method   mth;
    String   exception;
    MJIEnv   env = ti.getMJIEnv();
    ElementInfo ei = null;

    env.setCallEnvironment(mi);

    if ((mth = getMethod(mi)) == null) {
      return ti.createAndThrowException("java.lang.UnsatisfiedLinkError",
                                        "cannot find native " + ci.getName() + '.' +
                                        mi.getName());
    }

    try {
      args = getArguments(env, ti, mi, mth);

      // we have to lock here in case a native method does sync stuff, so that
      // we don't run into IllegalMonitorStateExceptions
      if (mi.isSynchronized()){
        ei = env.getElementInfo(((Integer)args[1]).intValue());
        ei.lock(ti);

        if (mi.isClinit()) {
          ci.setInitializing(ti);
        }
      }

      ret = mth.invoke(peerClass, args);

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
        // call it again
        return ti.getPC();
      }

      // Ok, we did 'return', clean up the stack
      // note that we don't have a stack frame for this
      // sucker (for state and speed sake), so we just pop the arguments here
      // watch out - that means the callers stack is modified during the INVOKE
      // (i.e. post-exec inspect would not see the args on the stack anymore)
      ti.removeArguments(mi);
      releaseArgArray(args);

      if (ret != null){
        pushReturnValue(ti, mi, ret, env.getReturnAttribute());
      }

    } catch (IllegalArgumentException iax) {
      logger.warning(iax.toString());
      return ti.createAndThrowException("java.lang.IllegalArgumentException",
                                        "calling " + ci.getName() + '.' +
                                        mi.getName());
    } catch (IllegalAccessException ilax) {
      logger.warning(ilax.toString());
      return ti.createAndThrowException("java.lang.IllegalAccessException",
                                        "calling " + ci.getName() + '.' +
                                        mi.getName());
    } catch (InvocationTargetException itx) {
      // this will catch all exceptions thrown by the native method execution
      // we don't try to hand them back to the application
      throw new JPFNativePeerException("exception in native method "
          + ci.getName() + '.' + mi.getName(), itx.getTargetException());

    } finally {
      // no matter what - if we grabbed the lock, we have to release it
      // but the native method body might actually have given up the lock, so
      // check first
      if (mi.isSynchronized() && ei != null && ei.isLocked()){
        ei.unlock(ti);

        if (mi.isClinit()) {
          ci.setInitialized();
        }
      }

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

  void initialize (Class<?> peerClass, ClassInfo ci, boolean cacheMethods) {
    if ((this.ci != null) || (this.peerClass != null)) {
      throw new RuntimeException("cannot re-initialize NativePeer: " +
                                 peerClass.getName());
    }

    this.ci = ci;
    this.peerClass = peerClass;

    loadMethods(cacheMethods);

    initializePeerClass();
  }

  void initializePeerClass() {
    // they are all static, so we can't use polymorphism
    try {
      Method m = peerClass.getDeclaredMethod("init", Config.class );
      try {
        m.invoke(null, config);
      } catch (IllegalArgumentException iax){
        // can't happen - static method
      } catch (IllegalAccessException iacx) {
        throw new RuntimeException("peer initialization method not accessible: "
                                   + peerClass.getName());
      } catch (InvocationTargetException itx){
        throw new RuntimeException("initialization of peer " +
                                   peerClass.getName() + " failed: " + itx.getCause());

      }
    } catch (NoSuchMethodException nsmx){
      // nothing to do
    }
  }

  private static boolean isMJICandidate (Method mth) {
    // only the public static ones are supposed to be native method impls
    if ((mth.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) != (Modifier.PUBLIC | Modifier.STATIC)) {
      return false;
    }

    // native method always have a MJIEnv and int as the first parameters
    Class<?>[] argTypes = mth.getParameterTypes();
    if ((argTypes.length >= 2) && (argTypes[0] == MJIEnv.class) && (argTypes[1] == int.class) ) {
      return true;
    } else {
      return false;
    }
  }

  private Object[] getArgArray (int n) {
    Object[] a;
    if (n < MAX) {
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
    if (n < MAX){
      if (argCache[n] == null){
        argCache[n] = a;
      }
    }

    // can't reset lastArgs because we haven't notified listeners yet
  }

  // this sucks - executeMethod is going to modify the caller
  // stack frame before it returns, hence any post-exec listener can't
  // see the arg values and attrs anymore. We have to cache them
  // Alternatively, we could use our Object[] arg array to retrieve the
  // values, but (a) this doesn't cover the attributes, and - worse -
  // (b) it turns reference values into Integer box objects (because they
  // are used in the reflection call of the native method)
  static StackFrame lastCaller;

  // this only makes sense from a executeInstruction/instructionExecuted listener context !!
  public static StackFrame getLastCaller() {
    return lastCaller;
  }

  /**
   * Get and convert the native method parameters off the ThreadInfo stack.
   * Use the MethodInfo parameter type info for this (not the reflect.Method
   * type array), or otherwise we won't have any type check
   */
  private Object[] getArguments (MJIEnv env, ThreadInfo ti, MethodInfo mi, Method mth) {
    int      nArgs = mi.getNumberOfArguments();
    Object[] a = getArgArray(nArgs + 2);
    byte[]   argTypes = mi.getArgumentTypes();
    int      stackOffset;
    int      i, j, k;
    int      ival;
    long     lval;
    StackFrame caller = ti.getTopFrame();

    lastCaller = caller.clone();

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

    if (mi.isStatic()) {
      a[1] = new Integer(ci.getClassObjectRef());
    } else {
      a[1] = new Integer(ti.getCalleeThis(mi));
    }

    a[0] = env;

    return a;
  }


  private Method getMethod (MethodInfo mi) {
    return getMethod(null, mi);
  }

  private Method getMethod (String prefix, MethodInfo mi) {
    String name = mi.getUniqueName();

    if (prefix != null) {
      name = prefix + name;
    }

    return methods.get(name);
  }

  /**
   * look at all public static methods in the peer and set their
   * corresponding model class MethodInfo attributes
   * <2do> pcm - this is too long, break it down
   */
  private void loadMethods (boolean cacheMethods) {
    Method[] m = peerClass.getDeclaredMethods();
    methods = new HashMap<String, Method>(m.length);

    Map<String,MethodInfo> methodInfos = ci.getDeclaredMethods();
    MethodInfo[] mis = null;

    for (int i = 0; i < m.length; i++) {
      Method  mth = m[i];

      if (isMJICandidate(mth)) {
        // Note that we can't mangle the name automatically, since we loose the
        // object type info (all mapped to int). This has to be handled
        // the same way like with overloaded JNI methods - you have to
        // mangle them manually
        String mn = mth.getName();

        // JNI doesn't allow <clinit> or <init> to be native, but MJI does
        // (you should know what you are doing before you use that, really)
        if (mn.startsWith("$clinit")) {
          mn = "<clinit>";
        } else if (mn.startsWith("$init")) {
          mn = "<init>" + mn.substring(5);
        }

        String mname = Types.getJNIMethodName(mn);
        String sig = Types.getJNISignature(mn);

        if (sig != null) {
          mname += sig;
        }

        // now try to find a corresponding MethodInfo object and mark it
        // as 'peer-ed'
        // <2do> in case of <clinit>, it wouldn't be strictly required to
        // have a MethodInfo upfront (we could create it). Might be handy
        // for classes where we intercept just a few methods, but need
        // to init before
        MethodInfo mi = methodInfos.get(mname);

        if ((mi == null) && (sig == null)) {
          // nothing found, we have to do it the hard way - check if there is
          // a single method with this name (still unsafe, but JNI behavior)
          // Note there's no point in doing that if we do have a signature
          if (mis == null) { // cache it for subsequent lookup
            mis = new MethodInfo[methodInfos.size()];
            methodInfos.values().toArray(mis);
          }

          mi = searchMethod(mname, mis);
        }

        if (mi != null) {
          if (logger.isLoggable(Level.INFO)) {
            logger.info("load MJI method: " + mname);
          }

          mi.setMJI(true);


          if (cacheMethods) {
            methods.put(mi.getUniqueName(), mth); // no use to store unless it can be called!
          } else {
            // otherwise we are just interested in setting the MethodInfo attributes
          }
        } else {
          // issue a warning if we have a NativePeer native method w/o a corresponding
          // method in the model class (this might happen due to compiler optimizations
          // silently skipping empty methods)
          logger.warning("orphant NativePeer method: " + ci.getName() + '.' + mname);
        }
      }
    }
  }

  private static MethodInfo searchMethod (String mname, MethodInfo[] methods) {
    int idx = -1;

    for (int j = 0; j < methods.length; j++) {
      if (methods[j].getName().equals(mname)) {
        // if this is actually a overloaded method, and the first one
        // isn't the right choice, we would get an IllegalArgumentException,
        // hence we have to go on and make sure it's not overloaded

        if (idx == -1) {
          idx = j;
        } else {
          throw new JPFException("overloaded native method without signature: " + mname);
        }
      }
    }

    if (idx >= 0) {
      return methods[idx];
    } else {
      return null;
    }
  }

  private void pushReturnValue (ThreadInfo ti, MethodInfo mi,
                                Object ret, Object retAttr) {
    int  ival;
    long lval;
    int  retSize = 1;

    // in case of a return type mismatch, we get a ClassCastException, which
    // is handled in executeMethod() and reported as a InvocationTargetException
    // (not completely accurate, but we rather go with safety)
    if (ret != null) {
      switch (mi.getReturnType()) {
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

