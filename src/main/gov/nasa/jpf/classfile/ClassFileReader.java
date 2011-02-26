//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.classfile;

/**
 * interface for classfile processors
 *
 * iteration groups always start with a
 *   setXCount(int xCount)
 *
 * followed by xCount notifications
 *   setX (int xIndex)
 *
 * with 0<=xIndex<xCount
 */
public interface ClassFileReader {

  void setClass(ClassFile cf, String clsName, String superClsName, int flags, int cpCount);


  void setInterfaceCount(ClassFile cf, int ifcCount);

  void setInterface(ClassFile cf, int ifcIndex, String ifcName);


  void setFieldCount(ClassFile cf, int fieldCount);

  void setField(ClassFile cf, int fieldIndex, int accessFlags, String name, String descriptor);

  void setFieldAttributeCount(ClassFile cf, int fieldIndex, int attrCount);

  void setFieldAttribute(ClassFile cf, int fieldIndex, int attrIndex, String name, int attrLength);

  //--- standard field attributes
  void setConstantValue(ClassFile cf, int fieldIndex, Object value);


  void setMethodCount(ClassFile cf, int methodCount);

  void setMethod(ClassFile cf, int methodIndex, int accessFlags, String name, String descriptor);

  void setMethodAttributeCount(ClassFile cf, int methodIndex, int attrCount);

  void setMethodAttribute(ClassFile cf, int methodIndex, int attrIndex, String name, int attrLength);


  //--- standard method attributes
  void setExceptionCount (ClassFile cf, int methodIndex, int exceptionCount);

  void setException (ClassFile cf, int methodIndex, int exceptionIndex, String exceptionType);

  void setCode(ClassFile cf, int methodIndex, int maxStack, int maxLocals, int codeLength);

  void setExceptionTableCount (ClassFile cf, int methodIndex, int exceptionTableCount);

  void setExceptionTableEntry(ClassFile cf, int methodIndex, int exceptionIndex, int startPc, int endPc, int handlerPc, String catchType);

  void setCodeAttributeCount(ClassFile cf, int methodIndex, int attrCount);

  void setCodeAttribute(ClassFile cf, int methodIndex, int attrIndex, String name, int attrLength);

  //--- standard code attribute attributes (yes, attributes can be nested)
  void setLineNumberTableCount(ClassFile cf, int methodIndex, int lineNumberCount);
  
  void setLineNumber(ClassFile cf, int methodIndex, int lineIndex, int lineNumber, int startPc);

  void setLocalVarTableCount(ClassFile cf, int methodIndex, int localVarCount);

  void setLocalVar(ClassFile cf, int methodIndex, int localVarIndex, String varName, String descriptor,
                      int scopeStartPc, int scopeEndPc, int slotIndex);


  void setClassAttributeCount(ClassFile cf, int attrCount);

  void setClassAttribute(ClassFile cf, int attrIndex, String name, int attrLength);

  //--- standard class attributes
  void setSourceFile(ClassFile cf, String pathName);

  void setInnerClassCount(ClassFile cf, int innerClsCount);

  void setInnerClass(ClassFile cf, int innerClsIndex, String outerName, String innerName, String innerSimpleName, int accessFlags);


  //--- annotations
  void setRuntimeInvisibleAnnotationCount(ClassFile cf, int annotationCount);

  void setRuntimeVisibleAnnotationCount(ClassFile cf, int annotationCount);

  void setAnnotation(ClassFile cf, int annotationIndex, String annotationType, int nValuePairs);

  void setPrimitiveAnnotationValue(ClassFile cf, int annotationIndex, int valueIndex, String elementName, int arrayIndex, Object val);

  void setStringAnnotationValue(ClassFile cf, int annotationIndex, int valueIndex, String elementName, int arrayIndex, String s);

  void setClassAnnotationValue(ClassFile cf, int annotationIndex, int valueIndex, String elementName, int arrayIndex, String typeName);

}
