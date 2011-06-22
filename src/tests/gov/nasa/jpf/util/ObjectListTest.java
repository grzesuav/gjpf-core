//
// Copyright (C) 2010 United States Government as represented by the
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

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

/**
 * regression unit test for ObjectList API
 */
public class ObjectListTest extends TestJPF {
  
  Object attr;
  
  @Test
  public void testAdd(){
    assertTrue( ObjectList.isEmpty(attr));
    
    attr = ObjectList.add(attr, "one");
    assertTrue( ObjectList.size(attr) == 1);
    assertTrue( attr != null && attr.equals("one"));
    
    attr = ObjectList.add(attr, new Integer(2));
    assertTrue( ObjectList.size(attr) == 2);
    assertTrue( attr != null && !(attr instanceof Integer));
  }
  
  
  @Test
  public void testListIteration() {
    Object[] v = { new Integer(2), "one" };
    
    for (Object a: ObjectList.iterator(attr)){
      fail("list should be empty");
    }
    
    for (int i=v.length-1; i>=0; i--){
      attr = ObjectList.add(attr, v[i]);
    }
    
    int len = ObjectList.size(attr);
    for (int i=0; i<len; i++){
      Object a = ObjectList.get(attr, i);
      System.out.printf("[%d] = %s\n", i, a.toString());
      assertEquals( a, v[i]);
    }

    attr = ObjectList.add(attr, "three");
    attr = ObjectList.add(attr, new Integer(4));
    
    int i=0;
    for (Integer a = ObjectList.getFirst(attr, Integer.class); a!=null; a = ObjectList.getNext(attr, Integer.class, a)){
      System.out.println(a);
      i++;
    }
    assertTrue( i == 2);

    System.out.println("-- untyped iterator");
    i=0;
    for (Object a: ObjectList.iterator(attr)){
      System.out.printf("[%d] = %s\n", i++, a.toString());
    }
    assertTrue( i == 4);
    
    System.out.println("-- typed iterator (all Strings)");
    i=0;
    for (String a: ObjectList.typedIterator(attr, String.class)){
      System.out.printf("[%d] = %s\n", i++, a.toString());
    }
    assertTrue(i == 2);
    
    System.out.println("-- typed iterator (all Floats) => none");
    i=0;
    for (float f: ObjectList.typedIterator(attr, Float.class)){
      System.out.printf("[%d] = %f\n", i++, f);
    }
    assertTrue(i == 0);
    
  }
  
  @Test
  public void testSingleValueIteration(){
    attr = ObjectList.add(attr, "one");
    
    System.out.println("-- untyped iterator");
    int i=0;
    for (Object a: ObjectList.iterator(attr)){
      System.out.printf("[%d] = %s\n", i++, a.toString());
    }
    assertTrue( i == 1);
    
    System.out.println("-- typed iterator (all Strings)");
    i=0;
    for (String a: ObjectList.typedIterator(attr, String.class)){
      System.out.printf("[%d] = %s\n", i++, a.toString());
    }
    assertTrue(i == 1);
    
    System.out.println("-- typed iterator (all Floats) => none");
    i=0;
    for (float f: ObjectList.typedIterator(attr, Float.class)){
      System.out.printf("[%d] = %f\n", i++, f);
    }
    assertTrue(i == 0);
  }
  
  
  @Test
  public void testRemove(){
    attr = ObjectList.add(attr, "one");
    
    attr = ObjectList.remove(attr, attr);
    assertTrue(attr == null);
    
    String one = "one";
    String two = "two";
    attr = ObjectList.add(attr, one);
    attr = ObjectList.add(attr, two);
    attr = ObjectList.remove( attr, one);
    assertTrue(attr != null && attr == two);
    
    attr = one;
    attr = ObjectList.add(attr, two);
    attr = ObjectList.remove( attr, two);
    assertTrue(attr != null && attr == one);
   
    
    attr = one;
    attr = ObjectList.add(attr, two);
    attr = ObjectList.add(attr, "three");
    attr = ObjectList.remove( attr, two);
    int i=0;
    for (Object a: ObjectList.iterator(attr)){
      System.out.printf("[%d] = %s\n", i++, a.toString());
    }    
    assertTrue( ObjectList.size(attr) == 2);
  }
  
  @Test
  public void testReplace(){
    String one = "one";
    attr = ObjectList.add(attr, one);
    
    Integer i1 = new Integer(1);
    attr = ObjectList.replace(attr, one, i1);
    assertTrue(attr == i1);
    
    String two = "two";
    Integer i2 = new Integer(2);
    attr = ObjectList.add(attr, two);
    attr = ObjectList.replace(attr, two, i2);
    Integer o = ObjectList.getFirst(attr, Integer.class);
    assertTrue( o == i2);
    
    int i=0;
    for (Object a: ObjectList.iterator(attr)){
      System.out.printf("[%d] = %s\n", i++, a.toString());
    }    
    assertTrue( ObjectList.size(attr) == 2);
  }
}
