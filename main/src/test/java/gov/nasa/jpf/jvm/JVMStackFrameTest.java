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

import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.VM;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


/**
 * unit test for StackFrame operations
 */
public class JVMStackFrameTest {

  @Test
  public void shouldCreateStackWithSlotsBigEnoughForLocalVariableAndOperands()
  {
    int locals = 4;
    int operands = 3;
    JVMStackFrame frame = new JVMStackFrame(locals, operands);

    assertThat(frame.getSlots()).hasSize(locals + operands);
  }

  @RunWith(PowerMockRunner.class)
  @PrepareForTest(VM.class)
  public static class SetExceptionReferenceTest {

    private final int _exceptionReference = 1;

    @Mock
    private SystemState _systemState;

    @Mock
    private VM _vm;

    private JVMStackFrame jvmStackFrame = new JVMStackFrame(0, 10);

    @Before
    public void setUp() {
      MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldClearOperandStack() {
      setupVMClass();
      givenNonEmptyStackFrame();

      jvmStackFrame.setExceptionReference(_exceptionReference);

      assertThat(jvmStackFrame.getSlotAttr(0)).isNull();
    }

    @Test
    public void shouldPutExceptionReferenceOnStack() {
      setupVMClass();
      givenNonEmptyStackFrame();

      jvmStackFrame.setExceptionReference(_exceptionReference);

      assertThat(jvmStackFrame.getSlot(0)).isEqualTo(_exceptionReference);
    }

    private void givenNonEmptyStackFrame() {
      jvmStackFrame.push(10);
      jvmStackFrame.setOperandAttr(new Object());
    }

    private void setupVMClass() {
      mockStatic(VM.class);
      given(VM.getVM()).willReturn(_vm);
      given(_vm.getSystemState()).willReturn(_systemState);
    }
  }

}
