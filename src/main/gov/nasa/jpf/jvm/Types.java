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

import gov.nasa.jpf.JPFException;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.bcel.Constants;


/**
 * various type mangling/demangling routines
 */
public class Types {  
  public static final byte T_ARRAY     = Constants.T_ARRAY;
  public static final byte T_BOOLEAN   = Constants.T_BOOLEAN;
  public static final byte T_BYTE      = Constants.T_BYTE;
  public static final byte T_CHAR      = Constants.T_CHAR;
  public static final byte T_DOUBLE    = Constants.T_DOUBLE;
  public static final byte T_FLOAT     = Constants.T_FLOAT;
  public static final byte T_INT       = Constants.T_INT;
  public static final byte T_LONG      = Constants.T_LONG;
  public static final byte T_OBJECT    = Constants.T_OBJECT;
  public static final byte T_REFERENCE = Constants.T_REFERENCE;
  public static final byte T_SHORT     = Constants.T_SHORT;
  public static final byte T_VOID      = Constants.T_VOID;

  
  public static byte[] getArgumentTypes (String signature) {
    int i;
    int j;
    int nArgs;

    for (i = 1, nArgs = 0; signature.charAt(i) != ')'; nArgs++) {
      i += getTypeLength(signature, i);
    }

    byte[] args = new byte[nArgs];

    for (i = 1, j = 0; j < nArgs; j++) {
      int    end = i + getTypeLength(signature, i);
      String arg = signature.substring(i, end);
      i = end;

      args[j] = getBaseType(arg);
    }

    return args;
  }

  public static String[] getArgumentTypeNames (String signature) {
    int len = signature.length();

    if ((len > 1) && (signature.charAt(1) == ')')) {
      return new String[0]; // 'no args' shortcut
    }
    
    ArrayList<String> a = new ArrayList<String>();

    for (int i = 1; signature.charAt(i) != ')';) {
      int end = i + getTypeLength(signature,i);
      String arg = signature.substring(i, end);
      i = end;

      a.add(getTypeName(arg));
    }

    String[] typeNames = new String[a.size()];
    a.toArray(typeNames);
    
    return typeNames;
  }
  
  
  /**
   * get size in stack slots (ints), excluding this
   */
  public static int getArgumentsSize (String sig) {
    int  n = 0;
    for (int i = 1; sig.charAt(i) != ')'; i++) {
      switch (sig.charAt(i)) {
      case 'L':
        do i++; while (sig.charAt(i) != ';');
        n++;
        break;
      case '[':
        do i++; while (sig.charAt(i) == '[');
        if (sig.charAt(i) == 'L') {
          do i++; while (sig.charAt(i) != ';');
        }
        n++;
        break;
      case 'J':
      case 'D':
        // the two-slot types
        n += 2;
        break;
      default:
        // just one slot entry
        n++;
      }
    }
    return n;
  }

  public static String getArrayElementType (String type) {
    if (type.charAt(0) != '[') {
      throw new JPFException("not an array type");
    }

    return type.substring(1);
  }

  public static byte getBaseType (String type) {
    switch (type.charAt(0)) {
    case 'B':
      return T_BYTE;

    case 'C':
      return T_CHAR;

    case 'D':
      return T_DOUBLE;

    case 'F':
      return T_FLOAT;

    case 'I':
      return T_INT;

    case 'J':
      return T_LONG;

    case 'L':
      return T_OBJECT; // T_REFERENCE is deprecated

    case 'S':
      return T_SHORT;

    case 'V':
      return T_VOID;

    case 'Z':
      return T_BOOLEAN;

    case '[':
      return T_ARRAY;
    }

    throw new JPFException("invalid type string: " + type);
  }

