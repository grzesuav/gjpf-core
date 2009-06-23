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
package gov.nasa.jpf.tools;

import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.Types;

import java.io.PrintWriter;

import java.lang.reflect.*;


/**
 * tool to create NativePeerDispatchers, i.e. classes that do the NativePeer
 * method lookup with explicit hahcode values instead of reflection.
 * Given a NativePeer, this class computes the hashcodes and creates a
 * dispatcher class that mostly consists of one big dispatcher method directly
 * calling the native methods
 * Since reflection call efficiency got significantly improved since Java 1.4,
 * dispatcher classes are not really required anymore.
 */
public class GenPeerDispatcher {
  static final String SYS_PKG = "gov.nasa.jpf.jvm";
  static final String INDENT = "  ";
  static final String EXECUTE = "Instruction executeMethod (ThreadInfo ti, MethodInfo mi)";
  static final String IS_COND_DETERMINISTIC = "boolean isMethodCondDeterministic (ThreadInfo ti, MethodInfo mi)";
  static final String IS_COND_EXECUTABLE = "boolean isMethodCondExecutable (ThreadInfo ti, MethodInfo mi)";
  static final String EXEC_COND = "$isExecutable_";
  static final String DETERM_COND = "$isDeterministic_";
  static final int    MJI_MODS = Modifier.PUBLIC | Modifier.STATIC;
  static String       clsName;
  static PrintWriter  pw;
  static Method[]     tmethods; // target class method cache (for reverse lookup)

  public static void main (String[] args) {
    if ((args.length == 0) || !readOptions(args)) {
      showUsage();

      return;
    }

    pw = new PrintWriter(System.out, true);

    Class<?> cls = getClass(clsName);

    if (cls != null) {
      printNativePeerDispatcher(cls);
    }
  }

  static Class<?> getClass (String cname) {
    Class<?> clazz = null;

    try {
      clazz = Class.forName(cname);
    } catch (ClassNotFoundException cnfx) {
      System.err.println("target class not found: " + cname);
    } catch (Throwable x) {
      x.printStackTrace();
    }

    return clazz;
  }

  static boolean isMJIDetermCondCandidate (Method m) {
    if ((m.getModifiers() & MJI_MODS) == MJI_MODS) {
      String name = m.getName();

      return name.startsWith(DETERM_COND);
    }

    return false;
  }

  static boolean isMJIExecCondCandidate (Method m) {
    if ((m.getModifiers() & MJI_MODS) == MJI_MODS) {
      String name = m.getName();

      return name.startsWith(EXEC_COND);
      // theoretically we should also check the param types, but who uses the prefix for non MJI methods
    }

    return false;
  }

  static boolean isMJIExecuteCandidate (Method m) {
    if ((m.getModifiers() & MJI_MODS) == MJI_MODS) {
      String name = m.getName();

      Class[] at = m.getParameterTypes();
      if ((at.length >=2) && (at[0] == MJIEnv.class) && (at[1] == int.class)) {
        return !(name.startsWith(EXEC_COND) || name.startsWith(DETERM_COND));
      }
    }

    return false;
  }

  static String getSignature (Method m) {
    String mname = m.getName();

    if (mname.equals("$clinit") || mname.equals("$init")) {
      // <2do> - if we want to be real good, we really have to treat ctors
      return "()";
    }

    // bad, we have to do a reverse lookup from the target class
    // (can't use our argTypes because we lost the object classes)
    Method tm = getTargetMethod(m);

    if (tm != null) {
      // let's tinker us a signature
      Class[]       argTypes = tm.getParameterTypes();
      StringBuilder sb = new StringBuilder();

      sb.append('(');

      for (int i = 0; i < argTypes.length; i++) {
        Class<?> t = argTypes[i];

        while (t.isArray()) {
          sb.append('[');
          t = t.getComponentType();
        }

        if (t == Boolean.TYPE) {
          sb.append('Z');
        } else if (t == Byte.TYPE) {
          sb.append('B');
        } else if (t == Character.TYPE) {
          sb.append('C');
        } else if (t == Short.TYPE) {
          sb.append('S');
        } else if (t == Integer.TYPE) {
          sb.append('I');
        } else if (t == Long.TYPE) {
          sb.append('J');
        } else if (t == Float.TYPE) {
          sb.append('F');
        } else if (t == Double.TYPE) {
          sb.append('D');
        } else {
          sb.append('L');
          sb.append(t.getName().replace('.', '/'));
          sb.append(';');
        }
      }

      sb.append(')');

      return sb.toString();
    }

    return "()"; // a bloody guess, probably will fail
  }

