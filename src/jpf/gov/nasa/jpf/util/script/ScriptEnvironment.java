//
// Copyright  (C) 2008 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
//  (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
//  (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.util.script;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.StateExtensionClient;
import gov.nasa.jpf.util.StateExtensionListener;

/**
 * class representing a statemachine environment that produces SCEventGenerators
 * from scripts
 */
public abstract class ScriptEnvironment<CG extends ChoiceGenerator<?>> 
         implements StateExtensionClient<ScriptEnvironment<CG>.ActiveSnapshot> {

  static final String DEFAULT = "default";

  //--- just a helper tuple
  static class ActiveSequence implements Cloneable {
    String stateName;
    Section section;
    SequenceInterpreter intrp;

    public ActiveSequence (String stateName, Section section, SequenceInterpreter intrp) {
      this.stateName = stateName;
      this.section = section;
      this.intrp = intrp;
    }

    public Object clone() {
      try {
        ActiveSequence as = (ActiveSequence) super.clone();
        as.intrp = (SequenceInterpreter) intrp.clone();
        return as;
      } catch (CloneNotSupportedException nonsense) {
        return null; // we are a Cloneable, so we don't get here
      }
    }

    public boolean isDone() {
      return intrp.isDone();
    }
  }

  //--- our state extension - we need this mostly for cloning (deep copy)
  class ActiveSnapshot implements Cloneable {
    ActiveSequence[] actives;

    ActiveSnapshot () {
      actives = new ActiveSequence[0];
    }

    ActiveSnapshot (ActiveSequence[] as) {
      actives = as;
    }

    public ActiveSequence get (String stateName) {
      for (ActiveSequence as : actives) {
        if (as.stateName.equals(stateName)) {
          return as;
        }
      }
      return null;
    }

    public Object clone() {
      try {
        ActiveSnapshot ss = (ActiveSnapshot)super.clone();
        for (int i=0; i<actives.length; i++) {
          ActiveSequence as = actives[i];
          ss.actives[i] = (ActiveSequence)as.clone();
        }
        return ss;
      } catch (CloneNotSupportedException nonsense) {
        return null; // we are a Cloneable, so we don't get here
      }
    }

    ActiveSnapshot advance (String[] activeStates, BitSet isReEntered) {
      ActiveSequence[] newActives = new ActiveSequence[activeStates.length];

      //--- carry over the persisting entries
      for (int i=0; i<activeStates.length; i++) {
        String sn = activeStates[i];
        for (ActiveSequence as : actives) {
          if (as.stateName.equals(sn) ) {
            // we could use isReEntered to determine if we want to restart sequences
            // <2do> how do we factor this out as policy?
            newActives[i] = (ActiveSequence)as.clone();
          }
        }
      }

      //--- add the new ones
      int skipped = 0;
      nextActive:
      for (int i=0; i<activeStates.length; i++) {
        if (newActives[i] == null) {
          // get the script section
          Section sec = getSection(activeStates[i]);
          if (sec != null) {

            // check if that section is already processed by another active state, in which case we skip
            for (int j=0; j<newActives.length; j++) {
              if (newActives[j] != null && newActives[j].section == sec) {
                skipped++;
                continue nextActive;
              }
            }

            // check if it was processed by a prev state (superstate section by a
            // common parent of a new and an old state - this is the common case
            for (int j=0; j<actives.length; j++) {
              // <2do> how do we handle state re-entering?
              if (actives[j].section == sec) {
                ActiveSequence as = new ActiveSequence(activeStates[i], sec, actives[j].intrp);
                newActives[i] = as;
                continue nextActive;
              }
            }

            // it's a new one
            ActiveSequence as = new ActiveSequence(activeStates[i], sec,
                                                   new SequenceInterpreter(sec));
            newActives[i] = as;

          } else { // sec == null : we didn't find any sequence for this state
            skipped++;
          }
        }
      }

      //--- compress if we skipped any active states
      if (skipped > 0) {
        int n = activeStates.length - skipped;
        ActiveSequence[] na = new ActiveSequence[n];
        for (int i=0, j=0; j<n; i++) {
          if (newActives[i] != null) {
            na[j++] = newActives[i];
          }
        }
        newActives = na;
      }

      return new ActiveSnapshot(newActives);
    }
  }

  //--- start of ScriptEnvronment

  String scriptName;
  Reader scriptReader;
  Script script;
  ActiveSnapshot cur;

  HashMap<String,Section> sections = new HashMap<String,Section>();
  Section defaultSection;

  //--- initialization
  public ScriptEnvironment (String fname) throws FileNotFoundException {
    this( fname, new FileReader(fname));
  }

  public ScriptEnvironment (String name, Reader r) {
    this.scriptName = name;
    this.scriptReader = r;
  }

  public void parseScript () throws ESParser.Exception {
    ESParser parser= new ESParser(scriptName, scriptReader);
    script = parser.parse();

    initSections();

    cur = new ActiveSnapshot();
  }

  void initSections() {
    Section defSec = new Section(script, DEFAULT);

    for (ScriptElement e : script) {

      if (e instanceof Section) {
        Section sec = (Section)e;
        List<String> secIds = sec.getIds();
        if (secIds.size() > 0) {
          for (String id : secIds) {
            sections.put(id, (Section)sec.clone()); // clone to guarantee different identities
          }
        } else {
          sections.put(secIds.get(0), sec);
        }
      } else { // add copy to default sequence
        defSec.add(e.clone());
      }
    }

    if (defSec.getNumberOfChildren() > 0) {
      defaultSection = defSec;
    }
  }

  Section getSection (String id) {
    Section sec = null;

    while (id != null) {
      sec = sections.get(id);
      if (sec != null) {
        return sec;
      }

      int idx = id.lastIndexOf('.');
      if (idx > 0) {
        id = id.substring(0, idx); // ?? do we really want this recursive? that's policy
      } else {
        id = null;
      }
    }

    return defaultSection;
  }

  void addExpandedEvent(ArrayList<Event> events, Event se) {
    for (Event e : se.expand()) {
      if (!events.contains(e)) {
        events.add(e);
      }
    }
  }

  static final String[] ACTIVE_DEFAULT = { DEFAULT };

  public CG getNext () {
    return getNext(ACTIVE_DEFAULT, null);
  }

  public CG getNext (String[] activeStates) {
    return getNext(activeStates, null);
  }

  // this is our main purpose in life, but there is some policy in here
  public CG getNext (String[] activeStates, BitSet isReEntered) {

    cur = cur.advance(activeStates, isReEntered);

    ArrayList<Event> events = new ArrayList<Event>(cur.actives.length);
    for (ActiveSequence as : cur.actives) {

      while (true) {
        ScriptElement se = as.intrp.getNext();
        if (se != null) {
          if (se instanceof Event) {
            addExpandedEvent(events, (Event)se);
            break;
          } else if (se instanceof Alternative) {
            for (ScriptElement ase : (Alternative)se) {
              if (ase instanceof Event) {
                addExpandedEvent(events, (Event)ase);
              }
            }
            break;
          } else {
            // get next event
          }
        } else {
          break; // process next active sequence
        }
      }
    }

    return createCGFromEvents(events);
  }

  protected abstract CG createCGFromEvents(List<Event> events);

  //--- StateExtension interface
  public ActiveSnapshot getStateExtension() {
    return cur;
  }

  public void restore(ActiveSnapshot stateExtension) {
    cur = stateExtension;
  }

  public void registerListener(JPF jpf) {
    StateExtensionListener<ActiveSnapshot> sel = new StateExtensionListener(this);
    jpf.addSearchListener(sel);
  }

  /**************************************************************************
   * test drivers & infrastructure (move to reg tests at some point)
   */

  static class EventChoice extends ChoiceGenerator<Event> {
    int cur = -1;
    Event e;

    protected EventChoice() {}

    EventChoice (Event e){
      this.e = e;
    }

    public void advance() {
      cur++;
    }

    public boolean hasMoreChoices() {
      return (cur<0);
    }

    public Class<Event> getChoiceType() {
      return Event.class;
    }

    public Event getNextChoice() {
      return (cur == 0) ? e : null;
    }

    public int getProcessedNumberOfChoices() {
      return cur+1;
    }

    public int getTotalNumberOfChoices() {
      return 1;
    }


    public ChoiceGenerator randomize() {
      return null;
    }

    public void reset() {
      cur = -1;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(getClass().getName());

      sb.append("[id=\"");
      sb.append(id);
      sb.append("\",");

      if (cur >= 0) {
        sb.append(MARKER);
      }
      sb.append(e.toString());

      sb.append(']');
      return sb.toString();
    }

  }

  static class EventChoiceSet extends EventChoice {
    ArrayList<Event> list = new ArrayList<Event>();

    public EventChoiceSet () {
    }

    public void add (Event e) {
      list.add(e);
    }

    public boolean hasMoreChoices() {
      return (cur<list.size());
    }

    public Event getNextChoice() {
      return (cur >= 0 && cur < list.size()) ? list.get(cur) : null;
    }

    public int getTotalNumberOfChoices() {
      return list.size();
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(getClass().getName());

      sb.append("[id=\"");
      sb.append(id);
      sb.append("\",");

      for (int i=0; i<list.size(); i++) {
        if (i > 0) {
          sb.append(',');
        }
        if (i == cur) {
          sb.append(MARKER);
        }
        sb.append(list.get(i).toString());
      }
      sb.append(']');
      return sb.toString();
    }
  }

  static class EventEnv extends ScriptEnvironment<EventChoice> {

    public EventEnv (String fname) throws FileNotFoundException {
      super(fname);
    }

    public EventEnv(String name, Reader r) {
      super(name,r);
    }

    protected EventChoice createCGFromEvents(List<Event> events) {
      if (events.isEmpty()) {
        return null;
      } else if (events.size() == 1) {
        return new EventChoice( events.get(0));
      } else {
        EventChoiceSet cg = new EventChoiceSet();
        for (Event e : events) {
          cg.add( e);
        }
        return cg;
      }
    }
  }

  public static void main (String[] args) {
    //testNoMoveSequence();
    //testBacktrack();
    testMoveSequence();
  }

  static void testNoMoveSequence () {
    String s = "SECTION A {\n" +
               "  start, ANY {a1,a2}, REPEAT 2 {r}, end \n" +
               "}\n" +
               "SECTION B {\n" +
               "  foo, bar, baz \n" +
               "}";

    String[][] a = { { "A", "B" },
                     { "A", "B" },
                     { "A", "B" },
                     { "A", "B" },
                     { "A", "B" },
                     { "A", "B" } };

    try {
      ScriptEnvironment env = new EventEnv("test", new StringReader(s));
      env.parseScript();

      for (String[] sections : a) {
        ChoiceGenerator cg = env.getNext(sections);
        System.out.println(Misc.toString(sections, "[", ",", "]") + " => " + cg);
      }

    } catch (Throwable t){
      t.printStackTrace();
    }
  }

  static void testMoveSequence () {
    String s = "SECTION A {\n" +
               "  start, ANY {a1,a2}, REPEAT 2 {r}, end \n" +
               "}\n" +
               "SECTION B {\n" +
               "  foo, bar, baz \n" +
               "}\n" +
               "SECTION C {\n" +
               "  du, da \n" +
               "}";


    String[][] a = { { "A", "B" },
                     { "A", "B" },
                     { "A", "C" },
                     { "B" } };

    try {
      EventEnv env = new EventEnv("test", new StringReader(s));
      env.parseScript();

      for (String[] sections : a) {
        ChoiceGenerator<?> cg = env.getNext(sections);
        System.out.println(Misc.toString(sections, "[", ",", "]") + " => " + cg);
      }

    } catch (Throwable t){
      t.printStackTrace();
    }
  }


  static void testBacktrack () {
    String s = "SECTION A {\n" +
               "  start, ANY {a1,a2}, REPEAT 2 {r}, end \n" +
               "}\n" +
               "SECTION B {\n" +
               "  foo, bar, baz \n" +
               "}";

    String[][] a = { { "A", "B" },
                     { "A", "B" },
                     { "A", "B" },
                     { "A", "B" },
                     { "A", "B" },
                     { "A", "B" } };

    try {
      EventEnv env = new EventEnv("test", new StringReader(s));
      env.parseScript();

      ChoiceGenerator<?> cg = null;
      int i =0;

      cg = env.getNext(a[i]);
      System.out.println(Misc.toString(a[i], "[", ",", "]") + " => " + cg);
      i++;

      cg = env.getNext(a[i]);
      System.out.println(Misc.toString(a[i], "[", ",", "]") + " => " + cg);
      i++;

      ScriptEnvironment.ActiveSnapshot snap = env.getStateExtension();

      cg = env.getNext(a[i]);
      System.out.println(Misc.toString(a[i], "[", ",", "]") + " => " + cg);
      //i++;

      env.restore(snap);
      cg = env.getNext(a[i]);
      System.out.println(Misc.toString(a[i], "[", ",", "]") + " => " + cg);
    } catch (Throwable t){
      t.printStackTrace();
    }

  }
}
