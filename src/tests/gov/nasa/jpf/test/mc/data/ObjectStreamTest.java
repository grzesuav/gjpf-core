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
package gov.nasa.jpf.test.mc.data;

import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.util.test.TestJPF;
import java.io.File;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Ivan Mushketik
 */
public class ObjectStreamTest extends TestJPF {
  String osFileName = "file";
  
  @Before
  public void beforeClass() {
    File osFile = new File(osFileName);

    if (osFile.exists()) {
      osFile.delete();
    }
  }


  @Test
  public void testWriteReadInteger() {
    if (!isJPFRun()) {
      Verify.writeObjectToFile(new Integer(123), osFileName);
    }

    if (verifyNoPropertyViolation()) {
      Integer i = Verify.readObjectFromFile(Integer.class, osFileName);

      assert i == 123;
    }
  }

  @Test
  public void testWriteReadString() {
    if (!isJPFRun()) {
      Verify.writeObjectToFile(new String("hello"), osFileName);
    }

    if (verifyNoPropertyViolation()) {
      String s = Verify.readObjectFromFile(String.class, osFileName);
      assert s.equals("hello");
    }
  }
}
