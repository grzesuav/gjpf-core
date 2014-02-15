//
// Copyright (C) 2014 United States Government as represented by the
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

package gov.nasa.jpf.test.mc.basic;

import gov.nasa.jpf.util.test.TestJPF;
import java.util.HashMap;
import org.junit.Test;

/**
 * regression test for NullTracker.
 * 
 * Well, not really a regression test since NullTracker only prints out reports, but at least
 * we can see if it has errors while running
 */
public class NullTrackerTest extends TestJPF {
  
  static class TestObject {
    String d;
  
    TestObject(){
      // nothing, we forget to init d;
    }
    
    TestObject (String d){
      this.d = d;
    }
    
    int getDLength(){
      return d.length();
    }
    
    void foo(){
      // nothing
    }
  }

  TestObject o;
  
  TestObject getTestObject (){
    return null;
  }
  
  void accessReturnedObject (){
    TestObject o = getTestObject();
    System.out.println("now accessing testObject");
    String d = o.d; // that will NPE
  }
  
  void accessObject (TestObject o){
    System.out.println("now accessing testObject");
    String d = o.d; // that will NPE    
  }
  
  void createAndAccessObject(){
    TestObject o = getTestObject();
    accessObject(o);
  }
  
  
  @Test
  public void testGetAfterIntraMethodReturn (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      accessReturnedObject();
    }
  }
  
  @Test
  public void testGetAfterInterMethodReturn (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      createAndAccessObject();
    }
  }

  @Test
  public void testGetAfterIntraPut (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      o = null; // the null source
      
      String d = o.d; // causes the NPE
    }    
  }
  
  @Test
  public void testCallAfterIntraPut (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      o = null; // the null source
      
      o.foo(); // causes the NPE
    }    
  }

  @Test
  public void testGetAfterASTORE (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      TestObject myObj = null; // the null source
      
      myObj.foo(); // causes the NPE
    }    
  }

  
  HashMap<String,TestObject> map = new HashMap<String,TestObject>();
  
  TestObject lookupTestObject (String name){
    return map.get(name);
  }
  
  @Test
  public void testHashMapGet (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      TestObject o = lookupTestObject("FooBar");
      o.foo();
    }
  }
  
  //------------------------------------------------------------------
    
  TestObject createTestObject (){
    return new TestObject();
  }
  
  
  TestObject createTestObject (String d){
    return new TestObject(d);
  }
  
  @Test
  public void testMissingCtorInit (){
    if (verifyUnhandledException("java.lang.NullPointerException", "+listener=.listener.NullTracker")){
      TestObject o = createTestObject("blah");
      int len = o.getDLength(); // that should be fine
      
      o = createTestObject();
      len = o.getDLength(); // that should NPE and report the default ctor as culprit
    }    
  }
}
