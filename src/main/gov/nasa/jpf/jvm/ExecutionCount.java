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

import gov.nasa.jpf.SystemAttribute;


/**
 * stores how many times a certain Instruction got executed within the same
 * thread for each different calling context.
 * Used as a Instruction attribute for NEW.. instructions
 */
public class ExecutionCount implements SystemAttribute {

  public static class Restorer implements ClosedMemento {

    ExecutionCount ec;         // what object to restore into
    ThreadEntry[] threadEntries;   // what value to restore
    
    ThreadEntry[] cloneThreadEntries( ThreadEntry[] te){
      ThreadEntry[] a = new ThreadEntry[te.length];
      for (int i=0; i<a.length; i++){
        a[i] = te[i].clone();
      }
      return a;
    }
    
    public Restorer (ExecutionCount ec){
      this.ec = ec;
      if (ec.threadEntries != null){
        threadEntries = cloneThreadEntries(ec.threadEntries);
      }
    }
    
    @Override
    public void restore() {
      if (threadEntries != null){
        ec.threadEntries = cloneThreadEntries(threadEntries);
      } else {
        ec.threadEntries = null;
      }
    }
  }
  
  /**
   * represents an execution count of this intstruction with the same caller chain
   */
  public static class ExecEntry implements Cloneable {
    int callerContext;  // simplified - we don't keep the object array
    int count;
    
    ExecEntry (int ccId){
      callerContext = ccId;
      count = 0;
    }
    
    public ExecEntry clone(){
      try {
        return (ExecEntry)super.clone();
      } catch (CloneNotSupportedException cx){
        return null;
      }
    }
    
    public int getCount(){
      return count;
    }
    
    public int incCount(){
      return ++count;
    }
    
    public int getCallerContextId(){
      return callerContext;
    }
  }
  
  
  /**
   * represents all exec counts for this instruction for a given thread
   */
  static class ThreadEntry implements Cloneable {
    
    ThreadInfo ti;
    ExecEntry[] execEntries;
    
    ThreadEntry (ThreadInfo ti){
      this.ti = ti;
      execEntries = new ExecEntry[1];
      execEntries[0] = new ExecEntry(getCallerContextId(ti));
    }
    
    public int getCallerContextId(ThreadInfo ti) {
      int h = -1;
      
      for (StackFrame f = ti.getTopFrame().getPrevious(); f != null; f = f.getPrevious()) {
        Instruction pc = f.getPC();
        
        if (h < 0) {
          h += h;
          h ^= 0x88888EEF;
        } else {
          h += h;
        }

        h ^= pc.hashCode();
      }
      
      return ((h >>> 4) ^ (h & 15));
    }
    
    ExecEntry getExecEntry(){
      int ccId = getCallerContextId(ti);
      
      if (execEntries == null){
        ExecEntry ee = new ExecEntry(ccId);
        execEntries = new ExecEntry[1];
        execEntries[0] = ee;
        return ee;
        
      } else {
        int idx = getEntryIndex(ccId);
        if (execEntries[idx].callerContext != ccId){
          insertEntry( idx, new ExecEntry(ccId));
        } else {
          // this is where we should check/resolve collisions
        }
        return execEntries[idx];
      }
    }
    
    int getEntryIndex(int ccId){
      int min = 0;
      int max = execEntries.length-1;
      ExecEntry[] a = execEntries;
    
      while (max > min) {
        int mid = (min + max) / 2;
        if (a[mid].callerContext < ccId) {
          min = mid + 1;
        } else {
          max = mid;
        }
      }
    
      return min;
    }
    
    void insertEntry (int idx, ExecEntry ee){
      ExecEntry[] newEntries = new ExecEntry[execEntries.length + 1];
      if (idx > 0){
        System.arraycopy(execEntries, 0, newEntries, 0, idx);
      }
      if (idx < execEntries.length){
        System.arraycopy(execEntries, idx, newEntries, idx+1, execEntries.length-idx);
      }

      newEntries[idx] = ee;
      
      execEntries = newEntries;
    }
    
    public ThreadEntry clone(){
      try {
        ThreadEntry te = (ThreadEntry)super.clone();
        ExecEntry[] ee = new ExecEntry[execEntries.length];
        for (int i=0; i<ee.length; i++){
          ee[i] = execEntries[i].clone();
        }
        return te;
      } catch (CloneNotSupportedException x){
        return null;
      }    
    }
  }
  
  
  
  //--- instance fields
  
  // assuming the number of threads will be limited, we keep this in a simple array
  ThreadEntry[] threadEntries;
  
  
  
  //--- our public methods
  
  public ExecEntry getExecEntry (ThreadInfo ti){
    if (threadEntries == null){
      ThreadEntry te = new ThreadEntry(ti);
      threadEntries = new ThreadEntry[1];
      threadEntries[0] = te;
      return te.execEntries[0]; // there is only one at this point
      
    } else {
      for (int i = 0; i < threadEntries.length; i++) {
        ThreadEntry te = threadEntries[i];
        if (te.ti == ti) {
          return te.getExecEntry();
        }
      }
    
      // thread is not yet in our list
      ThreadEntry te = new ThreadEntry(ti);
      ThreadEntry[] newThreadEntries = new ThreadEntry[threadEntries.length+1];
      System.arraycopy(threadEntries,0,newThreadEntries,0,threadEntries.length);
      newThreadEntries[threadEntries.length] = te;
      threadEntries = newThreadEntries;    
      return te.execEntries[0]; // there is only one at this point
    }
  }
  
  public Restorer createRestorer(){
    return new Restorer(this);
  }
}
