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

import java.io.PrintWriter;

/**
 * simple tool to print contents of a classfile
 *
 * <2do> use indentation level variable and formated output
 */
public class ClassFilePrinter extends ClassFileReaderAdapter {

  PrintWriter pw;

  public static void main(String[] args){
    ClassFilePrinter printer = new ClassFilePrinter();

    try {
      ClassFile cf = new ClassFile(args[0]);
      cf.parse(printer);

    } catch (ClassFileException cfx){
      cfx.printStackTrace();
    }
  }

  public ClassFilePrinter (){
    pw = new PrintWriter(System.out, true);
  }

  public void setClass(ClassFile cf, String clsName, String superClsName, int flags, int cpCount) {
    pw.println( "--------------------------------------------------- constpool section");
    printCp(pw,cf);

    pw.println( "--------------------------------------------------- class section");
    pw.print( "  class=");
    pw.println( clsName);
    pw.print( "  superclass=");
    pw.println( superClsName);
    pw.print( "  flags=0x");
    pw.println( Integer.toHexString(flags));
  }

  //--- interfaces
  public void setInterfaceCount(ClassFile cf, int ifcCount) {
    pw.print( "  interface count=");
    pw.println( ifcCount);
  }

  public void setInterface(ClassFile cf, int ifcIndex, String ifcName) {
    pw.print( "  [");
    pw.print( ifcIndex);
    pw.print( "]: name=");
    pw.println( ifcName);
  }

  //--- fields
  public void setFieldCount(ClassFile cf, int fieldCount) {
    pw.println( "--------------------------------------------------- field section");
    pw.print( "  field count=");
    pw.println(fieldCount);
  }

  public void setField(ClassFile cf, int fieldIndex, int accessFlags, String name, String descriptor) {
    pw.print( "  [");
    pw.print( fieldIndex);
    pw.print( "]: name=");
    pw.print( name);
    pw.print( ",descriptor=");
    pw.print( descriptor);
    pw.print( ",flags=0x");
    pw.println(Integer.toHexString(accessFlags));
  }

  public void setFieldAttributeCount(ClassFile cf, int fieldIndex, int attrCount) {
    pw.print( "       field attr count=");
    pw.println( attrCount);
  }

  public void setFieldAttribute(ClassFile cf, int fieldIndex, int attrIndex, String name, int attrLength) {
    pw.print( "       [");
    pw.print( attrIndex);
    pw.print( "]: ");

    if (name == ClassFile.CONST_VALUE_ATTR) {
      cf.parseConstValueAttr(this, fieldIndex);

    } else if (name == ClassFile.RUNTIME_VISIBLE_ANNOTATIONS_ATTR){
      cf.parseRuntimeVisibleAnnotationsAttr(this);

    } else if (name == ClassFile.RUNTIME_INVISIBLE_ANNOTATIONS_ATTR){
      cf.parseRuntimeInvisibleAnnotationsAttr(this);

    } else {
      pw.print(name);
      pw.print(" data=[");
      printRawData(pw, cf, attrLength, 10);
      pw.println(']');
    }
  }

  public void setConstantValue(ClassFile cf, int fieldIndex, Object value) {
    pw.print("ConstantValue=");
    pw.println(value);
  }

  //--- methods
  public void setMethodCount(ClassFile cf, int methodCount) {
    pw.println( "--------------------------------------------------- method section");
    pw.print( "  method count=");
    pw.println( methodCount);
  }

  public void setMethod(ClassFile cf, int methodIndex, int accessFlags, String name, String descriptor) {
    pw.print( "  [");
    pw.print( methodIndex);
    pw.print( "]: name=");
    pw.print( name);
    pw.print( ",descriptor=");
    pw.print( descriptor);
    pw.print( ",flags=0x");
    pw.println( Integer.toHexString(accessFlags));
  }

  public void setMethodAttributeCount(ClassFile cf, int methodIndex, int attrCount) {
    pw.print( "       method attr count=");
    pw.println( attrCount);
  }

