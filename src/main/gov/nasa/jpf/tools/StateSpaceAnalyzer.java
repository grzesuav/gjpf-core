//
// Copyright (C) 2007 United States Government as represented by the
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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.ThreadChoiceGenerator;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.MONITORENTER;
import gov.nasa.jpf.jvm.bytecode.MONITOREXIT;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.ElementCreator;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.TwoTypeComparator;

/**
 * a listener that collects information about ChoiceGenerators, choices and
 * where they are used. The purpose is to find out what causes the state space
 * size, and to give hints of how to reduce it.
 * The interesting part is that this is a listener that doesn't work off traces,
 * but needs to collect info up to a point where we want it to report. That's
 * state space or resource related, i.e. a combination of
 * 
 *  - number of transitions
 *  - memory consumption
 *  - elapsed time
 *  
 * once the limit is reached, we stop the search and report.
 * 
 * There are two parts we are interested in:
 * 
 *  - what CGs do we have
 *  - what creates those CGs (thread,insn,source) = last step insn
 */
public class StateSpaceAnalyzer extends ListenerAdapter  implements PublisherExtension {

  //--- these are our search termination conditions
  int maxStates;
  long maxTime;
  long maxMemory;
  
  long startTime;
  
  // this is where we store all the CGs, so that we don't loose any in
  // backtracking (we want the whole search graph up to the point where we stop the search)
  ArrayList<ChoiceGenerator> cgs;
  
  //--- some general state space stats
  int nEndStates;
  int nVisitedStates;
  int nNewStates;
  
  int nTotalInsn;
  int nNewStateInsn;
  int nMaxInsnPerNewState = -1;
  int nMinInsnPerNewState = Integer.MAX_VALUE;
  
  
  //--- internal storage/caches
  int nInsn; // number of insn in the last transition
  int maxInsns; // how many insns do we display in the report
  
  public StateSpaceAnalyzer (Config config, JPF jpf) {
    
    maxStates = config.getInt("ssa.max_states", Integer.MAX_VALUE);
    maxTime = config.getDuration("ssa.max_time", Long.MAX_VALUE);
    maxMemory = config.getMemorySize("ssa.max_memory", Long.MAX_VALUE);
    
    maxInsns = config.getInt("ssa.max_insn", 10); 
    
    cgs = new ArrayList<ChoiceGenerator>();
    
    jpf.addPublisherExtension(ConsolePublisher.class, this);
  }

  /*
   * create a map insn->{CG} that shows which CGs where created by
   * which insn
   */
  HashMap<Instruction,ArrayList<ChoiceGenerator>> getCGInsnMap () {
    HashMap<Instruction,ArrayList<ChoiceGenerator>> map =
      new HashMap<Instruction,ArrayList<ChoiceGenerator>>();
    
    for (ChoiceGenerator cg : cgs) {
      Instruction insn = cg.getInsn();
      ArrayList<ChoiceGenerator> list = map.get(insn);
      if (list == null) {
        list = new ArrayList<ChoiceGenerator>();
        map.put(insn,list);
      }
      list.add(cg);
    }
    
    return map;
  }
  
  static int getChoiceCount (ArrayList<ChoiceGenerator> list) {
    int n=0;
    for (ChoiceGenerator cg : list) {
      n += cg.getTotalNumberOfChoices();
    }
    return n;
  }
  
  static class InsnInfo {
    Instruction insn;
    ArrayList<ChoiceGenerator> cgList;
    int nChoices;
    
    InsnInfo (Instruction insn, ArrayList<ChoiceGenerator> cgList) {
      this.insn = insn;
      this.cgList = cgList;
      nChoices = getChoiceCount(cgList);
    }
  }
  
  /*
   * sort the CG instructions based hwo many CGs they create
   */
  ArrayList<InsnInfo> getSortedCGInsnList (HashMap<Instruction,ArrayList<ChoiceGenerator>> map){
    return
      Misc.createSortedList( map,
        new TwoTypeComparator<Map.Entry<Instruction,ArrayList<ChoiceGenerator>>,InsnInfo>(){
          public int compare (Map.Entry<Instruction,ArrayList<ChoiceGenerator>> e, InsnInfo ii) {
            return Integer.signum(e.getValue().size() - ii.cgList.size());
          }
        },
        new ElementCreator<Map.Entry<Instruction,ArrayList<ChoiceGenerator>>,InsnInfo>(){
          public InsnInfo create(Map.Entry<Instruction,ArrayList<ChoiceGenerator>> e) {
            return new InsnInfo(e.getKey(),e.getValue());
          }
        });
  }

