//
// Copyright (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
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
package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.bytecode.InstanceFieldInstruction;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;
import gov.nasa.jpf.jvm.bytecode.PUTSTATIC;
import gov.nasa.jpf.jvm.bytecode.StaticFieldInstruction;
import gov.nasa.jpf.search.Search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

/**
 * Simple field access race detector example
 *
 * This implementation so far doesn't deal with synchronization via signals, it
 * only checks if the lockset intersections of reads and writes from different
 * threads get empty.
 *
 * To rule out false positives, we have to verify that there are at least
 * two paths leading to the conflict with a different order of the read/write
 * instructions
 *
 *  See also the PreciseRaceDetector, which requires less work and avoids
 *  these false positives
 */
public class RaceDetector extends PropertyListenerAdapter {


  /*** helper classes ***************************************************************/

  static class FieldAccess {
    ThreadInfo ti;
    Object[] locksHeld;          // the ones we have during this operation (not really required)
    Object[] lockCandidates;     // the intersection for all prev operations
    FieldInstruction finsn;

    FieldAccess prev;

    FieldAccess (ThreadInfo ti, FieldInstruction finsn) {
      this.ti = ti;
      this.finsn = finsn;

      // we have to do some sort of cloning, since the lockSet and the
      // ElementInfos in it are going to be changed by JPF
      LinkedList<ElementInfo> lockSet = ti.getLockedObjects();
      locksHeld = new Object[lockSet.size()];
      if (locksHeld.length > 0) {
        Iterator<ElementInfo> it = lockSet.iterator();
        for (int i=0; it.hasNext(); i++) {
          locksHeld[i] = it.next().toString(); // <2do> - that's lame, but convenient
        }
      }

      // <2do> we should also hash the threads callstack here
    }

    <T> T[] intersect (T[] a, T[] b) {
      ArrayList<T> list = new ArrayList<T>(a.length);
      for (int i=0; i<a.length; i++) {
        for (int j=0; j<b.length; j++) {
          if (a[i].equals(b[j])) {
            list.add(a[i]);
            break;
          }
        }
      }
      return (list.size() == a.length) ? a : list.toArray(a.clone());
    }

    void updateLockCandidates () {
      if (prev == null) {
        lockCandidates = locksHeld;
      } else {
        lockCandidates = intersect(locksHeld, prev.lockCandidates);
      }
    }

    boolean hasLockCandidates () {
      return (lockCandidates.length > 0);
    }

    boolean isWriteAccess () {
      return ((finsn instanceof PUTFIELD) || (finsn instanceof PUTSTATIC));
    }

    FieldAccess getConflict () {
      boolean isWrite = isWriteAccess();

      for (FieldAccess c = prev; c != null; c = c.prev) {
        if ((c.ti != ti) && (isWrite != c.isWriteAccess())) {
          return c;
        }
      }
      return null; // no potential conflict found
    }

    public boolean equals (Object other) {
      if (other instanceof FieldAccess) {
        FieldAccess that = (FieldAccess)other;
        if (this.ti != that.ti) return false;
        if (this.finsn != that.finsn) return false;
        // <2do> we should also check for same callstack, but that's a detail we leave out for now

        return true;
      } else {
        return false;
      }
    }

    public int hashCode() {
      assert false : "hashCode not designed";
      return 42; // any arbitrary constant will do
      // thanks, FindBugs!
    }

    String describe () {
      String s = isWriteAccess() ? "write" : "read";
      s += " from thread: \"";
      s += ti.getName();
      s += "\", holding locks {";
      for (int i=0; i<locksHeld.length; i++) {
        if (i>0) s += ',';
        s += locksHeld[i];
      }
      s += "} in ";
      s += finsn.getSourceLocation();
      return s;
    }
  }

  static class FieldAccessSequence {
    String id;
    FieldAccess lastAccess;

    FieldAccessSequence (String id) {
      this.id = id;
    }

    void addAccess (FieldAccess fa) {
      fa.prev = lastAccess;
      lastAccess = fa;
      fa.updateLockCandidates();
    }

    void purgeLastAccess () {
      lastAccess = lastAccess.prev;
    }

  }


  /*** private fields and methods ****************************************/
  HashMap<String, FieldAccessSequence> fields = new HashMap<String, FieldAccessSequence>();

  Stack<ArrayList<FieldAccessSequence>> transitions = new Stack<ArrayList<FieldAccessSequence>>();   // the stack of FieldStateChanges
  ArrayList<FieldAccessSequence> pendingChanges;          // changed FieldStates during the last transition