  public void setMethodAttribute(ClassFile cf, int methodIndex, int attrIndex, String name, int attrLength) {
    pw.print( "       [");
    pw.print( attrIndex);
    pw.print( "]: ");

    if (name == ClassFile.CODE_ATTR) {
      cf.parseCodeAttr(this, methodIndex);

    } else if (name == ClassFile.EXCEPTIONS_ATTR){
      cf.parseExceptionAttr(this, methodIndex);

    } else if (name == ClassFile.RUNTIME_VISIBLE_ANNOTATIONS_ATTR){
      cf.parseRuntimeVisibleAnnotationsAttr(this);

    } else if (name == ClassFile.RUNTIME_INVISIBLE_ANNOTATIONS_ATTR){
      cf.parseRuntimeInvisibleAnnotationsAttr(this);

    } else {
      pw.print(name);
      pw.print(" data=[");
      printRawData(pw, cf, attrLength, 10);
      pw.println(']');
    }
  }

  public void setExceptionCount(ClassFile cf, int methodIndex, int exceptionCount){
    pw.print("Exceptions count=");
    pw.println(exceptionCount);
  }

  public void setException(ClassFile cf, int methodIndex, int exceptionIndex, String exceptionType){
    pw.print( "            [");
    pw.print( exceptionIndex);
    pw.print( "]: ");
    pw.print( "type=");
    pw.println(exceptionType);
  }

  public void setCode(ClassFile cf, int methodIndex, int maxStack, int maxLocals, int codeLength) {
    pw.print("Code ");
    pw.print( "maxStack=");
    pw.print( maxStack);
    pw.print( ",maxLocals=");
    pw.print( maxLocals);
    pw.print( ",length=");
    pw.print( codeLength);
    /**
    pw.print( ",data=[");
    printRawData(pw, cf, codeLength, 10);
    pw.print(']');
    **/
    pw.println();
    ByteCodePrinter bcPrinter = new ByteCodePrinter(pw, cf, "            ");
    cf.parseBytecode(bcPrinter, methodIndex, codeLength);
  }

  public void setExceptionTableCount(ClassFile cf, int methodIndex, int exceptionTableCount) {
    pw.print( "            exception table count=");
    pw.println( exceptionTableCount);
  }

  public void setExceptionTableEntry(ClassFile cf, int methodIndex, int exceptionIndex,
          int startPc, int endPc, int handlerPc, String catchType) {
    pw.print( "            [");
    pw.print( exceptionIndex);
    pw.print( "]: ");
    pw.print( "type=");
    pw.print( catchType);
    pw.print( ",startPc=");
    pw.print( startPc);
    pw.print( ",endPc=");
    pw.print( endPc);
    pw.print( ",handlerPc=");
    pw.println( handlerPc);
  }

  public void setCodeAttributeCount(ClassFile cf, int methodIndex, int attrCount) {
    pw.print( "            code attr count=");
    pw.println( attrCount);
  }

  public void setCodeAttribute(ClassFile cf, int methodIndex, int attrIndex, String name, int attrLength) {
    pw.print( "            [");
    pw.print( attrIndex);
    pw.print( "]: ");

    if (name == ClassFile.LINE_NUMBER_TABLE_ATTR) {
      pw.println( "LineNumberTable");
      cf.parseLineNumberTableAttr(this, methodIndex);

    } else if (name == ClassFile.LOCAL_VAR_TABLE_ATTR) {
      pw.println( "LocalVarTable");
      cf.parseLocalVarTableAttr(this, methodIndex);

    } else {  // generic
      pw.print(name);
      pw.print(" data=[");
      printRawData(pw, cf, attrLength, 10);
      pw.println(']');
    }
  }

  public void setLineNumberTableCount(ClassFile cf, int methodIndex, int lineNumberCount) {
    pw.print( "                 linenumber table count=");
    pw.println( lineNumberCount);
  }

  public void setLineNumber(ClassFile cf, int methodIndex, int lineIndex, int lineNumber, int startPc) {
    pw.print( "                 [");
    pw.print( lineIndex);
    pw.print( "]: ");
    pw.print( "line=");
    pw.print( lineNumber);
    pw.print( ",startPc=");
    pw.println( startPc);
  }

