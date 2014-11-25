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
 * abstract AnnotationInfo base for Java 8 type annotations
 */
public abstract class AbstractTypeAnnotationInfo extends AnnotationInfo {
  
  protected int targetType;   // see section 3.2 of JSR 308 - constants defined in .jvm.ClassFile
  protected short[] typePath; // the type path for compound type annotations as per 3.4 of JSR 308
  
  protected AbstractTypeAnnotationInfo (AnnotationInfo base, int targetType, short[] typePath) {
    super(base);
    
    this.targetType = targetType;
    this.typePath = typePath;
  }
  
  // <2do> add typePath query
  
  public int getTargetType(){
    return targetType;
  }
  
  protected void addArgs(StringBuilder sb){
    // nothing here
  }
  
  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName());
    sb.append( '{');
    
    sb.append( "targetType:");
    sb.append( Integer.toHexString(targetType));
    
    sb.append( ",name:");
    sb.append( name);
    
    if (typePath != null){
      sb.append( ",path:");
      for (int i=0; i<typePath.length;i++){
        int e = typePath[i];
        sb.append('(');
        sb.append( Integer.toString((e>>8) & 0xff));
        sb.append( Integer.toString(e & 0xff));
        sb.append(')');
      }
    }
    
    addArgs(sb); // overridden by subclasses
    sb.append( '}');    
    
    return sb.toString();
  }
}
