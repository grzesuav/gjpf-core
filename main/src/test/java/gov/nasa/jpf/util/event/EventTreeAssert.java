package gov.nasa.jpf.util.event;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;

import static java.lang.String.format;

/**
 * Created by grzesiek on 22.08.16.
 */
public class EventTreeAssert extends AbstractAssert<EventTreeAssert, EventTree> {

  protected EventTreeAssert(EventTree actual) {
    super(actual, EventTreeAssert.class);
  }

  public static EventTreeAssert assertThat(EventTree actual) {
    return new EventTreeAssert(actual);
  }

  public EventTreeAssert generatesPaths(String... expectedPaths) {
    SoftAssertions softly = new SoftAssertions();
    int counter = 0;
    for (Event event : actual.visibleEndEvents()) {
      String actualPath = event.getPathString(null);
      softly.assertThat(actual.checkPath(event, expectedPaths))
          .as(format("Path \"%s\" was present, but is not expected", actualPath))
          .isTrue();
      counter++;
    }
    for (int i = counter; i < expectedPaths.length; i++) {
      softly.assertThat(false)
          .as("Expected path not generated :" + expectedPaths[i])
          .isTrue();
    }
    softly.assertAll();
    return this;
  }

  public EventTreeAssert hasMaxDepthOf(int expectedDepth) {
    Assertions.assertThat(actual.getMaxDepth()).isEqualTo(expectedDepth);
    return this;
  }

}