  FieldAccessSequence raceField;     // if this becomes non-null, we have a race and terminate

  ArrayList<FieldAccess> raceAccess1 = new ArrayList<FieldAccess>(); // to store our potential race candidate pairs in
  ArrayList<FieldAccess> raceAccess2 = new ArrayList<FieldAccess>(); // case of verifyCyle=true

  String[] watchFields;              // list of regular expressions to match class/field names
  boolean terminate;                 // terminate search when we found a (potential) race
  boolean verifyCycle;               // don't report potentials, go on until encountering
                                     // the suspicious insns in both orders

  public RaceDetector (Config config) {
    watchFields = config.getStringArray("race.fields");
    terminate = config.getBoolean("race.terminate", true);
    verifyCycle = config.getBoolean("race.verify_cycle", false);
  }

  public void reset() {
    raceField = null;
    // <2do> that's probably not all
  }

  boolean isWatchedField (FieldInstruction finsn) {
    if (watchFields == null) {
      return true;
    }

    String fname = finsn.getVariableId();

    for (int i = 0; i<watchFields.length; i++) {
      if (fname.matches(watchFields[i])){
        return true;
      }
    }

    return false;
  }


  /*** GenericProperty **************************************************/

  public boolean check(Search search, JVM vm) {
    return (raceField == null);
  }

  public String getErrorMessage () {
    return ("potential field race: " + raceField.id);
  }

  /*** SearchListener ****************************************************/

  public void stateAdvanced(Search search) {
    transitions.push(pendingChanges);
    pendingChanges = null;
  }

  public void stateBacktracked(Search search) {
    ArrayList<FieldAccessSequence> fops = transitions.pop();
    if (fops != null) {
      for (FieldAccessSequence fs : fops) {
        fs.purgeLastAccess();
      }
    }
  }

  /*** VMListener *******************************************************/

  public void instructionExecuted(JVM jvm) {
    Instruction insn = jvm.getLastInstruction();

    if (insn instanceof FieldInstruction) {
      ThreadInfo ti = jvm.getLastThreadInfo();
      FieldInstruction finsn = (FieldInstruction)insn;
      String id = null;

      if (raceField != null) { // we only report the first one
        return;
      }

      if (ti.hasOtherRunnables() && isWatchedField(finsn)) {
        if (finsn instanceof StaticFieldInstruction) { // that's shared per se

          if (finsn.getMethodInfo().isClinit(finsn.getFieldInfo().getClassInfo())) {
            // this is a static field access from within the <clinit> of it's owner, skip it
            return;
          }

          id = finsn.getVariableId();
        } else { // instance field, check if the object is shared
          ElementInfo ei = ((InstanceFieldInstruction)insn).getLastElementInfo();
          if ((ei != null) && ei.isShared()) { // if it's null, we should have gotten an NPE
            id = finsn.getId(ei);
          }
        }

        if (id != null) {
          FieldAccessSequence fs = fields.get(id);
          if (fs == null) { // first time
            fs = new FieldAccessSequence(id);
            fields.put(id, fs);
          }

          FieldAccess fa = new FieldAccess(ti, finsn);
          fs.addAccess(fa);

          if (pendingChanges == null) {
            pendingChanges = new ArrayList<FieldAccessSequence>(5);
          }
          pendingChanges.add(fs);

          if (!fa.hasLockCandidates()) {
            FieldAccess conflict = fa.getConflict();
            if (conflict != null) {
              if (verifyCycle) {
                int idx = raceAccess1.indexOf(conflict);

                if ((idx >=0) && (fa.equals(raceAccess2.get(idx)))) {
                  // this is the second time we encounter the pair, this time in reverse order -> report
                  if (terminate) {
                    raceField = fs;
                  }
                  System.err.println("race detected (access occurred in both orders): " + fs.id);
                  System.err.println("\t" + fa.describe());
                  System.err.println("\t" + conflict.describe());
                } else {
                  // first time we see this pair -> store
                  raceAccess1.add(fa);
                  raceAccess2.add(conflict);
                }
              } else {
                if (terminate) {
                  raceField = fs;
                }
                System.err.println("potential race detected: " + fs.id);
                System.err.println("\t" + fa.describe());
                System.err.println("\t" + conflict.describe());
              }
            }
          }
        }
      }
    }
  }
}
