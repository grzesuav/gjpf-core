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
import gov.nasa.jpf.JPFSite;
import gov.nasa.jpf.jvm.bytecode.ALOAD;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.ObjVector;
import gov.nasa.jpf.util.Source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.ClassPath.ClassFile;


/**
 * Describes the JVM's view of a java class.  Contains descriptions of the
 * static and dynamic fields, methods, and information relevant to the
 * class.
 */
public class ClassInfo extends InfoObject implements Iterable<MethodInfo> {

  public static final int UNINITIALIZED = -1;
  // 'INITIALIZING' is any number >=0, which is the thread index that executes the clinit
  public static final int INITIALIZED = -2;

  static Logger logger = JPF.getLogger("gov.nasa.jpf.jvm.ClassInfo");

  static Config config;

  /**
   * this is our BCEL classpath. Note that this actually might be
   * turned into a call or ClassInfo instance field if we ever support
   * ClassLoaders (for now we keep it simple)
   */
  protected static ClassPath modelClassPath;

  /**
   * ClassLoader that loaded this class.
   */
  protected static final ClassLoader thisClassLoader = ClassInfo.class.getClassLoader();

  /**
   * Loaded classes, indexed by id number.
   */
  protected static final ObjVector<ClassInfo> loadedClasses =
    new ObjVector<ClassInfo>(100);

  /**
   * optionally used to determine atomic methods of a class (during class loading)
   */
  protected static Attributor attributor;

  /**
   * our abstract factory to create object and class fields
   */
  protected static FieldsFactory fieldsFactory;

  /**
   * here we get infinitely recursive, so keep it around for identity checks
   */
  static ClassInfo classClassInfo;

  /*
   * some distinguished classInfos we keep around for efficiency reasons
   */
  static ClassInfo objectClassInfo;
  static ClassInfo stringClassInfo;
  static ClassInfo weakRefClassInfo;
  static ClassInfo refClassInfo;
  static ClassInfo enumClassInfo;

  static FieldInfo[] emptyFields = new FieldInfo[0];


  /**
   * support to auto-load listeners from annotations
   */
  static HashSet<String> autoloadAnnotations;
  static HashSet<String> autoloaded;

  /**
   * Name of the class. e.g. "java.lang.String"
   */
  protected String name;

  // various class attributes
  protected boolean      isClass = true;
  protected boolean      isWeakReference = false;
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

  protected ClassInfo  superClass;

  /**
   * Interfaces implemented by the class.
   */
  protected Set<String> interfaces;

  /** all interfaces (parent interfaces and interface parents) - lazy eval */
  protected Set<String> allInterfaces;

  /** Name of the package. */
  protected String packageName;

  /** Name of the file which contains the source of this class. */
  protected String sourceFileName;

  /** A unique id associate with this class. */
  protected int uniqueId;

  /**
   * this is the object we use to execute methods in the underlying JVM
   * (it replaces Reflection)
   */
  private NativePeer nativePeer;

  /** Source file associated with the class.*/
  protected Source source;

  static String[] assertionPatterns;
  boolean enableAssertions;

