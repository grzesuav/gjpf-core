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

package gov.nasa.jpf.vm;

import gov.nasa.jpf.jvm.DefaultJVMClassFactory;
import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

/**
 * unit test for AnnotationInfo creation
 */
public class AnnotationInfoTest extends TestJPF {

  @interface X {
    String value() default "nothing";
  }

  @interface Y {
    String name();
    int[] someArray() default { 1, 2, 3 };
  }

  @Test
  public void testStringDefaultValue() {
    ClassLoaderInfo.setClassFactory( new DefaultJVMClassFactory());
    ClassPath cp = new ClassPath(new String[] {"build/tests"});
    
    try {
      String annotationName = "gov.nasa.jpf.vm.AnnotationInfoTest$X";
      ClassFileMatch match = cp.findMatch( annotationName);
      
      AnnotationInfo ai = match.createAnnotationInfo(null);
      AnnotationInfo.Entry[] entries = ai.getEntries();
      
      assertTrue(entries.length == 1);
      assertTrue(entries[0].getKey().equals("value"));
      assertTrue(entries[0].getValue().equals("nothing"));
    
    } catch (Throwable t){
      t.printStackTrace();
      fail("unexpected exception: " + t);
    }
  }

  @Test
  public void testIntArrayDefaultValue() {
    ClassLoaderInfo.setClassFactory( new DefaultJVMClassFactory());
    ClassPath cp = new ClassPath(new String[] {"build/tests"});

    try {
      String annotationName = "gov.nasa.jpf.vm.AnnotationInfoTest$Y";
      ClassFileMatch match = cp.findMatch( annotationName);
      
      AnnotationInfo ai = match.createAnnotationInfo(null);
      AnnotationInfo.Entry[] entries = ai.getEntries();

      assertTrue(entries.length == 2);
      assertTrue(entries[1].getKey().equals("someArray"));

      Object[] a = (Object[]) entries[1].getValue();
      assertTrue(a.length == 3);
      assertTrue((Integer)a[0] == 1 && (Integer)a[1] == 2 && (Integer)a[2] == 3);
      
    } catch (Throwable t){
      t.printStackTrace();
      fail("unexpected exception: " + t);
    }
  }
}
