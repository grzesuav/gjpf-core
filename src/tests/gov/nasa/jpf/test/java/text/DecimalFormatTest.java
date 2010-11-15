
package gov.nasa.jpf.test.java.text;

import gov.nasa.jpf.util.test.TestJPF;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import org.junit.Test;

/**
 * simple regression test for java.text.DecimalFormat
 */
public class DecimalFormatTest extends TestJPF {

  @Test
  public void testDoubleConversion() {

    if (verifyNoPropertyViolation()) {
      StringBuffer sb = new StringBuffer();
      DecimalFormat dFormat = new DecimalFormat();
      sb = dFormat.format(new Double(42), sb, new FieldPosition(0));
      String output = sb.toString();
      try {
        double d = Double.parseDouble(output);
        assert (d == 42.0) : "parsed value differs: " + output;
      } catch (NumberFormatException e) {
        assert false : "output did not parse " + e;
      }
    }
  }
}
