//
// Copyright (C) 2011 United States Government as represented by the
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
import gov.nasa.jpf.jvm.serialize.AbstractionAdapter;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

/**
 * regression test for field value abstractions
 */
public class DynamicAbstractionTest extends TestJPF {
  
  static class MyClass {
    int data;
    double notAbstracted;
  }
  
  public static class MyClassDataAbstraction extends AbstractionAdapter {
    
    @Override
    public int getAbstractValue (int data){
      int cat = 0;
      if (data > 5) cat = 1;
      if (data > 10) cat = 2;
      
      System.out.println("abstracted value for " + data + " = " + cat);
      return cat;
    }
  }
  
  @Test
  public void testMyClass() {
    if (!isJPFRun()){
      Verify.resetCounter(0);
    }
    
    if (verifyNoPropertyViolation("+listener=.listener.DynamicStateAbstractor",
                                  "+vm.serializer.class=.listener.DynamicStateAbstractor$Serializer",
                                  "+dabs.fields=data", 
                                  "+dabs.data.field=*$MyClass.data",
                                  "+dabs.data.abstraction=gov.nasa.jpf.test.mc.data.DynamicAbstractionTest$MyClassDataAbstraction")){
      MyClass myClass = new MyClass();
      myClass.data = Verify.getInt(0, 20);
      
      Verify.breakTransition();
      System.out.println("new state for myClass.data = " + myClass.data);
      Verify.incrementCounter(0);
    }
    
    if (!isJPFRun()){
      assertTrue( Verify.getCounter(0) == 3);
    }    
  }
  
  @Test
  public void testMixedFields(){
    if (!isJPFRun()){
      Verify.resetCounter(0);
    }
    
    if (verifyNoPropertyViolation("+listener=.listener.DynamicStateAbstractor",
                                  "+vm.serializer.class=.listener.DynamicStateAbstractor$Serializer",
                                  "+dabs.fields=data", 
                                  "+dabs.data.field=*$MyClass.data",
                                  "+dabs.data.abstraction=gov.nasa.jpf.test.mc.data.DynamicAbstractionTest$MyClassDataAbstraction")){
      MyClass myClass = new MyClass();
      myClass.data = Verify.getInt(0, 20);
      
      if (myClass.data % 4 == 0){
        System.out.println("  notAbstracted=1");
        myClass.notAbstracted = 1;
      }
      
      Verify.breakTransition(); // matching point
      System.out.println("new state for myClass.data = " + myClass.data + ", " + myClass.notAbstracted);
      Verify.incrementCounter(0);
    }
    
    if (!isJPFRun()){
      assertTrue( Verify.getCounter(0) == 6);
    }        
  }
}
