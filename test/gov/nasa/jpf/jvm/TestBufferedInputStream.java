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

package gov.nasa.jpf.jvm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * raw test for java.io.BufferedInputStream
 */
public class TestBufferedInputStream {

  static File testFile = new File("__test__");
  static final byte[] TEST_DATA = { 42, 42, 42 };
  
  static boolean createTestFile() {
    try {
      FileOutputStream fo = new FileOutputStream(testFile);
      fo.write(TEST_DATA);
      fo.close();
      return true;
      
    } catch (Throwable t) {
      return false;
    }
  }
  
  static void deleteTestFile() {
    if (testFile.exists()){
      testFile.delete();
    }
  }
  
  public void testSimpleRead() {
    //createTestFile();
    
    try {
      FileInputStream fis = new FileInputStream(testFile);
      BufferedInputStream bis = new BufferedInputStream(fis);
      int n = bis.available();
      
      assert n == TEST_DATA.length : "wrong available count: " + n;
      
      for (int i=0; i<n; i++) {
        int d = bis.read();
        System.out.print(d);
        System.out.print(',');
        assert d == TEST_DATA[i] : "wrong read data"; 
      }
      System.out.println();
      
      bis.close();
      
    } catch (Throwable t) {
      assert false : "BufferedInputStream test failed: " + t;
    } finally {
      //deleteTestFile();
    }
  }
  
  //-------------------- driver to execute single test methods
  public static void main(String[] args) throws InvocationTargetException {
    TestBufferedInputStream t = new TestBufferedInputStream();
    Class<?> cls = t.getClass();
    Object[] a = new Object[0];
    
    if (args.length > 0) {
      // just run the specified tests
      for (int i = 0; i < args.length; i++) {
        String func = args[i];

        try {
          Method m = cls.getMethod(func);
          m.invoke(t, a);
        } catch (NoSuchMethodException e) {
          throw new IllegalArgumentException("unknown test function: " + func);
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException("illegal access of function: " + func);
        }
      }
    }
  }

}
