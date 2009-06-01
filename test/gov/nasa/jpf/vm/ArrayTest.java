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
package gov.nasa.jpf.vm;

import gov.nasa.jpf.util.test.RawTest;

/**
 * JPF part of unit test for standard VM array operations.
 */
public class ArrayTest extends RawTest {

  public static void main (String[] selectedMethods) {
    runTests( new ArrayTest(), selectedMethods);
  }

  public void test2DArray () {
    long[][] a = new long[2][3];

    a[0][1] = 42;

    assert (a.getClass().isArray());
    assert (a.getClass().getName().equals("[[J"));
    assert (a.getClass().getComponentType().getName().equals("[J"));
    assert (a[0][1] == 42);
  }

  public void test2DStringArray () {
    String[][] a = new String[3][5];

    a[2][2] = "fortytwo";

    assert (a.getClass().isArray());
    assert (a.getClass().getName().equals("[[Ljava.lang.String;"));
    assert (a.getClass().getComponentType().getName().equals("[Ljava.lang.String;"));
    assert (a[2][2].equals("fortytwo"));
  }

  public void testAoBX () {
    int[] a = new int[2];

    assert (a.length == 2);

    try {
      a[2] = 42;
    } catch (ArrayIndexOutOfBoundsException aobx) {
      return;
    }

    throw new RuntimeException("array bounds check failed (no ArrayIndexOutOfBoundsException)");
  }

  public void testArrayStoreException () {
    try {
      Object x[] = new String[1];
      x[0] = new Integer(42);
    } catch (ArrayStoreException osx) {
      return;
    }
    
    throw new RuntimeException("array element type check failed (no ArrayStoreException)");    
  }
  
  public void testArrayStoreExceptionArraycopy () {
    
    boolean caught = false;
    try {
      System.arraycopy(new Object[]{new Integer(42)}, 0, new String[2], 0, 1);
    } catch (ArrayStoreException x) {
      caught = true;
    }
    if (!caught) {
      throw new RuntimeException("array element type check failed (no ArrayStoreException: Integer[] -> String[])");
    }
    
    caught = false;
    try {
      System.arraycopy(new Object[]{new Integer(42)}, 0, new int[2], 0, 1);      
    } catch (ArrayStoreException x) {
      caught = true;
    }
    if (!caught) {
      throw new RuntimeException("array element type check failed (no ArrayStoreException: Integer[] -> int[])");
    }
    
    caught = false;
    try {
      System.arraycopy(new int[]{42,42}, 0, new long[2], 0, 1);      
    } catch (ArrayStoreException x) {
      caught = true;
    }
    if (!caught) {
      throw new RuntimeException("array element type check failed (no ArrayStoreException: int[] -> long[])");
    }
    
    caught = false;
    try {
      System.arraycopy(new int[]{42,42}, 0, new float[2], 0, 1);      
    } catch (ArrayStoreException x) {
      caught = true;
    }
    if (!caught) {
      throw new RuntimeException("array element type check failed (no ArrayStoreException: int[] -> float[])");
    }
    
    caught = false;
    try {
      System.arraycopy(new double[]{42,42}, 0, new long[2], 0, 1);      
    } catch (ArrayStoreException x) {
      caught = true;
    }
    if (!caught) {
      throw new RuntimeException("array element type check failed (no ArrayStoreException: double[] -> long[])");
    }
    
  }
  
  public void testCharArray () {
    char[] a = new char[5];

    a[2] = 'Z';

    assert (a.getClass().isArray());
    assert (a.getClass().getName().equals("[C"));
    assert (a.getClass().getComponentType() == char.class);
    assert (a[2] == 'Z');
  }

  public void testIntArray () {
    int[] a = new int[10];

    a[1] = 42;

    assert (a.getClass().isArray());
    assert (a.getClass().getName().equals("[I"));    
    assert (a.getClass().getComponentType() == int.class);
    assert (a[1] == 42);
  }

  public void testStringArray () {
    String[] a = { "one", "two", "three" };

    assert (a.getClass().isArray());
    assert (a.getClass().getName().equals("[Ljava.lang.String;"));
    assert (a.getClass().getComponentType() == String.class);
    assert (a[1].equals("two"));
  }

  public void testArrayCopy () {
    String s1 = "1";
    String s2 = "2";
    String s3 = "3";
    String[] sa = { s1, s2, s3 };
    String[] sb = { s3, s2, s1 };
    
    System.arraycopy(sa, 0, sb, 1, 2);
    assert (sb[0] == s3 && sb[1] == s1 && sb[2] == s2);

    System.arraycopy(sa, 0, sa, 1, 2);
    assert (sa[0] == s1 && sa[1] == s1 && sa[2] == s2);
    
    System.arraycopy(sb, 1, sb, 0, 2);
    assert (sb[0] == s1 && sb[1] == s2 && sb[2] == s2);

    System.arraycopy(sa, 3, sb, 0, 0);
    assert (sb[0] == s1 && sb[1] == s2 && sb[2] == s2);

    
    long[] la = { 1L, 2L, 3L };
    long[] lb = { 3L, 2L, 1L };

    System.arraycopy(la, 0, lb, 1, 2);
    assert (lb[0] == 3 && lb[1] == 1 && lb[2] == 2);

    System.arraycopy(la, 0, la, 1, 2);
    assert (la[0] == 1 && la[1] == 1 && la[2] == 2);
    
    System.arraycopy(lb, 1, lb, 0, 2);
    assert (lb[0] == 1 && lb[1] == 2 && lb[2] == 2);

    System.arraycopy(la, 3, lb, 0, 0);
    assert (lb[0] == 1 && lb[1] == 2 && lb[2] == 2);
  }
}
