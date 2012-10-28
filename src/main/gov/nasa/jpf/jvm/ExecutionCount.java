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

package gov.nasa.jpf.jvm;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.IntTable;


/**
 * stores how many times an ExecutionContext (list of instructions on the stack) got
 * executed within a given ThreadInfo
 */
public class ExecutionCount {

  public static class Restorer implements ClosedMemento {
    ExecutionCount ec;
    IntTable.Snapshot<PreciseAllocationContext> snapshot;
    
    public Restorer (ExecutionCount ec){
      this.ec = ec;
      this.snapshot = ec.countTable.getSnapshot();
    }
    
    @Override
    public void restore() {
      ec.countTable.restore(snapshot);
    }
  }
  
  //--- static fields (we need to reset)
  
  static List<ExecutionCount> ecList = new ArrayList<ExecutionCount>();

  public static boolean init (Config conf){
    ecList = new ArrayList<ExecutionCount>();
    return true;
  }
  
  
  //--- instance fields
  
  /**
   * fields to identify this instance, since they are kept in a global list
   */
  protected String name;
  protected int id;
  
  /**
   * this is where we keep the execution counts per ExecutionContext (ThreadInfo and callstack)
   */
  protected IntTable<PreciseAllocationContext> countTable = new IntTable<PreciseAllocationContext>(6);
    
  
  //--- our public methods
  
  public ExecutionCount(String name){
    ecList.add(this);
    
    this.name = name;
    this.id = ecList.size();
  }
  
  public IntTable.Entry<PreciseAllocationContext> getExecEntry (ThreadInfo ti){
    PreciseAllocationContext key = PreciseAllocationContext.getExecutionContext(ti);
    IntTable.Entry<PreciseAllocationContext> e = countTable.get(key);
    if (e == null){
      e = countTable.add(key, 0);
    }
    return e;
  }
  
  public int getIncCount (ThreadInfo ti){
    IntTable.Entry<PreciseAllocationContext> e = getExecEntry(ti);
    e.val++;

    return e.val;
  }
  
  public Restorer createRestorer(){
    return new Restorer(this);
  }
  
  public String getName(){
    return name;
  }
  
  public int getId(){
    return id;
  }
  
  // for debugging purposes
  public void dump(){
    countTable.dump();
  }
}
