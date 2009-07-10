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
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.ThreadInfo;
import java.util.ArrayList;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.bytecode.VariableAccessor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.jvm.bytecode.StoreInstruction;
import gov.nasa.jpf.jvm.bytecode.ArrayStoreInstruction;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.GETSTATIC;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.bytecode.ALOAD;


/**
 * simple listener tool to find out which variables (locals and fields) are
 * changed how often and from where. This should give a good idea if a state
 * space blows up because of some counter/timer vars, and where to apply the
 * necessary abstractions to close/shrink it
 */
public class VarTracker extends ListenerAdapter {
  
  int changeThreshold = 1;
  boolean filterSystemVars = false;
  String classFilter = null;
  
  ArrayList<VarChange> queue = new ArrayList<VarChange>();
  ThreadInfo lastThread;
  HashMap<String, VarStat> stat = new HashMap<String, VarStat>();
  int nStates = 0;
  int maxDepth;
  
  void print (int n, int length) {
    String s = Integer.toString(n);
    int l = length - s.length();
    
    for (int i=0; i<l; i++) {
      System.out.print(' ');
    }
    
    System.out.print(s);
  }
  
  void report (String message) {
    System.out.println("VarTracker results:");
    System.out.println("           states:        " + nStates);
    System.out.println("           max depth:     " + maxDepth);
    System.out.println("           term reason:   " + message);
    System.out.println();
    System.out.println("           minChange:     " + changeThreshold);
    System.out.println("           filterSysVars: " + filterSystemVars);
    
    if (classFilter != null) {
      System.out.println("           classFilter:   " + classFilter);
    }
    
    System.out.println();
    System.out.println("      change    variable");
    System.out.println("---------------------------------------");
    
    Collection<VarStat> values = stat.values();
    List<VarStat> valueList = new ArrayList<VarStat>();
    valueList.addAll(values);
    Collections.sort(valueList);
    
    for (VarStat s : valueList) {
      int n = s.getChangeCount();
      
      if (n < changeThreshold) {
        break;
      }
      
      print(s.nChanges, 12);
      System.out.print("    ");
      System.out.println(s.id);
    }
  }
  
  public void stateAdvanced(Search search) {
    
    if (search.isNewState()) { // don't count twice
      int stateId = search.getStateNumber();
      nStates++;
      int depth = search.getDepth();
      if (depth > maxDepth) maxDepth = depth;
      
      if (!queue.isEmpty()) {
        for (Iterator<VarChange> it = queue.iterator(); it.hasNext(); ){
          VarChange change = it.next();
            String id = change.getVariableId();
            VarStat s = stat.get(id);
            if (s == null) {
              s = new VarStat(id, stateId);
              stat.put(id, s);
            } else {
              // no good - we should filter during reg (think of large vectors or loop indices)
              if (s.lastState != stateId) { // count only once per new state
                s.nChanges++;
                s.lastState = stateId;
              }
            }
        }
      }
    }

    queue.clear();
  }
  
  
  public void propertyViolated(Search search) {
    report("property violated");
  }
    
  public void searchConstraintHit(Search search) {
    report("search constraint hit");
    
    System.exit(0); // just for now, quick hack
  }
  
  public void searchFinished(Search search) {
    report("search finished");
  }
    
