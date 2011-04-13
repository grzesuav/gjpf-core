//
// Copyright (C) 2009 United States Government as represented by the
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


import gov.nasa.jpf.*;
import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.search.*;
import gov.nasa.jpf.util.test.*;
import org.junit.Test;


/**
 * test various Verify APIs
 */
public class VerifyTest extends TestJPF {

  @Test public void testBreak () {

    if (verifyNoPropertyViolation()) {
      int y = 4;
      int x = 0;

      while (x != y) { // JPF should state match on the backjump
        x = x + 1;
        if (x > 3) {
          x = 0;
        }

        Verify.breakTransition(); // this should eventually state match
      }

      assert false : "we should never get here";
    }
  }

  @Test public void testProperties () {

    if (verifyNoPropertyViolation()) {
      String target = Verify.getProperty("target");
      System.out.println("got target=" + target);
      assert target.equals(TestJPFHelper.class.getName());

      Verify.setProperties("foo=bar");
      String p = Verify.getProperty("foo");
      System.out.println("got foo=" + p);
      assert "bar".equals(p);
    }
  }
  
  @Test public void testChangeListener () {
    
    if (verifyNoPropertyViolation()) {
      Verify.setProperties("listener=gov.nasa.jpf.listener.StateSpaceAnalyzer");  // This used to cause a NullPointerException
    }
  }
  
  @Test public void testGetBoolean () {

    Verify.resetCounter(0);
    Verify.resetCounter(1);

    if (verifyNoPropertyViolation()) {
      Verify.incrementCounter(Verify.getBoolean() ? 1 : 0);
    } else {
      assert Verify.getCounter(0) == 1;
      assert Verify.getCounter(1) == 1;
    }
  }

  @Test public void testGetBooleanFalseFirst () {
    boolean falseFirst, value;

    Verify.resetCounter(0);

    if (verifyNoPropertyViolation()) {
    
      falseFirst = Verify.getBoolean();
    
      Verify.resetCounter(0);
    
      value = Verify.getBoolean(falseFirst);
      
      Verify.ignoreIf(Verify.getCounter(0) != 0);
      
      Verify.incrementCounter(0);
      
      assert value == !falseFirst;
    }
  }
  
  /**
   * This test ensures that stateBacktracked() is called even if the transistion 
   * is ignored.  This is important for listeners that keep a state that must 
   * match the JVM's state exactly and the state is updated in the middle of 
   * transitions.  This is not possible if a backtrack happens on an ignored 
   * transition and the stateBacktracked is not called.
   */
  @Test
  public void backtrackNotificationAfterIgnore()
  {
     if (verifyNoPropertyViolation("+listener+=," + CountBacktrack.class.getName()))
     {
        if (Verify.getBoolean(false))
           Verify.ignoreIf(true);
     }
     else
     {
        assertEquals(2, CountBacktrack.getBacktrackedCount());
     }
  }
  
  public static class CountBacktrack extends ListenerAdapter
  {
     private static int m_backtrackedCount;

     public void stateBacktracked(Search search)
     {
        m_backtrackedCount++;
     }
     
     public static int getBacktrackedCount()
     {
        return(m_backtrackedCount);
     }
  }
   
  // <2do>... and many more to come

  private String getInternalClassName(String className) {
    return className;
  }

  private Object getFilledObject(Class<?> clazz, String jsonString) {
    return Verify.createFromJSON(getInternalClassName(clazz.getName()), jsonString);
  }

  class MySup {
    int j;
  }


  @Test
  public void testFillFromJSONSingleClass() {
    if (verifyNoPropertyViolation()) {
      String json = "{"
              + "\"j\" : 123"
              + "}";
      MySup sup = (MySup) getFilledObject(MySup.class, json);
      
      assert sup.j == 123;
    }
  }

  class MyClass extends MySup {
    int i;
  }

  @Test
  public void testFillFromJSONInheritance() {
    if (verifyNoPropertyViolation()) {
      String json = "{"
              + "\"j\" : 123,"
              + "\"i\" : 321"
              + "}";
      MyClass sup = (MyClass) getFilledObject(MyClass.class, json);

      assert sup.j == 123;
      assert sup.i == 321;
    }
  }

  class Primitives {
    boolean z;
    byte b;
    short s;
    int i;
    long l;
    float f;
    double d;
  }

  @Test
  public void testFillPrivimitivesFromJSON() {
    if (verifyNoPropertyViolation()) {
      String json = "{"
              + "\"z\" : true,"
              + "\"b\" : 10,"
              + "\"s\" : 1000,"
              + "\"i\" : 321,"
              + "\"l\" : 123456,"
              + "\"f\" : 12.34,"
              + "\"d\" : 23.45"
              + "}";
      Primitives p = (Primitives) getFilledObject(Primitives.class, json);

      assert p.z == true;
      assert p.b == 10;
      assert p.s == 1000;
      assert p.i == 321;
      assert p.l == 123456;
      assertEquals(12.34, p.f, 0.001);
      assertEquals(12.34, p.f, 0.001);
    }
  }

  class IntArr {
    int ints[];
  }

  @Test
  public void testFillIntArrayFromJSON() {
    if (verifyNoPropertyViolation()) {
      String json = "{"
              + "\"ints\" : [1, 2, 3]"
              + "}";
      IntArr ia = (IntArr) getFilledObject(IntArr.class, json);

      assert ia.ints[0] == 1;
      assert ia.ints[1] == 2;
      assert ia.ints[2] == 3;      
    }
  }

}
