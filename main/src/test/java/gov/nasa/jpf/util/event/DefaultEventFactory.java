package gov.nasa.jpf.util.event;

/**
 * Created by grzesiek on 22.08.16.
 */
public class DefaultEventFactory implements EventConstructor {

  static EventConstructor create() {
    return new DefaultEventFactory();
  }
}