  static Method getTargetMethod (Method m) {
    // <2do> - no non-default ctors support
    String mname = m.getName();

    if (tmethods == null) {
      String tcn = m.getDeclaringClass().getName();
      tcn = tcn.substring(tcn.indexOf("JPF") + 4);
      tcn = tcn.replace('_', '.');

      try {
        // <2do> - doesn't work for bootclasspath candidates, of course!
        Class<?> tcls = Class.forName(tcn);
        tmethods = tcls.getDeclaredMethods();
      } catch (ClassNotFoundException cnfx) {
        System.err.println("!! cannot find target class " + tcn + 
                           " to determine signature of: " + mname);

        return null;
      }
    }

    for (int i = 0; i < tmethods.length; i++) {
      if (tmethods[i].getName().equals(mname)) {
        return tmethods[i];
      }
    }

    System.err.println(
          "!! cannot find target method to determine signature of: " + mname);

    return null;
  }

  static int calcStackSize (Class[] argTypes) {
    int n = 0;

    // the first two args are the MJIEnv object and the 'this' / class object ref
    // we don't count them here
    for (int i = 2; i < argTypes.length; i++) {
      if ((argTypes[i] == Long.TYPE) || (argTypes[i] == Double.TYPE)) {
        n += 2;
      } else {
        n++;
      }
    }

    return n;
  }

  static void iprint (int level, String s) {
    printIndent(level);
    pw.print(s);
  }

  static void iprintln (int level, String s) {
    printIndent(level);
    pw.println(s);
  }

  static void printCall (Class<?> cls, Method m) {
    Class[] argTypes = m.getParameterTypes();
    //Class   retType = m.getReturnType();
    int     stackOffset = calcStackSize(argTypes); // not counting this/class ref

    pw.print(cls.getName());
    pw.print('.');
    pw.print(m.getName());
    pw.print("( env, rThis");

    if (argTypes.length > 2) {
      pw.println(',');
    }

    for (int i = 2; i < argTypes.length;) {
      stackOffset--;

      if (argTypes[i] == Boolean.TYPE) {
        iprint(7, "Types.intToBoolean( ti.peek(" + stackOffset + "))");
      } else if (argTypes[i] == Byte.TYPE) {
        iprint(7, "(byte) ti.peek(" + stackOffset + ")");
      } else if (argTypes[i] == Character.TYPE) {
        iprint(7, "(char) ti.peek(" + stackOffset + ")");
      } else if (argTypes[i] == Short.TYPE) {
        iprint(7, "(short) ti.peek(" + stackOffset + ")");
      } else if (argTypes[i] == Integer.TYPE) {
        iprint(7, "ti.peek(" + stackOffset + ")");
      } else if (argTypes[i] == Long.TYPE) {
        stackOffset--;
        iprint(7, "ti.longPeek(" + stackOffset + ")");
      } else if (argTypes[i] == Float.TYPE) {
        iprint(7, "Types.intToFloat( ti.peek(" + stackOffset + "))");
      } else if (argTypes[i] == Double.TYPE) {
        stackOffset--;
        iprint(7, "Types.longToDouble( ti.longPeek(" + stackOffset + "))");
      } else {
        iprint(7, "ti.peek(" + stackOffset + ")");
      }

      if ((++i) < argTypes.length) {
        pw.println(',');
      }
    }

    pw.print(")");
  }

