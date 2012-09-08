package gov.stringTest;

import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

public class StringPrototypeTest  extends TestJPF {
	  @Test
	  public void testDump() {
	    if (verifyNoPropertyViolation()) {
	      boolean c1 = Verify.getBoolean(); // to do some state storing / backtracking
	      String a = "Blah".intern();
	      String b = new String("Blah");

	      assert (a != b) : "'new String(intern) != intern' failed";

	      boolean c2 = Verify.getBoolean(); // to do some more storing / backtracking

	      String c = b.intern();

	      assert (a == c) : "'(new String(intern)).intern() == intern' failed";
	      StringPrototype.dumpStatistics();
	    }
	  }

}
