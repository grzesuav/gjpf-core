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

package gov.nasa.jpf.jvm;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassParseException;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpf.jvm.MethodInfoTest.ToStringInstructionExtractor.asString;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junitparams.JUnitParamsRunner.$;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * unit test for MethodInfos
 */
@RunWith(JUnitParamsRunner.class)
public class MethodInfoTest {

  @Rule
  public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

  private final String[] line__44__instructions = new String[] { "iconst_0", "istore_3", "iload_3", "iload_2", "if_icmpge 17", "iinc", "goto 2" };

  private final String[] line__46__instructions = new String[] { "iload_1", "iload_1", "iadd", "istore_1" };

  private final String[] line__48__instructions = new String[] { "iload_1", "ireturn" };

  @Test
  @Parameters(method = "methodAndArguments")
  @TestCaseName(value = "method: {0}{1} should have arguments: {2}")
  public void methodHaveGivenArguments(String methodName, String methodSignature, List<String> expectedArguments)
      throws ClassParseException{
    ClassInfo classInfo = givenClassInfo();
    MethodInfo method = classInfo.getMethod(methodName, methodSignature, false);

    LocalVarInfo[] argumentLocalVars = method.getArgumentLocalVars();

    assertThat(argumentLocalVars).isNotNull().extracting("name").containsExactly(expectedArguments.toArray());
  }

  @Test
  public void shouldGetInstructionsForEveryLineInMethod()
      throws ClassParseException{
    MethodInfo methodInfo = givenMethodInfo();

    softly.assertThat(methodInfo.getInstructionsForLine(44)).extracting(asString()).containsExactly(line__44__instructions);
    softly.assertThat(methodInfo.getInstructionsForLine(46)).extracting("mnemonic").containsExactly(line__46__instructions);
    softly.assertThat(methodInfo.getInstructionsForLine(48)).extracting("mnemonic").containsExactly(line__48__instructions);
  }

  @Test
  public void shouldGetInstructionsForGivenLineRangeInMethod()
      throws ClassParseException{
    MethodInfo methodInfo = givenMethodInfo();
    String[] line__46_68__instructions = concat(line__46__instructions, line__48__instructions);

    softly.assertThat(methodInfo.getInstructionsForLineInterval(46, 48)).extracting("mnemonic").containsExactly(line__46_68__instructions);
  }

  private String[] concat(final String[] first, final String[] second){
    List<String> concatenated = new ArrayList<>(first.length + second.length);
    concatenated.addAll(asList(first));
    concatenated.addAll(asList(second));
    return concatenated.toArray(new String[concatenated.size()]);
  }

  private ClassInfo givenClassInfo()
      throws ClassParseException{
    File file = givenExampleClass();
    return new NonResolvedClassInfo("gov.nasa.jpf.jvm.MethodInfoSampleClass", file);
  }

  private File givenExampleClass(){
    return new File(
        "build/resources/test/gov/nasa/jpf/jvm/MethodInfoSampleClass.class");
  }

  private MethodInfo givenMethodInfo()
      throws ClassParseException{
    ClassInfo classInfo = givenClassInfo();
    return classInfo.getMethod("instanceCycleMethod", "(II)I", false);
  }

  private Object methodAndArguments(){
    return $(
        $("staticNoArgs", "()D", emptyList()),
        $("staticInt", "(I)D", singletonList("intArg")),
        $("staticIntString", "(ILjava/lang/String;)D", asList("intArg", "stringArg")),
        $("instanceNoArgs", "()D", singletonList("this")),
        $("instanceInt", "(I)D", asList("this", "intArg")),
        $("instanceIntString", "(ILjava/lang/String;)D", asList("this", "intArg", "stringArg"))
    );
  }

  static class ToStringInstructionExtractor
      implements Extractor<Instruction, String> {

    private ToStringInstructionExtractor(){
      // to prevent insantiation
    }

    @Override
    public String extract(final Instruction input){
      return input.toString();
    }

    static ToStringInstructionExtractor asString(){
      return new ToStringInstructionExtractor();
    }
  }

}
