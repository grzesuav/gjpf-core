//
// Copyright (C) 2013 United States Government as represented by the
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

import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.vm.AnnotationInfo;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassParseException;
import gov.nasa.jpf.vm.ClassParser;
import gov.nasa.jpf.vm.ExceptionHandler;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.GenericSignatureHolder;
import gov.nasa.jpf.vm.InfoObject;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.Types;

/**
 * a class parser that reads Java class file formats
 * 
 * This is actually more of an adapter between the Java specific ClassFile (which does the real parsing),
 * and the Java agnostic ClassInfo, which is only accessed through its initialization API since it resides
 * in another package
 */
public class ClassFileParser extends ClassFileReaderAdapter implements ClassParser {

  protected ClassFile cf;
  protected JVMCodeBuilder cb;
  protected ClassInfo ci;

  public ClassFileParser (ClassFile cf, JVMCodeBuilder cb){
    this.cf = cf;
    this.cb = cb;
  }
  
  //--- the ClassParser interface
  
  @Override
  public void parse (ClassInfo ci) throws ClassParseException {
    this.ci = ci;
    
    cf.parse(this);
  }

  @Override
  public void parse() throws ClassParseException {
    // <2do> - turn into inner JVMClassInfo object
  }
  
  //--- the overridden ClassFileReader methods  
  
  @Override
  public void setClass (ClassFile cf, String clsName, String superClsName, int flags, int cpCount) throws ClassParseException {
    ci.setClass(clsName, superClsName, flags, cpCount);
  }

  @Override
  public void setClassAttribute (ClassFile cf, int attrIndex, String name, int attrLength) {
    if (name == ClassFile.SOURCE_FILE_ATTR) {
      cf.parseSourceFileAttr(this, null);

    } else if (name == ClassFile.SIGNATURE_ATTR) {
      cf.parseSignatureAttr(this, ci);

    } else if (name == ClassFile.RUNTIME_VISIBLE_ANNOTATIONS_ATTR) {
      cf.parseAnnotationsAttr(this, ci);

    } else if (name == ClassFile.RUNTIME_INVISIBLE_ANNOTATIONS_ATTR) {
      //cf.parseAnnotationsAttr(this, ClassInfo.this);
    } else if (name == ClassFile.INNER_CLASSES_ATTR) {
      cf.parseInnerClassesAttr(this, ci);

    } else if (name == ClassFile.ENCLOSING_METHOD_ATTR) {
      cf.parseEnclosingMethodAttr(this, ci);
    }
  }

  
  //--- inner/enclosing classes
  
  String[] innerClassNames;
  
  @Override
  public void setInnerClassCount (ClassFile cf, Object tag, int classCount) {
    innerClassNames = new String[classCount];
  }
    
  @Override
  public void setInnerClass (ClassFile cf, Object tag, int innerClsIndex,
          String outerName, String innerName, String innerSimpleName, int accessFlags) {
    // Ok, this is a total mess - some names are in dot notation, others use '/'
    // and to make it even more confusing, some InnerClass attributes refer NOT
    // to the currently parsed class, so we have to check if we are the outerName,
    // but then 'outerName' can also be null instead of our own name.
    // Oh, and there are also InnerClass attributes that have their own name as inner names
    // (see java/lang/String$CaseInsensitiveComparator or ...System and java/lang/System$1 for instance)
    if (outerName != null) {
      outerName = Types.getClassNameFromTypeName(outerName);
    }

    innerName = Types.getClassNameFromTypeName(innerName);
    if (!innerName.equals(ci.getName())) {
      innerClassNames[innerClsIndex] = innerName;

    } else {
      // this refers to ourself, and can be a force fight with setEnclosingMethod
      if (outerName != null) { // only set if this is a direct member, otherwise taken from setEnclosingMethod
        ci.setEnclosingClass(outerName);
      }
    }
  }
    
