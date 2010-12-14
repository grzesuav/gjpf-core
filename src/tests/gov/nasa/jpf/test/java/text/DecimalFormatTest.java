
package gov.nasa.jpf.test.java.text;

import gov.nasa.jpf.util.test.TestJPF;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
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

  @Test
  public void testIsParseIntegerOnly () {
    if (verifyNoPropertyViolation()) {
      DecimalFormat dFormat = new DecimalFormat();
      assertFalse(dFormat.isParseIntegerOnly());
      dFormat.setParseIntegerOnly(true);
      assertTrue(dFormat.isParseIntegerOnly());
      dFormat.setParseIntegerOnly(false);
      assertFalse(dFormat.isParseIntegerOnly());
      NumberFormat format = NumberFormat.getIntegerInstance();
      assertTrue(format.isParseIntegerOnly());
      format = NumberFormat.getNumberInstance();
      assertFalse(format.isParseIntegerOnly());
    }
  }

  @Test
  public void testIsGroupingUsed() {
    if (verifyNoPropertyViolation()) {
      DecimalFormat dFormat = new DecimalFormat();
      assertTrue(dFormat.isGroupingUsed());
      dFormat.setGroupingUsed(false);
      assertFalse(dFormat.isGroupingUsed());
      dFormat.setGroupingUsed(true);
      assertTrue(dFormat.isGroupingUsed());
    }
  }

  @Test
  public void testSetGroupingUsed() {

    if (verifyNoPropertyViolation()) {
      DecimalFormat dFormat = new DecimalFormat();
      String s = dFormat.format(4200000L);
      assertTrue(s.length() == 9);
      dFormat.setGroupingUsed(false);
      s = dFormat.format(4200000L);
      assertTrue(s.equals("4200000"));
      dFormat.setGroupingUsed(true);
      s = dFormat.format(4200000L);
      assertTrue(s.length() == 9);
    }
  }
}
