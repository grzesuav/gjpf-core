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
package gov.nasa.jpf.tools;

import gov.nasa.jpf.*;
import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.jvm.bytecode.*;
import gov.nasa.jpf.jvm.choice.*;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.StringSetMatcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * This is a Race Detection Algorithm that is precise in its calculation of races, i.e. no false warnings.
 * It exploits the fact that every thread choice selection point could be due to a possible race. It just runs
 * through all the thread choices and checks whether there are more than one thread trying to read & write to the
 * same field of an object.
 *
 * Current limitation is that it is only sound, i.e. will not miss a race, if the sync-detection is switched off
 * during model checking. This is due to the fact that the sync-detection guesses that an acess is lock-protected
 * when it in reality might not be. It does not check for races on array entries.
 *
 * This algorithm came out of a discussion with Franck van Breugel and Sergey Kulikov from the University of York.
 * All credits for it goes to Franck and Sergey, all the bugs are mine.
 *
 * Author: Willem Visser
 *
 */

public class PreciseRaceDetector extends PropertyListenerAdapter {

  FieldInfo raceField;
  ThreadInfo[] racers = new ThreadInfo[2];
  Instruction[] insns = new Instruction[2];

  StringSetMatcher includes = null; //  means all
  StringSetMatcher excludes = null; //  means none
  
  public PreciseRaceDetector (Config conf) {
    includes = StringSetMatcher.getNonEmpty(conf.getStringArray("race.include"));
    excludes = StringSetMatcher.getNonEmpty(conf.getStringArray("race.exclude"));
  }
  
  public boolean check(Search search, JVM vm) {
    return (raceField == null);
  }

  public void reset() {
    raceField = null;
    racers[0] = racers[1] = null;
    insns[0] = insns[1] = null;
  }

  public String getErrorMessage () {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.print("race for: \"");
    pw.print(raceField);
    pw.println("\"");

    for (int i=0; i<2; i++) {
      pw.print("  ");
      pw.print( racers[i].getName());
      pw.print(" at ");
      pw.println(insns[i].getSourceLocation());
      pw.print("\t\t\"" + insns[i].getSourceLine().trim());
      pw.print("\"  : ");
      pw.println(insns[i]);
    }

    pw.flush();
    return sw.toString();
  }

  private boolean isPutInsn(Instruction insn) {
    return (insn instanceof PUTFIELD) || (insn instanceof PUTSTATIC);
  }

  //----------- our VMListener interface

  public void choiceGeneratorSet(JVM vm) {
    ChoiceGenerator<?> cg = vm.getLastChoiceGenerator();

    if (cg instanceof ThreadChoiceFromSet) {
      ThreadInfo[] threads = ((ThreadChoiceFromSet)cg).getAllThreadChoices();

      ElementInfo[] eiCandidates = null;
      FieldInfo[] fiCandidates = null;

      for (int i=0; i<threads.length; i++) {
        ThreadInfo ti = threads[i];
        Instruction insn = ti.getPC();
        
        if (insn instanceof FieldInstruction) { // Ok, we have to check
          FieldInstruction finsn = (FieldInstruction)insn;
          FieldInfo fi = finsn.getFieldInfo();

          if (StringSetMatcher.isMatch(fi.getFullName(), includes, excludes)) {
          
            if (eiCandidates == null) {
              eiCandidates = new ElementInfo[threads.length];
              fiCandidates = new FieldInfo[threads.length];
            }

            ElementInfo ei = finsn.peekElementInfo(ti);

            // check if we have seen it before
            int idx=-1;
            for (int j=0; j<i; j++) {
              if ((ei == eiCandidates[j]) && (fi == fiCandidates[j])) {
                idx = j;
                break;
              }
            }

            if (idx >= 0){ // yes, we have multiple accesses on the same object/field
              Instruction otherInsn = threads[idx].getPC();

              // that's maybe a bit too strong, but chances are that if there are
              // racing writes, a read will soon follow (at least at some point), and
              // if they weren't synced, the read probably would forget about it too
              // we could easily check if they are of different type though
              if (isPutInsn(otherInsn) || isPutInsn(insn)) {
                raceField = ((FieldInstruction)insn).getFieldInfo();
                racers[0] = threads[idx];
                insns[0] = otherInsn;
                racers[1] = threads[i];
                insns[1] = insn;
                return;
              }
            } else {
              eiCandidates[i] = ei;
              fiCandidates[i] = fi;
            }
          }
        }
        
        // what about AtomicFieldUpdaters?
      }
    }
  }

  public void executeInstruction (JVM jvm) {
    if (raceField != null) {
      // we're done, report as quickly as possible
      ThreadInfo ti = jvm.getLastThreadInfo();
      //ti.skipInstruction();
      ti.breakTransition();
    }
  }

}