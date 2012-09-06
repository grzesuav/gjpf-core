package gov.nasa.jpf.jvm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.IntTable;
import gov.nasa.jpf.util.ObjVector;

import java.util.Iterator;

public class ObjVectorHeap extends GenericHeapImpl {

  IntTable<AllocationContext> allocCounts;
  
  ObjVector<ElementInfo> elementInfos;
  int size;  // non-null elements - we need to maintain this ourselves since ObjVector size is different
  
  
  //--- constructors
  
  public ObjVectorHeap (Config config, KernelState ks){
    super(config, ks);
    
    allocCounts = new IntTable<AllocationContext>();
    elementInfos = new ObjVector<ElementInfo>();
  }

  //--- heap interface
  
  /**
   * return number of non-null elements
   */
  @Override
  public int size() {
    return size;
  }
  
  //--- the allocator primitives
  protected int getNewElementInfoIndex (ClassInfo ci, ThreadInfo ti, String loc) {
    AllocationContext ctx = HashedAllocationContext.getAllocationContext(ci, ti, loc);
    
    // <2do> this is where the SGOID computation goes
    return 0;
  }
  
  protected void set (int index, ElementInfo ei) {
    elementInfos.set(index, ei);
  }



  @Override
  public int newArray(String elementType, int nElements, ThreadInfo ti, String location) {
    return 0;
  }

  @Override
  public int newString(String str, ThreadInfo ti) {
    return 0;
  }

  @Override
  public int newInternString(String str, ThreadInfo ti) {
    return 0;
  }

  /**
   * we treat ref < 0 as NULL reference instead of throwing an exception
   */
  @Override
  public ElementInfo get(int ref) {
    if (ref < 0) {
      return null;
    } else {
      return elementInfos.get(ref);
    }
  }

  @Override
  protected void remove(int ref) {
    if (elementInfos.remove(ref) != null) {
      size--;
    }
  }

  @Override
  public Iterator<ElementInfo> iterator() {
    return elementInfos.nonNullIterator();
  }

  @Override
  public Iterable<ElementInfo> liveObjects() {
    return elementInfos.elements();
  }

  @Override
  public void resetVolatiles() {
  }

  @Override
  public void restoreVolatiles() {
  }

  @Override
  public Memento<Heap> getMemento(MementoFactory factory) {
    return null;
  }

  @Override
  public Memento<Heap> getMemento() {
    return null;
  }

}
