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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StackFrameLongTest {

  private final long _long = 0x123456780ABCDEFL;

  private StackFrame _frame = new StackFrameTest.StackFrameForTest(0, 2);

  @Test
  public void shouldGetLocalValueObjectAfterPushLong(){
    _frame.pushLong(_long);

    Object obj_Long = _frame.getLocalValueObject(new LocalVarInfo("testLong", "J", "J", 0, 0, 0));

    assertThat(obj_Long).isInstanceOf(Long.class);
    assertThat((Long)obj_Long).isEqualTo(_long);
  }

  @Test
  public void shouldPushAndPopTheSameLongValue(){
    _frame.pushLong(_long);

    long result_popLong = _frame.popLong();

    assertThat(result_popLong).isEqualTo(_long);
  }
}
