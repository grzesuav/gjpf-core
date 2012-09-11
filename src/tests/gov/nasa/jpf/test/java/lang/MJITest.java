package gov.nasa.jpf.test.java.lang;

import gov.nasa.jpf.util.test.TestJPF;

import java.nio.charset.Charset;
import java.util.TreeSet;

import org.junit.Test;

public class MJITest extends TestJPF {

	@Test
	public void testMJI(){
		// Just checking MJI wiring
		String x=new String("Hey boo-boo bear, let's get us a pickinic basket!");
		x.codePointAt(0);
		x.codePointBefore(0);
		x.codePointCount(0, 0);
		char dst[]=new char[]{'a','b','c'};
		x.getChars(0,0,dst,0);
		byte dstb[]=new byte[]{1,2,3};
		x.getBytes(0,0,dstb,0);

		Charset d=Charset.defaultCharset();
		String dname=d.name();

		x.getBytes(d);
		x.getBytes();
		
		StringBuffer buf=new StringBuffer();
		x.contentEquals(buf);
		x.contentEquals(x);
		
		TreeSet<String> compareSet=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		compareSet.add(x);
		compareSet.add(x+"I don't know Yogi");
		
	}

}
