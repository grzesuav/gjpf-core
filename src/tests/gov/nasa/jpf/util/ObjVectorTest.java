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
package gov.nasa.jpf.util;

import org.junit.Test;

import gov.nasa.jpf.util.test.TestJPF;

/**
 * regression test for ObjVector
 */
public class ObjVectorTest extends TestJPF {

  void assertEquals( ObjVector<Integer> v1, ObjVector<Integer> v2) {
    assertTrue( v1.size() == v2.size());

    int n = v1.size();
    for (int i=0; i<n; i++) {
      Object a = v1.get(i);
      Object b = v2.get(i);
      if (a == null) {
        assertTrue( b == null);
      } else {
        assertTrue( a.equals(b));
      }
    }
  }
  
  @Test
  public void testSnapshot () {
    ObjVector<Integer> v = new ObjVector<Integer>(100);
    
    // all empty snapshot
    ObjVector.Snapshot<Integer> snap = v.getSnapshot();
    Integer val = Integer.valueOf(42);
    v.set(0,  val);
    assertTrue(v.size() == 1 && v.get(0) == val);
    v.restore(snap);
    assertTrue(v.size() == 0 && v.get(0) == null);
    
    //--- all full snapshot
    for (int i=0; i<100; i++) {
      v.set(i, i);
    }
    ObjVector<Integer> v0 = v.clone();
    ObjVector.Snapshot<Integer> snap0 = v.getSnapshot();
    v.clear();
    v.restore(snap0);
    assertEquals( v0, v);
    
    //--- punch holes into it
    v.setRange(11,  20, null);
    v.set( 25,null);
    v.set( 26, null);
    v.set( 42, null);
    v.setRange(70, 85, null);
    ObjVector.Snapshot<Integer> snap1 = v.getSnapshot();    
    ObjVector<Integer> v1 = v.clone();
    v.clear();
    v.restore(snap1);
    //v.printOn( System.out);
    assertEquals( v1, v);
    
    //--- chop off the ends
    v.restore(snap0);
    v.setRange(81, 99, null);
    v.setRange(0, 19, null);
    ObjVector.Snapshot<Integer> snap2 = v.getSnapshot();    
    ObjVector<Integer> v2 = v.clone();
    v.clear();
    v.restore(snap2);
    assertEquals( v2, v);
    
    
  }
}
