package gov.nasa.jpf.jvm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.SparseObjVector;

import static gov.nasa.jpf.util.OATHash.*;

/**
 * note - this is a HashMap key type which has to obey the hashCode/equals contract
 */
public class HashedAllocationContext implements AllocationContext {

  static SparseObjVector<HashedAllocationContext> pool;
  
  
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
