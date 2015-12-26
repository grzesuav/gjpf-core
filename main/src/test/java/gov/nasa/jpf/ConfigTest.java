/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package gov.nasa.jpf;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Matcher;

import static junitparams.JUnitParamsRunner.$;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * unit test for Config
 */
@RunWith(JUnitParamsRunner.class)
public class ConfigTest {
  private static final String EXAMPLE_MESSAGE = "exampleMessage";

  @Rule
  public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  private final String _configTestAppJPF = "configTestApp.jpf";

  private final String _configTestIncludesJPF = "configTestIncludes.jpf";

  private final String _configTestRequiresFailJPF = "configTestRequiresFail.jpf";

  private final String _configTestRequiresJPF = "configTestRequires.jpf";

  private final String _configTestSite = "configTestSite.properties";

  private final String _configurationDirectory = "src/test/resources/config/";

  @Test
  public void doNotLoadPropertyWhenRequiredPropertyIsNotSet(){
    Config conf = givenConfigurationWithPropertiesAndAppProperties(_configTestRequiresFailJPF);

    String propertyValue = conf.getString("whoa");

    assertThat(propertyValue).isNull();
  }

  @Test
  public void loadCorrectlyWhenRequiredPropertyIsSet(){
    Config conf = givenConfigurationWithPropertiesAndAppProperties(_configTestRequiresJPF);

    String propertyValue = conf.getString("whoa");

    assertThat(propertyValue).isEqualTo("boa");
  }

  @Test
  public void retrievePropertyArrayValues(){
    String dir = _configurationDirectory;
    String[] args = { "+site=" + dir + _configTestSite,
        "+arr=-42,0xff,0" };

    Config conf = givenConfigurationWithArguments(args);
    int[] a = conf.getIntArray("arr");

    assertThat(a).containsExactly(-42, 0xff, 0);
  }

  @Test
  public void shouldExpandPropertyKey(){
    Config conf = givenConfigurationWithAppProperties(_configTestAppJPF);
    String expandedKey = "mySUT.location";

    String propertyValue = conf.getString(expandedKey);

    assertThat(propertyValue).endsWith(expectedLocation());
  }

  @Test
  public void shouldHaveNonEmptyBootClasspath(){
    String dir = _configurationDirectory;
    String[] args = { "+site=" + dir + _configTestSite,
        "+app=" + dir + _configTestAppJPF };

    Config conf = givenConfigurationWithArguments(args);
    // those properties are very weak!
    String[] bootClasspath = conf.asStringArray("boot_classpath");

    assertThat(bootClasspath).isNotEmpty();
  }

  @Test
  public void shouldHaveNonEmptyNativeClasspath(){
    String dir = _configurationDirectory;
    String[] args = { "+site=" + dir + _configTestSite,
        "+app=" + dir + _configTestAppJPF };

    Config conf = givenConfigurationWithArguments(args);
    // those properties are very weak!
    String[] nativeClasspath = conf.asStringArray("native_classpath");

    assertThat(nativeClasspath).isNotEmpty();
  }

  @Parameters(method = "defaultAppPropertyInit")
  @TestCaseName(value = "For {0} expected value is {1}")
  @Test
  public void shouldInitializeDefaultProperties(String property, String expectedValue){
    Config conf = givenConfigurationWithAppProperties(_configTestAppJPF);

    String propertyValue = conf.getString(property);

    assertThat(propertyValue).isEqualTo(expectedValue);
  }

  @Test
  public void shouldLoadAppPropertiesGivenWithAppKey(){
    String dir = _configurationDirectory;
    String[] args = { "+site=" + dir + _configTestSite, "+app=" + dir + _configTestAppJPF };

    Config conf = givenConfigurationWithArguments(args);
    String target = conf.getString(ConfigConstants.TARGET);

    assertThat(target).isEqualTo("urgh.org.MySystemUnderTest");
  }

  @Test
  public void shouldLoadArgument(){
    String targetClass = "urgh.org.MySystemUnderTest";
    String[] args = { targetClass };
    Config conf = givenConfigurationWithArguments(args);

    String[] freeArgs = conf.getFreeArgs();

    assertThat(freeArgs).containsExactly(targetClass);
  }

  @Test
  public void shouldLoadPropertiesFromIncludedFile(){
    String dir = _configurationDirectory;
    String[] args = { "+site=" + dir + _configTestSite,
        dir + _configTestIncludesJPF };

    Config conf = givenConfigurationWithArguments(args);
    String propertyValue = conf.getString("my.common");

    assertThat(propertyValue).isEqualTo("whatever");
  }

  @Test
  public void shouldOverrideTargetArgs(){

    String dir = _configurationDirectory;
    String[] args = { dir + _configTestAppJPF, "x", "y" };

    Config conf = givenConfigurationWithArguments(args);
    String[] targetArgs = conf.getStringArray(ConfigConstants.TARGET_ARGS);
    String[] freeArgs = conf.getFreeArgs();

    assertThat(targetArgs).containsExactly("a", "b", "c");
    assertThat(freeArgs).containsExactly("x", "y");
  }

  @Test
  public void shouldPrintToSystemOutWhenLoggingEnabled(){
    Config conf = givenConfigurationWithAppProperties(_configTestAppJPF);
    Config.enableLogging(true);

    conf.log(EXAMPLE_MESSAGE);

    assertThat(systemOutRule.getLog()).isEqualToIgnoringWhitespace(EXAMPLE_MESSAGE);
    Config.enableLogging(false);
  }

  @Test
  public void shouldNotPrintToSystemOutWhenLoggingDisabled(){
    Config.enableLogging(false);
    Config conf = givenConfigurationWithAppProperties(_configTestAppJPF);

    conf.log(EXAMPLE_MESSAGE);

    assertThat(systemOutRule.getLog()).isEqualTo("");
  }

  private Object defaultAppPropertyInit(){
    return $(
        $(ConfigConstants.VM_CLASS, "gov.nasa.jpf.vm.SingleProcessVM"),
        $(ConfigConstants.TARGET, "urgh.org.MySystemUnderTest")
    );
  }

  private String expectedLocation(){
    String configurationDirectory = _configurationDirectory.substring(0, _configurationDirectory.length() - 1);
    if (!"/".equals(File.separator)) {
      configurationDirectory = configurationDirectory
          .replaceAll("/", Matcher.quoteReplacement(File.separator));  // On UNIX Config returns / and on Windows Config returns \\
    }
    return configurationDirectory;
  }

  private Config givenConfigurationWithAppProperties(String configurationName){
    return givenConfigurationWithArguments(new String[] { _configurationDirectory + configurationName });
  }

  private Config givenConfigurationWithArguments(final String[] args){
    return new Config(args);
  }

  private Config givenConfigurationWithPropertiesAndAppProperties(String configurationName){
    String[] args = { "+site=" + _configurationDirectory + _configTestSite, _configurationDirectory + configurationName };
    return givenConfigurationWithArguments(args);
  }
}