  public void instructionExecuted(JVM jvm) {
    Instruction insn = jvm.getLastInstruction();
    ThreadInfo ti = jvm.getLastThreadInfo();
    String varId;
    
    if ( ((((insn instanceof GETFIELD) || (insn instanceof GETSTATIC)))
            && ((FieldInstruction)insn).isReferenceField()) ||
         (insn instanceof ALOAD)) {
      // a little extra work - we need to keep track of variable names, because
      // we cannot easily retrieve them in a subsequent xASTORE, which follows
      // a pattern like:  ..GETFIELD.. some-stack-operations .. xASTORE
      int objRef = ti.peek();
      if (objRef != -1) {
        ElementInfo ei = DynamicArea.getHeap().get(objRef);
        if (ei.isArray()) {
          varId = ((VariableAccessor)insn).getVariableId();
          
          // <2do> unfortunately, we can't filter here because we don't know yet
          // how the array ref will be used (we would only need the attr for
          // subsequent xASTOREs)
          ti.setOperandAttr( varId);
        }
      }
    }
    // here come the changes - note that we can't update the stats right away,
    // because we don't know yet if the state this leads into has already been
    // visited, and we want to detect only var changes that lead to *new* states
    // (objective is to find out why we have new states)
    else if (insn instanceof StoreInstruction) {
      if (insn instanceof ArrayStoreInstruction) {
        // did we have a name for the array?
        // stack is ".. ref idx [l]value => .."
        Object attr = ti.getOperandAttr(-1);
        if (attr != null) {
          varId = attr.toString() + "[]";
        } else {
          varId = "?[]";
        }
      } else {
        varId = ((VariableAccessor)insn).getVariableId();
      }
      
      if (filterChange(varId)) {
        queue.add(new VarChange(varId));
        lastThread = ti;
      }
    }
  }
  
  boolean filterChange (String varId) {
    
    // filter based on the field owner
    if (filterSystemVars) {
      if (varId.startsWith("java.")) return false;
      if (varId.startsWith("javax.")) return false;
      if (varId.startsWith("sun.")) return false;
    }
    
    // yes, it's a bit simplistic for now..
    if ((classFilter != null) && (!varId.startsWith(classFilter))) {
      return false;
    }
    
    // filter subsequent changes in the same transition (to avoid gazillions of
    // VarChanges for loop variables etc.)
    for (int i=0; i<queue.size(); i++) {
      VarChange change = queue.get(i);
      if (change.getVariableId().equals(varId)) {
        return false;
      }
    }
    
    return true;
  }
  
  
  void filterArgs (String[] args) {
    for (int i=0; i<args.length; i++) {
      if (args[i] != null) {
        if (args[i].equals("-noSystemVars")) {
          filterSystemVars = true;
          args[i] = null;
        } else if (args[i].equals("-minChange")) {
          args[i++] = null;
          if (i < args.length) {
            changeThreshold = Integer.parseInt(args[i]);
            args[i] = null;
          }
        } else if (args[i].equals("-classFilter")) {
          args[i++] = null;
          if (i < args.length) {
            classFilter = args[i];
            args[i] = null;
          }
        }
      }
    }
  }
  
  static void printUsage () {
    System.out.println("VarTracker - a JPF listener tool to report how often variables changed");
    System.out.println("             at least once in every state (to detect state space holes)");
    System.out.println("usage: java gov.nasa.jpf.tools.VarTracker <jpf-options> <varTracker-options> <class>");
    System.out.println("       -noSystemVars : don't report system variable changes (java*)");
    System.out.println("       -minChange <num> : don't report variables with less than <num> changes");
    System.out.println("       -classFilter <string> : only report changes in classes starting with <string>");
  }
  
}

// <2do> expand into types to record value ranges
class VarStat implements Comparable<VarStat> {
  String id;               // class[@objRef].field || class[@objref].method.local
  int nChanges;
  
  int lastState;           // this was changed in (<2do> don't think we need this)
  
  // might have more info in the future, e.g. total number of changes vs.
  // number of states incl. this var change, source locations, threads etc.
  
  VarStat (String varId, int stateId) {
    id = varId;
    nChanges = 1;
    
    lastState = stateId;
  }
  
  int getChangeCount () {
    return nChanges;
  }
  
  public int compareTo (VarStat other) {
    if (other.nChanges > nChanges) {
      return 1;
    } else if (other.nChanges == nChanges) {
      return 0;
    } else {
      return -1;
    }
  }
}

// <2do> expand into types to record values
class VarChange {
  String id;
  
  VarChange (String varId) {
    id = varId;
  }
  
  String getVariableId () {
    return id;
  }
}
