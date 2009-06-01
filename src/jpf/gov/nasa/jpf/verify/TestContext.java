//
// Copyright (C) 2007 United States Government as represented by the
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

package gov.nasa.jpf.verify;

import gov.nasa.jpf.util.Misc;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * instantiates Objects based on classname and arg lists, trying to perform
 * required type conversions
 */
public class TestContext {
  
  static final Object[] NO_ARGS = {};
  static final Class<?>[] NO_ARG_TYPES = {};

  static HashMap<String,Class<? extends Goal>> goalMap = new HashMap<String,Class<? extends Goal>>();
  
  Class<?> tgtClass;
  String pkgPrefix;
  
  PrintWriter log;
  PrintWriter err;
  
  Object tgtObject;
  
  static {
    goalMap.put("AllocLimit", MemoryGoal.class);
    goalMap.put("NoAlloc", MemoryGoal.class);
  }
  
  public TestContext (Class<?> tgtClass, PrintWriter log, PrintWriter err){
    Package pkg = tgtClass.getPackage();
    pkgPrefix = pkg != null ? pkg.getName() : null;
    this.tgtClass = tgtClass; 
    
    this.log = log;
    this.err = err;
  }
  
  // our generic backdoor goal factory method
  public Goal createGoal (String id, ArgList args){
    
    Class<? extends Goal> goalCls = goalMap.get(id);
    
    if (goalCls == null){
      try {
        Class<?> cls = Class.forName(id);
        if (Goal.class.isAssignableFrom(cls)){
          goalCls = (Class<? extends Goal>)cls;
        } else {
          error("not a goal class: " + cls.getName());
          return null;
        }
      } catch (ClassNotFoundException cnfx){
        error("goal class not found: " + id);
      }
    }
    
    Class<?>[] at = { TestContext.class, ArgList.class };
    Object[] a = { this, args };
    
    Goal g = Misc.createObject(goalCls, at, a);
    if (g == null){
      error("cannot instantiate goal class: " + goalCls.getName());
    }
    
    return g;
  }
  
  public void setTargetObject (Object o){
    tgtObject = o;
  }
  
  public Object getTargetObject () {
    return tgtObject;
  }
  
  public Class<?> getTargetClass() {
    return tgtClass;
  }
  
  public void error (String msg){
    err.print("@ ERROR ");
    err.println(msg);
  }
  
  public void log (String msg){
    log.print("@ ");
    log.println(msg);
  }
  
  public static Class<?> resolveClass (String pkgPrefix, String clsName){
    Class<?> cls;
    
    try {
      cls = Class.forName(clsName);
      return cls;
    } catch (ClassNotFoundException cnfx1){
      if (clsName.indexOf('.') < 0){  // class name does not have explicit package
        
        if (pkgPrefix != null) {
          try {
            cls = Class.forName(pkgPrefix + '.' + clsName); // try the package
            return cls;
          } catch (ClassNotFoundException cnfx2){
            // we can still try java.lang
          }
        }
          
        try {
          cls = Class.forName("java.lang." + clsName); // try java.lang
          return cls;
        } catch (ClassNotFoundException cfnx3){ // give up
          // give up
        }
      }
    }
    
    return null;
  }
  
  public Object create (String clsName) throws TestException {
    return create(clsName, NO_ARGS);
  }
  
  public Object create (String clsName, Object[] args) throws TestException {
    try {
      Class<?> cls=resolveClass(pkgPrefix, clsName);
      if (cls == null){
        throw new TestException("class not found: " + clsName);        
      }
      
      for (Constructor<?> ctor : cls.getDeclaredConstructors()){
        Object[] compatibleArgs = getCompatibleArgs(args, ctor.getParameterTypes());
        if (compatibleArgs != null) {
          return ctor.newInstance(compatibleArgs);
        }
      }

      throw new TestException("no compatible constructor found: " + clsName);
      
    } catch (IllegalAccessException iax){
      throw new TestException("default constructor not accessible: " + clsName);
    } catch (InstantiationException ix){
      throw new TestException("class not instantiable: " + clsName);
    } catch (SecurityException sx){
      throw new TestException("security exception");
    } catch (InvocationTargetException e) {
      throw new TestException("exception during instantiation: " + e.getCause(), e.getCause());
    }

  }
  
  public static Object[] getCompatibleArgs (Object[] args, Class<?>[] pTypes){
    if (args == null && pTypes.length == 0){
      return NO_ARGS;
    }
    
    if (args.length != pTypes.length){
      return null;
      
    } else {
      
      Object[] cArgs = new Object[args.length];
      for (int i=0; i<cArgs.length; i++){
        Object a = getCompatibleObject(args[i], pTypes[i]);
        if (a == null){
          return null;
        } else {
          cArgs[i] = a;
        }
      }
      return cArgs;
    }
  }
  
