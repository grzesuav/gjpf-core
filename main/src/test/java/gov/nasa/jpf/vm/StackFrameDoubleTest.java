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

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class StackFrameDoubleTest {

  @Rule
  public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

  private final double _value = Math.PI;

  private StackFrame _frame = new StackFrameTest.StackFrameForTest(2, 10);

  @Test
  public void shouldGetLocalValueObjectAfterPushDouble(){
    StackFrameTest.givenStackWithThreeValues(_frame);
    _frame.pushDouble(_value);

    Object obj_Double = _frame.getLocalValueObject(new LocalVarInfo("testDouble", "D", "D", 0, 0, _frame.getTopPos() - 1));

    softly.assertThat(obj_Double).isInstanceOf(Double.class);
    softly.assertThat((Double)obj_Double).isEqualTo(_value);
  }

  @Test
  public void shouldPushAndPopTheSameDoubleValue(){
    // Push/Pop double value and also  JVMStackFrame.getLocalValueObject
    StackFrameTest.givenStackWithThreeValues(_frame);
    _frame.pushDouble(_value);

    final double result_popLong = _frame.popDouble();

    softly.assertThat(result_popLong).isEqualTo(_value);
    assertRestOfStackIsTheSame();
  }

  private void assertRestOfStackIsTheSame(){
    StackFrameTest.assertThatValueAtPositionIsEqual(0, 3, softly, _frame);
    StackFrameTest.assertThatValueAtPositionIsEqual(1, 2, softly, _frame);
    StackFrameTest.assertThatValueAtPositionIsEqual(2, 1, softly, _frame);
  }
}
