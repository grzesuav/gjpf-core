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

import java.util.Comparator;

import gov.nasa.jpf.util.IntTable;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;

/**
 * Superclass of all nodes in the abstract state graph.
 * 
 * @author peterd
 */
public abstract class StateNode {
  public final ObjVector<StateNode> getRefs() {
	  ObjVector<StateNode> v = new ObjVector<StateNode>(0);
	  addRefs(v);
	  return v;
  }
  
  public abstract void addRefs(ObjVector<StateNode> v);
  
  public abstract boolean refsOrdered();
  
  public final IntVector getPrimData() {
	  IntVector v = new IntVector(0);
	  addPrimData(v);
	  return v;
  }

  public abstract void addPrimData(IntVector v);

  
  
  
  
  // LINEARIZATION/SERIALIZATION
  public int vmNodeId = INVALID_VM_ID;
  
  public int linearNodeId = INVALID_LINEAR_ID;

  protected static IntTable<Class<?>> nodeTypes = new IntTable<Class<?>>(6);

  
  // PUBLIC:
  public static final int INVALID_LINEAR_ID = -2;
  public static final int NULL_LINEAR_ID = -1;
  
  public static int linearIdOf(StateNode n) {
    return n == null ? NULL_LINEAR_ID : n.linearNodeId;
  }
  
  public static final Comparator<StateNode> linearIdComparator =
    new Comparator<StateNode> () {
      public int compare(StateNode o1, StateNode o2) {
        return linearIdOf(o1) - linearIdOf(o2);
      }
  };

  
  public static final int INVALID_VM_ID = -2;
  public static final int NULL_VM_ID = -1;
  public static final int ROOT_VM_ID = 1;
  public static final int CLASSES_VM_ID = 2;
  public static final int THREADS_VM_ID = 3;
  public static final int THREAD_VM_ID_START = 1000;
  public static final int STATIC_VM_ID_START = 1000000;
  public static final int DYNAMIC_VM_ID_START = 1000000000;

  public static int vmIdOf(StateNode n) {
    return n == null ? NULL_VM_ID : n.vmNodeId;
  }
  
  public static final Comparator<StateNode> vmIdComparator =
    new Comparator<StateNode> () {
      public int compare(StateNode o1, StateNode o2) {
        return vmIdOf(o1) - vmIdOf(o2);
      }
  };
  
  
  public static final int NULL_TYPE_ID = -1;
  
  public static int typeIdOf(StateNode n) {
    return n == null ? NULL_TYPE_ID : n.getNodeTypeId();
  }
  
  public final int getNodeTypeId() {
    return nodeTypes.poolIndex(this.getClass());
  }
}
