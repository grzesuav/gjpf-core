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

import gov.nasa.jpf.util.IdentityArrayObjectSet;

/**
 * an IdentityObjectSet for ThreadInfos with a few extra methods to
 * check against the threadlist
 */
public class ThreadInfoSet extends IdentityArrayObjectSet<ThreadInfo> {
  
  public ThreadInfoSet (ThreadInfo ti){
    super(ti);
  }
  
  public int getNumberOfLiveThreads(){
    if (size == 0){
      return 0;
      
    } else {
      int n = 0;
      
      for (int i=0; i<size; i++){
        ThreadInfo ti = (ThreadInfo) elements[i];
        
        if (!ti.isTerminated() && ti.isInCurrentThreadList()){
          n++;
        }
      }
      
      return n;
    }
  }
  
  public boolean hasMultipleLiveThreads(){
    if (size == 0){
      return false;
      
    } else {
      int n = 0;
      
      for (int i=0; i<size; i++){
        ThreadInfo ti = (ThreadInfo) elements[i];
        if (!ti.isTerminated() && ti.isInCurrentThreadList()){
          if (n++ > 0){
            return true;
          }
        }
      }
      
      return false;
    }
  }
  
  public int getNumberOfLiveNonBlockedThreads(){
    if (size == 0){
      return 0;
      
    } else {
      int n = 0;
      
      for (int i=0; i<size; i++){
        ThreadInfo ti = (ThreadInfo) elements[i];
        if (!ti.isTerminated() && !ti.isBlocked()){
          if (ti.isInCurrentThreadList()){
            n++;
          }
        }
      }
      
      return n;
    }
  }

  public boolean containsLiveThread (ThreadInfo ti){
    return (contains(ti) && ti.isInCurrentThreadList());
  }
}
