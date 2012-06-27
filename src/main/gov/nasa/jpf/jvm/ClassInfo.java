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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.JPFListener;
import gov.nasa.jpf.classfile.ClassFile;
import gov.nasa.jpf.classfile.ClassFileContainer;
import gov.nasa.jpf.classfile.ClassFileException;
import gov.nasa.jpf.classfile.ClassFileReaderAdapter;
import gov.nasa.jpf.classfile.ClassPath;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.ImmutableList;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.LocationSpec;
import gov.nasa.jpf.util.MethodSpec;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.ObjVector;
import gov.nasa.jpf.util.ObjectList;
import gov.nasa.jpf.util.Source;
import gov.nasa.jpf.util.StringSetMatcher;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;


/**
 * Describes the JVM's view of a java class.  Contains descriptions of the
 * static and dynamic fields, methods, and information relevant to the
 * class.
 */
public class ClassInfo extends InfoObject implements Iterable<MethodInfo>, GenericSignatureHolder, Cloneable {

  //--- ClassInfo states, in chronological order
  // note the somewhat strange, decreasing values - >= 0 (=thread-id) means 
  // we are in clinit
  // ideally, we would have a separate RESOLVED state, but (a) this is somewhat
  // orthogonal to REGISTERED, and - more importantly - (b) we need the
  // superClass instance when initializing our Fields (instance field offsets).
  // Doing the field initialization during resolveClass() seems awkward and
  // error prone (there is not much you can do with an unresolved class then)
  
  // not registered or clinit'ed (but cached in loadedClasses)
  public static final int UNINITIALIZED = -1;
  // 'REGISTERED' simply means 'sei' is set (we have a StaticElementInfo)
  // 'INITIALIZING' is any number >=0, which is the thread objRef that executes the clinit
  public static final int INITIALIZED = -2;


  static JPFLogger logger = JPF.getLogger("gov.nasa.jpf.jvm.ClassInfo");

  static Config config;

  /**
   * ClassLoader that loaded this class.
   */
  protected static final ClassLoader thisClassLoader = ClassInfo.class.getClassLoader();

  /**
   * Map from the classFileUrl of classes to their original ClassInfo instances
   */
   protected static Map<String,ClassInfo> loadedClasses = new HashMap<String, ClassInfo>();

   /**
    * Map from globalIds to instances of ClassInfos. This map includes all the ClassInfos
    * defined by any classloaders.
    */
   protected static Map<Integer,ClassInfo> classes = new HashMap<Integer, ClassInfo>();

  /**
   * optionally used to determine atomic methods of a class (during class loading)
   */
  protected static Attributor attributor;

  /**
   * our abstract factory to createAndInitialize object and class fields
   */
  protected static FieldsFactory fieldsFactory;

  
  static FieldInfo[] emptyFields = new FieldInfo[0];

  static String[] emptyInnerClassNames = new String[0];
  
  static final String UNINITIALIZED_STRING = "UNINITIALIZED"; 
  
  /**
   * support to auto-load listeners from annotations
   */
  static HashSet<String> autoloadAnnotations;
  static HashSet<String> autoloaded;

  /**
   * Name of the class. e.g. "java.lang.String"
   * NOTE - this is the expanded name for builtin types, e.g. "int", but NOT
   * for arrays, which are for some reason in Ldot notation, e.g. "[Ljava.lang.String;" or "[I"
   */
  protected String name;
  
  /** type erased signature of the class. e.g. "Ljava/lang/String;" */
  protected String signature;

  /** Generic type signatures of the class as per para. 4.4.4 of the revised VM spec */
  protected String genericSignature;

  /** The classloader that defined (directly loaded) this class */
  protected ClassLoaderInfo classLoader;

  // various class attributes
  protected boolean      isClass = true;
  protected boolean      isWeakReference = false;
  protected boolean      isObjectClassInfo = false;
  protected boolean      isStringClassInfo = false;
  protected boolean      isRefClassInfo = false;
  protected boolean      isArray = false;
  protected boolean      isEnum = false;
  protected boolean      isReferenceArray = false;
  protected boolean      isAbstract = false;
  protected boolean      isBuiltin = false;

  // that's ultimately where we keep the attributes
  // <2do> this is currently quite redundant, but these are used in reflection
  int modifiers;

  protected MethodInfo   finalizer = null;

  /** type based object attributes (for GC, partial order reduction and
   * property checks)
   */
  protected int elementInfoAttrs = 0;

  /**
   * all our declared methods (we don't flatten, this is not
   * a high-performance VM)
   */
  protected Map<String, MethodInfo> methods;

  /**
   * our instance fields.
   * Note these are NOT flattened, i.e. only contain the declared ones
   */
  protected FieldInfo[] iFields;

  /** the storage size of instances of this class (stored as an int[]) */
  protected int instanceDataSize;

  /** where in the instance data array (int[]) do our declared fields start */
  protected int instanceDataOffset;

  /** total number of instance fields (flattened, not only declared ones) */
  protected int nInstanceFields;

  /**
   * our static fields. Again, not flattened
   */
  protected FieldInfo[] sFields;

  /** the storage size of static fields of this class (stored as an int[]) */
  protected int staticDataSize;

  /** where to get static field values from - it can be used quite frequently
   * (to find out if the class needs initialization, so cache it.
   * BEWARE - this is volatile (has to be reset&restored during backtrack */
  StaticElementInfo sei;

  /**
   * we only set the superClassName upon creation, it is instantiated into
   * a ClassInfo by resolveClass(), which is required to be called before
   * we can createAndInitialize objects of this type
   */
  protected ClassInfo  superClass;

  protected String enclosingClassName;
  protected String enclosingMethodName;

  protected String[] innerClassNames = emptyInnerClassNames;
    
  /** direct ifcSet implemented by this class */
  protected Set<String> interfaceNames;
  
  /** cache of all interfaceNames (parent interfaceNames and interface parents) - lazy eval */
  protected Set<String> allInterfaces;
  
  /** Name of the package. */
  protected String packageName;

  /** this is only set if the classfile has a SourceFile class attribute */
  protected String sourceFileName;

  /** 
   * Uniform resource locater for the class file. NOTE: since for builtin classes
   * there is no class file assigned is set to the typeName 
   */ 
  protected String classFileUrl;

  /** from where the corresponding classfile was loaded (if this is not a builtin) */
  protected ClassFileContainer container;

  /** A unique id associate with this class. */
  protected int uniqueId;

  /**
   * this is the object we use to execute methods in the underlying JVM
   * (it replaces Reflection)
   */
  private NativePeer nativePeer;

  /** Source file associated with the class.*/
  protected Source source;


  /** user defined attribute objects */
  protected Object attr;
  
  static StringSetMatcher enabledAssertionPatterns;
  static StringSetMatcher disabledAssertionPatterns;

  protected boolean enableAssertions;

  /** actions to be taken when an object of this type is gc'ed */
  protected ImmutableList<ReleaseAction> releaseActions; 
          
  
  static boolean init (Config config) {

    ClassInfo.config = config;
    
    reset();
    
    setSourceRoots(config);
    //buildBCELModelClassPath(config);

    attributor = config.getEssentialInstance("vm.attributor.class",
                                                         Attributor.class);

    fieldsFactory = config.getEssentialInstance("vm.fields_factory.class",
                                                FieldsFactory.class);

    enabledAssertionPatterns = StringSetMatcher.getNonEmpty(config.getStringArray("vm.enable_assertions"));
    disabledAssertionPatterns = StringSetMatcher.getNonEmpty(config.getStringArray("vm.disable_assertions"));


    autoloadAnnotations = config.getNonEmptyStringSet("listener.autoload");
    if (autoloadAnnotations != null) {
      autoloaded = new HashSet<String>();

      if (logger.isLoggable(Level.INFO)) {
        for (String s : autoloadAnnotations){
          logger.info("watching for autoload annotation @" + s);
        }
      }
    }

    return true;
  }

  public static boolean isObjectClassInfo (ClassInfo ci){
    return ci.isObjectClassInfo();
  }

  public static boolean isStringClassInfo (ClassInfo ci){
    return ci.isStringClassInfo();
  }

  class Initializer extends ClassFileReaderAdapter {

    @Override
    public void setClass(ClassFile cf, String clsName, String superClsName, int flags, int cpCount) {
      name = Types.getClassNameFromTypeName(clsName);

      String requestedName = cf.getRequestedTypeName();
      // check if the ClassFile does not represent a class with the requested name
      if(requestedName!=null && !requestedName.equals(name)) {
        throw new ClassInfoException("wrong class name, should be " + name, 
                                     "java.lang.NoClassDefFoundError", name);
      }

      // the enclosingClassName is set on demand since it requires loading enclosing class candidates
      // to verify their innerClass attributes
      
      int i = name.lastIndexOf('.');
      packageName = (i>0) ? name.substring(0, i) : "";

      if (superClsName != null){
        // this is where we get recursive
        superClass = loadSuperClass( Types.getClassNameFromTypeName(superClsName));
      }

      computeInheritedAnnotations(superClass);      

      modifiers = flags;
      isClass = ((flags & Modifier.INTERFACE) == 0);

      if (attributor != null){
        attributor.setElementInfoAttributes(ClassInfo.this);
      }
    }