  static boolean init (Config config) {

    ClassInfo.config = config;

    loadedClasses.clear();
    classClassInfo = null;
    objectClassInfo = null;
    stringClassInfo = null;
    weakRefClassInfo = null;

    setSourceRoots(config);
    buildModelClassPath(config);

    attributor = config.getEssentialInstance("vm.attributor.class",
                                                         Attributor.class);

    fieldsFactory = config.getEssentialInstance("vm.fields_factory.class",
                                                FieldsFactory.class);

    assertionPatterns = config.getStringArray("vm.enable_assertions");

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

  public static ClassPath getModelClassPath() {
    return modelClassPath;
  }

  private ClassInfo () {
    // for explicit construction only
  }

  /**
   * ClassInfo ctor used for builtin types (arrays and primitive types)
   * i.e. classes we don't have class files for
   */
  protected ClassInfo (String builtinClassName, int uniqueId) {
    isArray = (builtinClassName.charAt(0) == '[');
    isReferenceArray = isArray && builtinClassName.endsWith(";");
    isBuiltin = true;

    name = builtinClassName;

    logger.log(Level.FINE, "generating builtin class: %1$s", name);

    packageName = ""; // builtin classes don't reside in java.lang !
    sourceFileName = null;
    source = null;

    // no fields
    iFields = emptyFields;
    sFields = emptyFields;

    if (isArray) {
      superClass = objectClassInfo;
      interfaces = loadArrayInterfaces();
      methods = loadArrayMethods();
    } else {
      superClass = null; // strange, but true, a 'no object' class
      interfaces = loadBuiltinInterfaces(name);
      methods = loadBuiltinMethods(name);
    }

    enableAssertions = true; // doesn't really matter - no code associated

    this.uniqueId = uniqueId;
    loadedClasses.set(uniqueId,this);
  }

  /**
   * create a fully synthetic implementation of an Annotation proxy
   */
  ClassInfo (ClassInfo annotationCls, String name, int uniqueId) {
    this.name = name;
    isClass = true;

    //superClass = objectClassInfo;
    superClass = ClassInfo.getClassInfo("gov.nasa.jpf.AnnotationProxyBase");

    interfaces = new HashSet<String>();
    interfaces.add(annotationCls.name);
    packageName = annotationCls.packageName;
    sourceFileName = annotationCls.sourceFileName;

    sFields = new FieldInfo[0]; // none
    staticDataSize = 0;

    methods = new HashMap<String,MethodInfo>();
    iFields = new FieldInfo[annotationCls.methods.size()];
    nInstanceFields = iFields.length;

    // all accessor methods of ours make it into iField/method combinations
    int idx = 0;
    int off = 0;  // no super class
    for (MethodInfo mi : annotationCls.getDeclaredMethodInfos()){
      String mname = mi.getName();
      String mtype = mi.getReturnTypeName();

      // create an instance field for it
      FieldInfo fi = FieldInfo.create(mname, mtype, 0, null, this, idx, off);
      iFields[idx++] = fi;
      off += fi.getStorageSize();

      // now create a public accessor for this field


      InstructionFactory insnFactory = MethodInfo.getInstructionFactory();

      MethodInfo pmi = new MethodInfo(this, mi.getUniqueName(), 1, 2, Modifier.PUBLIC);
      MethodInfo.CodeBuilder cb = pmi.getCodeBuilder();

      ALOAD aload = (ALOAD)insnFactory.create(this,"ALOAD");
      aload.setIndex(0); // load this
      cb.append(aload);

      GETFIELD getfield = (GETFIELD)insnFactory.create(this,"GETFIELD");
      getfield.setField(mname, name);
      cb.append(getfield);

      if (fi.isReference()){
        cb.append(insnFactory.create(this, "ARETURN"));
      } else {
        if (fi.getStorageSize() == 1) {
          cb.append(insnFactory.create(this, "IRETURN"));
        } else {
          cb.append(insnFactory.create(this, "LRETURN"));
        }
      }

      cb.setCode();

      methods.put(pmi.getUniqueName(), pmi);
    }

    instanceDataSize = computeInstanceDataSize();
    instanceDataOffset = 0;

    this.uniqueId = uniqueId;
    loadedClasses.set(uniqueId,this);
  }


  protected ClassInfo (JavaClass jc, int uniqueId) {
    initialize( jc, uniqueId);
  }

  /**
   * Creates a new class from the JavaClass information.
   */
  protected void initialize (JavaClass jc, int uniqueId) {
    name = jc.getClassName();

    logger.log(Level.FINE, "loading class: %1$s", name);

    this.uniqueId = uniqueId;
    loadedClasses.set(uniqueId,this);

    if ((objectClassInfo == null) && name.equals("java.lang.Object")) {
      objectClassInfo = this;
    } else if ((classClassInfo == null) && name.equals("java.lang.Class")) {
      classClassInfo = this;
    } else if ((stringClassInfo == null) && name.equals("java.lang.String")) {
      stringClassInfo = this;
    } else if ((weakRefClassInfo == null) && name.equals("java.lang.ref.WeakReference")) {
      weakRefClassInfo = this;
    } else if ((refClassInfo == null) && name.equals("java.lang.ref.Reference")) {
      refClassInfo = this;
    } else if ((enumClassInfo == null) && name.equals("java.lang.Enum")){
      enumClassInfo = this;
    }

    modifiers = jc.getModifiers();

    isClass = jc.isClass();
    superClass = loadSuperClass(jc);

    interfaces = loadInterfaces(jc);
    packageName = jc.getPackageName();

    iFields = loadInstanceFields(jc);
    instanceDataSize = computeInstanceDataSize();
    instanceDataOffset = computeInstanceDataOffset();
    nInstanceFields = (superClass != null) ?
      superClass.nInstanceFields + iFields.length : iFields.length;

    sFields = loadStaticFields(jc);
    staticDataSize = computeStaticDataSize();

    methods = loadMethods(jc);

    // Used to execute native methods (in JVM land).
    // This needs to be initialized AFTER we get our
    // MethodInfos, since it does a reverse lookup to determine which
    // ones are handled by the peer (by means of setting MethodInfo attributes)
    nativePeer = NativePeer.getNativePeer(this);

    sourceFileName = computeSourceFileName(jc);
    source = null;

    isWeakReference = isWeakReference0();
    finalizer = getFinalizer0();
    isAbstract = jc.isAbstract();
    isEnum = isEnum0();

    // get type specific object and field attributes
    elementInfoAttrs = loadElementInfoAttrs(jc);

    // the corresponding java.lang.Class object gets set when we initialize
    // this class from StaticArea.newClass() - we don't know the
    // DynamicArea (Heap) here, until we turn this into a global object

    enableAssertions = getAssertionStatus();

    loadAnnotations(jc.getAnnotationEntries());
    processJPFConfigAnnotation();
    loadAnnotationListeners();

    // be advised - we don't have fields initialized yet (that's in <clinit>)
    JVM.getVM().notifyClassLoaded(this);
  }

  /**
   * we need to get this from BCEL since this might be a non-public class
   * (we can deal with inner classes via name, but not non-publics)
   */
  String computeSourceFileName (JavaClass jc) {
    char sep = File.separatorChar;

    // contrary to the BCEL doc, this is NOT the source where the class was read from
    // it's the sourcefile base name without package, but with ".java"
    String sfn = jc.getSourceFileName();

    // bcel seems to behave differently on Windows, returning <Unknown>,
    // which gives us problems when writing/reading XML traces
    if (sfn.equalsIgnoreCase("<Unknown>") ||  sfn.equalsIgnoreCase("Unknown")) {
      // this is just a guess for classes defined in their own file
      sfn = name.replace('.', sep);
      int idx = sfn.indexOf('$'); // might be an inner class - return outermost enclosing one
      if (idx>0) {
        sfn = sfn.substring(0, idx);
      }
      sfn += ".java";
      return sfn;

    } else {
      if (packageName.length() > 0) {
        sfn = packageName.replace('.', sep) + sep + sfn;
      }
    }

    return sfn;
  }

  void processJPFConfigAnnotation() {
    AnnotationInfo ai = getAnnotation("gov.nasa.jpf.JPFConfig");
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
              if (logger.isLoggable(Level.INFO)) {
                logger.info("autoload annotation listener: @" + aName + " => " + listener.getClass().getName());
              }
              JVM.getVM().getJPF().addUniqueTypeListener(listener); // <2do> that's a BAD reference chain

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
   * required by InfoObject interface
   */
  public ClassInfo getClassInfo() {
    return this;
  }

  boolean getAssertionStatus () {
    if ((assertionPatterns == null) || (assertionPatterns.length == 0)){
      return false;
    } else if ("*".equals(assertionPatterns[0])) {
      return true;
    } else {
      for (int i=0; i<assertionPatterns.length; i++) {
        if (name.matches(assertionPatterns[i])) { // Ok, not very efficient
          return true;
        }
      }

      return false;
    }
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

  public boolean isInterface() {
    return ((modifiers & Modifier.INTERFACE) != 0);
  }

  public boolean isReferenceArray () {
    return isReferenceArray;
  }

  /**
   * Loads the class specified.
   * @param className The fully qualified name of the class to load.
   * @return the ClassInfo for the classname passed in,
   * or null if null is passed in.
   * @throws JPFException if class cannot be found (by BCEL)
   */
  public static synchronized ClassInfo getClassInfo (String className) {
    if (className == null) {
      return null;
    }

    String typeName = Types.getCanonicalTypeName(className);

    // <2do> this is BAD - fix it!
    int idx = JVM.getVM().getStaticArea().indexFor(typeName);

    ClassInfo ci = loadedClasses.get(idx);

    if (ci != null) {
      return ci;

    } else if (isBuiltinClass(typeName)) {
      // this is a array or builtin type class - there's no class file for this, it
      // gets automatically generated by the VM
      return new ClassInfo(typeName, idx);

    } else {
      JavaClass clazz = getJavaClass(className);
      if (clazz != null){
        return new ClassInfo(clazz, idx);
      } else {
        return null;
      }
    }
  }

  public static ClassInfo getAnnotationProxy (ClassInfo ciAnnotation){
    StaticArea sa = JVM.getVM().getStaticArea();

    // make sure the annotationCls is initialized (no code there)
    if (!ciAnnotation.isInitialized()) {
      ThreadInfo ti = ThreadInfo.getCurrentThread();
      if (!sa.containsClass(ciAnnotation.getName())) {
        sa.addClass(ciAnnotation, ti);  // that creates the class object
        ciAnnotation.setInitialized();
      }
    }


    String cname = ciAnnotation.getName() + "$Proxy";
    int idx = sa.indexFor(cname);
    ClassInfo ci = loadedClasses.get(idx);

    if (ci != null) {
      return ci;
    } else {
      return new ClassInfo(ciAnnotation, cname, idx);
    }
  }

  static InputStream getClassFileStream (String className){
    String slashName = className.replace('.', '/');

    InputStream is = null;
    try {
      ClassFile file = modelClassPath.getClassFile(slashName, ".class");
      if (file != null) {
        is = file.getInputStream();
      }
    } catch (IOException ioe) {
      // try resource
    }

    if (is == null) {
      is = thisClassLoader.getResourceAsStream(slashName + ".class");
    }

    if (is == null) { // Ok, we give up - no class file data
      return null;
    }

    return is;
  }

  static JavaClass getJavaClass (String className){
    InputStream is = getClassFileStream(className);

    if (is != null){
      try {
        ClassParser parser = new ClassParser(is, className);
        JavaClass clazz = parser.parse();
        return clazz;
      } catch (IOException e) {
        throw new JPFException("error reading classfile: " + className);
      }
    } else {
      return null; // not found
    }
  }

  public void reload (){
    if (isBuiltinClass(name)) {
      // nothing to do, no code involved
    } else {
       JavaClass jc = getJavaClass(name);
       initialize(jc, uniqueId);
    }
  }

  public boolean areAssertionsEnabled() {
    return enableAssertions;
  }

  public boolean hasInstanceFields () {
    return (instanceDataSize > 0);
  }

  public int getClassObjectRef () {
    return (sei != null) ? sei.getClassObjectRef() : -1;
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

    //interfaces can have static fields too
    for (String interface_name : getAllInterfaces()) {
        fi = ClassInfo.getClassInfo(interface_name).getDeclaredStaticField(fName);
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


  /**
   * Returns the name of the class.  e.g. "java.lang.String".  similar to
   * java.lang.Class.getName().
   */
  public String getName () {
    return name;
  }

  public String getSimpleName () {
    int i = name.lastIndexOf('.');
    return name.substring(i+1);
  }

  public String getPackageName () {
    return packageName;
  }

  public int getUniqueId() {
    return uniqueId;
  }

  public int getFieldAttrs (int fieldIndex) {
    return 0;
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
    return "L" + name.replace('.', '/') + ";";
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
  public boolean isRefClass () {
    return (this == refClassInfo);
  }

  /**
   * whether this refers to a primitive type.
   */
  public boolean isPrimitive() {
    return superClass == null && this != objectClassInfo;
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
      cname = Types.getCanonicalTypeName(cname);

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


  /**
   * clean up statics for another 'main' run
   */
  public static void reset () {
    loadedClasses.clear();

    classClassInfo = null;
    objectClassInfo = null;
    stringClassInfo = null;
  }

  public static int getNumberOfLoadedClasses() {
    return loadedClasses.size();
  }

  public static ClassInfo[] getLoadedClasses() {
    ClassInfo classes[] = new ClassInfo[loadedClasses.size()];
    loadedClasses.toArray(classes);
    return(classes);
  }


  public static String[] getClassPathElements() {
    String cp = modelClassPath.toString();
    return cp.split("[:;]");
  }

  protected static void buildModelClassPath (Config config) {
    StringBuilder buf = new StringBuilder(256);
    char ps = File.pathSeparatorChar;
    String  v;

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

    String cp = config.asPlatformPath(buf.toString());
    modelClassPath = new ClassPath(cp);
  }

  protected static Set<String> loadArrayInterfaces () {
    Set<String> interfaces;

    interfaces = new HashSet<String>();
    interfaces.add("java.lang.Cloneable");
    interfaces.add("java.io.Serializable");

    return Collections.unmodifiableSet(interfaces);
  }

  protected static Set<String> loadBuiltinInterfaces (String type) {
    return Collections.unmodifiableSet(new HashSet<String>(0));
  }

  /**
   * Loads the interfaces of a class.
   */
  protected static Set<String> loadInterfaces (JavaClass jc) {
    Set<String> interfaces;
    String[]    interfaceNames;

    interfaceNames = jc.getInterfaceNames();
    interfaces = new HashSet<String>();

    for (int i = 0, l = interfaceNames.length; i < l; i++) {
      interfaces.add(interfaceNames[i]);
    }

    return Collections.unmodifiableSet(interfaces);
  }

  FieldInfo[] loadInstanceFields (JavaClass jc) {
    Field[] fields = jc.getFields();
    int i, j, n;
    int off = (superClass != null) ? superClass.instanceDataSize : 0;

    for (i=0, n=0; i<fields.length; i++) {
      if (!fields[i].isStatic()) n++;
    }

    int idx = (superClass != null) ? superClass.nInstanceFields : 0;
    FieldInfo[] ifa = new FieldInfo[n];

    for (i=0, j=0; i<fields.length; i++) {
      Field f = fields[i];
      if (!f.isStatic()) {
        FieldInfo fi = FieldInfo.create(f, this, idx, off);
        ifa[j++] = fi;
        off += fi.getStorageSize();
        idx++;

        if (attributor != null) {
          fi.setAttributes( attributor.getFieldAttributes(jc, f));
        }
      }
    }

    return ifa;
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

  FieldInfo[] loadStaticFields (JavaClass jc) {
    Field[] fields = jc.getFields();
    int i, n;
    int off = 0;

    for (i=0, n=0; i<fields.length; i++) {
      if (fields[i].isStatic()) n++;
    }

    FieldInfo[] sfa = new FieldInfo[n];
    int idx = 0;

    for (i=0; i<fields.length; i++) {
      Field f = fields[i];
      if (f.isStatic()) {
        FieldInfo fi = FieldInfo.create(f, this, idx, off);
        sfa[idx] = fi;
        idx++;
        off += fi.getStorageSize();

        if (attributor != null) {
          fi.setAttributes( attributor.getFieldAttributes(jc, f));
        }
      }
    }

    return sfa;
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
   * get names of all interfaces (transitive, i.e. incl. bases and super-interfaces)
   * @return a Set of String interface names
   */
  Set<String> getAllInterfaces () {
    if (allInterfaces == null) {
      HashSet<String> set = new HashSet<String>();

      for (ClassInfo ci=this; ci != null; ci=ci.superClass) {
        loadInterfaceRec(set, ci);
      }

      allInterfaces = Collections.unmodifiableSet(set);
    }

    return allInterfaces;
  }

  /**
   * get names of directly implemented interfaces
   */
  public Set<String> getInterfaces () {
    return interfaces;
  }

  public ClassInfo getComponentClassInfo () {
    if (isArray()) {
      String cn = name.substring(1);

      if (cn.charAt(0) != '[') {
        cn = Types.getTypeName(cn);
      }

      ClassInfo cci = getClassInfo(cn);

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

  public MethodInfo[] getDeclaredMethodInfos() {
    MethodInfo[] a = new MethodInfo[methods.size()];
    methods.values().toArray(a);
    return a;
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


  public int createClassObject (ThreadInfo th, int cref) {
    int         objref;
    int         cnref;
    DynamicArea da = DynamicArea.getHeap();

    objref = da.newObject(classClassInfo, th);
    cnref = da.newInternString(name, th);

    // we can't execute methods nicely for which we don't have caller bytecode
    // (would run into a (pc == null) assertion), so we have to bite the bullet
    // and init the java.lang.Class object explicitly. But that's probably Ok
    // since it is a very special beast, anyway
    ElementInfo e = da.get(objref);

    try {
      e.setReferenceField("name", cnref);

      // this is the StaticArea ElementInfo index of what we refer to
      e.setIntField("cref", cref);
    } catch (Exception x) {
      // if we don't have the right (JPF specific) java.lang.Class version,
      // we are screwed in terms of java.lang.Class usage
      if (classClassInfo == null) { // report it just once
        logger.severe("FATAL ERROR: wrong java.lang.Class version (wrong 'vm.classpath' property)");
      }

      return -1;
    }

    return objref;
  }

  public static String findResource (String resourceName){
    // would have been nice to just delegate this to the BCEL ClassPath, but
    // unfurtunately it's getPath() doesn't indicate at all if the resource
    // is in a jar :<
    try {
    for (String cpe : getClassPathElements()) {
      if (cpe.endsWith(".jar")){
        JarFile jar = new JarFile(cpe);
        JarEntry e = jar.getJarEntry(resourceName);
        if (e != null){
          File f = new File(cpe);
          return "jar:" + f.toURI().toURL().toString() + "!/" + resourceName;
        }
      } else {
        File f = new File(cpe, resourceName);
        if (f.exists()){
          return f.toURI().toURL().toString();
        }
      }
    }
    } catch (MalformedURLException mfx){
      return null;
    } catch (IOException iox){
      return null;
    }

    return null;
  }

  public boolean isInitializing () {
    return ((sei != null) && (sei.getStatus() >= 0));
  }

  public boolean isInitialized () {
    return ((sei != null) && (sei.getStatus() == INITIALIZED));
  }

  public boolean needsInitialization () {
    return ((sei == null) || (sei.getStatus() == UNINITIALIZED));
  }

  public void setInitializing(ThreadInfo ti) {
    sei.setStatus(ti.getIndex());
  }

  public void setInitialized() {
    sei.setStatus(INITIALIZED);

    // we don't emitt classLoaded() notifications for non-builtin classes
    // here anymore because it would be confusing to get instructionExecuted()
    // notifications from the <clinit> execution before the classLoaded()
  }

  // this one is for classes w/o superclasses or clinits (e.g. array and builtin classes)
  public void loadAndInitialize (ThreadInfo ti) {
    StaticArea sa = ti.getVM().getStaticArea();
    if (!sa.containsClass(name)) {
      sa.addClass(this, ti);
      setInitialized();
    }
  }

  /**
   * return the number of clinit stack frames pushed
   */
  public int loadAndInitialize (ThreadInfo ti, Instruction continuation) {
    ClassInfo  ci = this;
    StaticArea sa = ti.getVM().getStaticArea();
    int pushedFrames = 0;

    // first do all the base classes
    while (ci != null) {
      if (initialize(sa, ci, ti, continuation)) {
        continuation = null;
        pushedFrames++;
      }

      ci = ci.getSuperClass();
    }

    // now we have to do all the interfaces (this sucks)
    for (String ifc : getAllInterfaces()) {
      ci = getClassInfo(ifc);
      if (initialize(sa, ci, ti, continuation)) {
        continuation = null;
        pushedFrames++;
      }
    }

    return pushedFrames;
  }

  protected boolean initialize (StaticArea sa, ClassInfo ci, ThreadInfo ti, Instruction continuation) {
    StaticElementInfo ei = ci.getStaticElementInfo();

    if (ei == null) {
      sa.addClass(ci, ti);  // that creates the class object
      ei = ci.getStaticElementInfo();
      assert ei != null : "static init failed: " + ci.getName();
    }

    int stat = ei.getStatus();

    if (stat != INITIALIZED) {
      if (stat != ti.getIndex()) {
        // even if it's already initializing - if it's not in the current thread
        // we have to sync, which we do by calling clinit
        MethodInfo mi = ci.getMethod("<clinit>()V", false);
        if (mi != null) {
          MethodInfo stub = mi.createDirectCallStub("[clinit]");
          StackFrame sf = new DirectCallStackFrame(stub, continuation);
          ti.pushFrame( sf);

          return true;
        } else {
          // it has no clinit, so it already is initialized
          ci.setInitialized();
        }
      } else {
        // if it's initialized by our own thread (recursive request), just go on
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
    Fields f = ei.getFields();

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
    return new HashMap<String, MethodInfo>(0);
  }

  /**
   * Loads the ClassInfo for named class.
   * @param set a Set to which the interface names (String) are added
   * @param ci class to find interfaces for.
   */
  void loadInterfaceRec (Set<String> set, ClassInfo ci) {
    if (ci != null) {
      for (String iname : ci.interfaces) {
        set.add(iname);

        ci = getClassInfo(iname);
        loadInterfaceRec(set, ci);
      }
    }
  }

  /**
   * this is a optimization to work around the BCEL strangeness that some
   * insn info (types etc.) are only accessible with modifiable ConstPools
   * (the ConstantPoolGen, which is costly to create), and some others
   * (toString) are only provided via ConstPools. It's way to expensive
   * to create this always on the fly, for each relevant insn, so we cache it
   * here
   */
  static ConstantPool cpCache;
  static ConstantPoolGen cpgCache;

  public static ConstantPoolGen getConstantPoolGen (ConstantPool cp) {
    if (cp != cpCache) {
      cpCache = cp;
      cpgCache = new ConstantPoolGen(cp);
    }

    return cpgCache;
  }

  /** avoid memory leaks */
  static void resetCPCache () {
    cpCache = null;
    cpgCache = null;
  }

  Map<String, MethodInfo> loadMethods (JavaClass jc) {
    Method[] ms = jc.getMethods();
    LinkedHashMap<String,MethodInfo>  map = new LinkedHashMap<String,MethodInfo>(ms.length);

    for (int i = 0; i < ms.length; i++) {
      MethodInfo mi = new MethodInfo( ms[i], this);
      String id = mi.getUniqueName();
      map.put(id, mi);

      if (attributor != null) {
        mi.setAtomic( attributor.isMethodAtomic(jc, ms[i], id));
      }

      if (autoloadAnnotations != null) {
        autoloadListeners(mi.getAnnotations());
      }
    }

    resetCPCache(); // no memory leaks

    return map;
  }

  ClassInfo loadSuperClass (JavaClass jc) {
    if (this == objectClassInfo) {
      return null;
    } else {
      String superName = jc.getSuperclassName();
      ClassInfo sci = getClassInfo(superName);
      if (sci == null){
        // <2do> this shouldn't be a JPFException, but how can we turn it over to the app?
        throw new NoClassInfoException(superName);
      }

      return sci;
    }
  }

  int loadElementInfoAttrs (JavaClass jc) {
    int attrs = 0;
    // we use the atomicizer for it because the only attribute for now is the
    // immutability, and it is used to determine if a field insn should be
    // a step boundary. Otherwise it's a bit artificial, but we don't want
    // to intro another load time class attributor for now
    if (attributor != null) {
      attrs = attributor.getObjectAttributes(jc);
    }

    // if it has no fields, it is per se immutable
    if (!isArray && (instanceDataSize == 0)) {
      attrs |= ElementInfo.ATTR_IMMUTABLE;
    }

    return attrs;
  }

  public String toString() {
    return "ClassInfo[name=" + name + "]";
  }

  private MethodInfo getFinalizer0 () {
    MethodInfo mi = getMethod("finalize()V", true);

    // we are only interested in non-empty method bodies, Object.finalize()
    // is a dummy
    if ((mi != null) && (mi.getClassInfo() != objectClassInfo)) {
      return mi;
    }

    return null;
  }

  private boolean isWeakReference0 () {
    for (ClassInfo ci = this; ci != objectClassInfo; ci = ci.superClass) {
      if (ci == weakRefClassInfo) {
        return true;
      }
    }

    return false;
  }

  private boolean isEnum0 () {
    for (ClassInfo ci = this; ci != objectClassInfo; ci = ci.superClass) {
      if (ci == enumClassInfo) {
        return true;
      }
    }

    return false;
  }
}


