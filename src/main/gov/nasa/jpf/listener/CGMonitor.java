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
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.VM;
import java.io.PrintStream;

/**
 * listener to report out what CGs and choices are processed during the search.
 * This is a simple tool to find out about the SUT state space
 */
public class CGMonitor extends ListenerAdapter {
  
  protected PrintStream out;
  
  protected int depth;
  
  // display options
  protected boolean showInsn = false;   // show the insn that caused the CG
  protected boolean showChoice = false; // show the choice value (-> show each CG.advance())
  protected boolean showDepth = true;   // show search depth at point of CG set/advance
  
  public CGMonitor (Config conf) {
    showInsn = conf.getBoolean("cgm.show_insn", showInsn);
    showChoice = conf.getBoolean("cgm.show_choice", showChoice);
    showDepth = conf.getBoolean("cgm.show_depth", showDepth);
    
    out = System.out;
  }
  
  @Override
  public void stateAdvanced (Search search) {
    depth++;
  }
  
  @Override
  public void stateBacktracked (Search search) {
    depth--;
  }
  
  @Override
  public void stateRestored (Search search) {
    depth = search.getDepth();    
  }
  
  void printPrefix(char c) {
    for (int i=0; i<depth; i++) {
      System.out.print(c);
    }
  }
  
  void printCG (ChoiceGenerator<?> cg, boolean printChoice){
    if (showDepth){
      printPrefix('.');
    }
    
    out.print(cg);

    if (printChoice){
      out.print(", ");
      out.print(cg.getNextChoice());
    }

    if (showInsn){
      out.print(", \"");
      out.print(cg.getInsn());
      out.print('\"');
    }

    out.println();    
  }
  
  @Override
  public void choiceGeneratorSet (VM vm, ChoiceGenerator<?> currentCG) {
    if (!showChoice){
      printCG( vm.getChoiceGenerator(), false);
    }
  }
  
  @Override
  public void choiceGeneratorAdvanced (VM vm, ChoiceGenerator<?> currentCG) {
    if (showChoice){
      printCG( vm.getChoiceGenerator(), true);      
    }
  }

}