    @Override
    public void setClassAttribute(ClassFile cf, int attrIndex, String name, int attrLength) {
      if (name == ClassFile.SOURCE_FILE_ATTR) {
        cf.parseSourceFileAttr(this, null);
        
      } else if (name == ClassFile.SIGNATURE_ATTR){
        cf.parseSignatureAttr(this, ClassInfo.this);

      } else if (name == ClassFile.RUNTIME_VISIBLE_ANNOTATIONS_ATTR) {
        cf.parseAnnotationsAttr(this, ClassInfo.this);

      } else if (name == ClassFile.RUNTIME_INVISIBLE_ANNOTATIONS_ATTR) {
        //cf.parseAnnotationsAttr(this, ClassInfo.this);
        
      } else if (name == ClassFile.INNER_CLASSES_ATTR){
        cf.parseInnerClassesAttr( this, ClassInfo.this);
        
      } else if(name == ClassFile.ENCLOSING_METHOD_ATTR) {
    	  cf.parseEnclosingMethodAttr(this, ClassInfo.this);
      }
    }

    //--- inner classes
    @Override
    public void setInnerClassCount (ClassFile cf, Object tag, int classCount){
      innerClassNames = new String[classCount];
    }
    
    @Override
    public void setInnerClass (ClassFile cf, Object tag, int innerClsIndex, 
                               String outerName, String innerName, String innerSimpleName, int accessFlags){
      // Ok, this is a total mess - some names are in dot notation, others use '/'
      // and to make it even more confusing, some InnerClass attributes refer NOT
      // to the currently parsed class, so we have to check if we are the outerName,
      // but then 'outerName' can also be null instead of our own name.
      // Oh, and there are also InnerClass attributes that have their own name as inner names
      // (see java/lang/String$CaseInsensitiveComparator or ...System and java/lang/System$1 for instance)
      if (outerName != null){
        outerName = Types.getClassNameFromTypeName(outerName);
      }
        
      innerName = Types.getClassNameFromTypeName(innerName);
      if (!innerName.equals(name)){
        innerClassNames[innerClsIndex] = innerName;
        
      } else {
        // this refers to ourself, and can be a force fight with setEnclosingMethod
        if (outerName != null){ // only set if this is a direct member, otherwise taken from setEnclosingMethod
          enclosingClassName = outerName;
        }
      }
    }
    
    @Override
    public void setEnclosingMethod(ClassFile cf, Object tag, String enclosingClassName, String enclosingMethodName, String descriptor) {
      ClassInfo.this.enclosingClassName = enclosingClassName;
      
      if (enclosingMethodName != null){
        ClassInfo.this.enclosingMethodName = enclosingMethodName + descriptor;
      }
    }
    
    @Override
    public void setInnerClassesDone (ClassFile cf, Object tag) {
      // we have to check if we allocated too many - see the mess above
      int count = 0;
      for (int i=0; i<innerClassNames.length; i++){
        innerClassNames = Misc.stripNullElements(innerClassNames);
      }
    }
    
    //--- source file
    
    @Override
    public void setSourceFile(ClassFile cf, Object tag, String fileName) {
      // we already know the package, so we just prepend it
      if (packageName.length() > 0){
        // Source will take care of proper separator chars later
        sourceFileName = packageName.replace('.', '/') + '/' + fileName;
      } else {
        sourceFileName = fileName;
      }
    }

    //--- interfaces
    Set<String> ifcSet = new HashSet<String>();

    @Override
    public void setInterface(ClassFile cf, int ifcIndex, String ifcName) {
      ifcSet.add(Types.getClassNameFromTypeName(ifcName));
    }

    @Override
    public void setInterfacesDone(ClassFile cf) {
      //loadInterfaceRec(null, ifcSet);
      interfaceNames =  Collections.unmodifiableSet(ifcSet);

      // check if this is an interface and one of its super interfaces is the interface
      // itself
      if(isInterface() && interfaceNames.contains(name)) {
        throw new ClassInfoException("a super interface of " + name + " is itself", 
                                     "java.lang.ClassCircularityError", name);
      }
    }

    //--- fields
    ArrayList<FieldInfo> instanceFields = new ArrayList<FieldInfo>();
    ArrayList<FieldInfo> staticFields = new ArrayList<FieldInfo>();
    int iOff, iIdx, sOff, sIdx;
    FieldInfo curFi; // need to cache for attributes

    @Override
    public void setFieldCount(ClassFile cf, int nFields){
      if (superClass != null) {
        iOff = superClass.instanceDataSize;
        iIdx = superClass.nInstanceFields;
      }
    }

    @Override
    public void setField(ClassFile cf, int fieldIndex, int accessFlags, String name, String descriptor) {
      FieldInfo fi=null;

      if ((accessFlags & Modifier.STATIC) == 0){ // instance field
        fi = FieldInfo.create (ClassInfo.this, name, descriptor, accessFlags, iIdx, iOff);
        instanceFields.add(fi);
        iIdx++;
        iOff += fi.getStorageSize();

      } else {  // static field
        fi = FieldInfo.create (ClassInfo.this, name, descriptor, accessFlags, sIdx, sOff);
        staticFields.add(fi);
        sIdx++;
        sOff += fi.getStorageSize();
      }

      curFi = fi; // for attributes

      if (attributor != null){
        attributor.setFieldInfoAttributes(curFi);
      }
    }

    @Override
    public void setFieldAttribute(ClassFile cf, int fieldIndex, int attrIndex, String name, int attrLength) {
      if (name == ClassFile.SIGNATURE_ATTR){
        cf.parseSignatureAttr(this, curFi);

      } else if (name == ClassFile.CONST_VALUE_ATTR){
        cf.parseConstValueAttr(this, curFi);

      } else if (name == ClassFile.RUNTIME_VISIBLE_ANNOTATIONS_ATTR) {
        cf.parseAnnotationsAttr(this, curFi);

      } else if (name == ClassFile.RUNTIME_INVISIBLE_ANNOTATIONS_ATTR) {
        //cf.parseAnnotationsAttr(this, curFi);
      }
    }

    @Override
    public void setConstantValue(ClassFile cf, Object tag, Object constVal){
      curFi.setConstantValue(constVal);
    }

    @Override
    public void setFieldsDone(ClassFile cf) {
      iFields = instanceFields.toArray(new FieldInfo[instanceFields.size()]);
      sFields = staticFields.toArray(new FieldInfo[staticFields.size()]);
    }

    //--- methods
    CodeBuilder cb;
    MethodInfo curMi;

    @Override
    public void setMethodCount(ClassFile cf, int methodCount){
      methods = new LinkedHashMap<String,MethodInfo>(methodCount);
    }

    @Override
    public void setMethod(ClassFile cf, int methodIndex, int accessFlags, String name, String signature) {
      // maxLocals and maxStack will be set from the Code attribute
      curMi = new MethodInfo( ClassInfo.this, name, signature, -1, -1, accessFlags);

      if (attributor != null){
        attributor.setMethodInfoAttributes(curMi);
      }
    }

    @Override
    public void setMethodDone(ClassFile cf, int methodIndex){
      methods.put(curMi.getUniqueName(), curMi);
    }

