package gov.nasa.jpf.mc;

import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;
import org.junit.runner.JUnitCore;

/**
 * JPF-level tests for the UntrackedField annotation.
 *
 * @author Milos Gligoric (milos.gligoric@gmail.com)
 * @author Tihomir Gvero (tihomir.gvero@gmail.com)
 *
 */
public class TestUntrackedFieldJPF extends TestJPF {
  static final String TEST_CLASS = TestUntrackedField.class.getName();

  public static void main (String[] args) {
    JUnitCore.main(TestUntrackedFieldJPF.class.getName());
  }


  /**************************** tests **********************************/

  static String[] args = { TEST_CLASS,
                           "+vm.static_area.class=gov.nasa.jpf.jvm.untracked.UntrackedStaticArea",
                           "+vm.dynamic_area.class=gov.nasa.jpf.jvm.untracked.UntrackedDynamicArea",
                           "+vm.restorer.class = gov.nasa.jpf.jvm.untracked.UntrackedCollapsingRestorer",
                           "+vm.fields_factory.class=gov.nasa.jpf.jvm.untracked.UntrackedFieldsFactory",
                           "+vm.untracked=true",
                           "<test-method>"
                           };

  @Test
  public void testPrimitiveField () {
    args[args.length-1]= "testPrimitiveField";
    noPropertyViolation(args);
  }

  @Test
  public void testPrimitiveArrayField () {
    args[args.length-1]=  "testPrimitiveArrayField";
    noPropertyViolation(args);
  }

  @Test
  public void testReferenceField () {
    args[args.length-1]=  "testReferenceField";
    noPropertyViolation(args);
  }

  @Test
  public void testUntrackedAndFilterFields () {
    args[args.length-1]=  "testUntrackedAndFilterFields";
    noPropertyViolation(args);
  }

  @Test
  public void testCounters () {
    args[args.length-1]=  "testCounters";
    noPropertyViolation(args);
  }

  @Test
  public void testUntrackedTrackedAliasing () {
    args[args.length-1]=  "testUntrackedTrackedAliasing";
    noPropertyViolation(args);
  }

  @Test
  public void testCycle () {
    args[args.length-1]=  "testCycle";
    noPropertyViolation(args);
  }

  @Test
  public void testUntrackedCycle () {
    args[args.length-1]=  "testUntrackedCycle";
    noPropertyViolation(args);
  }

  @Test
  public void testStaticNonStatic () {
    args[args.length-1]=  "testStaticNonStatic";
    noPropertyViolation(args);
  }

  @Test
  public void testInitializedObjectFirst () {
    args[args.length-1]=  "testInitializedObjectFirst";
    noPropertyViolation(args);
  }

  @Test
  public void testInitializedObjects () {
    args[args.length-1]=  "testInitializedObjects";
    noPropertyViolation(args);
  }

  @Test
  public void testInitializedObjectLast () {
    args[args.length-1]=  "testInitializedObjectLast";
    noPropertyViolation(args);
  }

  @Test
  public void testInheritedUntrackedFields () {
    args[args.length-1]=  "testInheritedUntrackedFields";
    noPropertyViolation(args);
  }

  @Test
  public void testUsingUntrackedFieldInSameClass () {
    args[args.length-1]=  "testUsingUntrackedFieldInSameClass";
    noPropertyViolation(args);
  }
}
