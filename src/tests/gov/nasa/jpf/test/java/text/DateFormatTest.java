package gov.nasa.jpf.test.java.text;

import gov.nasa.jpf.util.test.TestJPF;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import org.junit.Test;

public class DateFormatTest extends TestJPF {

  @Test
  public void testConversionCycle() {
    if (verifyNoPropertyViolation()) {
      DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
      df.setLenient(true);

      Date d1 = new Date();
      System.out.print("current date is: ");
      System.out.println(d1);

      String s = df.format(d1);
      System.out.print("formatted date is: ");
      System.out.println(s);

      try {
        Date d2 = df.parse(s);

        System.out.print("re-parsed date is: ");
        System.out.println(d2);

        long t1 = d1.getTime(); // in ms
        long t2 = d2.getTime(); // in ms
        long delta = Math.abs(t2 - t1);

        // since we loose the ms in String conversion, d2.after(d1) does not necessarily hold
        // some locales don't format the seconds, to we might loose them in the re-conversion
        assert delta <= 60000 : "difference > 1min";

      } catch (ParseException x) {
        assert false : "output did not parse: " + x;
      }
    }
  }
}
