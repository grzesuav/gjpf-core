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

import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * class hierarchy encapsulating the various contract-type specific 
 * variable lookup algorithms (policy), together with a cache mechanism
 * for values so that we
 *  - don't have to repeatedly resolve values of the same variables
 *  - can store old(operand) values
 *  
 * a bit convoluted, should probably separate lookup and storage better than
 * just base (storage) and concrete subclass (policy)
 */
public abstract class VarLookup {
  
  HashMap<Object,Object> cache;
  
  public abstract String getLookupType();
  
  // that's the workhorse method
  public Object getValue (String key) {
    return null;
  }
  
  public HashMap<Object,Object> getCache() {
    return cache;
  }
  
  VarLookup() {
    cache = new HashMap<Object,Object>();
  }
  
  VarLookup(HashMap<Object,Object> map) {
    cache = map;
  }

  Object getSpecialValue (String var) {
    // nothing yet, cold be used for values obtained from the VM
    // (e.g. memory footprint, timestamp etc.)
    return null;
  }

  
  //--- this is our public interface
  public boolean containsKey (Object key) {
    return cache.containsKey(key);
  }
  
  public Object lookup (Object key) {
    Object v = null;
    
    if (cache.containsKey(key)) {
      v = cache.get(key);
      
    } else if (key instanceof String) {
      String var = (String)key;
      
      v = getSpecialValue(var);
      if (v == null) {
        v = getValue(var);
      }
      cache.put(key, v);
    }
    
    return v;
  }

  public void put (Object key, Object value) {
    cache.put(key, value);
  }
  
  public void purgeVars () {
    
    // HashMap set iterators are fail-fast
    for (Iterator<Map.Entry<Object,Object>> it = cache.entrySet().iterator(); it.hasNext();) {
      Map.Entry<Object,Object> e = it.next();
      Object key = e.getKey();
      if (key instanceof String 
            && !Operand.RESULT.equals(key)) {  // that's weak, should use a different scheme to identify Result
        it.remove();
      }
    }
  }
  
  //--- our concrete implementations
  
  /**
   * no old values, args/locals > instance fields > static fields
   */
  public static class PreCond extends VarLookup {
    
    ThreadInfo ti;
    InvokeInstruction call;
        
    public PreCond (ThreadInfo ti, InvokeInstruction call) {
      this.ti = ti;
      this.call = call;
    }
    
    public Object getValue (String varName) {
      return call.getFieldOrArgumentValue(varName,ti);
    }
    
    public String getLookupType() {
      return "precondition";
    }
  }
  
  /**
   * the pre-exec postcond old value lookup
   */
  public static class PostCondPreExec extends VarLookup {
    ThreadInfo ti;
    
    public PostCondPreExec (ThreadInfo ti) {
      this.ti = ti;
    }
    
    public Object getValue (String varName) {      
      Object v = cache.get(varName);
      if (v == null) {
        v = ti.getTopFrame().getLocalOrFieldValue(varName);
        cache.put(varName, v);
      }
      
      return v;
    }

    public String getLookupType() {
      return "postcondition (pre-execution)";
    }

  }
  
  /**
   * post-exec postcond value lookup - no 'old' values, but with return value
   */
  public static class PostCond extends VarLookup {

    ThreadInfo ti;
    ReturnInstruction ret;

    public PostCond (ThreadInfo ti, ReturnInstruction ret, VarLookup old) {
      super(old.cache);
      
      this.ti = ti;
      this.ret = ret;
    }

    public Object getValue (String varName) {
      if (varName.equals(Operand.RESULT)) {
        return ret.getReturnValue(ti);
      } else {
        return ti.getTopFrame().getLocalOrFieldValue(varName);
      }
    }
    
    public String getLookupType() {
      return "postcondition";
    }

  }

  /**
   * only instance and static fields (depending on method attrs)
   */
  public static class Invariant extends VarLookup {
    
    ThreadInfo ti;
    
    public Invariant (ThreadInfo ti) {
      this.ti = ti;
    }
    
    public Object getValue (String varName) {
      return ti.getTopFrame().getFieldValue(varName);
    }
    
    public String getLookupType() {
      return "invariant";
    }

  }
}