  public void setLocalVarTableCount(ClassFile cf, int methodIndex, int localVarCount) {
    pw.print( "                 localvar table count=");
    pw.println( localVarCount);
  }

  public void setLocalVar(ClassFile cf, int methodIndex, int localVarIndex,
          String varName, String descriptor, int scopeStartPc, int scopeEndPc, int slotIndex) {
    pw.print( "                 [");
    pw.print( localVarIndex);
    pw.print( "]: ");
    pw.print( "name=");
    pw.print( varName);
    pw.print( ",descriptor=");
    pw.print( descriptor);
    pw.print( ",startPc=");
    pw.print( scopeStartPc);
    pw.print( ",endPc=");
    pw.print( scopeEndPc);
    pw.print( ",slot=");
    pw.println( slotIndex);
  }


  //--- class attributes
  public void setClassAttributeCount(ClassFile cf, int attrCount) {
    pw.println( "--------------------------------------------------- class attr section");
    pw.println("  class attr count=" + attrCount);
  }

  public void setClassAttribute(ClassFile cf, int attrIndex, String name, int attrLength) {
    pw.print( "  [");
    pw.print( attrIndex);
    pw.print( "]: ");

    if (name == ClassFile.SOURCE_FILE_ATTR) {
      pw.print("SourceFile file=");
      cf.parseSourceFileAttr(this);

    } else if (name == ClassFile.DEPRECATED_ATTR) {
      pw.println("Deprecated");

    } else if (name == ClassFile.INNER_CLASSES_ATTR) {
      pw.println("InnerClasses");
      cf.parseInnerClassesAttr(this);

    } else if (name == ClassFile.RUNTIME_VISIBLE_ANNOTATIONS_ATTR){
      cf.parseRuntimeVisibleAnnotationsAttr(this);

    } else if (name == ClassFile.RUNTIME_INVISIBLE_ANNOTATIONS_ATTR){
      cf.parseRuntimeInvisibleAnnotationsAttr(this);
      
    } else {
      pw.print(name);
      pw.print(" data=[");
      printRawData(pw, cf, attrLength, 10);
      pw.println(']');
    }
  }

  public void setSourceFile(ClassFile cf, String pathName){
    pw.print( " path=");
    pw.println(pathName);
  }

  public void setInnerClassCount(ClassFile cf, int innerClsCount) {
    pw.println( "       inner class count=" + innerClsCount);
  }

  public void setInnerClass(ClassFile cf, int innerClsIndex,
          String outerName, String innerName, String innerSimpleName, int accessFlags) {
    pw.print( "       [");
    pw.print( innerClsIndex);
    pw.print( "]: inner=");
    pw.print( innerName); 
    pw.print( ",simpleName=");
    pw.print( innerSimpleName);
    pw.print( ",outer=");
    pw.print( outerName); 
    pw.print( ",flags=0x");
    pw.println( Integer.toHexString(accessFlags));
  }

  public void setRuntimeInvisibleAnnotationCount(ClassFile cf, int annotationCount){
    pw.print( "RuntimeInvisibleAnnotations annotationCount=");
    pw.println(annotationCount);
  }

  public void setRuntimeVisibleAnnotationCount(ClassFile cf, int annotationCount){
    pw.print( "RuntimeVisibleAnnotations annotationCount=");
    pw.println(annotationCount);
  }

  public void setAnnotation(ClassFile cf, int annotationIndex, String annotationType, int nValuePairs){
    pw.print( "            [");
    pw.print( annotationIndex);
    pw.print( "]: ");
    pw.print(annotationType);
    pw.print(" valueCount=");
    pw.println(nValuePairs);
  }

  public void setPrimitiveAnnotationValue(ClassFile cf, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, Object val){
    pw.print( "                 [");
    pw.print( valueIndex);
    pw.print( "]: ");
    pw.print(elementName);
    pw.print("=");
    pw.println(val);
  }

  public void setStringAnnotationValue(ClassFile cf, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, String s){
    pw.print( "                 [");
    pw.print( valueIndex);
    pw.print( "]: ");
    pw.print(elementName);
    pw.print("=\"");
    pw.print(s);
    pw.println("\"");
  }

