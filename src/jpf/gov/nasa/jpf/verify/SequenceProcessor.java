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

import gov.nasa.jpf.util.Trace;

/**
 * interface for classes that process stored sequences, either by overriding
 * the central processing loop, or by implementing visitor callbacks 
 */
public abstract class SequenceProcessor {
  
  public void processSequence (String id, Trace<SequenceOp> trace) {
    for (SequenceOp o : trace.getOps()) {
      if (o.getSequenceId().equals(id)) {
        o.accept(this);
      }
    }
  }
  
  public void visit (SequenceOp.Start s) {
    // override in case of need
  }
  
  public void visit (SequenceOp.Event s) {
    // override in case of need    
  }
  
  public void visit (SequenceOp.Enter s) {
    // override in case of need
  }
  
  public void visit (SequenceOp.Exit s) {
    // override in case of need
  }
  
  public void visit (SequenceOp.End s) {
    // override in case of need
  }
}
