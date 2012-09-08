//
// Copyright (C) 2012 United States Government as represented by the
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
import gov.nasa.jpf.util.SparseObjVector;

import static gov.nasa.jpf.util.OATHash.*;

/**
 * an AllocationContext that uses a hash value for comparison. This is
 * lossy - heap implementations using this class have to check/handle
 * collisions.
 * 
 * However, given that we have very good hash data (search global object
 * references), the probability of collisions is low enough that heap
 * implementations might simply report this as a problem requiring a
 * non-lossy AllocationContext.
 * 
 * note - this is a HashMap key type which has to obey the hashCode/equals contract
 */
public class HashedAllocationContext implements AllocationContext {

  static SparseObjVector<HashedAllocationContext> pool;
  
static int N=0;
  
  public static AllocationContext getAllocationContext (ClassInfo ci, ThreadInfo ti, String loc) {
    int h = 0;
    
    h = hashMixin(h, ci.hashCode());
    h = hashMixin(h, ti.hashCode());
    
    for (StackFrame frame = ti.getTopFrame(); frame != null; frame = frame.getPrevious() ) {
      if (!(frame instanceof DirectCallStackFrame)) {
        Instruction insn = frame.getPC();
        h = hashMixin(h, insn.hashCode());        
      }
    }
    
    //h = hashMixin(h, loc.hashCode()); // this version is value based, which allows dynamically computed locs
    h = hashMixin(h, System.identityHashCode(loc));

    h = hashFinalize(h);
    
    HashedAllocationContext ctx = pool.get(h);
    if (ctx == null) {
      ctx = new HashedAllocationContext(h);
      pool.set(h, ctx);
    }
    
    return ctx;
  }

  public static boolean init (Config conf) {
    pool = new SparseObjVector<HashedAllocationContext>();
    return true;
  }
  
  //--- instance data
  
  // rolled up hash value for all context components
  protected final int id;

  
  //--- instance methods
  
  protected HashedAllocationContext (int id) {
    this.id = id;
  }
  
  
  public boolean equals (Object o) {
    if (o instanceof HashedAllocationContext) {
      HashedAllocationContext other = (HashedAllocationContext)o;
      return id == other.id; 
    }
    
    return false;
  }
  
  /**
   * @pre: must be the same for two objects that result in equals() returning true
   */
  public int hashCode() {
    return id;
  }
}