  /**
   * get the argument type part of the signature out of a
   * JNI mangled method name.
   * Note this is not the complete signature, since we don't have a
   * return type (which is superfluous since it's not overloading,
   * but unfortunately part of the signature in the class file)
   */
  public static String getJNISignature (String mangledName) {
    int    i = mangledName.indexOf("__");
    String sig = null;

    if (i > 0) {
      int k = 0;      
      int r = mangledName.indexOf("__", i+2); // maybe there is a return type part
      boolean gotReturnType = false;
      int len = mangledName.length();
      char[] buf = new char[len + 2];

      buf[k++] = '(';

      for (i += 2; i < len; i++) {

        if (i == r) { // here comes the return type part (that's not JNI, only MJI
          if ((i + 2) < len) {
            i++;
            buf[k++] = ')';
            gotReturnType = true;
            continue;
          } else {
            break;
          }
        }
        
        char c = mangledName.charAt(i);
        if (c == '_') {
          i++;

          if (i < len) {
            c = mangledName.charAt(i);

            switch (c) {
            case '1':
              buf[k++] = '_';

              break;

            case '2':
              buf[k++] = ';';

              break;

            case '3':
              buf[k++] = '[';

              break;

            default:
              buf[k++] = '/';
              buf[k++] = c;
            }
          } else {
            buf[k++] = '/';
          }
        } else {
          buf[k++] = c;
        }
      }

      if (!gotReturnType) {
        // if there was no return type spec, assume 'void'
        buf[k++] = ')';
        buf[k++] = 'V';
      }
        
      sig = new String(buf, 0, k);
    }

    // Hmm, maybe we should return "()V" instead of null, but that seems a bit too assuming
    return sig;
  }

  public static String getJNIMangledMethodName (Method m) {
    String      name = m.getName();
    Class<?>[]    pt = m.getParameterTypes();
    StringBuilder  s = new StringBuilder(name.length() + (pt.length * 16));

    s.append(name);
    s.append("__");

    // <2do> not very efficient, but we don't care for now
    for (int i = 0; i < pt.length; i++) {
      s.append(getJNITypeCode(pt[i].getName()));
    }

    // the return type part, which is not in JNI, but is required for
    // handling covariant return types
    Class<?> rt = m.getReturnType();
    s.append("__");
    s.append(getJNITypeCode(rt.getName()));
    
    return s.toString();
  }

  public static String getJNIMangledMethodName (String cls, String name,
                                                String signature) {
    StringBuilder s = new StringBuilder(signature.length() + 10);
    int           i;
    char          c;
    int           slen = signature.length();
    
    if (cls != null) {
      s.append(cls.replace('.', '_'));
    }

    s.append(name);
    s.append("__");

    // as defined in the JNI specs
    for (i = 1; i<slen; i++) {
      c = signature.charAt(i);
      switch (c) {
      case '/':
        s.append('_');
        break;

      case '_':
        s.append("_1");
        break;

      case ';':
        s.append("_2");
        break;

      case '[':
        s.append("_3");
        break;

      case ')':
        // the return type part - note this is not JNI, but saves us a lot of trouble with
        // the covariant return types of Java 1.5        
        s.append("__");
        break;
        
      default:
        s.append(c);
      }
    }

    return s.toString();
  }

  /**
   * return the name part of a JNI mangled method name (which is of
   * course not completely safe - you should only use it if you know
   * this is a JNI name)
   */
  public static String getJNIMethodName (String mangledName) {
    // note that's the first '__' group, which marks the beginning of the arg types
    int i = mangledName.indexOf("__");

    if (i > 0) {
      return mangledName.substring(0, i);
    } else {
      return mangledName;
    }
  }

