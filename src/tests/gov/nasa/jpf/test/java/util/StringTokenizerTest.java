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

package gov.nasa.jpf.test.java.util;

import gov.nasa.jpf.util.test.TestJPF;
import java.util.StringTokenizer;
import org.junit.Test;

/**
 *
 * @author Ivan Mushketik
 */
public class StringTokenizerTest extends TestJPF {

  @Test
  public void testCountTokens() {
    if (verifyNoPropertyViolation()) {
      StringTokenizer st1 = new StringTokenizer("This is a test");
      StringTokenizer st2 = new StringTokenizer("This is a\t\tstring", "\t\f\n ", true);

      assert st1.countTokens() == 4;
      assert st2.countTokens() == 8;
    }
  }

  @Test
  public void testHasNextToken() {
    if (verifyNoPropertyViolation()) {
      StringTokenizer st = new StringTokenizer("This is");
      
      assert st.hasMoreTokens() == true;
      assert st.nextToken().equals("This");
      assert st.hasMoreTokens() == true;
      assert st.nextToken().equals("is");
      assert st.hasMoreTokens() == false;
    }
  }

  @Test
  public void testStandardTokenizing() {
    if (verifyNoPropertyViolation()) {
      StringTokenizer st = new StringTokenizer("This \t\nis \fa test");
      String expectedResult[] = {"This", "is", "a", "test"};

      checkTokenizing(st, expectedResult);
    }
  }

  private void checkTokenizing(StringTokenizer st, String[] expectedResult) {
    for (int i = 0; i < expectedResult.length; i++) {
      String token = st.nextToken();

      assert token.equals(expectedResult[i]) == true;
    }
  }

  @Test
  public void testTokenizingWithReturningDelimeters() {
    if (verifyNoPropertyViolation()) {
      StringTokenizer st = new StringTokenizer("This is a   test", " ", true);
      String expectedResult[] = {"This", " ", "is", " ", "a", " ", " ", " ", "test"};

      checkTokenizing(st, expectedResult);
    }
  }

  @Test
  public void testChangingDelimeters() {
    if (verifyNoPropertyViolation()) {
      StringTokenizer st = new StringTokenizer("This is a*long*test");

      assert st.nextToken().equals("This");
      assert st.nextToken().equals("is");
      assert st.nextToken("*").equals(" a");
      assert st.nextToken().equals("long");
      assert st.nextToken().equals("test");
    }
  }
}
