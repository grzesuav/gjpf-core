
package gov.nasa.jpf.basic;

import gov.nasa.jpf.util.test.RawTest;

/**
 * raw test to test the test harness (how often can you say 'test' in one
 * sentence?)
 */
public class $01_HarnessTest extends RawTest {

  int d;

  public static void main (String[] selectedTests){
    runTestsOfThisClass(selectedTests);
  }

  public $01_HarnessTest () {
    d = 42;
  }


  public void test_1 () {
    d += 42;
    System.out.println("** this is test_1() - it should succeed");
  }
  public void test_2 () {
    System.out.println("** this is test_2() - it should succeed");
    assert d == 42 : "test object init failed: " + d;
  }
  public void test_3 () {
    System.out.println("** this is test_3() - it should fail");
    int[] a = { 1, 2 };
    a[3] = 0;
  }

  public static void testStatic (){
    System.out.println("** testStatic should not be executed");
    throw new RuntimeException("don't execute this");
  }
  public int testNonVoid () {
    System.out.println("** testNonVoid should not be executed");
    throw new RuntimeException("don't execute this");
  }
  public void testArgs (int dummy){
    System.out.println("** testArgs should not be executed");
    throw new RuntimeException("don't execute this");
  }
  protected void testNonPublic (){
    System.out.println("** testNonPublic should not be executed");
    throw new RuntimeException("don't execute this");
  }


}