  @Override
  public void setEnclosingMethod(ClassFile cf, Object tag, String enclosingClassName, String enclosingMethodName, String descriptor) {
    ci.setEnclosingClass(enclosingClassName);

    if (enclosingMethodName != null) {
      ci.setEnclosingMethod(enclosingMethodName + descriptor);
    }
  }
    
  @Override
  public void setInnerClassesDone(ClassFile cf, Object tag) {
    // we have to check if we allocated too many - see the mess above
    int count = 0;
    for (int i = 0; i < innerClassNames.length; i++) {
      innerClassNames = Misc.stripNullElements(innerClassNames);
    }
    
    ci.setInnerClassNames(innerClassNames);
  }

  
  //--- source file
    
  @Override
  public void setSourceFile (ClassFile cf, Object tag, String fileName) {
    ci.setSourceFile( fileName);
  }

  //--- interfaces
  
  String[] interfaceNames;

  @Override
  public void setInterfaceCount (ClassFile cf, int ifcCount) {
    interfaceNames = new String[ifcCount];
  }

  @Override
  public void setInterface (ClassFile cf, int ifcIndex, String ifcName) {
    interfaceNames[ifcIndex] = Types.getClassNameFromTypeName(ifcName);
  }

  @Override 
  public void setInterfacesDone (ClassFile cf){
    ci.setInterfaceNames(interfaceNames);
  }
  
  //--- fields
  
  FieldInfo[] fields;
  FieldInfo curFi; // need to cache for attributes

  @Override 
  public void setFieldCount (ClassFile cf, int fieldCount){
    fields = new FieldInfo[fieldCount];
  }
  
  @Override
  public void setField (ClassFile cf, int fieldIndex, int accessFlags, String name, String descriptor) {
    FieldInfo fi = FieldInfo.create(name, descriptor, accessFlags);
    fields[fieldIndex] = fi;
    curFi = fi; // for attributes
  }

  @Override
  public void setFieldAttribute (ClassFile cf, int fieldIndex, int attrIndex, String name, int attrLength) {
    if (name == ClassFile.SIGNATURE_ATTR) {
      cf.parseSignatureAttr(this, curFi);

    } else if (name == ClassFile.CONST_VALUE_ATTR) {
      cf.parseConstValueAttr(this, curFi);

    } else if (name == ClassFile.RUNTIME_VISIBLE_ANNOTATIONS_ATTR) {
      cf.parseAnnotationsAttr(this, curFi);

    } else if (name == ClassFile.RUNTIME_INVISIBLE_ANNOTATIONS_ATTR) {
      //cf.parseAnnotationsAttr(this, curFi);
    }
  }

  @Override
  public void setConstantValue (ClassFile cf, Object tag, Object constVal) {
    curFi.setConstantValue(constVal);
  }

  @Override
  public void setFieldsDone (ClassFile cf) {
    ci.setFields(fields);
  }

  
  //--- declaredMethods
  MethodInfo[] methods;
  MethodInfo curMi;

  @Override
  public void setMethodCount (ClassFile cf, int methodCount) {
    methods = new MethodInfo[methodCount];
  }

  @Override
  public void setMethod (ClassFile cf, int methodIndex, int accessFlags, String name, String signature) {
    MethodInfo mi = MethodInfo.create(name, signature, accessFlags);
    methods[methodIndex] = mi;
    curMi = mi;
  }

  @Override
  public void setMethodsDone (ClassFile cf) {
    ci.setMethods(methods);
  }

  @Override
  public void setMethodAttribute (ClassFile cf, int methodIndex, int attrIndex, String name, int attrLength) {
    if (name == ClassFile.CODE_ATTR) {
      cf.parseCodeAttr(this, curMi);

    } else if (name == ClassFile.SIGNATURE_ATTR) {
      cf.parseSignatureAttr(this, curMi);

    } else if (name == ClassFile.EXCEPTIONS_ATTR) {
      cf.parseExceptionAttr(this, curMi);

    } else if (name == ClassFile.RUNTIME_VISIBLE_ANNOTATIONS_ATTR) {
      cf.parseAnnotationsAttr(this, curMi);

    } else if (name == ClassFile.RUNTIME_INVISIBLE_ANNOTATIONS_ATTR) {
      //cf.parseAnnotationsAttr(this, curMi);
    } else if (name == ClassFile.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS_ATTR) {
      cf.parseParameterAnnotationsAttr(this, curMi);

    } else if (name == ClassFile.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS_ATTR) {
      //cf.parseParameterAnnotationsAttr(this, curMi);
    }
  }