  public static String getJNITypeCode (String type) {
    StringBuilder sb = new StringBuilder(32);
    int           l = type.length() - 1;

    for (; type.charAt(l) == ']'; l -= 2) {
      sb.append("_3");
    }

    type = type.substring(0, l + 1);

    if (type.equals("int")) {
      sb.append('I');
    } else if (type.equals("long")) {
      sb.append('J');
    } else if (type.equals("boolean")) {
      sb.append('Z');
    } else if (type.equals("char")) {
      sb.append('C');
    } else if (type.equals("byte")) {
      sb.append('B');
    } else if (type.equals("short")) {
      sb.append('S');
    } else if (type.equals("double")) {
      sb.append('D');
    } else if (type.equals("float")) {
      sb.append('F');
    } else if (type.equals("void")) {  // for return types
      sb.append('V');
    } else {
      sb.append('L');

      for (int i = 0; i < type.length(); i++) {
        char c = type.charAt(i);

        switch (c) {
        case '.':
          sb.append('_');

          break;

        case '_':
          sb.append("_1");

          break;

        default:
          sb.append(c);
        }
      }

      sb.append("_2");
    }

    return sb.toString();
  }

  // this includes the return type part
  public static int getNumberOfStackSlots (String signature, boolean isStatic) {
    int nArgSlots = 0;
    int n = isStatic ? 0 : 1;
    int sigLen = signature.length();

    for (int i = 1; i < sigLen; i++) {
      switch (signature.charAt(i)) {
      case ')' : // end of arg part, but there still might be a return type
        nArgSlots = n;
        n = 0;
        break;
      case 'L':   // reference = 1 slot
        i = signature.indexOf(';', i);    
        n++;
        break;
      case '[':
        do i++; while (signature.charAt(i) == '[');
        if (signature.charAt(i) == 'L') {
          i = signature.indexOf(';', i);
        }
        n++;
        break;
      case 'J':
      case 'D':
        n+=2;
        break;
      default:
        n++;
      }
    }
    
    return Math.max(n, nArgSlots);
  }
  
  public static int getNumberOfArguments (String signature) {
    int  i,n;
    int sigLen = signature.length();

    for (i = 1, n = 0; i<sigLen; n++) {
      switch (signature.charAt(i)) {
      case ')' :
        return n;
      case 'L':
        do i++; while (signature.charAt(i) != ';');
        break;

      case '[':
        do i++; while (signature.charAt(i) == '[');
        if (signature.charAt(i) == 'L') {
          do i++; while (signature.charAt(i) != ';');
        }
        break;

      default:
        // just a single type char
      }

      i++;
    }

    assert (false) : "malformed signature: " + signature;
    return n; // that would be a malformed signature
  }

  public static boolean isReference (String type) {
    int t = getBaseType(type);

    return (t == T_ARRAY) || (t == T_REFERENCE);
  }

  public static byte getReturnType (String signature) {
    int i = signature.indexOf(')');

    return getBaseType(signature.substring(i + 1));
  }

  public static String getReturnTypeName (String signature){
    int i = signature.indexOf(')');
    return getTypeName(signature.substring(i+1));
  }
  
  public static String getTypeCode (String type, boolean dotNotation) {
    String  t = null;
    int arrayDim = 0;
    
    type = dotNotation ? type.replace('/', '.') : type.replace('.', '/');
    
    if ((type.charAt(0) == '[') || (type.endsWith(";"))) {  // [[[L...;
      t = type;
    } else {
      
      while (type.endsWith("[]")) { // type[][][]
        type = type.substring(0, type.length() - 2);
        arrayDim++;
      }
      
      if (type.equals("byte")) {
        t = "B";
      } else if (type.equals("char")) {
        t = "C";
      } else if (type.equals("short")) {
        t = "S";
      } else if (type.equals("int")) {
        t = "I";
      } else if (type.equals("float")) {
        t = "F";
      } else if (type.equals("long")) {
        t = "J";
      } else if (type.equals("double")) {
        t = "D";
      } else if (type.equals("boolean")) {
        t = "Z";
      } else if (type.equals("void")) {
        t = "V";
      } else if (type.endsWith(";")) {
        t = type;
      }
      
      while (arrayDim-- > 0) {
        t = "[" + t;
      }
      
      if (t == null) {
        t = "L" + type + ';';
      }
    }

    return t;
  }

