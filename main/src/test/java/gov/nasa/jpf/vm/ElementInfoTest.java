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

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import gov.nasa.jpf.JPFException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.googlecode.catchexception.CatchException.catchException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * unit test for ElementInfos
 */
@RunWith(HierarchicalContextRunner.class)
public class ElementInfoTest {

  private ElementInfo info = new ElementInfoStub();

  public class AttributesTest {

    public class Alive {

      @Test
      public void shouldBeAliveAfterSetAliveToTrue(){
        info.setAlive(true);

        assertThat(info.isAlive(true)).isTrue();
        assertThat(info.isAlive(false)).isFalse();
      }

      @Test
      public void shouldNotBeAliveAfterSetAliveToFalse(){
        info.setAlive(false);

        assertThat(info.isAlive(true)).isFalse();
        assertThat(info.isAlive(false)).isTrue();
      }
    }

    public class Marked {

      @Test
      public void shouldNotBeMarkedAfterCreation(){
        assertThat(info.isMarked()).isFalse();
      }

      @Test
      public void shouldBeMarkedWhenSetAsMarked(){
        info.setMarked();

        assertThat(info.isMarked()).isTrue();
      }

      @Test
      public void shouldBeUnmarkedWhenSetAsUnmarked(){
        info.setMarked();

        info.setUnmarked();

        assertThat(info.isMarked()).isFalse();
      }
    }


    public class MarkedOrAlive {

      @Test
      public void shouldBeMarkedOrAliveWhenAliveAndNotMarked(){
        info.setUnmarked();
        info.setAlive(true);

        assertThat(info.isMarkedOrAlive(true)).isTrue();
        assertThat(info.isMarkedOrAlive(false)).isFalse();
      }

      @Test
      public void shouldNotBeMarkedOrAliveWhenNotAliveAndNotMarked(){
        info.setUnmarked();
        info.setAlive(false);

        assertThat(info.isMarkedOrAlive(false)).isTrue();
        assertThat(info.isMarkedOrAlive(true)).isFalse();
      }

      @Test
      public void shouldBeMarkedOrAliveWhenMarkedAndNotAlive(){
        info.setMarked();
        info.setAlive(false);

        assertThat(info.isMarkedOrAlive(true)).isTrue();
        assertThat(info.isMarkedOrAlive(false)).isTrue();
      }
    }

    public class PinnedDown {
      @Test
      public void shouldNotBePinnedDownAfterCreation(){
        assertThat(info.isPinnedDown()).isFalse();
      }

      @Test
      public void shouldReturnTrueWhenPinnedDownFirstTime(){
        assertThat(info.incPinDown()).isTrue();
      }


      @Test
      public void shouldReturnFalseWhenPinnedDownSecondTime(){
        info.incPinDown();

        assertThat(info.incPinDown()).isFalse();
      }

      @Test(expected = JPFException.class)
      public void shouldThrowExceptionWhenExceedPinDownCounter(){
        for (int i = 0; i < ElementInfo.ATTR_PINDOWN_MASK; i++) {
          info.incPinDown();
        }

        catchException(info.incPinDown());
      }

      @Test
      public void counterShowsAccureNumberOfIncrements(){
        info.incPinDown();
        info.incPinDown();

        assertThat(info.getPinDownCount()).isEqualTo(2);
      }


      @Test
      public void afterDecrementCounterIsLowerByOne(){
        info.incPinDown();
        info.incPinDown();
        final int before = info.getPinDownCount();

        info.decPinDown();

        assertThat(info.getPinDownCount()).isEqualTo(before - 1);
      }

      @Test
      public void returnTrueWhenDecreaseToZero()
      {
        info.incPinDown();

        assertThat(info.decPinDown()).isTrue();
      }

      @Test
      public void returnFalseWhenDecreaseToOne()
      {
        info.incPinDown();
        info.incPinDown();

        assertThat(info.decPinDown()).isFalse();
      }

      @Test
      public void doNotThrowExceptionWhenDecreaseBelowZero()
      {
        assertThat(info.decPinDown()).isFalse();
      }
    }

  }

  class ElementInfoStub
      extends ElementInfo {

    protected ElementInfoStub(final int objectId, final ClassInfo classInfo, final Fields fields, final Monitor monitor, final ThreadInfo ti){
      super(objectId, classInfo, fields, monitor, ti);
    }

    ElementInfoStub(){
      this(-2, null, null, null, mock(ThreadInfo.class));
    }

    @Override
    public FieldInfo getFieldInfo(final int fieldIndex){
      throw new UnsupportedOperationException("Stub");
    }

    @Override
    public ElementInfo getModifiableInstance(){
      throw new UnsupportedOperationException("Stub");
    }

    @Override
    public int getNumberOfFields(){
      throw new UnsupportedOperationException("Stub");
    }

    @Override
    public boolean hasFinalizer(){
      throw new UnsupportedOperationException("Stub");
    }

    @Override
    public boolean isObject(){
      throw new UnsupportedOperationException("Stub");
    }

    @Override
    protected FieldInfo getDeclaredFieldInfo(final String clsBase, final String fname){
      throw new UnsupportedOperationException("Stub");
    }

    @Override
    protected FieldInfo getFieldInfo(final String fname){
      throw new UnsupportedOperationException("Stub");
    }

    @Override
    protected int getNumberOfFieldsOrElements(){
      throw new UnsupportedOperationException("Stub");
    }
  }

}
