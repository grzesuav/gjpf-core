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
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * unit test for ClassInfo initialization
 */
public class ClassInfoTest {

  @Rule
  public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

  private final String CLASS_NAME = "gov.nasa.jpf.jvm.ClassInfoSampleClass";

  @Test
  public void shouldInitializeExampleClassProperly()
      throws ClassParseException{
    File exampleClass = givenExampleClass();

    ClassInfo classInfo = whenInitializeClass(exampleClass);

    softly.assertThat(classInfo.getName()).isEqualTo(CLASS_NAME);
    assertDeclaredInstanceFields(classInfo);
    assertDeclaredStaticFields(classInfo);
    assertDelcaredMethods(classInfo);
  }

  @Test
  public void shouldImplementCloneable()
      throws ClassParseException{
    File exampleClass = givenExampleClass();

    ClassInfo classInfo = whenInitializeClass(exampleClass);

    assertThat(classInfo.getDirectInterfaceNames()).containsExactly(Cloneable.class.getName());
  }
  @Test
  public void shouldHaveCorrectSourceFileName()
      throws ClassParseException{
    File exampleClass = givenExampleClass();

    ClassInfo classInfo = whenInitializeClass(exampleClass);

    assertThat(classInfo.getSourceFileName()).isEqualTo("gov/nasa/jpf/jvm/ClassInfoSampleClass.java");
  }

  private void assertDeclaredInstanceFields(final ClassInfo classInfo){
    softly.assertThat(classInfo.getNumberOfDeclaredInstanceFields()).isEqualTo(1);
    softly.assertThat(classInfo.getDeclaredInstanceFields()).extracting("type").containsExactly("java.lang.String");
    softly.assertThat(classInfo.getDeclaredInstanceFields()).extracting("name").containsExactly("s");
  }

  private void assertDeclaredStaticFields(final ClassInfo classInfo){
    softly.assertThat(classInfo.getNumberOfStaticFields()).isEqualTo(1);
    softly.assertThat(classInfo.getDeclaredStaticFields()).extracting("type").containsExactly("int");
    softly.assertThat(classInfo.getDeclaredStaticFields()).extracting("name").containsExactly("D");
  }

  private void assertDelcaredMethods(final ClassInfo classInfo){
    softly.assertThat(classInfo.getDeclaredMethodInfos()).extracting("uniqueName").containsExactly(
        "<init>(Ljava/lang/String;)V",
        "whatIsIt()I",
        "isItTheAnswer(ZILjava/lang/String;)Z",
        "foo()V",
        "getString()Ljava/lang/String;"
    );
  }

  private File givenExampleClass()
      throws ClassParseException{
    return new File("build/resources/test/gov/nasa/jpf/jvm/ClassInfoSampleClass.class");
  }

  private ClassInfo whenInitializeClass(File file)
      throws ClassParseException{
    return new NonResolvedClassInfo("gov.nasa.jpf.jvm.ClassInfoSampleClass", file);
  }

}