  /**
   * get the canonical representation of a type name, which happens to be
   *  (1) the name of the builtin type (e.g. "int")
   *  (2) the normal dot name for ordinary classes (e.g. "java.lang.String")
   *  (3) the coded dot name for arrays (e.g. "[B", "[[C", or "[Ljava.lang.String;")
   *  
   * not sure if we need to support internal class names (which use '/'
   * instead of '.' as separators
   *  
   * no idea what's the logic behind this, but let's implement it
   */
  public static String getCanonicalTypeName (String typeName) {
    typeName = typeName.replace('/','.');
    int n = typeName.length()-1;
    
    if (typeName.charAt(0) == '['){ // the "[<type>" notation
      if (typeName.charAt(1) == 'L'){
        if (typeName.charAt(n) != ';'){
          typeName = typeName + ';';
        }
      }
      
      return typeName;
    }
    
    int i=typeName.indexOf('[');
    if (i>0){ // the sort of "<type>[]"
      StringBuilder sb = new StringBuilder();
      sb.append('[');
      for (int j=i; (j=typeName.indexOf('[',j+1)) >0;){
        sb.append('[');
      }
      
      typeName = typeName.substring(0,i);
      if (isBasicType(typeName)){
        sb.append( getTypeCode(typeName, true));
      } else {
        sb.append('L');
        sb.append(typeName);
        sb.append(';');
      }
      
      return sb.toString();
    }
    
    if (typeName.charAt(n) == ';') {
      return typeName.substring(1,n);
    }
    
    return typeName;
  }

  
  public static boolean isTypeCode (String t) {
    char c = t.charAt(0);

    if (c == '[') {
      return true;
    }

    if ((t.length() == 1) &&
            ((c == 'B') || (c == 'I') || (c == 'S') || (c == 'C') ||
              (c == 'F') || (c == 'J') || (c == 'D') || (c == 'Z'))) {
      return true;
    }

    if (t.endsWith(";")) {
      return true;
    }

    return false;
  }

  public static boolean isBasicType (String typeName){
    return ("boolean".equals(typeName) ||
        "byte".equals(typeName) ||
        "char".equals(typeName) ||
        "int".equals(typeName) ||
        "long".equals(typeName) ||
        "double".equals(typeName) ||
        "short".equals(typeName) ||
        "float".equals(typeName));
  }
  
  public static String getTypeName (String type) {
    int  len = type.length();
    char c = type.charAt(0);

    if (len == 1) {
      switch (c) {
      case 'B':
        return "byte";

      case 'C':
        return "char";

      case 'D':
        return "double";

      case 'F':
        return "float";

      case 'I':
        return "int";

      case 'J':
        return "long";

      case 'S':
        return "short";

      case 'V':
        return "void";

      case 'Z':
        return "boolean";
      }
    }

    if (c == '[') {
      return getTypeName(type.substring(1)) + "[]";
    }

    if (type.charAt(len - 1) == ';') {
      return type.substring(1, type.indexOf(';')).replace('/', '.');
    }

    throw new JPFException("invalid type string: " + type);
  }

  /**
   * what would be the info size in bytes, not words
   * (we ignore 64bit machines for now)
   */
  public static int getTypeSizeInBytes (String type) {
    switch (type.charAt(0)) {
      case 'V':
        return 0;
        
      case 'Z': // that's a stretch, but we assume boolean uses the smallest addressable size
      case 'B':
        return 1;
        
      case 'S':
      case 'C':
        return 2;
        
      case 'L':
      case '[':
      case 'F':
      case 'I':
        return 4;
        
      case 'D':
      case 'J':
        return 8;
    }

    throw new JPFException("invalid type string: " + type);
  }
  
  public static int getTypeSize (String typeCode) {
    switch (typeCode.charAt(0)) {
    case 'V':
      return 0;

    case 'B':
    case 'C':
    case 'F':
    case 'I':
    case 'L':
    case 'S':
    case 'Z':
    case '[':
      return 1;

    case 'D':
    case 'J':
      return 2;
    }

    throw new JPFException("invalid type string: " + typeCode);
  }