    @Override
    public void setMethodAttribute(ClassFile cf, int methodIndex, int attrIndex, String name, int attrLength) {
      if (name == ClassFile.CODE_ATTR){
        cf.parseCodeAttr(this, curMi);

      } else if (name == ClassFile.SIGNATURE_ATTR){
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

    @Override
    public void setExceptionCount(ClassFile cf, Object tag, int exceptionCount) {
      curMi.startTrownExceptions(exceptionCount);
    }

    @Override
    public void setException(ClassFile cf, Object tag, int exceptionIndex, String exceptionType) {
      curMi.setException(exceptionIndex, exceptionType);
    }

    @Override
    public void setExceptionsDone(ClassFile cf, Object tag) {
      curMi.finishThrownExceptions();
    }


    @Override
    public void setCode(ClassFile cf, Object tag, int maxStack, int maxLocals, int codeLength){
      curMi.setMaxLocals(maxLocals);
      curMi.setMaxStack(maxStack);

      if (cb == null){
        cb = createCodeBuilder();
      }
      cb.initialize(cf, curMi);

      cf.parseBytecode(cb, tag, codeLength);
      cb.installCode();
    }

    @Override
    public void setExceptionHandlerTableCount(ClassFile cf, Object tag, int exceptionTableCount) {
      curMi.startExceptionHandlerTable(exceptionTableCount);
    }

    @Override
    public void setExceptionHandler(ClassFile cf, Object tag, int handlerIndex,
            int startPc, int endPc, int handlerPc, String catchType) {
      curMi.setExceptionHandler(handlerIndex, startPc, endPc, handlerPc, catchType);
    }

    @Override
    public void setExceptionHandlerTableDone(ClassFile cf, Object tag) {
      curMi.finishExceptionHandlerTable();
    }

    @Override
    public void setCodeAttributeCount(ClassFile cf, Object tag, int attrCount) {
    }

    @Override
    public void setCodeAttribute(ClassFile cf, Object tag, int attrIndex, String name, int attrLength) {
      if (name == ClassFile.LINE_NUMBER_TABLE_ATTR) {
        cf.parseLineNumberTableAttr(this, tag);

      } else if (name == ClassFile.LOCAL_VAR_TABLE_ATTR) {
        cf.parseLocalVarTableAttr(this, tag);
      }
    }

    @Override
    public void setCodeAttributesDone(ClassFile cf, Object tag) {
    }

    @Override
    public void setLineNumberTableCount(ClassFile cf, Object tag, int lineNumberCount) {
      curMi.startLineNumberTable(lineNumberCount);
    }

    @Override
    public void setLineNumber(ClassFile cf, Object tag, int lineIndex, int lineNumber, int startPc) {
      curMi.setLineNumber(lineIndex, lineNumber, startPc);
    }

    @Override
    public void setLineNumberTableDone(ClassFile cf, Object tag) {
      curMi.finishLineNumberTable();
    }

    @Override
    public void setLocalVarTableCount(ClassFile cf, Object tag, int localVarCount) {
      curMi.startLocalVarTable(localVarCount);
    }

    @Override
    public void setLocalVar(ClassFile cf, Object tag, int localVarIndex,
            String varName, String descriptor, int scopeStartPc, int scopeEndPc, int slotIndex) {
      curMi.setLocalVar(localVarIndex, varName, descriptor, scopeStartPc, scopeEndPc, slotIndex);
    }

    @Override
    public void setLocalVarTableDone(ClassFile cf, Object tag) {
      curMi.finishLocalVarTable();
    }


    //--- annotations

    AnnotationInfo curAi;
    AnnotationInfo[] curPai;
    Object[] values;

    @Override
    public void setAnnotationCount(ClassFile cf, Object tag, int annotationCount){
      if (tag instanceof InfoObject){
        if (tag instanceof ClassInfo){
          if (annotations == null){
            ((InfoObject) tag).startAnnotations(annotationCount);
          }
        } 
        else {
          ((InfoObject) tag).startAnnotations(annotationCount);
        }
      }
    }

    @Override
    public void setAnnotationsDone(ClassFile cf, Object tag){
    }

    @Override
    public void setParameterCount(ClassFile cf, Object tag, int parameterCount){
      if (tag == curMi){
        curMi.startParameterAnnotations(parameterCount);
      }
    }

    @Override
    public void setParameterAnnotationCount(ClassFile cf, Object tag, int paramIndex, int annotationCount){
      if (tag == curMi){
        curPai = new AnnotationInfo[annotationCount];
        curMi.setParameterAnnotations(paramIndex, curPai);
      }
    }

    @Override
    public void setParameterAnnotation(ClassFile cf, Object tag, int annotationIndex, String annotationType) {
      if (tag == curMi){
        curAi = new AnnotationInfo(Types.getClassNameFromTypeName(annotationType));
        curPai[annotationIndex] = curAi;
      }
    }
    
    @Override
    public void setParameterAnnotationsDone(ClassFile cf, Object tag, int paramIndex){
      curMi.finishParameterAnnotations();
    }

    @Override
    public void setParametersDone(ClassFile cf, Object tag){
    }


    @Override
    public void setAnnotation(ClassFile cf, Object tag, int annotationIndex, String annotationType) {
      ClassLoaderInfo cl = ClassLoaderInfo.getCurrentClassLoader();
      if (tag instanceof InfoObject){
        if (AnnotationInfo.annotationAttributes.get(annotationType) == null) {
          curAi = new AnnotationInfo(Types.getClassNameFromTypeName(annotationType), cl.getClassPath());
        } else {
          curAi = new AnnotationInfo(Types.getClassNameFromTypeName(annotationType));
        }
        if (tag instanceof ClassInfo) {
          if (((InfoObject)tag).getAnnotation(curAi.getName()) == null) {
            addAnnotation(curAi);
          } else {
            ((InfoObject)tag).getAnnotation(curAi.getName()).setInherited(false);
          }
        } else {
          ((InfoObject)tag).setAnnotation(annotationIndex, curAi);
        }
      }
    }

    @Override
    public void setAnnotationValueCount(ClassFile cf, Object tag, int annotationIndex, int nValuePairs){
      curAi.startEntries(nValuePairs);
    }

    @Override
    public void setPrimitiveAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int arrayIndex, Object val){
      if (arrayIndex >= 0){
        values[arrayIndex] = val;
      } else {
        curAi.setValue(valueIndex, elementName, val);
      }
    }

    @Override
    public void setStringAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int arrayIndex, String val){
      if (arrayIndex >= 0){
        values[arrayIndex] = val;
      } else {
        curAi.setValue(valueIndex, elementName, val);
      }
    }

    @Override
    public void setClassAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex, String elementName,
            int arrayIndex, String typeName){
      Object val = AnnotationInfo.getClassValue(typeName);
      if (arrayIndex >= 0){
        values[arrayIndex] = val;
      } else {
        curAi.setValue(valueIndex, elementName, val);
      }
    }