  /*
   * sort instructions based on how many choices their CGs account for
   */
  ArrayList<InsnInfo> getSortedChoiceInsnList (HashMap<Instruction,ArrayList<ChoiceGenerator>> map){
    return
      Misc.createSortedList( map,
        new TwoTypeComparator<Map.Entry<Instruction,ArrayList<ChoiceGenerator>>,InsnInfo>(){
          public int compare (Map.Entry<Instruction,ArrayList<ChoiceGenerator>> e, InsnInfo ii) {
            return Integer.signum(getChoiceCount(e.getValue()) - ii.nChoices);
          }
        },
        new ElementCreator<Map.Entry<Instruction,ArrayList<ChoiceGenerator>>,InsnInfo>(){
          public InsnInfo create(Map.Entry<Instruction,ArrayList<ChoiceGenerator>> e) {
            return new InsnInfo(e.getKey(),e.getValue());
          }
        });
  }
  
  void printSortedCGThreadsOn(PrintWriter pw, InsnInfo ii) {
    HashMap<ThreadInfo,Integer> tiMap = Misc.createOccurrenceMap(ii.cgList,
      new ElementCreator<ChoiceGenerator,ThreadInfo>() {
        public ThreadInfo create(ChoiceGenerator cg) {
          return cg.getThreadInfo();
        }
      });
    
    ArrayList<Map.Entry<ThreadInfo,Integer>> list =
      Misc.createSortedEntryList(tiMap, new Comparator<Map.Entry<ThreadInfo,Integer>>() {
        public int compare (Map.Entry<ThreadInfo,Integer> e1,
                            Map.Entry<ThreadInfo,Integer> e2) {
          return Integer.signum(e1.getValue().intValue() - e2.getValue().intValue());
        }
      });

    for (int i=0; i<list.size(); i++){
      Map.Entry<ThreadInfo,Integer> e = list.get(i);
      if (i > 0){
        pw.print(", ");
      }
      pw.print(e.getKey().getName());
      pw.print(": ");
      pw.print(e.getValue().intValue());
    }
  }
  
  void printSortedCGInsnListOn (PrintWriter pw) {
    HashMap<Instruction,ArrayList<ChoiceGenerator>> map = getCGInsnMap();
    List<InsnInfo> insns = getSortedChoiceInsnList(map);

    for (int i=0; i<insns.size(); i++) {
      InsnInfo ii = insns.get(i);
      pw.println("  loc:     " + ii.insn.getFileLocation());
      pw.println("  src:     \"" + ii.insn.getSourceLine().trim() + '"');
      pw.println("  insn:    " + ii.insn);
      pw.print("  cg:      " + ii.cgList.get(0).getClass().getName());
      pw.println(": " + ii.cgList.size() + ", choices: " + ii.nChoices);
      pw.print(  "  threads: ");
      printSortedCGThreadsOn(pw, ii);
      pw.println();
      pw.println();
      
      if (i >= maxInsns-1) {
        pw.println("  ...");
        break;
      }
    }    
  }
  