  public void setClassAnnotationValue(ClassFile cf, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, String typeName){
    pw.print( "                 [");
    pw.print( valueIndex);
    pw.print( "]: ");
    pw.print(elementName);
    pw.print("=class ");
    pw.println(typeName);
  }


  //--- internal stuff

  protected void printCp (PrintWriter pw, ClassFile cf){
    int nCpEntries = cf.getNumberOfCpEntries();

    for (int i=1; i<nCpEntries; i++){

      int j = cf.getDataPosOfCpEntry(i);

      pw.print("  [");
      pw.print(i);
      pw.print("]: ");

      if (j < 0) {
        pw.println("<unused>");
        continue;
      }

      switch (cf.u1(j)){
        case ClassFile.CONSTANT_UTF8:
          pw.print( "constant_utf8 {\"");
          pw.print( cf.getCpValue(i));
          pw.println("\"}");
          break;
        case ClassFile.CONSTANT_INTEGER:
          pw.print( "constant_integer {");
          pw.print( cf.getCpValue(i));
          pw.println("}");
          break;
        case ClassFile.CONSTANT_FLOAT:
          pw.print( "constant_float {");
          pw.print( cf.getCpValue(i));
          pw.println("}");
          break;
        case ClassFile.CONSTANT_LONG:
          pw.print( "constant_long {");
          pw.print( cf.getCpValue(i));
          pw.println("}");
          break;
        case ClassFile.CONSTANT_DOUBLE:
          pw.print( "constant_double {");
          pw.print( cf.getCpValue(i));
          pw.println("}");
          break;
        case ClassFile.CONSTANT_CLASS:
          pw.print("constant_class {name=#");
          pw.print( cf.u2(j+1));
          pw.print("(\"");
          pw.print( cf.classNameAt(i));
          pw.println("\")}");
          break;
        case ClassFile.CONSTANT_STRING:
          pw.print("constant_string {utf8=#");
          pw.print( cf.u2(j+1));
          pw.print("(\"");
          pw.print( cf.stringAt(i));
          pw.println("\")}");
          break;
        case ClassFile.FIELD_REF:
          printRef(pw, cf, i, j, "fieldref");
          break;
        case ClassFile.METHOD_REF:
          printRef(pw, cf, i, j, "methodref");
          break;
        case ClassFile.INTERFACE_METHOD_REF:
          printRef(pw, cf, i, j, "interface_methodref");
          break;
        case ClassFile.NAME_AND_TYPE:
          pw.print("name_and_type {name=#");
          pw.print( cf.u2(j+1));
          pw.print("(\"");
          pw.print(cf.utf8At(cf.u2(j+1)));
          pw.print("\"),desciptor=#");
          pw.print( cf.u2(j+3));
          pw.print("(\"");
          pw.print(cf.utf8At(cf.u2(j+3)));
          pw.println("\")}");
          break;
        default:
          pw.print("ERROR: illegal tag" + cf.u1(j));
      }
    }
    pw.println();
  }

  void printRef(PrintWriter pw, ClassFile cf, int cpIdx, int dataPos, String refType){
    pw.print(refType);
    pw.print(" {class=#");
    pw.print(cf.u2(dataPos + 1));
    pw.print("(\"");
    pw.print(cf.refClassNameAt(cpIdx));
    pw.print("\"),nameType=#");
    pw.print(cf.u2(dataPos + 3));
    pw.print("(\"");
    pw.print(cf.refNameAt(cpIdx));
    pw.print("\",\"");
    pw.print(cf.refDescriptorAt(cpIdx));
    pw.println("\")}");
  }

  void printRawData(PrintWriter pw, ClassFile cf, int dataLength, int maxBytes){
    int max = Math.min(dataLength, maxBytes);
    int max1 = max-1;
    for (int i=0; i<max1; i++){
      pw.printf("%02x ", cf.readU1());
    }
    pw.printf("%02x", cf.readU1());

    if (dataLength>maxBytes){
      pw.print("..");
    }
  }
}
