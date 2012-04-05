//
// Copyright (C) 2012 United States Government as represented by the
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

import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.AllocInstruction;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.util.LocationSpec;

/**
 * simulator for OutOfMemoryErrors. This can be configured to either
 * fire a specific location (file:line), at a specified number of live objects,
 * or a specified heap size threshold. For locations, we support transitive
 * scopes, i.e. every allocation that happens from within a certain range
 * of instructions of an active method execution
 * 
 * Since our only action is to inject OutOfMemoryErrors, we don't need
 * to implement a Property interface
 */
public class OOMEInjector extends ListenerAdapter {

  static class OOME {}
  static OOME throwOOME = new OOME(); // we can reuse the same object as an attribute
  
  List<LocationSpec> locations = new ArrayList<LocationSpec>();
  
  public OOMEInjector (Config config, JPF jpf){
    String[] spec = config.getStringArray("oome.locations");
    if (spec != null){
      for (String s : spec){
        LocationSpec locSpec = LocationSpec.createLocationSpec(s);
        locations.add(locSpec);
      }
    }
  }
  
  @Override
  public void classLoaded (JVM vm){
    ClassInfo ci = vm.getLastClassInfo();
    String fname = ci.getSourceFileName();
    
    for (LocationSpec locSpec : locations){
      if (locSpec.matchesFile(fname)){
        for (MethodInfo mi : ci.getDeclaredMethodInfos()){
          int[] lineNumbers = mi.getLineNumbers();
          for (int i=0; i<lineNumbers.length; i++){
            if (locSpec.includesLine(lineNumbers[i])){
              Instruction insn = mi.getInstruction(i);
              insn.addAttr(throwOOME);
            }
          }
        }
      }
    }
  }
  
  protected boolean checkOOMCondition (StackFrame frame, Instruction insn){
    if (insn.hasAttr(OOME.class)){
      return true;
    }
    
    if (frame.hasFrameAttr(OOME.class)){
      return true;
    }
    
    return false;
  }
  
  @Override
  public void executeInstruction (JVM vm){
    Instruction insn = vm.getLastInstruction();
    if (insn instanceof AllocInstruction){
      ThreadInfo ti = vm.getLastThreadInfo();
      if (checkOOMCondition(ti.getTopFrame(), insn)){
        // we could use Heap.setOutOfMemory(true), but then we would have to reset
        // if the app handles it so that it doesn't throw outside the specified locations.
        // This would require more effort than throwing explicitly
        Instruction nextInsn = ti.createAndThrowException("java.lang.OutOfMemoryError");
        ti.skipInstruction(nextInsn);
      }
    }
  }
  
  @Override
  public void instructionExecuted (JVM vm){
    Instruction insn = vm.getLastInstruction();
    
    if (insn instanceof InvokeInstruction){
      ThreadInfo ti = vm.getLastThreadInfo();
      StackFrame frame = ti.getTopFrame();
      
      if (frame.getPC() != insn){ // means the call did succeed
        if (checkOOMCondition(frame.getPrevious(), insn)){
          frame.addFrameAttr(throwOOME);
        }
      }
    }
  }
}
