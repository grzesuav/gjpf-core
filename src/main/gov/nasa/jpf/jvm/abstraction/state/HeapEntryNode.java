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
package gov.nasa.jpf.jvm.abstraction.state;

import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;

/**
 * Encapsulates the leafs of the non-heap part of the abstract state
 * graph.  In other words, the heap can only be entered from non-heap
 * nodes via a HeapEntryNode.
 * 
 * @author peterd
 */
public abstract class HeapEntryNode extends StateNode {
  public ObjVector<ObjectNode> refs;
  public IntVector             prims;
  
  // Meta; not critical
  public NodeMetaData meta;
  
  public void addPrimData(IntVector v) {
    v.append(prims);
  }

  public void addRefs(ObjVector<StateNode> v) {
    v.append(refs);
  }

  public boolean refsOrdered() {
    return true;
  }
}