  static void printCaseConst (Method m) {
    String mname = m.getName();
    String jniname = Types.getJNIMethodName(mname);

    String id = jniname;

    if (id.equals("$clinit")) {
      id = "<clinit>";
    } else if (id.equals("$init")) {
      id = "<init>";
    }

    String argSig = Types.getJNISignature(mname);

    if (argSig != null) {
      id += argSig;
    } else {
      // Ok, no type signature, we have to recreate this from the argTypes
      id += getSignature(m);
    }

    iprint(3, "case ");
    pw.print(id.hashCode());
    pw.print(": // ");
    pw.println(id);
  }

  static int printExecCallProlog (Method m) {
    Class<?> retType = m.getReturnType();

    if (retType == Void.TYPE) {
      iprintln(4, "retSize = 0;");
      iprint(4, "");
    } else if (retType == Boolean.TYPE) {
      iprintln(4, "retSize = 1;");
      iprint(4, "iret = Types.booleanToInt( ");

      return 1;
    } else if (retType == Byte.TYPE) {
      iprintln(4, "retSize = 1;");
      iprint(4, "iret = (int) ");
    } else if (retType == Character.TYPE) {
      iprintln(4, "retSize = 1;");
      iprint(4, "iret = (int) ");
    } else if (retType == Short.TYPE) {
      iprintln(4, "retSize = 1;");
      iprint(4, "iret = (int) ");
    } else if (retType == Integer.TYPE) {
      iprintln(4, "retSize = 1;");
      iprint(4, "iret = ");
    } else if (retType == Long.TYPE) {
      iprintln(4, "retSize = 2;");
      iprint(4, "lret = ");
    } else if (retType == Float.TYPE) {
      iprintln(4, "retSize = 1;");
      iprint(4, "iret = Types.floatToInt( ");

      return 1;
    } else if (retType == Double.TYPE) {
      iprintln(4, "retSize = 2;");
      iprint(4, "lret = Types.doubleToLong( ");

      return 1;
    } else {
      iprintln(4, "retSize = 1;");
      iprint(4, "iret = ");
    }

    return 0;
  }

  static void printExecute (Class<?> cls) {
    Method[] mths = cls.getDeclaredMethods();

    iprint(1, EXECUTE);
    pw.println(" {");

    iprintln(2, "int iret = 0;");
    iprintln(2, "long lret = 0;");
    iprintln(2, "int retSize = 0;");
    iprintln(2, "String exception = null;");
    iprintln(2, "int mid = mi.getUniqueName().hashCode();");
    pw.println();

    iprintln(2, "MJIEnv env = ti.getMJIEnv();");
    iprintln(2, 
             "int rThis = (mi.isStatic()) ? ci.getClassObjectRef() : ti.getCalleeThis(mi);");
    pw.println();

    iprintln(2, "env.setCallEnvironment( mi);");
    pw.println();

    iprintln(2, "try {");

    iprintln(3, "switch (mid) {");

    for (int i = 0; i < mths.length; i++) {
      Method m = mths[i];

      if (isMJIExecuteCandidate(m)) {
        printCaseConst(m);

        int openFuncs = printExecCallProlog(m);
        printCall(cls, m);

        for (int j = 0; j < openFuncs; j++) {
          pw.print(')');
        }

        pw.println(';');
        iprintln(4, "break;");
      }
    }

    iprintln(3, "default:");
    iprintln(4, 
             "return ti.createAndThrowException(  \"java.lang.UnsatisfiedLinkError\",");
    iprintln(6, "\"cannot find: \" + ci.getName() + '.' + mi.getName());");

    iprintln(3, "}");

    iprintln(2, "} catch (Throwable x) {");
    iprintln(3, "x.printStackTrace();");
    iprintln(3, 
             "return ti.createAndThrowException(  \"java.lang.reflect.InvocationTargetException\",");
    iprintln(5, "ci.getName() + '.' + mi.getName());");
    iprintln(2, "}");

    pw.println();
    iprintln(2, "if ((exception = env.getException()) != null) {");
    iprintln(3, "return ti.createAndThrowException(exception);");
    iprintln(2, "}");
    pw.println();
    iprintln(2, "if (env.getRepeat()) {");
    iprintln(3, "return ti.getPC();");
    iprintln(2, "}");
    pw.println();
    iprintln(2, "ti.removeArguments(mi);");
    pw.println();
    iprintln(2, "switch (retSize) {");
    iprintln(2, "case 0: break; // nothing to return");
    iprintln(2, "case 1: ti.push(iret, mi.isReferenceReturnType()); break;");
    iprintln(2, "case 2: ti.longPush(lret); break;");
    iprintln(2, "}");
    pw.println();
    iprintln(2, "return ti.getPC().getNext();");

    iprintln(1, "}");
  }

