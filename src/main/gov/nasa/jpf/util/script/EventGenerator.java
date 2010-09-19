package gov.nasa.jpf.util.script;

import gov.nasa.jpf.jvm.ChoiceGenerator;

/**
 * abstract ChoiceGenerator root for Event based generators
 */
public abstract class EventGenerator<T> extends ChoiceGenerator<T> {

  protected EventGenerator (String id){
    super(id);
  }
}
