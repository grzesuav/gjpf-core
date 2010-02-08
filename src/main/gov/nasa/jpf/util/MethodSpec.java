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

package gov.nasa.jpf.util;

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.Types;
import java.util.BitSet;

/**
 * utility class that can match methods/args against specs.
 * argument signatures can be given in dot notation (like javap), arguments
 * can be marked with a preceeding '^'
 * if the class or name part are ommitted, "*" is assumed
 * a preceeding '!' means the match is inverted
 *
 * spec examples
 *   "x.y.Foo.*"
 *   "java.util.HashMap.add(java.lang.Object,^java.lang.Object)"
 *   "*.*(x.y.MyClass)"
 */
public class MethodSpec {

  static final char MARK = '^';
  static final char SUB = '+';
  static final char INVERTED = '!';

  String src;

  // those can be wildcard expressions
  StringMatcher  clsSpec;
  StringMatcher  nameSpec;

  boolean matchInverted;  // matches everything that does NOT conform to the specs
  boolean matchOverride;  // matches overridden methods of the clsSpec

  String  sigSpec;  // this is only the argument part, including parenthesis
  BitSet  markedArgs;


  public static MethodSpec createMethodSpec (String s){
    String ts = null;  // type spec
    String ms = null;  // method name spec
    String ss = null;  // method signature spec
    boolean isInverted = false;

    s = s.trim();
    String src = s;

    if (s.length() > 0){
      if (s.charAt(0) == INVERTED){
        isInverted = true;
        s = s.substring(1).trim();
      }
    }

    int i = s.indexOf(('('));
    if (i >= 0){ // we have a signature part

      int j = s.lastIndexOf(')');
      if (j > i){
        ss = s.substring(i, j+1);
        s = s.substring(0, i);

      } else {
        return null; // error, unbalanced parenthesis
      }

    }

    i = s.lastIndexOf('.'); // beginning of name
    if (i >= 0){
      if (i==0){
        ts = "*";
      } else {
        ts = s.substring(0, i);
      }

      ms = s.substring(i+1);
      if (ms.length() == 0){
        return null;
      }

    } else { // no name, all methods
      if (s.length() == 0){
        ts = "*";
      } else {
        ts = s;
      }
      ms = "*";
    }

    try {
      return new MethodSpec(src, ts, ms, ss, isInverted);
    } catch (IllegalArgumentException iax){
      return null;
    }
  }


  MethodSpec (String rawSpec, String cls, String name, String argSig, boolean inverted){

    src = rawSpec;
    matchInverted = inverted;

    int l = cls.length()-1;
    if (cls.charAt(l) == SUB){
      cls = cls.substring(0, l);
      matchOverride = true;
    }

    clsSpec = new StringMatcher(cls);
    nameSpec = new StringMatcher(name);

    if (argSig != null){
      parseSignature(argSig);
    }
  }

  /**
   * assumed to be comma separated type list using fully qualified dot notation 
   * like javap, but arguments can be marked with preceeding '^', 
   * like "(java.lang.String,^int[])"
   * spec includes parnethesis
   */
  void parseSignature (String spec){
    BitSet m = null;
    StringBuilder sb = new StringBuilder();
    String al = spec.substring(1, spec.length()-1);
    String[] args = al.split(",");

    sb.append('(');
    int i=0;
    for (String a : args){
      a = a.trim();
      if (a.length() > 0){
        if (a.charAt(0) == MARK){
          if (m == null){
            m = new BitSet(args.length);
          }
          m.set(i);
          a = a.substring(1);
        }
        String tc = Types.getTypeCode(a, false);
        sb.append(tc);
        i++;

      } else {
        // error in arg type spec
      }
    }
    sb.append(')');

    sigSpec = sb.toString();
    markedArgs = m;
  }

  public String getSource() {
    return src;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MethodSpec {");
    if (clsSpec != null){
      sb.append("clsSpec:\"");
      sb.append(clsSpec);
      sb.append('"');
    }
    if (nameSpec != null){
      sb.append(",nameSpec:\"");
      sb.append(nameSpec);
      sb.append('"');
    }
    if (sigSpec != null){
      sb.append(",sigSpec:\"");
      sb.append(sigSpec);
      sb.append('"');
    }
    if (markedArgs != null){
      sb.append(",marked:");
      sb.append(markedArgs);
    }
    sb.append('}');
    return sb.toString();
  }

  public BitSet getMarkedArgs () {
    return markedArgs;
  }

  public boolean isMarkedArg(int idx){
    return (markedArgs == null || markedArgs.get(idx));
  }

  public boolean matchOverride () {
    return matchOverride;
  }

  //--- our matchers
  protected boolean isMatchingType(ClassInfo ci){
    if (clsSpec.matches(ci.getName())){  // also takes care of '*'
      return true;
    }

    if (matchOverride){
      // check all superclasses
      for (ClassInfo sci = ci.getSuperClass(); sci != null; sci = sci.getSuperClass()){
        if (clsSpec.matches(sci.getName())){
          return true;
        }
      }
    }

    // check interfaces (regardless of 'override' - interfaces make no sense otherwise
    for (String ifcName : ci.getAllInterfaces()) {
      if (clsSpec.matches(ifcName)) {
        return true;
      }
    }

    return false;
  }


  public boolean matches (MethodInfo mi){
    ClassInfo ci = mi.getClassInfo();

    if (isMatchingType(ci)){
      if (nameSpec.matches(mi.getName())){
        boolean isMatch = false;
        if (sigSpec != null){
          // sigSpec includes '(',')' but not return type
          isMatch = mi.getSignature().startsWith(sigSpec);
        } else { // no sigSpec -> matches all signatures
          isMatch = true;
        }

        return isMatch != matchInverted;
      }
    }

    return false;
  }

  //--- testing & debugging
  public static void main (String[] args){
    MethodSpec ms = createMethodSpec("x.y.Foo.bar(java.lang.String,^float[])");
    System.out.println(ms);

    ms = createMethodSpec("x.y.Foo+.*");
    System.out.println(ms);

    ms = createMethodSpec("*.foo(^int, ^double)");
    System.out.println(ms);

    ms = createMethodSpec("( ^int, ^double)");
    System.out.println(ms);

    ms = createMethodSpec(".foo");
    System.out.println(ms);

    System.out.println("---- those should produce null");

    ms = createMethodSpec(".(bla)");
    System.out.println(ms);

    ms = createMethodSpec("*.foo(^int, ^double");
    System.out.println(ms);
  }
}
