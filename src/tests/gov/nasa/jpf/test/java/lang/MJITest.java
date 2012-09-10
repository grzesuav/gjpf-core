package gov.nasa.jpf.test.java.lang;

import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

public class MJITest extends TestJPF {


		@Test
		public void testConstructor() {
			if (verifyNoPropertyViolation()) {
				int[]codePoints=new int[]{1};
				String s=new String(codePoints,0,0);
				
				s=new String(new char []{'h','i','t','s'}, 1,2);
				String t=new String(new char []{'h','i','t','s'}, 0,2);
				System.out.println(s+t);
			}
		}
}
