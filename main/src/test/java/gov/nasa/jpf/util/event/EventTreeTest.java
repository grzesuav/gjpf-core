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

package gov.nasa.jpf.util.event;

import org.junit.Test;

import java.util.stream.Stream;

import static gov.nasa.jpf.util.event.DefaultEventFactory.create;
import static gov.nasa.jpf.util.event.EventTreeAssert.assertThat;

/**
 * regression test for EventTree
 */
public class EventTreeTest {

  private static EventConstructor eventFactory = create();

  @Test
  public void shouldGenerateTwoPathsFromSequenceOfAlternatives() {
    EventTree treeWithAlternatives = treeOf(
        sequenceOf(
            event("a"),
            alternativeOf(
                event("1"),
                iterationOf(2, event("x"))
            ),
            event("b")
        ));

    assertThat(treeWithAlternatives).generatesPaths("a1b", "axxb");
  }

  @Test
  public void shouldGenerateCombinationOfFourElements() {
    EventTree combinationOfFourElements = treeOf(
        combinationOf(
            event("a"),
            event("b"),
            event("c"),
            event("d"))
    );

    String[] expected = {
        "",
        "a",
        "b",
        "ab",
        "c",
        "ac",
        "bc",
        "abc",
        "d",
        "ad",
        "bd",
        "abd",
        "cd",
        "acd",
        "bcd",
        "abcd"
    };

    assertThat(combinationOfFourElements).generatesPaths(expected);
  }

  @Test
  public void shouldGenerateFourPathsFromCombinationOfTwoLetters() {
    EventTree combinationOfTwoElements = treeOf(
        combinationOf(
            event("a"),
            event("b")
        )
    );

    assertThat(combinationOfTwoElements).generatesPaths(
        "a",
        "b",
        "ab",
        ""
    );
  }

  @Test
  public void shouldGenerateCombinationOfNestedEvents() {
    EventTree treeWithNestedCombinations = treeOf(sequenceOf(
        event("1"),
        combinationOf(
            event("a"),
            event("b")),
        event("2")));

    assertThat(treeWithNestedCombinations).generatesPaths(
        "12",
        "1ab2",
        "1a2",
        "1b2"
    );

  }

  @Test
  public void shouldHaveMaxDepthOfFive() {
    EventTree treeOfDepthFive = treeOf(
        sequenceOf(
            event("a"),
            alternativeOf(
                event("1"),
                sequenceOf(
                    event("X"),
                    event("Y")
                ),
                iterationOf(3,
                    event("r")
                )
            ),
            event("b")
        ));

    assertThat(treeOfDepthFive).hasMaxDepthOf(5);
  }

  @Test
  public void shouldGenerateSixPathsFromPermutationOfThreeLetters() {
    EventTree permutationOfThreeElements = treeOf(
        permutationOf(
            event("a"),
            event("b"),
            event("c")
        ));

    String[] expected = {
        "abc",
        "acb",
        "bac",
        "bca",
        "cab",
        "cba"
    };

    assertThat(permutationOfThreeElements).generatesPaths(expected);
  }

  ;

  @Test
  public void shouldGeneratePathsWithAddedOne() {
    EventTree baseTree = treeOf(
        sequenceOf("a", "b", "c")
    );

    baseTree.addPath(
        event("a"),
        event("b"),
        event("3"));

    assertThat(baseTree).generatesPaths("abc", "ab3");
  }

  @Test
  public void shouldGenerateInterleaveOfTwoSequences() {
    EventTree threeLetters = treeOf(
        sequenceOf(
            event("a"),
            event("b"),
            event("c")
        ));
    EventTree threeNumbers = treeOf(
        sequenceOf(
            event("1"),
            event("2"),
            event("3")
        ));

    EventTree interleave = threeLetters.interleave(threeNumbers);

    String[] expected = {
        "a123bc",
        "a12b3c",
        "a12bc3",
        "a1b23c",
        "a1b2c3",
        "a1bc23",
        "ab123c",
        "ab12c3",
        "ab1c23",
        "abc123",
        "123abc",
        "12a3bc",
        "12ab3c",
        "12abc3",
        "1a23bc",
        "1a2b3c",
        "1a2bc3",
        "1ab23c",
        "1ab2c3",
        "1abc23"
    };

    assertThat(interleave).generatesPaths(expected);
  }

  @Test
  public void shouldGenerateInterleavesOfTwoSequencesOfLengthTwo() {
    EventTree twoLetters = treeOf(
        sequenceOf(
            event("a"),
            event("b")
        ));
    EventTree twoNumbers = treeOf(
        sequenceOf(
            event("1"),
            event("2")
        ));

    EventTree interleave = twoLetters.interleave(twoNumbers);

    assertThat(interleave).generatesPaths(
        "12ab",
        "1a2b",
        "1ab2",
        "a12b",
        "a1b2",
        "ab12"
    );
  }

  @Test
  public void shouldRemovePathsFromSecondSource() {
    EventTree twoLetters = treeOf(
        sequenceOf(
            event("a"),
            event("b")
        ));
    Object source = new Object();
    EventTree twoNumbers = treeOf(
        sequenceOf(
            new Event("1", source),
            new Event("2", source)
        ));

    EventTree interleaveOfLetterAndNumbers = twoLetters.interleave(twoNumbers);
    EventTree interleaveWithoutNumbers = new EventTree(interleaveOfLetterAndNumbers.removeSource(source));

    assertThat(interleaveWithoutNumbers).generatesPaths("ab");
  }

  private static Event sequenceOf(String... sequence) {
    return sequenceOf(Stream.of(sequence).map(Event::new).toArray(Event[]::new));
  }

  private static Event event(String event) {
    return new Event(event);
  }

  private static EventTree treeOf(Event root) {
    return new EventTree(root);
  }

  private static Event sequenceOf(Event... events) {
    return eventFactory.sequence(events);
  }

  private static Event permutationOf(Event... events) {
    return eventFactory.anyPermutation(events);
  }

  private static Event combinationOf(Event... events) {
    return eventFactory.anyCombination(events);
  }

  private static Event alternativeOf(Event... events) {
    return eventFactory.alternatives(events);
  }

  private static Event iterationOf(int number, Event event) {
    return eventFactory.iteration(number, event);
  }

}