    @Override
    public void setEnumAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int arrayIndex, String enumType, String enumValue){
      Object val = AnnotationInfo.getEnumValue(enumType, enumValue);
      if (arrayIndex >= 0){
        values[arrayIndex] = val;
      } else {
        curAi.setValue(valueIndex, elementName, val);
      }
    }

    @Override
    public void setAnnotationValueElementCount(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int elementCount){
      values = new Object[elementCount];
    }

    @Override
    public void setAnnotationValueElementsDone(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName){
      curAi.setValue(valueIndex, elementName, values);
    }

    @Override
    public void setAnnotationValuesDone(ClassFile cf, Object tag, int annotationIndex){
      checkAnnotationDefaultValues(curAi);
    }


    //--- common attrs
    @Override
    public void setSignature(ClassFile cf, Object tag, String signature){
      if (tag instanceof GenericSignatureHolder){
        ((GenericSignatureHolder)tag).setGenericSignature(signature);
      }
    }
  }

  protected ClassInfo(ClassFile cf) throws ClassFileException {
    Initializer reader = new Initializer();
    cf.parse(reader);
  }

  public ClassInfo(ClassFile cf, ClassLoaderInfo classLoader, String url) throws ClassFileException {
    this.classLoader = classLoader;

    Initializer reader = new Initializer();
    cf.parse(reader); // this does the heavy lifting for ClassInfo init

    staticDataSize = computeStaticDataSize();
    instanceDataSize = computeInstanceDataSize();
    instanceDataOffset = computeInstanceDataOffset();
    nInstanceFields = (superClass != null) ?
      superClass.nInstanceFields + iFields.length : iFields.length;

    source = null;
    if (sourceFileName == null){ // apparently some classfiles don't have a SourceFile attribute?
      sourceFileName = computeSourceFileName();
    }

    isStringClassInfo = isStringClassInfo0();
    isObjectClassInfo = isObjectClassInfo0();
    isRefClassInfo = isRefClassInfo0();
    isWeakReference = isWeakReference0();
    finalizer = getFinalizer0();
    isAbstract = (modifiers & Modifier.ABSTRACT) != 0;
    isEnum = isEnum0();

    enableAssertions = getAssertionStatus();

    processJPFConfigAnnotation();
    loadAnnotationListeners();

    classFileUrl = url;
    addOriginalClass(this);

    // the 'sei' field gets initialized during registerClass(ti), since
    // it needs to be linked to a corresponding java.lang.Class object which
    // we can't createAndInitialize until we have a ThreadInfo context

    // be advised - we don't have fields initialized before initializeClass(ti,insn)
    // gets called

    // Used to execute native methods (in JVM land).
    // This needs to be initialized AFTER we get our
    // MethodInfos, since it does a reverse lookup to determine which
    // ones are handled by the peer (by means of setting MethodInfo attributes)
    nativePeer = NativePeer.getNativePeer(this);
    checkUnresolvedNativeMethods();
    
    if (superClass != null){
    // flatten so that it becomes more efficient to process at sweep time
      releaseActions = superClass.releaseActions;
    }
    
    JVM.getVM().notifyClassLoaded(this);
  }


  /**
   * ClassInfo ctor used for builtin types (arrays and primitive types)
   * i.e. classes we don't have class files for
   */
  protected ClassInfo (String builtinClassName, ClassLoaderInfo classLoader) {
    this.classLoader = classLoader;

    isArray = (builtinClassName.charAt(0) == '[');
    isReferenceArray = isArray && builtinClassName.endsWith(";");
    isBuiltin = true;

    name = builtinClassName;

    logger.log(Level.FINE, "generating builtin class: %1$s", name);

    packageName = ""; // builtin classes don't reside in java.lang !
    sourceFileName = null;
    source = null;
    genericSignature = "";

    // no fields
    iFields = emptyFields;
    sFields = emptyFields;

    if (isArray) {
      superClass = getResolvedClassInfo("java.lang.Object");
      interfaceNames = loadArrayInterfaces();
      methods = loadArrayMethods();
    } else {
      superClass = null; // strange, but true, a 'no object' class
      interfaceNames = loadBuiltinInterfaces(name);
      methods = loadBuiltinMethods(name);
    }

    enableAssertions = true; // doesn't really matter - no code associated

    classFileUrl = name;
    addOriginalClass(this);
    
    JVM.getVM().notifyClassLoaded(this);
  }

  private static void addOriginalClass(ClassInfo ci) {
    if(ci.classFileUrl != null) {
      loadedClasses.put(ci.classFileUrl, ci);
    }
  }

  private static ClassInfo getOriginalClassInfo(String url) {
    return loadedClasses.get(url);
  }

  /**
   * createAndInitialize a fully synthetic implementation of an Annotation proxy
   */
  ClassInfo(ClassInfo annotationCls, String name, ClassLoaderInfo classLoader, String url) {
    this.classLoader = classLoader;

    this.name = name;
    isClass = true;

    //superClass = objectClassInfo;
    superClass = ClassInfo.getResolvedClassInfo("gov.nasa.jpf.AnnotationProxyBase");

    interfaceNames = new HashSet<String>();
    interfaceNames.add(annotationCls.name);
    packageName = annotationCls.packageName;
    sourceFileName = annotationCls.sourceFileName;
    genericSignature = annotationCls.genericSignature;

    sFields = new FieldInfo[0]; // none
    staticDataSize = 0;

    methods = new HashMap<String, MethodInfo>();
    iFields = new FieldInfo[annotationCls.methods.size()];
    nInstanceFields = iFields.length;

    // all accessor methods of ours make it into iField/method combinations
    int idx = 0;
    int off = 0;  // no super class
    for (MethodInfo mi : annotationCls.getDeclaredMethodInfos()) {
      String mname = mi.getName();
      String mtype = mi.getReturnType();
      String genericSignature = mi.getGenericSignature();

      // createAndInitialize an instance field for it
      FieldInfo fi = FieldInfo.create(this, mname, mtype, 0, idx, off);
      iFields[idx++] = fi;
      off += fi.getStorageSize();

      fi.setGenericSignature(genericSignature);

      // now createAndInitialize a public accessor for this field
      MethodInfo pmi = new MethodInfo(this, mname, mi.getSignature(), 1, 2, Modifier.PUBLIC);
      pmi.setGenericSignature(genericSignature);

      CodeBuilder cb = pmi.createCodeBuilder();

      cb.aload(0);
      cb.getfield(mname, name, mtype);
      if (fi.isReference()) {
        cb.areturn();
      } else {
        if (fi.getStorageSize() == 1) {
          cb.ireturn();
        } else {
          cb.lreturn();
        }
      }

      cb.installCode();

      methods.put(pmi.getUniqueName(), pmi);
    }

    instanceDataSize = computeInstanceDataSize();
    instanceDataOffset = 0;

    classFileUrl = url;
    addOriginalClass(this);
    
    JVM.getVM().notifyClassLoaded(this);
  }


  protected String computeSourceFileName(){
    return name.replace('.', '/') + ".java";
  }


  /**
   * this is called from the annotated ClassInfo - the annotation type would
   * have to be resolved through the same classpath
   *
   * override this in case resolving annotation types is not wanted (e.g. for unit tests)
   */
  protected void checkAnnotationDefaultValues(AnnotationInfo ai){
    ClassLoaderInfo cl = ClassLoaderInfo.getCurrentClassLoader();
    ai.checkDefaultValues(cl.getClassPath());
  }

  /**
   * this returns a CodeBuilder that still needs to be initialized. It is here
   * to allow overriding it in derived classes, e.g. to facilitate unit tests
   * or specialized instruction factories
   */
  protected CodeBuilder createCodeBuilder(){
    InstructionFactory insnFactory = MethodInfo.getInstructionFactory();
    insnFactory.setClassInfoContext(this);

    return new CodeBuilder(insnFactory, null, null);
  }

  void checkUnresolvedNativeMethods(){
    for (MethodInfo mi : methods.values()){
      if (mi.isUnresolvedNativeMethod()){
        NativeMethodInfo nmi = new NativeMethodInfo(mi, null, nativePeer);
        nmi.replace(mi);
      }
    }
  }



  void processJPFConfigAnnotation() {
    AnnotationInfo ai = getAnnotation("gov.nasa.jpf.annotation.JPFConfig");
    if (ai != null) {
      for (String s : ai.getValueAsStringArray()) {
        config.parse(s);
      }
    }
  }

  void loadAnnotationListeners () {
    if (autoloadAnnotations != null) {
      autoloadListeners(annotations); // class annotations

      for (int i=0; i<sFields.length; i++) {
        autoloadListeners(sFields[i].getAnnotations());
      }

      for (int i=0; i<iFields.length; i++) {
        autoloadListeners(iFields[i].getAnnotations());
      }

      // method annotations are checked during method loading
      // (to avoid extra iteration)
    }
  }

  void autoloadListeners(AnnotationInfo[] annos) {
    if ((annos != null) && (autoloadAnnotations != null)) {
      for (AnnotationInfo ai : annos) {
        String aName = ai.getName();
        if (autoloadAnnotations.contains(aName)) {
          if (!autoloaded.contains(aName)) {
            autoloaded.add(aName);
            String key = "listener." + aName;
            String defClsName = aName + "Checker";
            try {
              JPFListener listener = config.getInstance(key, JPFListener.class, defClsName);
              
              JPF jpf = JVM.getVM().getJPF(); // <2do> that's a BAD access path
              jpf.addUniqueTypeListener(listener);

              if (logger.isLoggable(Level.INFO)){
                logger.info("autoload annotation listener: @", aName, " => ", listener.getClass().getName());
              }

            } catch (JPFConfigException cx) {
              logger.warning("no autoload listener class for annotation " + aName +
                             " : " + cx.getMessage());
              autoloadAnnotations.remove(aName);
            }
          }
        }
      }

      if (autoloadAnnotations.isEmpty()) {
        autoloadAnnotations = null;
      }
    }
  }

  /**
   * Returns the class loader that 
   */
  public ClassLoaderInfo getClassLoaderInfo() {
    return classLoader;
  }

  /**
   * required by InfoObject interface
   */
  public ClassInfo getClassInfo() {
    return this;
  }

  boolean getAssertionStatus () {
    return StringSetMatcher.isMatch(name, enabledAssertionPatterns, disabledAssertionPatterns);
  }
  
  public String getGenericSignature() {
    return genericSignature;
  }

  public void setGenericSignature(String sig){
    genericSignature = sig;
  }
  
  public boolean isArray () {
    return isArray;
  }

  public boolean isEnum () {
    return isEnum;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public boolean isBuiltin(){
    return isBuiltin;
  }
  
  public boolean isInterface() {
    return ((modifiers & Modifier.INTERFACE) != 0);
  }

  public boolean isReferenceArray () {
    return isReferenceArray;
  }

  public boolean isObjectClassInfo() {
    return isObjectClassInfo;
  }

  public boolean isStringClassInfo() {
    return isStringClassInfo;
  }

  public static ClassInfo getClassInfo(int uniqueId) {
    return classes.get(uniqueId);
  }

  public static ClassInfo getResolvedClassInfo (String className) throws ClassInfoException {
    ClassLoaderInfo cl = ClassLoaderInfo.getCurrentClassLoader();

    return cl.getResolvedClassInfo(className);
  }

  /**
   * obtain ClassInfo object for given class name
   *
   * this method does not return a value unless the required class, all
   * its super classes, and all implemented interfaceNames of this class hierarchy
   * can be resolved, i.e. this method is called recursively through the
   * ClassInfo constructor.
   *
   * Returned ClassInfo objects are not registered yet, i.e. still have to
   * be added to the StaticArea, and don't have associated java.lang.Class
   * objects until registerClass(ti) is called.
   *
   * Before any field or method access, the class also has to be initialized,
   * which can include overlayed execution of &lt;clinit&gt; methods, which is done
   * by calling initializeClass(ti,insn)
   *
   * @param className fully qualified classname to get a ClassInfo for
   * @return the ClassInfo for the classname passed in
   * @throws NoClassInfoException with missing className as message
   *
   * @see ClassInfo#registerClass(ThreadInfo)
   * @see ClassInfo#initializeClass(ThreadInfo)
   *
   * <2do> we could also separate resolveClass(), but this would require an
   * additional state {resolved,registered,initialized}, and it is questionable
   * what a non-resolvable ClassInfo would be good for anyways
   */
  public static ClassInfo getResolvedClassInfo (String className, ClassLoaderInfo cl, ClassPath.Match match) throws ClassInfoException {

    if (className == null) {
      return null;
    }

    String typeName = Types.getClassNameFromTypeName(className);

    String url = computeClassFileUrl(match, typeName, cl);
    ClassInfo ci = getOriginalClassInfo(url);

    if (ci != null) {
      return ci;
    }

    if (isBuiltinClass(typeName)) {
      if(cl.isSystemClassLoader()) {
        // this is a array or builtin type class - there's no class file for this, it
        // gets automatically generated by the VM
        return new ClassInfo(typeName, cl);
      } else{
        throw new ClassInfoException("non-system classloader cannot load the builtin class: " + typeName, 
                                     "java.lang.NoClassDefFoundError", typeName);
      }
    }

    logger.finer("resolve classinfo: ", className);

    return defineClass(typeName, cl, match, url);
  }

  private static ClassInfo defineClass(String typeName, ClassLoaderInfo cl, ClassPath.Match match, String url){
    try {
      if (match == null){
        throw new ClassInfoException("the class, " + typeName + ", is not found in the classloader search path", 
                                     "java.lang.NoClassDefFoundError", typeName);
      }

      ClassFile cf = new ClassFile( typeName, match.getBytes());
      
      JVM.getVM().notifyLoadClass(cf); // allow on-the-fly classfile modification

      ClassInfo ci = new ClassInfo(cf, cl, url);
      ci.setContainer(match.container);

      return ci;
      
    } catch (ClassFileException cfx){
      throw new ClassInfoException("error reading class " + typeName, 
                                   "java.lang.NoClassDefFoundError", typeName);
    }
  }

  private static ClassInfo defineClass(String typeName, byte[] data, int offset, int length, ClassLoaderInfo cl, String url) {
    try {
      ClassFile cf = new ClassFile( typeName, data, offset);
      
      JVM.getVM().notifyLoadClass(cf); // allow on-the-fly classfile modification

      return new ClassInfo(cf, cl, url);

    } catch (ClassFileException cfx){
      throw new JPFException("error reading class " + typeName, cfx);
    }

  }

  public static ClassInfo getResolvedClassInfo (String className, byte[] buffer, int offset, int length) throws ClassInfoException {
    ClassLoaderInfo cl = ClassLoaderInfo.getCurrentClassLoader();

    // for now, we do not keep dynamic proxy classes, which are defined through 
    // the java.lang.reflect.Proxy native peer, in the loadedClasses map
    return cl.getResolvedClassInfo(className, buffer, offset, length, null);
  }

  public static ClassInfo getResolvedClassInfo (String className, byte[] buffer, int offset, int length, ClassLoaderInfo cl, ClassPath.Match match) throws ClassInfoException {
    if (className == null) {
      return null;   
    }
    
    String typeName = Types.getClassNameFromTypeName(className);

    String url = (match != null)? computeClassFileUrl(match, typeName, cl): typeName;
    ClassInfo ci = getOriginalClassInfo(url);
    
    if (ci != null) {
      return ci;
    } else if (isBuiltinClass(typeName)) {
      if(cl.isSystemClassLoader()) {
        // this is a array or builtin type class - there's no class file for this, it
        // gets automatically generated by the VM
        return new ClassInfo(typeName, cl);
      } else{
        throw new ClassInfoException("non-system classloader cannot load the builtin class: " + typeName, 
                                     "java.lang.NoClassDefFoundError", typeName);
      }
    } else {
      return defineClass(typeName, buffer, offset, length, cl, (match != null)? url: null);
    }
  }

  /**
   * obtain ClassInfo from context that does not care about resolution, i.e.
   * does not check for NoClassInfoExceptions
   *
   * @param className fully qualified classname to get a ClassInfo for
   * @return null if class was not found
   */
  public static ClassInfo tryGetResolvedClassInfo (String className){
    ClassLoaderInfo cl = ClassLoaderInfo.getCurrentClassLoader();

    return cl.tryGetResolvedClassInfo(className);
  }

  public static ClassInfo getAnnotationProxy (ClassInfo ciAnnotation){
    StaticArea sa = JVM.getVM().getCurrentStaticArea();
    ThreadInfo ti = ThreadInfo.getCurrentThread();

    // make sure the annotationCls is initialized (no code there)
    if (!ciAnnotation.isInitialized()) {
      if (!sa.containsClass(ciAnnotation.getName())) {
        ciAnnotation.registerClass(ti);
        ciAnnotation.setInitialized(); // no clinit
      }
    }

    String url = computeProxyUrl(ciAnnotation);
    ClassInfo ci = getOriginalClassInfo(url);

    if (ci == null){
      String cname = ciAnnotation.getName() + "$Proxy";
      ci = new ClassInfo(ciAnnotation, cname, ciAnnotation.classLoader, url);
      if (!ci.isInitialized()){
        ci.registerClass(ti);
        ci.setInitialized();
      }
    }

    return ci;
  }


  public boolean areAssertionsEnabled() {
    return enableAssertions;
  }

  public boolean hasInstanceFields () {
    return (instanceDataSize > 0);
  }

  public ElementInfo getClassObject(){
    if (sei != null){
      int objref = sei.getClassObjectRef();
      return JVM.getVM().getElementInfo(objref);
    }

    return null;
  }

  public int getClassObjectRef () {
    return (sei != null) ? sei.getClassObjectRef() : -1;
  }

  public ClassFileContainer getContainer(){
    return container;
  }
  
  public void setContainer (ClassFileContainer c){
    container = c;
  }

  protected static String computeClassFileUrl(ClassPath.Match match, String typeName, ClassLoaderInfo cl) {
    // it has to be a built-in class or annotation proxy!
    if(match == null) {
      if(ClassInfo.isBuiltinClass(typeName)){
        return typeName;
      } else if(typeName.endsWith("$Proxy")) {
        String annotationName = typeName.substring(0, typeName.lastIndexOf('$'));
        ClassInfo annotationCls = cl.getDefinedClassInfo(annotationName);
        return computeProxyUrl(annotationCls);
      } else {
        return null;
      }
    }
    
    return match.container.getURL() + typeName.replace('.', '/') + ".class";
  }

  // Builds urls for annotation class proxies
  private static String computeProxyUrl(ClassInfo annotationCls) {
    String annotationUrl = annotationCls.getClassFileUrl();
    return annotationUrl.substring(0, annotationUrl.lastIndexOf(".class")) + "$Proxy";
  }
  
  public String getClassFileUrl (){
    return classFileUrl;
  }

  //--- type based object release actions
  
  public boolean hasReleaseAction (ReleaseAction action){
    return (releaseActions != null) && releaseActions.contains(action);
  }
  
  /**
   * NOTE - this can only be set *before* subclasses are loaded (e.g. from classLoaded() notification) 
   */
  public void addReleaseAction (ReleaseAction action){
    // flattened in ctor to super releaseActions
    releaseActions = new ImmutableList<ReleaseAction>( action, releaseActions);
  }
  
  /**
   * recursively process release actions registered for this type or any of
   * its super types (only classes). The releaseAction list is flattened during
   * ClassInfo initialization, to reduce runtime overhead during GC sweep
   */
  public void processReleaseActions (ElementInfo ei){
    if (superClass != null){
      superClass.processReleaseActions(ei);
    }
    
    if (releaseActions != null) {
      for (ReleaseAction action : releaseActions) {
        action.release(ei);
      }
    }
  }
  
  public int getModifiers() {
    return modifiers;
  }

  /**
   * Note that 'uniqueName' is the name plus the argument type part of the
   * signature, i.e. everything that's relevant for overloading
   * (besides saving some const space, we also ease reverse lookup
   * of natives that way).
   * Note also that we don't have to make any difference between
   * class and instance methods, because that just matters in the
   * INVOKExx instruction, when looking up the relevant ClassInfo to start
   * searching in (either by means of the object type, or by means of the
   * constpool classname entry).
   */
  public MethodInfo getMethod (String uniqueName, boolean isRecursiveLookup) {
    MethodInfo mi = methods.get(uniqueName);

    if ((mi == null) && isRecursiveLookup && (superClass != null)) {
      mi = superClass.getMethod(uniqueName, true);
    }

    return mi;
  }

  /**
   * if we don't know the return type
   * signature is in paren/dot notation
   */
  public MethodInfo getMethod (String name, String signature, boolean isRecursiveLookup) {
    MethodInfo mi = null;
    String matchName = name + signature;

    for (Map.Entry<String, MethodInfo>e : methods.entrySet()) {
      if (e.getKey().startsWith(matchName)){
        mi = e.getValue();
        break;
      }
    }

    if ((mi == null) && isRecursiveLookup && (superClass != null)) {
      mi = superClass.getMethod(name, signature, true);
    }

    return mi;
  }


  /**
   * almost the same as above, except of that Class.getMethod() doesn't specify
   * the return type. Not sure if that is a bug in the Java specs waiting to be
   * fixed, or if covariant return types are not allowed in reflection lookup.
   * Until then, it's awfully inefficient
   */
  public MethodInfo getReflectionMethod (String fullName, boolean isRecursiveLookup) {
    for (Map.Entry<String, MethodInfo>e : methods.entrySet()) {
      String name = e.getKey();
      if (name.startsWith(fullName)) {
        return e.getValue();
      }
    }

    if (isRecursiveLookup && (superClass != null)) {
      return superClass.getReflectionMethod(fullName, true);
    }

    return null;
  }
  
  /**
   * iterate over all methods of this class (and it's superclasses), until
   * the provided MethodLocator tells us it's done
   */
  public void matchMethods (MethodLocator loc) {
    for (MethodInfo mi : methods.values()) {
      if (loc.match(mi)) {
        return;
      }
    }
    if (superClass != null) {
      superClass.matchMethods(loc);
    }
  }

  /**
   * iterate over all methods declared in this class, until the provided
   * MethodLocator tells us it's done
   */
  public void matchDeclaredMethods (MethodLocator loc) {
    for (MethodInfo mi : methods.values()) {
      if (loc.match(mi)) {
        return;
      }
    }
  }

  public Iterator<MethodInfo> iterator() {
    return new Iterator<MethodInfo>() {
      ClassInfo ci = ClassInfo.this;
      Iterator<MethodInfo> it = ci.methods.values().iterator();

      public boolean hasNext() {
        if (it.hasNext()) {
          return true;
        } else {
          if (ci.superClass != null) {
            ci = ci.superClass;
            it = ci.methods.values().iterator();
            return it.hasNext();
          } else {
            return false;
          }
        }
      }

      public MethodInfo next() {
        if (hasNext()) {
          return it.next();
        } else {
          throw new NoSuchElementException();
        }
      }

      public void remove() {
        // not supported
        throw new UnsupportedOperationException("can't remove methods");
      }
    };
  }
  
  public Iterator<MethodInfo> declaredMethodIterator() {
    return methods.values().iterator();
  }

  /**
   * Search up the class hierarchy to find a static field
   * @param fName name of field
   * @return null if field name not found (not declared)
   */
  public FieldInfo getStaticField (String fName) {
    FieldInfo fi;
    ClassInfo c = this;

    while (c != null) {
      fi = c.getDeclaredStaticField(fName);
      if (fi != null) return fi;
      c = c.superClass;
    }

    //interfaceNames can have static fields too
    for (String interface_name : getAllInterfaces()) {
        fi = ClassInfo.getResolvedClassInfo(interface_name).getDeclaredStaticField(fName);
        if (fi != null) return fi;
    }

    return null;
  }

  public Object getStaticFieldValueObject (String id){
    ClassInfo c = this;
    Object v;

    while (c != null){
      ElementInfo sei = c.getStaticElementInfo();
      v = sei.getFieldValueObject(id);
      if (v != null){
        return v;
      }
      c = c.getSuperClass();
    }

    return null;
  }

  public FieldInfo[] getDeclaredStaticFields() {
    return sFields;
  }

  public FieldInfo[] getDeclaredInstanceFields() {
    return iFields;
  }

  /**
   * FieldInfo lookup in the static fields that are declared in this class
   * <2do> pcm - should employ a map at some point, but it's usually not that
   * important since we can cash the returned FieldInfo in the PUT/GET_STATIC insns
   */
  public FieldInfo getDeclaredStaticField (String fName) {
    for (int i=0; i<sFields.length; i++) {
      if (sFields[i].getName().equals(fName)) return sFields[i];
    }

    return null;
  }

  /**
   * base relative FieldInfo lookup - the workhorse
   * <2do> again, should eventually use Maps
   * @param fName the field name
   */
  public FieldInfo getInstanceField (String fName) {
    FieldInfo fi;
    ClassInfo c = this;

    while (c != null) {
      fi = c.getDeclaredInstanceField(fName);
      if (fi != null) return fi;
      c = c.superClass;
    }

    return null;
  }

  /**
   * FieldInfo lookup in the fields that are declared in this class
   */
  public FieldInfo getDeclaredInstanceField (String fName) {
    for (int i=0; i<iFields.length; i++) {
      if (iFields[i].getName().equals(fName)) return iFields[i];
    }

    return null;
  }
  
  public String getSignature() {
    if (signature == null) {
      signature = Types.getTypeSignature(name, false);
    }
    
    return signature;     
  }

  /**
   * Returns the name of the class.  e.g. "java.lang.String".  similar to
   * java.lang.Class.getName().
   */
  public String getName () {
    return name;
  }

  public String getSimpleName () {
    int i;
    String enclosingClassName = getEnclosingClassName();
    
    if(enclosingClassName!=null){
      i = enclosingClassName.length();      
    } else{
      i = name.lastIndexOf('.');
    }
    
    return name.substring(i+1);
  }

  public String getPackageName () {
    return packageName;
  }

  /**
   * The uniqueId for ClassInfo is set to an integer where the two left bytes store the
   * global id of the classloader that defines the class and the right two bytes store 
   * the ref that refers to the class object in the JPF heap
   * 
   * @limit This requires that both the classloader.gid, and sei index should not exceed 
   * 2^16
   */
  public void setUniqueId() {
    uniqueId = (classLoader.gid << 16) + sei.getObjectRef();
  }

  public int getUniqueId() {
    return uniqueId;
  }

  public int getFieldAttrs (int fieldIndex) {
    fieldIndex = 0; // Get rid of IDE warning
     
    return 0;
  }

  public void setElementInfoAttrs (int attrs){
    elementInfoAttrs = attrs;
  }

  public void addElementInfoAttr (int attr){
    elementInfoAttrs |= attr;
  }

  public int getElementInfoAttrs () {
    return elementInfoAttrs;
  }

  public Source getSource () {
    if (source == null) {
      source = loadSource();
    }

    return source;
  }

  public String getSourceFileName () {
    return sourceFileName;
  }

  /**
   * Returns the information about a static field.
   */
  public FieldInfo getStaticField (int index) {
    return sFields[index];
  }

  /**
   * Returns the name of a static field.
   */
  public String getStaticFieldName (int index) {
    return getStaticField(index).getName();
  }

  /**
   * Checks if a static method call is deterministic, but only for
   * abtraction based determinism, due to Bandera.choose() calls
   */
  public boolean isStaticMethodAbstractionDeterministic (ThreadInfo th,
                                                         MethodInfo mi) {
    //    Reflection r = reflection.instantiate();
    //    return r.isStaticMethodAbstractionDeterministic(th, mi);
    // <2do> - still has to be implemented
     
    th = null;  // Get rid of IDE warning
    mi = null;
     
    return true;
  }

  /**
   * Return the super class.
   */
  public ClassInfo getSuperClass () {
    return superClass;
  }

  /**
   * return the ClassInfo for the provided superclass name. If this is equal
   * to ourself, return this (a little bit strange if we hit it in the first place)
   */
  public ClassInfo getSuperClass (String clsName) {
    if (clsName.equals(name)) return this;

    if (superClass != null) {
      return superClass.getSuperClass(clsName);
    } else {
      return null;
    }
  }
  
  /**
   * beware - this loads (but not yet registers) the enclosing class
   */
  public String getEnclosingClassName(){
    return enclosingClassName;
  }
  
  /**
   * beware - this loads (but not yet registers) the enclosing class
   */
  public ClassInfo getEnclosingClassInfo() {
    String enclName = getEnclosingClassName();
    return (enclName == null ? null : getResolvedClassInfo(enclName));
  }

  public String getEnclosingMethodName(){
    return enclosingMethodName;
  }

  /**
   * same restriction as getEnclosingClassInfo() - might not be registered/initialized
   */
  public MethodInfo getEnclosingMethodInfo(){
    MethodInfo miEncl = null;
    
    if (enclosingMethodName != null){
      ClassInfo ciIncl = getEnclosingClassInfo();
      miEncl = ciIncl.getMethod( enclosingMethodName, false);
    }
    
    return miEncl;
  }
  
  /**
   * Returns true if the class is a system class.
   */
  public boolean isSystemClass () {
    return name.startsWith("java.") || name.startsWith("javax.");
  }

  /**
   * <2do> that's stupid - we should use subclasses for builtin and box types
   */
  public boolean isBoxClass () {
    if (name.startsWith("java.lang.")) {
      String rawType = name.substring(10);
      if (rawType.startsWith("Boolean") ||
          rawType.startsWith("Byte") ||
          rawType.startsWith("Character") ||
          rawType.startsWith("Integer") ||
          rawType.startsWith("Float") ||
          rawType.startsWith("Long") ||
          rawType.startsWith("Double")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the type of a class.
   */
  public String getType () {
    if (!isArray) {
      return "L" + name.replace('.', '/') + ";";
    } else {
      return name;
    }
  }

  /**
   * is this a (subclass of) WeakReference? this must be efficient, since it's
   * called in the mark phase on all live objects
   */
  public boolean isWeakReference () {
    return isWeakReference;
  }

  /**
   * note this only returns true is this is really the java.lang.ref.Reference classInfo
   */
  public boolean isReferenceClassInfo () {
    return isRefClassInfo;
  }

  /**
   * whether this refers to a primitive type.
   */
  public boolean isPrimitive() {
    return superClass == null && !isObjectClassInfo();
  }


  boolean hasRefField (int ref, Fields fv) {
    ClassInfo c = this;

    do {
      FieldInfo[] fia = c.iFields;
      for (int i=0; i<fia.length; i++) {
        FieldInfo fi = c.iFields[i];
        if (fi.isReference() && (fv.getIntValue( fi.getStorageOffset()) == ref)) return true;
      }
      c = c.superClass;
    } while (c != null);

    return false;
  }

  boolean hasImmutableInstances () {
    return ((elementInfoAttrs & ElementInfo.ATTR_IMMUTABLE) != 0);
  }

  public boolean hasInstanceFieldInfoAttr (Class<?> type){
    for (int i=0; i<nInstanceFields; i++){
      if (getInstanceField(i).hasAttr(type)){
        return true;
      }
    }
    
    return false;
  }
  
  public NativePeer getNativePeer () {
    return nativePeer;
  }

  /**
   * Returns true if the given class is an instance of the class
   * or interface specified.
   */
  public boolean isInstanceOf (String cname) {
    if (isPrimitive()) { // no inheritance for builtin types
      return Types.getJNITypeCode(name).equals(cname);

    } else {
      cname = Types.getClassNameFromTypeName(cname);

      for (ClassInfo c = this; c != null; c = c.superClass) {
        if (c.name.equals(cname)) {
          return true;
        }
      }

      return getAllInterfaces().contains(cname);
    }
  }

  public boolean isInstanceOf (ClassInfo ci) {
    return isInstanceOf(ci.name);
  }

  public boolean isInnerClassOf (String enclosingName){
    // don't register or initialize yet
    ClassInfo ciEncl = tryGetResolvedClassInfo( enclosingName);
    if (ciEncl != null){
      return ciEncl.hasInnerClass(name);
    } else {
      return false;
    }
  }
  
  public boolean hasInnerClass (String innerName){
    for (int i=0; i<innerClassNames.length; i++){
      if (innerClassNames[i].equals(innerName)){
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * clean up statics for another 'main' run
   */
  public static void reset () {
    loadedClasses.clear();
  }

  public static int getNumberOfLoadedClasses() {
    return loadedClasses.size();
  }

  public static ClassInfo[] getLoadedClasses() {
    return loadedClasses.values().toArray(new ClassInfo[loadedClasses.size()]);
  }

  public static String makeModelClassPath (Config config) {
    StringBuilder buf = new StringBuilder(256);
    String ps = File.pathSeparator;
    String v;

    for (File f : config.getPathArray("boot_classpath")){
      buf.append(f.getAbsolutePath());
      buf.append(ps);
    }

    for (File f : config.getPathArray("classpath")){
      buf.append(f.getAbsolutePath());
      buf.append(ps);
    }

    // finally, we load from the standard Java libraries
    v = System.getProperty("sun.boot.class.path");
    if (v != null) {
      buf.append(v);
    }
    
    return buf.toString();
  }

  protected static Set<String> loadArrayInterfaces () {
    Set<String> interfaces;

    interfaces = new HashSet<String>();
    interfaces.add("java.lang.Cloneable");
    interfaces.add("java.io.Serializable");

    return Collections.unmodifiableSet(interfaces);
  }

  protected static Set<String> loadBuiltinInterfaces (String type) {
    type = null; // Get rid of IDE warning 
     
    return Collections.unmodifiableSet(new HashSet<String>(0));
  }


  /**
   * Loads the ClassInfo for named class.
   * @param set a Set to which the interface names (String) are added
   * @param ifcSet class to find interfaceNames for.
   */
  void loadInterfaceRec (Set<String> set, Set<String> interfaces) throws ClassInfoException {
    if (interfaces != null) {
      for (String iname : interfaces) {

        ClassInfo ci = getResolvedClassInfo(iname);

        if (set != null){
          set.add(iname);
        }

        loadInterfaceRec(set, ci.interfaceNames);
      }
    }
  }

  /**
   * get the direct interface names of this class
   */
  protected Set<String> loadInterfaces (String[] ifcNames) throws ClassInfoException {
    Set<String> interfaces = new HashSet<String>();

    for (String iname : ifcNames){
      interfaces.add(iname);
    }

    loadInterfaceRec(null, interfaces);

    return Collections.unmodifiableSet(interfaces);
  }


  int computeInstanceDataOffset () {
    if (superClass == null) {
      return 0;
    } else {
      return superClass.getInstanceDataSize();
    }
  }

  int getInstanceDataOffset () {
    return instanceDataOffset;
  }

  ClassInfo getClassBase (String clsBase) {
    if ((clsBase == null) || (name.equals(clsBase))) return this;

    if (superClass != null) {
      return superClass.getClassBase(clsBase);
    }

    return null; // Eeek - somebody asked for a class that isn't in the base list
  }

  int computeInstanceDataSize () {
    int n = getDataSize( iFields);

    for (ClassInfo c=superClass; c!= null; c=c.superClass) {
      n += c.getDataSize(c.iFields);
    }

    return n;
  }

  public int getInstanceDataSize () {
    return instanceDataSize;
  }

  int getDataSize (FieldInfo[] fields) {
    int n=0;
    for (int i=0; i<fields.length; i++) {
      n += fields[i].getStorageSize();
    }

    return n;
  }

  public int getNumberOfDeclaredInstanceFields () {
    return iFields.length;
  }

  public FieldInfo getDeclaredInstanceField (int i) {
    return iFields[i];
  }

  public int getNumberOfInstanceFields () {
    return nInstanceFields;
  }

  public FieldInfo getInstanceField (int i) {
    int idx = i - (nInstanceFields - iFields.length);
    if (idx >= 0) {
      return ((idx < iFields.length) ? iFields[idx] : null);
    } else {
      return ((superClass != null) ? superClass.getInstanceField(i) : null);
    }
  }

  public FieldInfo[] getInstanceFields(){
    FieldInfo[] fields = new FieldInfo[nInstanceFields];
    
    for (int i=0; i<fields.length; i++){
      fields[i] = getInstanceField(i);
    }
    
    return fields;
  }

  public int getStaticDataSize () {
    return staticDataSize;
  }

  int computeStaticDataSize () {
    return getDataSize(sFields);
  }

  public int getNumberOfStaticFields () {
    return sFields.length;
  }

  protected Source loadSource () {
    return Source.getSource(sourceFileName);
  }

  static boolean isBuiltinClass (String cname) {
    char c = cname.charAt(0);

    // array class
    if ((c == '[') || cname.endsWith("[]")) {
      return true;
    }

    // primitive type class
    if (Character.isLowerCase(c)) {
      if ("int".equals(cname) || "byte".equals(cname) ||
          "boolean".equals(cname) || "double".equals(cname) ||
          "long".equals(cname) || "char".equals(cname) ||
          "short".equals(cname) || "float".equals(cname) || "void".equals(cname)) {
        return true;
      }
    }

    return false;
  }

  /**
   * set the locations where we look up sources
   */
  static void setSourceRoots (Config config) {
    Source.init(config);
  }

  /**
   * get names of all interfaceNames (transitive, i.e. incl. bases and super-interfaceNames)
   * @return a Set of String interface names
   */
  public Set<String> getAllInterfaces () {
    if (allInterfaces == null) {
      HashSet<String> set = new HashSet<String>();

      for (ClassInfo ci=this; ci != null; ci=ci.superClass) {
        loadInterfaceRec(set, ci.interfaceNames);
      }

      allInterfaces = Collections.unmodifiableSet(set);
    }

    return allInterfaces;
  }

  /**
   * get names of directly implemented interfaceNames
   */
  public Set<String> getInterfaces () {
    return interfaceNames;
  }

  public Set<ClassInfo> getInterfaceClassInfos() {
    Set<ClassInfo> set = new HashSet<ClassInfo>();
    for (String ifcName : interfaceNames) {
      set.add(getResolvedClassInfo(ifcName));
    }
    return set;
  }

  /**
   * not very efficient, but chances are we cache the allInterfaces, and then
   * repetitive use would be faster
   */
  public Set<ClassInfo> getAllInterfaceClassInfos() {
    Set<ClassInfo> set = new HashSet<ClassInfo>();
    for (String ifcName : getAllInterfaces()) {
      set.add(getResolvedClassInfo(ifcName));
    }
    return set;
  }

  
  /**
   * get names of direct inner classes
   */
  public String[] getInnerClasses(){
    return innerClassNames;
  }
  
  public ClassInfo[] getInnerClassInfos(){
    ClassInfo[] innerClassInfos = new ClassInfo[innerClassNames.length];
    
    for (int i=0; i< innerClassNames.length; i++){
      innerClassInfos[i] = getResolvedClassInfo(innerClassNames[i]);
    }
    
    return innerClassInfos;
  }
  

  public ClassInfo getComponentClassInfo () {
    if (isArray()) {
      String cn = name.substring(1);

      if (cn.charAt(0) != '[') {
        cn = Types.getTypeName(cn);
      }

      ClassInfo cci = getResolvedClassInfo(cn);

      return cci;
    }

    return null;
  }

  /**
   * most definitely not a public method, but handy for the NativePeer
   */
  Map<String, MethodInfo> getDeclaredMethods () {
    return methods;
  }

  /**
   * be careful, this replaces or adds MethodInfos dynamically
   */
  public MethodInfo putDeclaredMethod (MethodInfo mi){
    return methods.put(mi.getUniqueName(), mi);
  }

  public MethodInfo[] getDeclaredMethodInfos() {
    MethodInfo[] a = new MethodInfo[methods.size()];
    methods.values().toArray(a);
    return a;
  }

  public Instruction[] getMatchingInstructions (LocationSpec lspec){
    Instruction[] insns = null;

    if (lspec.matchesFile(sourceFileName)){
      for (MethodInfo mi : methods.values()) {
        Instruction[] a = mi.getMatchingInstructions(lspec);
        if (a != null){
          if (insns != null) {
            // not very efficient but probably rare
            insns = Misc.appendArray(insns, a);
          } else {
            insns = a;
          }

          // little optimization
          if (!lspec.isLineInterval()) {
            break;
          }
        }
      }
    }

    return insns;
  }

  public List<MethodInfo> getMatchingMethodInfos (MethodSpec mspec){
    ArrayList<MethodInfo> list = null;
    if (mspec.matchesClass(name)) {
      for (MethodInfo mi : methods.values()) {
        if (mspec.matches(mi)) {
          if (list == null) {
            list = new ArrayList<MethodInfo>();
          }
          list.add(mi);
        }
      }
    }
    return list;
  }

  public MethodInfo getFinalizer () {
    return finalizer;
  }

  public MethodInfo getClinit() {
    // <2do> braindead - cache
    for (MethodInfo mi : methods.values()) {
      if ("<clinit>".equals(mi.getName())) {
        return mi;
      }
    }
    return null;
  }

  public boolean hasCtors() {
    // <2do> braindead - cache
    for (MethodInfo mi : methods.values()) {
      if ("<init>".equals(mi.getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * this one is for clients that need to synchronously get an initialized classinfo.
   * NOTE: we don't handle clinits here. If there is one, this will throw
   * an exception. NO STATIC BLOCKS / FIELDS ALLOWED
   */
  public static ClassInfo getInitializedClassInfo (String clsName, ThreadInfo ti){
    ClassInfo ci = ClassInfo.getResolvedClassInfo(clsName);

    ci.registerClass(ti); // this is safe to call on already loaded classes

    if (!ci.isInitialized()) {
      if (ci.initializeClass(ti)) {
        throw new ClinitRequired(ci);
      }
    }

    return ci;
  }

  // Note: JVM.registerStartupClass() must be kept in sync
  public void registerClass (ThreadInfo ti){
    classLoader.registerClass(ti, this);
  }

  public boolean isRegistered () {
    return (sei != null);
  }

  // note this requires 'sei' to be already set
  ElementInfo createClassObject (ThreadInfo ti){
    Heap heap = JVM.getVM().getHeap(); // ti can be null (during main thread initialization)

    int clsObjRef = heap.newObject(getResolvedClassInfo("java.lang.Class"), ti);
    ElementInfo ei = heap.get(clsObjRef);

    int clsNameRef = heap.newInternString(name, ti);
    ei.setReferenceField("name", clsNameRef);

    // link the class object to the StaticElementInfo
    ei.setIntField("cref", sei.getObjectRef());

    // link the StaticElementInfo to the class object
    sei.setClassObjectRef(ei.getObjectRef());
    setUniqueId();
    classes.put(uniqueId, this);

    return ei;
  }

  public boolean isInitializing () {
    return ((sei != null) && (sei.getStatus() >= 0));
  }

  public boolean isInitialized () {
    return ((sei != null) && (sei.getStatus() == INITIALIZED));
  }

  public boolean needsInitialization () {
    return ((sei == null) || (sei.getStatus() > INITIALIZED));
  }

  public void setInitializing(ThreadInfo ti) {
    sei.setStatus(ti.getId());
  }
  
  public boolean requiresClinitExecution (ThreadInfo ti){
    if (!isRegistered()) {
      registerClass(ti); // this sets sei
    }

    if (sei.getStatus() == UNINITIALIZED){
      if (initializeClass(ti)) {
        return true; // there are new <clinit> frames on the stack, execute them
      }
    }

    return false;
  }
  
  public void setInitialized() {
    sei.setStatus(INITIALIZED);

    // we don't emit classLoaded() notifications for non-builtin classes
    // here anymore because it would be confusing to get instructionExecuted()
    // notifications from the <clinit> execution before the classLoaded()
  }

  /**
   * perform static initialization of class
   * this recursively initializes all super classes, but NOT the interfaces
   *
   * @param ti executing thread
   * @return  true if clinit stackframes were pushed, i.e. context instruction
   * needs to be re-executed
   */
  public boolean initializeClass (ThreadInfo ti) {
    int pushedFrames = 0;

    // push clinits of class hierarchy (upwards, since call stack is LIFO)
    for (ClassInfo ci = this; ci != null; ci = ci.getSuperClass()) {
      if (ci.pushClinit(ti)) {
        // we can't do setInitializing() yet because there is no global lock that
        // covers the whole clinit chain, and we might have a context switch before executing
        // a already pushed subclass clinit - there can be races as to which thread
        // does the static init first. Note this case is checked in INVOKECLINIT
        // (which is one of the reasons why we have it).
        pushedFrames++;
      }
    }

    if (pushedFrames > 0){
      // caller needs to reexecute the insn that caused the initialization
      return true;

    } else {
      return false;
    }
  }

  /**
   * local class initialization
   * @return true if we pushed a &lt;clinit&gt; frame
   */
  protected boolean pushClinit (ThreadInfo ti) {
    int stat = sei.getStatus();
    
    if (stat != INITIALIZED) {
      if (stat != ti.getId()) {
        // even if it is already initializing - if it does not happen in the current thread
        // we have to sync, which we do by calling clinit
        MethodInfo mi = getMethod("<clinit>()V", false);
        if (mi != null) {
          MethodInfo stub = mi.createDirectCallStub("[clinit]");
          StackFrame sf = new DirectCallStackFrame(stub);
          ti.pushFrame( sf);
          return true;

        } else {
          // it has no clinit, so it already is initialized
          setInitialized();
        }
      } else {
        // ignore if it's already being initialized  by our own thread (recursive request)
      }
    }

    return false;
  }

  protected void setStaticElementInfo (StaticElementInfo sei) {
    this.sei = sei;
  }

  public StaticElementInfo getStaticElementInfo () {
    return sei;
  }

  Fields createArrayFields (String type, int nElements, int typeSize, boolean isReferenceArray) {
    return fieldsFactory.createArrayFields( type, this,
                                            nElements, typeSize, isReferenceArray);
  }

  /**
   * Creates the fields for a class.  This gets called by the StaticArea
   * when a class is loaded.
   */
  Fields createStaticFields () {
    return fieldsFactory.createStaticFields(this);
  }

  void initializeStaticData (ElementInfo ei) {
    for (int i=0; i<sFields.length; i++) {
      FieldInfo fi = sFields[i];
      fi.initialize(ei);
    }
  }

  /**
   * Creates the fields for an object.
   */
  public Fields createInstanceFields () {
    return fieldsFactory.createInstanceFields(this);
  }

  void initializeInstanceData (ElementInfo ei) {
    // Note this is only used for field inits, and array elements are not fields!
    // Since Java has only limited element init requirements (either 0 or null),
    // we do this ad hoc in the ArrayFields ctor

    // the order of inits should not matter, since this is only
    // for constant inits. In case of a "class X { int a=42; int b=a; ..}"
    // we have a explicit "GETFIELD a, PUTFIELD b" in the ctor, but to play it
    // safely we init top down

    if (superClass != null) { // do superclasses first
      superClass.initializeInstanceData(ei);
    }

    for (int i=0; i<iFields.length; i++) {
      FieldInfo fi = iFields[i];
      fi.initialize(ei);
    }
  }

  Map<String, MethodInfo> loadArrayMethods () {
    return new HashMap<String, MethodInfo>(0);
  }

  Map<String, MethodInfo> loadBuiltinMethods (String type) {
    type = null;  // Get rid of IDE warning 
     
    return new HashMap<String, MethodInfo>(0);
  }

  protected ClassInfo loadSuperClass (String superName) {
    if(classLoader != null) {
      return classLoader.loadSuperClass(this, superName);
    } else {
      ClassLoaderInfo cl = ClassLoaderInfo.getCurrentClassLoader();
      return cl.loadSuperClass(this, superName);
    }
    
  }

  public String toString() {
    return "ClassInfo[name=" + name + "]";
  }

  private MethodInfo getFinalizer0 () {
    MethodInfo mi = getMethod("finalize()V", true);

    // we are only interested in non-empty method bodies, Object.finalize()
    // is a dummy
    if ((mi != null) && (!mi.getClassInfo().isObjectClassInfo())) {
      return mi;
    }

    return null;
  }

  private boolean isObjectClassInfo0 () {
	if (name.equals("java.lang.Object")) {
	  return true;
	}
	return false;
  }

  private boolean isStringClassInfo0 () {
    if(name.equals("java.lang.String")) {
      return true;
    }
    return false;
  }

  private boolean isRefClassInfo0 () {
    if(name.equals("java.lang.ref.Reference")) {
      return true;
    }
    return false;
  }

  private boolean isWeakReference0 () {
	if(name.equals("java.lang.ref.WeakReference")) {
      return true;
	}

    for (ClassInfo ci = this; !ci.isObjectClassInfo(); ci = ci.superClass) {
      if (ci.isWeakReference()) {
        return true;
      }
    }

    return false;
  }

  private boolean isEnum0 () {
	if(name.equals("java.lang.Enum")) {
      return true;
	}

    for (ClassInfo ci = this; !ci.isObjectClassInfo(); ci = ci.superClass) {
      if (ci.isEnum()) {
        return true;
      }
    }

    return false;
  }

  //--- the generic attribute API

  public boolean hasAttr () {
    return (attr != null);
  }

  public boolean hasAttr (Class<?> attrType){
    return ObjectList.containsType(attr, attrType);
  }

  /**
   * this returns all of them - use either if you know there will be only
   * one attribute at a time, or check/process result with ObjectList
   */
  public Object getAttr(){
    return attr;
  }

  /**
   * this replaces all of them - use only if you know 
   *  - there will be only one attribute at a time
   *  - you obtained the value you set by a previous getXAttr()
   *  - you constructed a multi value list with ObjectList.createList()
   */
  public void setAttr (Object a){
    attr = a;    
  }

  public void addAttr (Object a){
    attr = ObjectList.add(attr, a);
  }

  public void removeAttr (Object a){
    attr = ObjectList.remove(attr, a);
  }

  public void replaceAttr (Object oldAttr, Object newAttr){
    attr = ObjectList.replace(attr, oldAttr, newAttr);
  }

  /**
   * this only returns the first attr of this type, there can be more
   * if you don't use client private types or the provided type is too general
   */
  public <T> T getAttr (Class<T> attrType) {
    return ObjectList.getFirst(attr, attrType);
  }

  public <T> T getNextAttr (Class<T> attrType, Object prev) {
    return ObjectList.getNext(attr, attrType, prev);
  }

  public ObjectList.Iterator attrIterator(){
    return ObjectList.iterator(attr);
  }
  
  public <T> ObjectList.TypedIterator<T> attrIterator(Class<T> attrType){
    return ObjectList.typedIterator(attr, attrType);
  }

  // -- end attrs -- 

  /**
   * It creates an instance from a original ClassInfo instance. It doesn't copy sei & 
   * uniqueId.
   * 
   * It is used for the cases where cl tries to load a class that the original version 
   * of which has been loaded by some other classloader.
   */
  public ClassInfo getInstanceFor(ClassLoaderInfo cl) {
    ClassInfo ci;

    try {
      ci = (ClassInfo)super.clone();

      ci.annotations = annotations;

      ci.name = name;
      ci.signature = signature;
      ci.genericSignature = genericSignature;
      ci.classLoader = cl;

      ci.isClass = isClass;
      ci.isWeakReference = isWeakReference;
      ci.isObjectClassInfo = isObjectClassInfo;
      ci.isStringClassInfo = isStringClassInfo;
      ci.isRefClassInfo = isRefClassInfo;
      ci.isArray = isArray;
      ci.isEnum = isEnum;
      ci.isReferenceArray = isReferenceArray;
      ci.isAbstract = isAbstract;
      ci.isBuiltin = isBuiltin;
      ci.modifiers = modifiers;

      ci.finalizer = finalizer;
      ci.elementInfoAttrs = elementInfoAttrs;

      ci.methods = new LinkedHashMap<String,MethodInfo>(methods);

      ci.iFields = iFields.clone();
      ci.instanceDataSize = instanceDataSize;
      ci.instanceDataOffset = instanceDataOffset;
      ci.nInstanceFields = nInstanceFields;

      ci.sFields = sFields.clone();
      ci.staticDataSize = staticDataSize;

      if(superClass!=null) {
        ci.superClass = cl.getResolvedClassInfo(superClass.getName());
      }

      ci.enclosingClassName = enclosingClassName;
      ci.enclosingMethodName = enclosingMethodName;

      ci.innerClassNames = innerClassNames.clone();
      for(int i=0; i<innerClassNames.length; i++) {
        ci.innerClassNames[i] = innerClassNames[i];
      }

      ci.interfaceNames = interfaceNames;
      ci.allInterfaces = allInterfaces;

      ci.packageName = packageName;
      ci.sourceFileName = sourceFileName;

      // we are going to get rid of this after classloader implementation?
      ci.container = container;

      if(nativePeer != null) {
        ci.nativePeer = nativePeer.getInstanceFor(ci);
      }

      ci.source = source;

      ci.attr = attr;
      ci.enableAssertions = enableAssertions;
      ci.releaseActions = releaseActions;

    } catch (CloneNotSupportedException cnsx){
      cnsx.printStackTrace();
      return null;
    }
    return ci;
  }
}


