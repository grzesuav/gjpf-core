package gov.nasa.jpf.vm;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StackFrameTest {
  private StackFrame _frame;

  @Test
  public void cloneShouldReturnDeepCopyOfStack(){
    givenStackWithThreeValuesAndAttributes(_frame);

    StackFrame clone = _frame.clone();

    assertThat(clone).isEqualToComparingFieldByField(_frame);
  }

  @Before
  public void setup(){
    _frame = new StackFrameForTest(0, 10);
  }

  @Test
  public void shouldReplaceOperandAttributeAtGivenPosition(){
    givenStackWithThreeValuesAndAttributes(_frame);

    _frame.replaceOperandAttr(0, "3", "1");

    assertThat(_frame.getOperandAttr(0)).isEqualTo("1");
  }

  @Test
  public void shouldNotReplaceOperandAttributeAtGivenPositionWhenOldValueDoesNotMatch(){
    givenStackWithThreeValuesAndAttributes(_frame);
    String originalValue = "3";
    String newAttribute = "1";
    String incorrectOldAttribute = "2";

    _frame.replaceOperandAttr(0, incorrectOldAttribute, newAttribute);

    assertThat(_frame.getOperandAttr(0)).isEqualTo(originalValue);
  }

  @Test
  public void shouldReturnNullWhenOperandAttributeDoesNotExist(){
    givenStackWithThreeValues(_frame);

    Object operandAttr = _frame.getOperandAttr(0);

    assertThat(operandAttr).isNull();
  }

  @Test
  public void shouldReturnNullWhenOperandAttributeOfClassDoesNotExist(){
    givenStackWithThreeValues(_frame);

    Object operandAttr = _frame.getOperandAttr(0, String.class);

    assertThat(operandAttr).isNull();
  }

  @Test
  public void shouldReturnNullWhenOperandAttributeOfClassIsAnotherClass(){
    givenStackWithThreeValues(_frame);

    Object operandAttr = _frame.getOperandAttr(0, Integer.class);

    assertThat(operandAttr).isNull();
  }

  @Test
  public void shouldReturnOperandAttributeOfClassWhenExist(){
    givenStackWithThreeValuesAndAttributes(_frame);

    Object operandAttr = _frame.getOperandAttr(0, String.class);

    assertThat(operandAttr).isEqualTo("3");
  }

  @Test
  public void shouldReturnOperandAttributeWhenExist(){
    givenStackWithThreeValuesAndAttributes(_frame);

    Object operandAttr = _frame.getOperandAttr(0);

    assertThat(operandAttr).isEqualTo("3");
  }

  static void assertGivenStackValues(Object[][] values, final JUnitSoftAssertions softly, final StackFrame frame){
    for (Object[] stackValues : values) {
      int position = (int)stackValues[0];
      int value = (int)stackValues[1];
      assertThatValueAtPositionIsEqual(position, value, softly, frame);
    }
  }

  static void assertGivenStackValuesWithAttributes(Object[][] values, final JUnitSoftAssertions softly, final StackFrame frame){
    for (Object[] stackValues : values) {
      int position = (int)stackValues[0];
      int value = (int)stackValues[1];
      String attribute = (String)stackValues[2];
      assertThatValueAtPositionHave(position, value, attribute, softly, frame);
    }
  }

  static void assertThatValueAtPositionHave(int position, int value, String attribute, final JUnitSoftAssertions softly, final StackFrame frame){
    assertThatValueAtPositionIsEqual(position, value, softly, frame);
    softly.assertThat(frame.getOperandAttr(position)).isEqualTo(attribute); // same const pool string
  }

  static void assertThatValueAtPositionIsEqual(int position, int value, final JUnitSoftAssertions softly, final StackFrame frame){
    softly.assertThat(frame.peek(position)).isEqualTo(value);
  }

  static void givenStackWithThreeValues(final StackFrame frame){
    frame.push(1);
    frame.push(2);
    frame.push(3);
  }

  static void givenStackWithThreeValuesAndAttributes(final StackFrame frame){
    frame.push(1);
    frame.setOperandAttr("1");
    frame.push(2);
    frame.setOperandAttr("2");
    frame.push(3);
    frame.setOperandAttr("3");
  }

  public static class Dup2_x1Test {
    @Rule
    public final JUnitSoftAssertions _softly = new JUnitSoftAssertions();

    private StackFrame _frame;

    @Before
    public void setup(){
      _frame = new StackFrameForTest(0, 10);
    }

    @Test
    @Ignore(value = "Github issue #11")
    public void shouldDuplicateTopOperandValueAndInsertTwoValuesDown(){
      givenStackWithTwoValues();

      _frame.dup2_x1();

      assertThatStackHaveThreeValues();
      assertThreeValuesOnStackInRightOrder();
    }

    @Test
    public void shouldDuplicateTwoTopOperandValuesAndInsertThreeValuesDown(){
      // 1 2 3  => 2 3.1 2 3
      givenStackWithThreeValues(_frame);

      _frame.dup2_x1();

      assertThatStackHaveFiveValues();
      assertFiveValuesOnStackInRightOrder();
    }

    @Test
    public void shouldDuplicateTwoTopOperandValuesWithAttributesAndInsertThreeValuesDown(){
      // 1 2 3  => 2 3.1 2 3
      givenStackWithThreeValuesAndAttributes(_frame);

      _frame.dup2_x1();

      assertThatStackHaveFiveValues();
      assertThatFiveValuesOnStackWithAttributesInRightOrder();
    }

    private void assertFiveValuesOnStackInRightOrder(){
      assertGivenStackValues(new Object[][]
          {// position | value
              { 4, 2 },
              { 3, 3 },
              { 2, 1 },
              { 1, 2 },
              { 0, 3 }
          }, _softly, _frame);
    }

    private void assertThatFiveValuesOnStackWithAttributesInRightOrder(){
      assertGivenStackValuesWithAttributes(new Object[][]
          {// position | value | attribute
              { 4, 2, "2" },
              { 3, 3, "3" },
              { 2, 1, "1" },
              { 1, 2, "2" },
              { 0, 3, "3" }
          }, _softly, _frame
      );
    }

    private void assertThatStackHaveFiveValues(){
      _softly.assertThat(_frame.getTopPos()).isEqualTo(4);
    }

    private void assertThatStackHaveThreeValues(){
      _softly.assertThat(_frame.getTopPos()).isEqualTo(2);
    }

    private void assertThreeValuesOnStackInRightOrder(){
      assertGivenStackValues(new Object[][]
          {// position | value
              { 2, 1 },
              { 1, 2 },
              { 0, 1 }
          }, _softly, _frame);
    }

    private void givenStackWithTwoValues(){
      _frame.push(1);
      _frame.push(2);
    }
  }

  public static class Dup2_x2Test {

    @Rule
    public final JUnitSoftAssertions _softly = new JUnitSoftAssertions();

    private StackFrame _frame;

    @Before
    public void setup(){
      _frame = new StackFrameForTest(0, 10);
    }

    @Test
    public void shouldDuplicateTwoTopOperandValuesAndInsertFourValuesDown(){
      // 1 2 3 4  => 3 4.1 2 3 4
      givenStackWithFourValues();

      _frame.dup2_x2();

      assertThatStackHaveSixValues();
      assertThatSixValuesOnStackInRightOrder();
    }

    @Test
    public void shouldDuplicateTwoTopOperandValuesWithAttributesAndInsertFourValuesDown(){
      // 1 2 3 4  => 3 4.1 2 3 4
      givenStackWithFourValuesAndAttributes();

      _frame.dup2_x2();

      assertThatStackHaveSixValues();
      assertStackHaveSixValuesWithAttributesInRightOrder();
    }

    private void assertStackHaveSixValuesWithAttributesInRightOrder(){
      assertGivenStackValuesWithAttributes(new Object[][]
          {
              { 5, 3, "3" },
              { 4, 4, "4" },
              { 3, 1, "1" },
              { 2, 2, "2" },
              { 1, 3, "3" },
              { 0, 4, "4" }
          }, _softly, _frame);
    }

    private void assertThatSixValuesOnStackInRightOrder(){
      assertGivenStackValues(new Object[][]
          {// position | value
              { 5, 3 },
              { 4, 4 },
              { 3, 1 },
              { 2, 2 },
              { 1, 3 },
              { 0, 4 }
          }, _softly, _frame);
    }

    private void assertThatStackHaveSixValues(){
      assert _frame.getTopPos() == 5;
    }

    private void givenStackWithFourValues(){
      _frame.push(1);
      _frame.push(2);
      _frame.push(3);
      _frame.push(4);
    }

    private void givenStackWithFourValuesAndAttributes(){
      _frame.push(1);
      _frame.setOperandAttr("1");
      _frame.push(2);
      _frame.setOperandAttr("2");
      _frame.push(3);
      _frame.setOperandAttr("3");
      _frame.push(4);
      _frame.setOperandAttr("4");
    }
  }

  static final class StackFrameForTest
      extends StackFrame {

    StackFrameForTest(final int locals, final int operands){
      super(locals, operands);
    }

    @Override
    public void setArgumentLocal(final int idx, final int value, final Object attr){
      //test dummy
    }

    @Override
    public void setLongArgumentLocal(final int idx, final long value, final Object attr){
      //test dummy
    }

    @Override
    public void setReferenceArgumentLocal(final int idx, final int ref, final Object attr){
      //test dummy
    }
  }
}