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

import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.MutableInteger;
import gov.nasa.jpf.util.MutableIntegerRestorer;
import gov.nasa.jpf.util.SparseObjVector;

/**
 * helper object to compute search global id's, e.g. to create canonical
 * orders. Usually, there is one instance per type (e.g. ThreadInfo or
 * ClassLoaderInfo), and it is kept in a static field of the respective class.
 */
public class GlobalIdManager {

  SparseObjVector<MutableInteger> perThreadPathInstances = new SparseObjVector<MutableInteger>();
  
  
  public int getNewId (SystemState ss, ThreadInfo executingThread, Instruction loc){
    int gid = executingThread.getSearchGlobalId();
    
    MutableInteger mInt = perThreadPathInstances.get(gid);
    if (mInt == null){
      mInt = new MutableInteger(0);
      perThreadPathInstances.set(gid, mInt);
    }
    
    // make sure we properly restore the original value upon backtrack
    if (!ss.hasRestorer(mInt)){
      ss.putRestorer(mInt, new MutableIntegerRestorer(mInt));
    }
    
    mInt.inc(); // we got here because there is a new object we need an id for
    
    
    
    return 0;
  }
}