  public static int getTypeSize (byte typeCode){
    if (typeCode == T_LONG || typeCode == T_DOUBLE){
      return 2;
    } else {
      return 1;
    }
  }
  
  public static String asTypeName (String type) {
    if (type.startsWith("[") || type.endsWith(";")) {
      return getTypeName(type);
    }

    return type;
  }

  public static int booleanToInt (boolean b) {
    return b ? 1 : 0;
  }

  public static long doubleToLong (double d) {
    return Double.doubleToLongBits(d);
  }

  public static int floatToInt (float f) {
    return Float.floatToIntBits(f);
  }

  public static int hiDouble (double d) {
    return hiLong(Double.doubleToLongBits(d));
  }

  public static int hiLong (long l) {
    return (int) (l >> 32);
  }

  public static boolean instanceOf (String type, String ofType) {
    int bType = getBaseType(type);

    if ((bType == T_ARRAY) && ofType.equals("Ljava.lang.Object;")) {
      return true;
    }

    int bOfType = getBaseType(ofType);

    if (bType != bOfType) {
      return false;
    }

    switch (bType) {
    case T_ARRAY:
      return instanceOf(type.substring(1), ofType.substring(1));

    case T_REFERENCE:
      return ClassInfo.getResolvedClassInfo(getTypeName(type))
                      .isInstanceOf(getTypeName(ofType));

    default:
      return true;
    }
  }

  public static boolean intToBoolean (int i) {
    return i != 0;
  }

  public static float intToFloat (int i) {
    return Float.intBitsToFloat(i);
  }

  public static double intsToDouble (int l, int h) {
    return longToDouble(intsToLong(l, h));
  }

  public static long intsToLong (int l, int h) {
    return ((long) h << 32) | (/*(long)*/ l & 0xFFFFFFFFL);
  }

  public static int loDouble (double d) {
    return loLong(Double.doubleToLongBits(d));
  }

  public static int loLong (long l) {
    return (int) (l & 0xFFFFFFFFL);
  }

  public static double longToDouble (long l) {
    return Double.longBitsToDouble(l);
  }

  private static int getTypeLength (String signature, int idx) {
    switch (signature.charAt(idx)) {
    case 'B':
    case 'C':
    case 'D':
    case 'F':
    case 'I':
    case 'J':
    case 'S':
    case 'V':
    case 'Z':
      return 1;

    case '[':
      return 1 + getTypeLength(signature, idx + 1);

    case 'L':

      int semicolon = signature.indexOf(';', idx);

      if (semicolon == -1) {
        throw new JPFException("invalid type signature: " +
                                         signature);
      }

      return semicolon - idx + 1;
    }

    throw new JPFException("invalid type signature");
  }

  /**
   * return the JPF internal representation of a method signature that is given
   * in dot-notation (like javap),
   *
   *  e.g.  "int foo(int[],java.lang.String)" -> "foo([ILjava/lang/String;)I"
   *
   */
  public static String getSignatureName (String methodDecl) {

    StringBuffer sb = new StringBuffer(128);
    String retType = null;

    int i = methodDecl.indexOf('(');
    if (i>0){

      //--- name and return type
      String[] a = methodDecl.substring(0, i).split(" ");
      if (a.length > 0){
        sb.append(a[a.length-1]);

        if (a.length > 1){
          retType = getTypeCode(a[a.length-2], false);
        }
      }

      //--- argument types
      int j = methodDecl.lastIndexOf(')');
      if (j > 0){
        sb.append('(');
        for (String type : methodDecl.substring(i+1,j).split(",")){
          if (!type.isEmpty()){
            type = type.trim();
            if (!type.isEmpty()){
              sb.append( getTypeCode(type,false));
            }
          }
        }
        sb.append(')');

        if (retType != null){
          sb.append(retType);
        }

        return sb.toString();
      }
    }

    throw new JPFException("invalid method declaration: " + methodDecl);
  }
  
}
