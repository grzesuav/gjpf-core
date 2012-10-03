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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.IntTable;
import gov.nasa.jpf.util.ObjVector;
import gov.nasa.jpf.util.Processor;

/**
 * ObjVectorHeap that freezes ElementInfos upon state storage
 * during getMemento(), to avoid repetitive ElementInfo memento creation.
 * 
 * Note that this requires proper use of getModifiable(ref) for
 * all callers
 */
public class FreezingObjVectorHeap extends ObjVectorHeap {
  
  // a heap memento that stores frozen ElementInfos instead of mementos of them
  static class FreezeMemento extends OVMemento {
    IntTable.Snapshot<AllocationContext> ctxSnap;
    ObjVector.Snapshot<ElementInfo> eiSnap;
   
    FreezeMemento (ObjVectorHeap heap){
      super(heap);
      
      heap.elementInfos.process( new Processor<ElementInfo>() {
        public void process(ElementInfo ei) {
          ei.freeze();
        }
      });

      eiSnap = heap.elementInfos.getSnapshot();      
    }
    
    @Override
    public Heap restore(Heap inSitu) {
      super.restore( inSitu);

      ObjVectorHeap heap = (ObjVectorHeap)inSitu;
      heap.elementInfos.restore(eiSnap);
      return heap;
    }    
  }
  
  public FreezingObjVectorHeap (Config config, KernelState ks){
    super(config, ks);
  }
  
  public Memento<Heap> getMemento(){
    return new FreezeMemento(this);
  }
}
