package gov.nasa.jpf.util;

import org.junit.Test;

import gov.nasa.jpf.util.test.TestJPF;

public class SparseIntVectorTest extends TestJPF {
  
  void assertEquals( SparseIntVector v1, SparseIntVector v2) {
    assertTrue( v1.size() == v2.size());

    int n = v1.size();
    for (int i=0; i<n; i++) {
      int a = v1.get(i);
      int b = v2.get(i);
      assertTrue(a == b);
    }
  }
  
  @Test
  public void testSnapshot () {
    SparseIntVector v = new SparseIntVector();

    // all empty snapshot
    SparseIntVector.Snapshot snap = v.getSnapshot();
    int val = 42;
    v.set(0,  val);
    assertTrue(v.size() == 1 && v.get(0) == val);
    v.restoreSnapshot(snap);
    assertTrue(v.size() == 0);

    //--- all full snapshot
    for (int i=0; i<100; i++) {
      v.set(i, i);
      assertTrue( "size out of sync: " + i, v.size() == (i+1));
    }
    SparseIntVector.Snapshot snap0 = v.getSnapshot();
    v.clear();
    v.restoreSnapshot(snap0);
    for (int i=0; i<100; i++) {
      assertTrue( i == v.get(i));
    }
    
    //--- punch holes into it
    v.setRange(11,  20, 0);
    v.set( 25,0);
    v.set( 26, 0);
    v.set( 42, 0);
    v.setRange(70, 85, 0);
    SparseIntVector.Snapshot snap1 = v.getSnapshot();    
    SparseIntVector v1 = v.clone();
    v.clear();
    v.restoreSnapshot(snap1);
    //v.printOn( System.out);
    assertEquals( v1, v);
    
    //--- chop off the ends
    v.restoreSnapshot(snap0);
    v.setRange(81, 99, 0);
    v.setRange(0, 19, 0);
    SparseIntVector.Snapshot snap2 = v.getSnapshot();    
    SparseIntVector v2 = v.clone();
    v.clear();
    v.restoreSnapshot(snap2);
    assertEquals( v2, v); 
  }
}
