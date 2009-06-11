package gov.nasa.jpf.test.annotation;

import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

public class TestUtilsJPF extends TestJPF {
	@Test
	public void testMatcher() {
		String[] args = { "gov.nasa.jpf.test.TestRegex"};
		noPropertyViolation(args);
	}
}