  //--- throws list
  String[] exceptions;

  @Override
  public void setExceptionCount (ClassFile cf, Object tag, int exceptionCount) {
    exceptions = new String[exceptionCount];
  }

  @Override
  public void setException (ClassFile cf, Object tag, int exceptionIndex, String exceptionType) {
    exceptions[exceptionIndex] = Types.getClassNameFromTypeName(exceptionType);
  }

  @Override
  public void setExceptionsDone (ClassFile cf, Object tag) {
    curMi.setThrownExceptions(exceptions);
  }

  //--- exception handlers
  ExceptionHandler[] handlers;
  
  @Override
  public void setExceptionHandlerTableCount (ClassFile cf, Object tag, int exceptionTableCount) {
    handlers = new ExceptionHandler[exceptionTableCount];
  }

  @Override
  public void setExceptionHandler (ClassFile cf, Object tag, int handlerIndex,
          int startPc, int endPc, int handlerPc, String catchType) {
    ExceptionHandler xh = new ExceptionHandler(catchType, startPc, endPc, handlerPc);
    handlers[handlerIndex] = xh;
  }

  @Override
  public void setExceptionHandlerTableDone (ClassFile cf, Object tag) {
    curMi.setExceptionHandlers(handlers);
  }

  //--- code
  
  @Override
  public void setCode (ClassFile cf, Object tag, int maxStack, int maxLocals, int codeLength) {
    curMi.setMaxLocals(maxLocals);
    curMi.setMaxStack(maxStack);

    cb.reset( cf, curMi);

    cf.parseBytecode(cb, tag, codeLength);
    cb.installCode();
  }

  @Override
  public void setCodeAttribute (ClassFile cf, Object tag, int attrIndex, String name, int attrLength) {
    if (name == ClassFile.LINE_NUMBER_TABLE_ATTR) {
      cf.parseLineNumberTableAttr(this, tag);

    } else if (name == ClassFile.LOCAL_VAR_TABLE_ATTR) {
      cf.parseLocalVarTableAttr(this, tag);
    }
  }

  //--- line numbers
  int[] lines, startPcs;
  
  @Override
  public void setLineNumberTableCount (ClassFile cf, Object tag, int lineNumberCount) {
    lines = new int[lineNumberCount];
    startPcs = new int[lineNumberCount];
  }

  @Override
  public void setLineNumber (ClassFile cf, Object tag, int lineIndex, int lineNumber, int startPc) {
    lines[lineIndex] = lineNumber;
    startPcs[lineIndex] = startPc;
  }

  @Override
  public void setLineNumberTableDone (ClassFile cf, Object tag) {
    curMi.setLineNumbers(lines, startPcs);
  }

  
  //--- local variables
  LocalVarInfo[] localVars;
  
  @Override
  public void setLocalVarTableCount (ClassFile cf, Object tag, int localVarCount) {
    localVars = new LocalVarInfo[localVarCount];
  }

  @Override
  public void setLocalVar (ClassFile cf, Object tag, int localVarIndex,
          String varName, String descriptor, int scopeStartPc, int scopeEndPc, int slotIndex) {
    LocalVarInfo lvi = new LocalVarInfo(varName, descriptor, "", scopeStartPc, scopeEndPc, slotIndex);
    localVars[localVarIndex] = lvi;
  }

  @Override
  public void setLocalVarTableDone (ClassFile cf, Object tag) {
    curMi.setLocalVarTable(localVars);
  }


