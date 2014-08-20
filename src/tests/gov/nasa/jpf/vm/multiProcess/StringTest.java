//
// Copyright (C) 2013 United States Government as represented by the
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

package gov.nasa.jpf.vm.multiProcess;

import java.io.IOException;

import org.junit.Test;

import gov.nasa.jpf.util.test.TestMultiProcessJPF;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 */
public class StringTest extends TestMultiProcessJPF {
  String[] args = {};
  
  @Test
  public void testInterns() throws IOException {
    
    if (mpVerifyNoPropertyViolation(2, args)) {
      String s0 = "something"; // interned string
      String s1 = new String("something"); // a new string which is not interned
      String s2 = s1.intern(); // taken from intern table
      String s3 = "something".intern(); // taken from intern table
      
      assertSame(s0.getClass(), java.lang.String.class);
      assertSame(s1.getClass(), java.lang.String.class);
      
      assertEquals(s0,s1);
      assertFalse(s0==s1);
      
      assertSame(s0,s2);
      assertSame(s0,s3);
    }
  }
}
