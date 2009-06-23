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

import org.apache.bcel.classfile.*;


/**
 * type, name, mod info about integer fields
 */
public class IntegerFieldInfo extends FieldInfo {
  int init;

  public IntegerFieldInfo (String name, String type, int modifiers,
                           ConstantValue cv, ClassInfo ci, int idx, int off) {
    super(name, type, modifiers, cv, ci, idx, off);
    init = (cv != null) ? Integer.parseInt(cv.toString()) : 0;
  }

  public void initialize (ElementInfo ei) {
    ei.getFields().setIntValue(ei, storageOffset, init);
  }

  public String valueToString (Fields f) {
    int i = f.getIntValue(storageOffset);
    return Integer.toString(i);
  }

  public Object getValueObject (Fields f){
    int i = f.getIntValue(storageOffset);

    if (type.equals("int")){
      return new Integer(i);
    } else if (type.equals("boolean")){
      return new Boolean(i != 0);
    } else if (type.equals("byte")){
      return new Byte((byte)i);
    } else if (type.equals("char")){
      return new Character((char)i);
    } else if (type.equals("short")){
      return new Short((short)i);
    } else {
      throw new UnsupportedOperationException("unknown field type: " + type);
    }
  }
}
