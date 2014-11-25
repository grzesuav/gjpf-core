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
 * type annotation for local vars and resource vars
 */
public class VariableAnnotationInfo extends AbstractTypeAnnotationInfo {
  
  protected long[] scopeEntries;
  
  public VariableAnnotationInfo (AnnotationInfo base, int targetType, short[] typePath, long[] scopeEntries) {
    super( base, targetType, typePath);
    
    this.scopeEntries = scopeEntries;
  }
  
  public int getNumberOfScopeEntries(){
    return scopeEntries.length;
  }
  
  public int getStartPC (int idx){
    return (int)(scopeEntries[idx] >> 32) & 0xffff;
  }
  
  public int getLength (int idx){
    return (int)(scopeEntries[idx] >> 16) & 0xffff;
  }
  
  public int getEndPC (int idx){
    long e = scopeEntries[idx];
    
    int startPC = (int)(e >> 32) & 0xffff;
    int len = (int)(e >> 16) & 0xffff;
    
    return startPC + len;
  }
  
  public int getSlotIndex (int idx){
    return (int)scopeEntries[idx] & 0xffff;    
  }
  
  
  @Override
  protected void addArgs(StringBuilder sb){
    sb.append(",scope:");
    for (int i=0; i<scopeEntries.length;i++){
      long e = scopeEntries[i];
      int slotIndex = (int)(e & 0xffff);
      int length = (int)((e >> 16) & 0xffff);
      int startPc = (int)((e >> 32) & 0xffff);
      
      if (i>0){
        sb.append(',');
      }
      
      sb.append('[');
      sb.append( Integer.toString(startPc));
      sb.append("..");
      sb.append( Integer.toString(startPc + length-1));
      sb.append("]#");
      sb.append(slotIndex);
    }
  }
  
  // 2do - perhaps we should map to LocalVarInfos here (in case we have them), but
  // this would probably belong to LocalVarInfo (turning them into full InfoObjects)
}