  public static Object getCompatibleObject (Object a, Class<?> pType){
    Class<?> aCls = a.getClass();
    
    if (pType.isAssignableFrom(aCls)){
      return a;
    }
    
    if (pType == getPrimitiveType(aCls)){
      return a;
    }
  
    Object casted = getCastedObject(a, pType);
    if (casted != null){
      return casted;
    }
        
    return null;
  }
  
  
  public Object getFieldValue (FieldReference fr){
    return fr.getValue(tgtClass, tgtObject);
  }
  
  public static Class<?> getPrimitiveType( Class<?> cls){
    if (cls == Integer.class) return int.class;
    if (cls == Boolean.class) return boolean.class;
    if (cls == Long.class) return long.class;
    if (cls == Double.class) return double.class;
    if (cls == Byte.class) return byte.class;
    if (cls == Character.class) return char.class;
    if (cls == Short.class) return short.class;
    if (cls == Float.class) return float.class;
    
    return null;
  }
  
  public static Object getCastedObject (Object o, Class<?> type){
    Class<?> cls = o.getClass();
    
    if (cls == Integer.class){
      return castInteger((Integer)o, type);
    }

    if (cls == Long.class){
      return castLong((Long)o, type);
    }
    
    if (cls == Double.class){
      return castDouble((Double)o,type);
    }
    
    if (cls == Object[].class){
      return castArray((Object[])o, type);
    }
    
    return null;
  }

  static Object castLong (Long o, Class<?> type){
    long val = o;

    if (type == int.class || type == Integer.class){
      if (val <= Integer.MAX_VALUE && val >= Integer.MIN_VALUE){
        return new Integer((int)val);
      }        
    }
    if (type == short.class || type == Short.class){
      if (val <= Short.MAX_VALUE && val >= Short.MIN_VALUE){
        return new Short((short)val);
      }
    }
    if (type == byte.class || type == Byte.class){
      if (val <= Byte.MAX_VALUE && val >= Byte.MIN_VALUE){
        return new Byte((byte)val);
      }        
    }
    
    // <??> perhaps we should report these
    if (type == double.class || type == Double.class){
      return new Double((double)val);
    }
    if (type == float.class || type == Float.class){
      return new Float((float)val);
    }
    
    return null;
  }
  
  static Object castInteger (Integer o, Class<?> type){
    int val = o;
    
    if (type == short.class || type == Short.class){
      if (val <= Short.MAX_VALUE && val >= Short.MIN_VALUE){
        return new Short((short)val);
      }
    }
    if (type == byte.class || type == Byte.class){
      if (val <= Byte.MAX_VALUE && val >= Byte.MIN_VALUE){
        return new Byte((byte)val);
      }        
    }
    if (type == long.class || type == Long.class){
      return new Long(val);
    }
    
    // <??> perhaps we should report these
    if (type == double.class || type == Double.class){
      return new Double((double)val);
    }
    if (type == float.class || type == Float.class){
      return new Float((float)val);
    }
    
    return null;
  }
  
  static Object castDouble (Double o, Class<?> type){
    double val = o;
    
    if (type == float.class || type == Float.class){
      if (val <= Float.MAX_VALUE && val >= Float.MIN_VALUE){
        return new Float((float)val);
      }
    }
    
    return null;
  }
  
  static Object castArray (Object[] o, Class<?> type){
    // just very few supported at this time
    if (type == Object[].class){
      return o;
      
    } else if (type == int[].class){
      int[] a = new int[o.length];
      for (int i=0; i<a.length; i++){
        if (o[i] instanceof Number){
          a[i] = ((Number)o[i]).intValue();
        } else {
          return null; // incompatible element type
        }
      }
      return a;
      
    } else if (type == String[].class){
      String[] a = new String[o.length];
      for (int i=0; i<a.length; i++){
        a[i] = o[i].toString();
      }
      return a;
      
    } else if (type == double[].class){
      double[] a = new double[o.length];
      for (int i=0; i<a.length; i++){
        if (o[i] instanceof Number){
          a[i] = ((Number)o[i]).doubleValue();
        } else {
          return null; // incompatible element type
        }
      }
      return a;
      
    } else if (type == List.class || type == ArrayList.class){
      // <2do> element type check??
      ArrayList<Object> list = new ArrayList<Object>(o.length);
      for (int i=0; i<o.length; i++){
        list.add(o[i]);
      }
      return list;
      
    } else if (type == Vector.class){
      // <2do> element type check??
      Vector<Object> v = new Vector<Object>(o.length);
      for (int i=0; i<o.length; i++){
        v.add(o[i]);
      }
      return v;
    }
      
    return null;
  }
}
