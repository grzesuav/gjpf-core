//
// Copyright (C) 2014 United States Government as represented by the
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

package gov.nasa.jpf.jvm.bytecode;

import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * helper class to factor out common PUT code
 * 
 * <2do> This is going to be moved into a Java 8 interface with default methods
 */
public class PutHelper {

  protected static boolean hasNewValue (ThreadInfo ti, StackFrame frame, ElementInfo eiFieldOwner, FieldInfo fi){
    Object valAttr = null;
    int fieldSize = fi.getStorageSize();
    
    if (fieldSize == 1){
      valAttr = frame.getOperandAttr();
      int val = frame.peek();
      if (eiFieldOwner.get1SlotField(fi) != val){
        return true;
      }
      
    } else {
      valAttr = frame.getLongOperandAttr();
      long val = frame.peekLong();
      if (eiFieldOwner.get2SlotField(fi) != val){
        return true;
      }
    }
    
    if (eiFieldOwner.getFieldAttr(fi) != valAttr){
      return true;
    }
    
    return false;
  }
  
  protected static int setReferenceField (ThreadInfo ti, StackFrame frame, ElementInfo eiFieldOwner, FieldInfo fi){
    Object valAttr = frame.getOperandAttr();
    int val = frame.peek();
    eiFieldOwner.set1SlotField(fi, val);
    eiFieldOwner.setFieldAttr(fi, valAttr);
    return val;
  }
  
  protected static long setField (ThreadInfo ti, StackFrame frame, ElementInfo eiFieldOwner, FieldInfo fi){
    int fieldSize = fi.getStorageSize();
    
    if (fieldSize == 1){
      Object valAttr = frame.getOperandAttr();
      int val = frame.peek();
      eiFieldOwner.set1SlotField(fi, val);
      eiFieldOwner.setFieldAttr(fi, valAttr);
      return val;
      
    } else {
      Object valAttr = frame.getLongOperandAttr();
      long val = frame.peekLong();
      eiFieldOwner.set2SlotField(fi, val);
      eiFieldOwner.setFieldAttr(fi, valAttr);
      return val;
    }
  }
}