  static void printFooter (Class<?> cls) {
    pw.println("}");
  }

  static void printHeader (Class<?> cls) {
    pw.print("package ");
    pw.print(SYS_PKG);
    pw.println(';');
    pw.println();

    String cname = cls.getName();
    int    idx = cname.lastIndexOf('.');

    if (idx > 0) {
      cname = cname.substring(idx + 1);
    }

    pw.println("import gov.nasa.jpf.JPFVMException;");
    pw.println("import gov.nasa.jpf.jvm.bytecode.Instruction;");
    pw.println();

    pw.print("class ");
    pw.print(cname);
    pw.println("$ extends NativePeer {");
  }

  static void printIndent (int level) {
    for (int i = 0; i < level; i++) {
      pw.print(INDENT);
    }
  }

  static void printIsCond (Class<?> cls, String condPrefix) {
    Method[] mths = cls.getDeclaredMethods();

    iprint(1, condPrefix);
    pw.println(" {");

    iprintln(2, "boolean ret = false;");
    iprintln(2, "int mid = mi.getUniqueName().hashCode();");
    pw.println();

    iprintln(2, "MJIEnv env = ti.getMJIEnv();");
    iprintln(2, 
             "int rThis = (mi.isStatic()) ? ci.getClassObjectRef() : ti.getCalleeThis(mi);");
    pw.println();

    iprintln(2, "env.setCallEnvironment( mi);");
    pw.println();

    iprintln(2, "try {");

    iprintln(3, "switch (mid) {");

    for (int i = 0; i < mths.length; i++) {
      Method m = mths[i];

      if (((condPrefix == IS_COND_DETERMINISTIC) && 
                isMJIDetermCondCandidate(m)) || 
              ((condPrefix == IS_COND_EXECUTABLE) && 
                isMJIExecCondCandidate(m))) {
        printCaseConst(m);

        iprint(4, "ret = ");
        printCall(cls, m);
        pw.println(';');
      }
    }

    iprintln(3, "default:");

    if (condPrefix == IS_COND_EXECUTABLE) {
      iprintln(4, 
               "throw new JPFVMException(\"no isExecutable() condition: \" + mi.getName());");
    } else {
      iprintln(4, 
               "throw new JPFVMException(\"no isDeterministic() condition: \" + mi.getName());");
    }

    iprintln(3, "}");

    iprintln(2, "} catch (Throwable x) {");
    iprintln(3, "x.printStackTrace();");
    iprintln(2, "}");
    pw.println();

    iprintln(2, "return ret;");
    iprintln(1, "}");
  }

  static void printIsCondDeterministic (Class<?> cls) {
    printIsCond(cls, IS_COND_DETERMINISTIC);
  }

  static void printIsCondExecutable (Class<?> cls) {
    printIsCond(cls, IS_COND_EXECUTABLE);
  }

  static void printNativePeerDispatcher (Class<?> cls) {
    printHeader(cls);
    pw.println();

    printExecute(cls);
    pw.println();
    printIsCondDeterministic(cls);
    pw.println();
    printIsCondExecutable(cls);
    pw.println();

    printFooter(cls);
  }

  static boolean readOptions (String[] args) {
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      if (arg.charAt(0) == '-') {
        System.err.println("unknown option: " + arg);
        showUsage();

        return false;
      } else {
        if (clsName == null) {
          clsName = arg;
        }
      }
    }

    return (clsName != null);
  }

  static void showUsage () {
    System.out.println("usage:   'GenPeerDispatcher <className>'");
  }
}
