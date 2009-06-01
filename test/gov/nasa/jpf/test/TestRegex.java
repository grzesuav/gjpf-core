package gov.nasa.jpf.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRegex {
	public static void main(String args[]) {
		// this is simpy a test to see if the native calls
		// to java.util.regex.Matcher work.
	
		String text = "I get knocked down";
		Matcher matcher = Pattern.compile("\\S+").matcher(text);
		
		if(matcher.find()) {
			matcher.end();
		}
		
	}
}