  //--- annotations
  AnnotationInfo[] annotations;
  AnnotationInfo curAi;
  AnnotationInfo[][] parameterAnnotations;
  Object[] values;

  @Override
  public void setAnnotationCount (ClassFile cf, Object tag, int annotationCount) {
    annotations = new AnnotationInfo[annotationCount];
  }

  @Override
  public void setAnnotationsDone (ClassFile cf, Object tag) {
    if (tag instanceof InfoObject) {
      ((InfoObject) tag).setAnnotations(annotations);
    }
  }

  @Override
  public void setParameterCount (ClassFile cf, Object tag, int parameterCount) {
    parameterAnnotations = new AnnotationInfo[parameterCount][];
  }

  @Override
  public void setParameterAnnotationCount (ClassFile cf, Object tag, int paramIndex, int annotationCount) {
    annotations = new AnnotationInfo[annotationCount];
    parameterAnnotations[paramIndex] = annotations;
  }

  @Override
  public void setParameterAnnotation (ClassFile cf, Object tag, int annotationIndex, String annotationType) {
    curAi = ci.getResolvedAnnotationInfo(Types.getClassNameFromTypeName(annotationType));
    annotations[annotationIndex] = curAi;
  }

  @Override
  public void setParametersDone (ClassFile cf, Object tag) {
    curMi.setParameterAnnotations(parameterAnnotations);
  }


  @Override
  public void setAnnotation (ClassFile cf, Object tag, int annotationIndex, String annotationType) {
    if (tag instanceof InfoObject) {
      curAi = ci.getResolvedAnnotationInfo(Types.getClassNameFromTypeName(annotationType));
      annotations[annotationIndex] = curAi;
    }
  }

  //--- AnnotationInfo entries
    
  @Override
  public void setAnnotationValueCount (ClassFile cf, Object tag, int annotationIndex, int nValuePairs) {
    // if we have values, we need to clone the defined annotation so that we can overwrite entries
    curAi = curAi.cloneForOverriddenValues();
    annotations[annotationIndex] = curAi;
  }

  @Override
  public void setPrimitiveAnnotationValue (ClassFile cf, Object tag, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, Object val) {
    if (arrayIndex >= 0) {
      values[arrayIndex] = val;
    } else {
      curAi.setClonedEntryValue(elementName, val);
    }
  }

  @Override
  public void setStringAnnotationValue (ClassFile cf, Object tag, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, String val) {
    if (arrayIndex >= 0) {
      values[arrayIndex] = val;
    } else {
      curAi.setClonedEntryValue(elementName, val);
    }
  }

  @Override
  public void setClassAnnotationValue (ClassFile cf, Object tag, int annotationIndex, int valueIndex, String elementName,
          int arrayIndex, String typeName) {
    Object val = AnnotationInfo.getClassValue(typeName);
    if (arrayIndex >= 0) {
      values[arrayIndex] = val;
    } else {
      curAi.setClonedEntryValue(elementName, val);
    }
  }

  @Override
  public void setEnumAnnotationValue (ClassFile cf, Object tag, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, String enumType, String enumValue) {
    Object val = AnnotationInfo.getEnumValue(enumType, enumValue);
    if (arrayIndex >= 0) {
      values[arrayIndex] = val;
    } else {
      curAi.setClonedEntryValue(elementName, val);
    }
  }

  @Override
  public void setAnnotationValueElementCount (ClassFile cf, Object tag, int annotationIndex, int valueIndex,
          String elementName, int elementCount) {
    values = new Object[elementCount];
  }

  @Override
  public void setAnnotationValueElementsDone (ClassFile cf, Object tag, int annotationIndex, int valueIndex, String elementName) {
    curAi.setClonedEntryValue(elementName, values);
  }

  //--- common attrs
  @Override
  public void setSignature (ClassFile cf, Object tag, String signature) {
    if (tag instanceof GenericSignatureHolder) {
      ((GenericSignatureHolder) tag).setGenericSignature(signature);
    }
  }

}
