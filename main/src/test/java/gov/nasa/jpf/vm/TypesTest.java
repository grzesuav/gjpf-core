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

package gov.nasa.jpf.vm;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junitparams.JUnitParamsRunner.$;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * unit tests for gov.nasa.jpf.vm.Types
 */
@RunWith(JUnitParamsRunner.class)
public class TypesTest {

  @Test
  @Parameters(method = "signatures")
  @TestCaseName( value = "Signature for {0} is {1}")
  public void shouldGetExpectedSignatureForGivenMethodDeclaration(String methodDeclaration, String expectedSignature){
    assertThat(Types.getSignatureName(methodDeclaration)).isEqualTo(expectedSignature);
  }

  private Object signatures(){
    return $(
        $("int foo(int,java.lang.String)", "foo(ILjava/lang/String;)I"),
        $("double[] what_ever (char[], X )", "what_ever([CLX;)[D"),
        $("bar()", "bar()")
    );
  }

  //... and many more to come
}
