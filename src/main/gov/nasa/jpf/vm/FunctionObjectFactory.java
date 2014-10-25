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
package gov.nasa.jpf.vm;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 */
public class FunctionObjectFactory {
  
  public int getFunctionObject(ThreadInfo ti, ClassInfo fiClassInfo, String samUniqueName, BootstrapMethodInfo bmi, 
                                         String[] freeVariableTypeNames, Object[] freeVariableValues) {
    
    ClassLoaderInfo cli = bmi.enclosingClass.getClassLoaderInfo();
    
    ClassInfo funcObjType = cli.getResolvedFuncObjType(fiClassInfo, samUniqueName, bmi, freeVariableTypeNames);
    
    funcObjType.registerClass(ti);

    Heap heap = ti.getHeap();
    ElementInfo ei = heap.newObject(funcObjType, ti);
    
    setFuncObjFields(ei, bmi, freeVariableTypeNames, freeVariableValues);
    
    return ei.getObjectRef();
  }
  
  public void setFuncObjFields(ElementInfo funcObj, BootstrapMethodInfo bmi, String[] freeVarTypeNames, Object[] freeVarValues) {
    Fields fields = funcObj.getFields();
    
    for(int i = 0; i<freeVarTypeNames.length; i++) {
      String typeName = freeVarTypeNames[i];
      if (typeName.equals("byte")) {
        fields.setByteValue(i, (byte)freeVarValues[i]);
      } else if (typeName.equals("char")) {
        fields.setCharValue(i, (char)freeVarValues[i]);
      } else if (typeName.equals("short")) {
        fields.setShortValue(i, (short)freeVarValues[i]);
      } else if (typeName.equals("int")) {
        fields.setIntValue(i, (int)freeVarValues[i]);
      } else if (typeName.equals("float")) {
        fields.setFloatValue(i, (float)freeVarValues[i]);
      } else if (typeName.equals("long")) {
        fields.setLongValue(i, (long)freeVarValues[i]);
      } else if (typeName.equals("double")) {
        fields.setDoubleValue(i, (double)freeVarValues[i]);
      } else if (typeName.equals("boolean")) {
        fields.setBooleanValue(i, (boolean)freeVarValues[i]);
      } else {
        int val = ((ElementInfo)freeVarValues[i]).getObjectRef();
        fields.setReferenceValue(i, val);
      }
    }
  }
}