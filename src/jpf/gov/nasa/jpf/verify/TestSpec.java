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

package gov.nasa.jpf.verify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


public class TestSpec {

  ArrayList<ArgList> targetArgs = new ArrayList<ArgList>();
  ArrayList<ArgList> callArgs = new ArrayList<ArgList>();
  
  ArrayList<Goal> goals = new ArrayList<Goal>();

  public void addTargetArgs (ArgList alist){
    targetArgs.add(alist);
  }
  
  public void addCallArgs (ArgList alist){
    callArgs.add(alist);
  }
    
  public List<ArgList> getTargetArgs() {
    return targetArgs;
  }
    
  public List<ArgList> getCallArgs() {
    return callArgs;
  }
   
  public List<Object[]> getCallArgCombinations() {
    ArrayList<Object[]> ac = new ArrayList<Object[]>();
    
    for (ArgList al : callArgs){
      ac.addAll(al.getArgCombinations());
    }
    
    return ac;
  }
  
  public void addGoal (Goal g){
    goals.add(g);
  }
  
  
  public List<Goal> getGoals () {
    return goals;
  }
  
  public void printOn (PrintWriter pw){
    pw.println("TestSpec {");
    
    if (!targetArgs.isEmpty()){
      for (ArgList al : targetArgs){  
        pw.println("  targetArgs={");
        for (ValSet vs : al){
          pw.print("    "); vs.printOn(pw); pw.println();
        }
        pw.println("  }");
      }
    }
    
    if (!callArgs.isEmpty()){
      for (ArgList al : callArgs){  
        pw.println("  callArgs={");
        for (ValSet vs : al){
          pw.print("    "); vs.printOn(pw); pw.println();
        }
        pw.println("  }");
      }
    }
    
    if (!goals.isEmpty()){
      for (Goal g : goals){
        pw.print("  goal=");
        g.printOn(pw);
        pw.println();
      }
    }
    
    pw.println("}");
  }
}
