

package gov.nasa.jpf.test.unit;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;


/**
 * unit test for Config
 */
public class ConfigTest extends TestJPF {

  public static void main (String[] args){
    runTestsOfThisClass(args);
  }

  @Test
  public void testDefaultAppPropertyInit () {

    String dir = "src/tests/gov/nasa/jpf/test/unit/";
    String[] args = {dir + "configTestApp.jpf"};

    Config conf = new Config(args, Config.class);

    String val = conf.getString("vm.class");
    assert "gov.nasa.jpf.jvm.JVM".equals(val);

    val = conf.getTarget(); // from configTest.jpf
    assert "urgh.org.MySystemUnderTest".equals(val);
  }

  @Test
  public void testDefaultExplicitTargetInit ()  {
    String[] args = {"urgh.org.MySystemUnderTest"};

    Config conf = new Config( args, Config.class);
    assert "urgh.org.MySystemUnderTest".equals(conf.getTarget());
  }

  @Test
  public void testExplicitLocations () {
    String dir = "src/tests/gov/nasa/jpf/test/unit/";
    String[] args = {"+jpf.default=" + dir + "configTestDefault.properties",
                     "+jpf.site=" + dir + "configTestSite.properties",
                     "+jpf.app=" + dir + "configTestApp.jpf" };

    Config conf = new Config( args, Config.class);
    conf.printEntries();

    assert "urgh.org.MySystemUnderTest".equals(conf.getTarget());
  }

  @Test
  public void testTargetArgsOverride () {

    String dir = "src/tests/gov/nasa/jpf/test/unit/";
    String[] args = {"+jpf.default=" + dir + "configTestDefault.properties",
                      dir + "configTestApp.jpf",
                      "x", "y"};

    Config conf = new Config(args, Config.class);
    conf.printEntries();

    String[] ta = conf.getTargetArgs();
    assert ta.length == 2;
    assert "x".equals(ta[0]);
    assert "y".equals(ta[1]);
  }

  @Test
  public void testExtensions () {
    String dir = "src/tests/gov/nasa/jpf/test/unit/";
    String[] args = {"+jpf.default=" + dir + "configTestDefault.properties",
                     "+jpf.site=" + dir + "configTestSite.properties",
                     "+jpf.app=" + dir + "configTestApp.jpf" };

    Config conf = new Config( args, Config.class);
    conf.setExtensionProperties();

    conf.printEntries();

    // add a property check
  }

  @Test
  public void testCore () {
    String dir = "src/tests/gov/nasa/jpf/test/unit/";
    String[] args = {"+jpf.default=" + dir + "configTestDefault.properties",
                     "+jpf.site=" + dir + "configTestSite.properties",
                     "+jpf.app=" + dir + "configTestApp.jpf" };

    Config conf = new Config( args, Config.class);
    conf.setCoreProperties();

    conf.printEntries();

    // add a property check
  }

}
