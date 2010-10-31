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
package gov.nasa.jpf.jvm;

public class LocalVarInfo {
  private final String name;
  private final String type;
  private final String genericSignature;
  private final int    startPC;
  private final int    length;
    
  public LocalVarInfo(String name, String type, String genericSignature, int startPC, int length) {
    if (name == null) {
      throw new NullPointerException("name == null"); 
    }   

    if (type == null) {
      throw new NullPointerException("type == null"); 
    }   
      
    if (genericSignature == null) {
      throw new NullPointerException("genericSignature == null"); 
    }   
      
    if (startPC < 0) {
      throw new NullPointerException("startPC < 0 : " + startPC); 
    }

    if (length < 0) {
      throw new NullPointerException("length < 0 : " + length);
    }
   
    this.name             = name;
    this.type             = type;
    this.genericSignature = genericSignature;
    this.startPC          = startPC;
    this.length           = length;
  }
    
  public String getName() {
    return name; 
  }

  public String getType() {
    return type; 
  }
  
  public String getGenericSignature() {
    return genericSignature;
  }
  
  public int getStartPC() {
    return startPC; 
  }
     
  public int getLength() {
    return length; 
  }
}

