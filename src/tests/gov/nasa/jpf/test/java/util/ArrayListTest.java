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

package gov.nasa.jpf.test.java.util;

import gov.nasa.jpf.util.test.TestJPF;
import java.util.ArrayList;
import org.junit.Test;

/**
 *
 * @author Ivan Mushketik
 */
public class ArrayListTest extends TestJPF {

  @Test
  public void testClear() {
    if (verifyNoPropertyViolation()) {
      ArrayList<Integer> al = new ArrayList<Integer>();
      al.add(new Integer(1));

      assert al.size() == 1;
      assert al.isEmpty() == false;

      al.clear();
      assert al.size() == 0;
      assert al.isEmpty() == true;
    }
  }

  @Test
  public void testToArray() {
    if (verifyNoPropertyViolation()) {
      ArrayList<Integer> al = new ArrayList<Integer>();
      int length = 5;
      for (int i = 0; i < length; i++) {
        al.add(new Integer(i));
      }

      Object[] ints = al.toArray();

      assert ints.length == length;

      for (int i = 0; i < length; i++) {
        assert ints[i].equals(new Integer(i)) == true;
      }
    }
  }

  @Test
  public void testRemoveNullWhenNotExists() {
    if (verifyNoPropertyViolation()) {
      ArrayList<Integer> al = new ArrayList<Integer>() {{
        add(1); add(2); add(3);
      }};

      assert al.remove(null) == false;
      Integer[] expected = {new Integer(1), new Integer(2), new Integer(3)};
      assertList(al, expected);
    }
  }

  @Test
  public void testRemoveNullWhenExists() {
    if (verifyNoPropertyViolation()) {
      ArrayList<Integer> al = new ArrayList<Integer>() {{
        add(1); add(2); add(null); add(3);
      }};

      assert al.remove(null) == true;      
      Integer[] expected = {new Integer(1), new Integer(2), new Integer(3)};
      assertList(al, expected);
    }
  }

  private void assertList(ArrayList<Integer> al, Integer[] expected) {
    assert al.size() == expected.length;

    for (int i = 0; i < expected.length; i++) {
      Integer listI = al.get(i);
      assert listI.equals(expected[i]) == true;
    }
  }

  @Test
  public void testRemoveFromBegining() {
    if (verifyNoPropertyViolation()) {
      ArrayList<Integer> al = new ArrayList<Integer>() {{
        add(1); add(2); add(3);
      }};

      assert al.remove(new Integer(1)) == true;
      Integer[] expected = {new Integer(2), new Integer(3)};
      assertList(al, expected);
    }
  }

  @Test
  public void testRemoveInTheMiddle() {
    if (verifyNoPropertyViolation()) {
      ArrayList<Integer> al = new ArrayList<Integer>() {{
        add(1); add(2); add(3);
      }};

      assert al.remove(new Integer(2)) == true;
      Integer[] expected = {new Integer(1), new Integer(3)};
      assertList(al, expected);
    }
  }

  @Test
  public void testRemoveAtTheEnd() {
    if (verifyNoPropertyViolation()) {
      ArrayList<Integer> al = new ArrayList<Integer>() {{
        add(1); add(2); add(3);
      }};

      assert al.remove(new Integer(3)) == true;
      Integer[] expected = {new Integer(1), new Integer(2)};
      assertList(al, expected);
    }
  }

  @Test
  public void testRemoveWhenNotExists() {
    if (verifyNoPropertyViolation()) {
      ArrayList<Integer> al = new ArrayList<Integer>() {{
        add(1); add(2); add(3);
      }};

      assert al.remove(new Integer(4)) == false;
      Integer[] expected = {new Integer(1), new Integer(2), new Integer(3)};
      assertList(al, expected);
    }
  }
}