  void printCGStatisticsOn (PrintWriter pw){
    
    int nWait=0, nNotify=0, nSyncCall=0, nSyncReturn=0,
        nSyncEnter=0, nSyncExit=0, nTerminate=0, nStart=0, nField=0;
    
    for (ChoiceGenerator cg : cgs) {
      if (cg instanceof ThreadChoiceGenerator){
        Instruction insn = cg.getInsn();
        if (insn instanceof InvokeInstruction){
          MethodInfo mi = ((InvokeInstruction)insn).getInvokedMethod();
          String mname = mi.getName();
          String cname = mi.getClassInfo().getName();
          
          if ("java.lang.Object".equals(cname)){
            if ("wait".equals(mname)){
              nWait++;
            } else if ("notify".equals(mname) || "notifyAll".equals(mname)){
              nNotify++;
            }
          } else if ("java.lang.Thread".equals(cname)){
            if ("start".equals(mname)){
              nStart++;
            }
          }
          
          if (mi.isSynchronized()){
            nSyncCall++;
          }
          
        } else if (insn instanceof MONITORENTER){
          nSyncEnter++;
          
        } else if (insn instanceof MONITOREXIT){
          nSyncExit++;

        } else if (insn instanceof ReturnInstruction){
          if ("run".equals(insn.getMethodInfo().getName())){ // thread??
            nTerminate++;
          } else {
            nSyncReturn++;
          }
          
        } else if (insn instanceof FieldInstruction) {
          nField++;
        }
      }      
    }
    
    HashMap<Class,Integer> map =
      Misc.createOccurrenceMap(cgs, new ElementCreator<ChoiceGenerator,Class>() {
      public Class create (ChoiceGenerator cg){
        if (cg instanceof ThreadChoiceGenerator){
          return ThreadChoiceGenerator.class;
        } else {
          return cg.getClass();
        }
      }
    });
                                                   
    ArrayList<Map.Entry<Class,Integer>> list =
      Misc.createSortedEntryList(map, new Comparator<Map.Entry<Class,Integer>>() {
        public int compare (Map.Entry<Class,Integer> e1,
                            Map.Entry<Class,Integer> e2) {
          return Integer.signum(e1.getValue().intValue() - e2.getValue().intValue());
        }
      });
                                                      
    for (Map.Entry<Class,Integer> e : list) {
      pw.print("  ");
      pw.print( e.getKey().getName());
      pw.print(": ");
      pw.println(e.getValue().intValue());
                                                        
      if (e.getKey() == ThreadChoiceGenerator.class){
        pw.println("\tsignal:       " + nWait + " - " + nNotify);
        pw.println("\tsync call:    " + nSyncCall + " - " + nSyncReturn);
        pw.println("\tsync block:   " + nSyncEnter + " - " + nSyncExit);
        pw.println("\tthread:       " + nStart + " - " + nTerminate);
        pw.println("\tshared field: " + nField);
      }
    }
  }
  
  //------------- our various listener notifications

  public void searchStarted (Search search) {
    startTime = System.currentTimeMillis();
  }
  
  public void stateAdvanced (Search search) {
        
    if (search.isNewState()) {
      nNewStateInsn += nInsn;
      nNewStates++;

      if (nInsn > nMaxInsnPerNewState) {
        nMaxInsnPerNewState = nInsn;
      }
      if (nInsn < nMinInsnPerNewState) {
        nMinInsnPerNewState = nInsn;
      }
      if (search.isEndState()) {
        nEndStates++; 
      }
      
    } else {
      nVisitedStates++;
    }

    nTotalInsn += nInsn;
    nInsn = 0;
    
    //...here we should eval our JPF termination condition (if we could
    //   check this exhaustively, we don't need this tool)
    if (((System.currentTimeMillis() - startTime) > maxTime) ||
        (nNewStates > maxStates) ||
        (Runtime.getRuntime().totalMemory() > maxMemory)){
      search.terminate();
    }
  }
  
  public void choiceGeneratorSet (JVM vm) {
    // we just have to store them - all the info we later-on need for our
    // stats is stored in the CG itself
    
    // NOTE: we get this from SystemState.nextSuccessor, i.e. when the CG
    // is actually used (which doesn't necessarily mean it produces a new state,
    // but it got created from a new state)
    cgs.add( vm.getLastChoiceGenerator());
  }

  //--- those are the notifications to find out about thread interactions
  public void objectLocked (JVM vm) {
  }

  public void objectUnlocked (JVM vm) {
  }

  public void objectWait (JVM vm) {
  }

  public void objectNotify (JVM vm) {
  }

  public void objectNotifyAll (JVM vm) {
  }

  public void threadBlocked (JVM vm){
  }

  
  
  //------------- the PublisherExtension interface
  
  public void publishFinished (Publisher publisher) {
    PrintWriter pw = publisher.getOut();
    
    publisher.publishTopicStart("CG statistics");    
    printCGStatisticsOn(pw);
    
    publisher.publishTopicStart("CG instructions");
    printSortedCGInsnListOn(pw);
  }

}
