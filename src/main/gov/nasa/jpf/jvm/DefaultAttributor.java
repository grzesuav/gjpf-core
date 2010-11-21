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
import gov.nasa.jpf.util.StringSetMatcher;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Field;

/**
 * default Attributor implementation to set method and fiel attributes
 * at class load time. Note this is critical functionality, esp.
 * with respect to threading
 */
public class DefaultAttributor implements Attributor {
  
  static StringSetMatcher neverBreak;
  static StringSetMatcher breakShared;
  
  public DefaultAttributor (Config conf) {
    String[] val;
          
    val = conf.getStringArray("vm.por.field_boundaries.never");
    if (val != null) {
      neverBreak = new StringSetMatcher(val);
    }
      
    if (conf.getBoolean("vm.por.field_boundaries")) {
      val = conf.getStringArray("vm.por.field_boundaries.break");
      if (val != null) {
        breakShared = new StringSetMatcher(val);
      }
    }
  }
  
  // <2do> we should turn atomicity and scheduling relevance into general
  // MethodInfo attributes, to keep it consistent with object and field attrs
  
  public boolean isMethodAtomic (JavaClass jc, Method mth, String uniqueName) {
    
    // per default, we set all standard library methods atomic
    // (aren't we nicely optimistic, are we?)
    if (jc.getPackageName().startsWith( "java.")) {
      String clsName = jc.getClassName();
      
      // except of the signal methods, of course
      if (clsName.equals("java.lang.Object")) {
        if (uniqueName.startsWith("wait(") ||
            uniqueName.equals("notify()V")) {
          return false;
        }
      } else if (clsName.equals("java.lang.Thread")) {
        if (uniqueName.equals("join()V")) {
          return false;
        }
      }
      
      return true;
    }
    
    return false;
  }
    
  /**
   * answer the type based object attributes for this class. See
   * ElementInfo for valid choices
   */
  public int getObjectAttributes (JavaClass jc) {
    String clsName = jc.getClassName();
    
    // very very simplistic for now
    if (clsName.equals("java.lang.String") ||
       clsName.equals("java.lang.Integer") ||
       clsName.equals("java.lang.Long") ||
       clsName.equals("java.lang.Class")
        /* ..and a lot more.. */
       ) {
      return ElementInfo.ATTR_IMMUTABLE;
    } else {
      return 0;
    }
  }
  
  
  public int getFieldAttributes (JavaClass jc, Field f) {
    int attr = ElementInfo.ATTR_PROP_MASK;
    
    String fid = jc.getClassName() + '.' + f.getName();

    
    if (f.isFinal()) {
      // <2do> Hmm, finals are not really immutable, they only
      // can be set once
      attr |= ElementInfo.ATTR_IMMUTABLE;
    }
    // <2do> what about other immutable fields? don't say there are none - try to set one
    // of the compiler generated magics (like 'this$xx' in inner classes)

    //--- por field boundary attributes
    if (neverBreak != null) {
      if (neverBreak.matchesAny(fid)) {
        attr |= FieldInfo.NEVER_BREAK;
      }
    }
    
    if (breakShared != null) {
      if (breakShared.matchesAny(fid)) {
        attr |= FieldInfo.BREAK_SHARED;
      }
    }
        
    return attr;
  }
}

