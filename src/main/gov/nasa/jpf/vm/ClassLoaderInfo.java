//
// Copyright (C) 2012 United States Government as represented by the
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.classfile.ClassFileException;
import gov.nasa.jpf.jvm.classfile.ClassPath;
import gov.nasa.jpf.util.SparseIntVector;
import gov.nasa.jpf.util.StringSetMatcher;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 *  
 * Represents the classloader construct in VM which is responsible for loading
 * classes.
 */
public class ClassLoaderInfo 
     implements Iterable<ClassInfo>, Comparable<ClassLoaderInfo>, Cloneable, Restorable<ClassLoaderInfo> {

  // the model class field name where we store our id 
  static final String ID_FIELD = "nativeId";
  
  
  // Map that keeps the classes defined (directly loaded) by this loader and the
  // ones that are resolved from these defined classes
  protected Map<String,ClassInfo> resolvedClasses;

  // Represents the locations where this classloader can load classes form
  protected ClassPath cp;

  // The type of the corresponding class loader object
  protected ClassInfo classInfo;

  // The area containing static fields and  classes
  protected Statics statics;

  protected boolean roundTripRequired = false;

  // Search global id, which is the basis for canonical order of classloaders
  protected int id;

  // The java.lang.ClassLoader object reference
  protected int objRef;

  protected ClassLoaderInfo parent;

  static Config config;

  //static GlobalIdManager gidManager = new GlobalIdManager();
  static SparseIntVector globalCLids;

  static class ClMemento implements Memento<ClassLoaderInfo> {
    // note that we don't have to store the invariants (gid, parent, isSystemClassLoader)
    ClassLoaderInfo cl;
    Memento<Statics> staticsMemento;
    Memento<ClassPath> cpMemento;
    Map<String, Boolean> classAssertionStatus;
    Map<String, Boolean> packageAssertionStatus;
    boolean defaultAssertionStatus;
    boolean isDefaultSet;

    ClMemento (ClassLoaderInfo cl){
      this.cl = cl;
      staticsMemento = cl.statics.getMemento();
      cpMemento = cl.cp.getMemento();
      classAssertionStatus = new HashMap<String, Boolean>(cl.classAssertionStatus);
      packageAssertionStatus = new HashMap<String, Boolean>(cl.packageAssertionStatus);
      defaultAssertionStatus = cl.defaultAssertionStatus;
      isDefaultSet = cl.isDefaultSet;
    }

    public ClassLoaderInfo restore(ClassLoaderInfo ignored) {
      staticsMemento.restore(cl.statics);
      cpMemento.restore(null);
      cl.classAssertionStatus = this.classAssertionStatus;
      cl.packageAssertionStatus = this.packageAssertionStatus;
      cl.defaultAssertionStatus = this.defaultAssertionStatus;
      cl.isDefaultSet = this.isDefaultSet;
      return cl;
    }
  }

  protected ClassLoaderInfo(VM vm, int objRef, ClassPath cp, ClassLoaderInfo parent) {
    resolvedClasses = new HashMap<String,ClassInfo>();

    this.cp = cp;
    this.parent = parent;
    this.objRef = objRef;

    Class<?>[] argTypes = { Config.class, KernelState.class };
    Object[] args = { config, vm.getKernelState() };

    this.statics = config.getEssentialInstance("vm.statics.class", Statics.class, argTypes, args);

    vm.registerClassLoader(this);

    ElementInfo ei = vm.getModifiableElementInfo(objRef);

    // For systemClassLoaders, this object is still null, since the class java.lang.ClassLoader 
    // class cannot be loaded before creating the systemClassLoader
    if(ei!=null) {
      this.id = this.computeId(objRef);
      ei.setIntField( ID_FIELD, id);
      if(parent != null) {
        ei.setReferenceField("parent", parent.objRef);
      }
      classInfo = ei.getClassInfo();
      setRoundTripRequired();
    }
  }

  public Memento<ClassLoaderInfo> getMemento (MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<ClassLoaderInfo> getMemento(){
    return new ClMemento(this);
  }

  /**
   * this is our internal, search global id that is used for the
   * canonical root set
   */
  public int getId() {
    return id;
  }

  /**
   * Returns the type of the corresponding class loader object
   */
  public ClassInfo getClassInfo () {
    return classInfo;
  }

  /**
   * Returns the object reference.
   */
  public int getClassLoaderObjectRef () {
    return objRef;
  }

//  protected int computeGlobalId (VM vm){
//    ThreadInfo tiExec = vm.getCurrentThread();
//    Instruction insn = null;
//    
//    if (tiExec != null){
//      insn = tiExec.getTopFrame().getPC();  
//    }
//
//    return gidManager.getNewId(vm.getSystemState(), tiExec, insn);
//  }

  protected int computeId (int objRef) {
    int id = globalCLids.get(objRef);
    if (id == 0) {
      id = globalCLids.size() + 1; // the first systemClassLoader is not in globalCLids and always has id '0'
      globalCLids.set(objRef, id);
    }
    return id;
  }

  /**
   * For optimizing the class loading mechanism, if the class loader class and the 
   * classes of the whole parents hierarchy are descendant of URLClassLoader and 
   * do not override the ClassLoader.loadClass() & URLClassLoader.findClass, resolving 
   * the class is done natively within JPF
   */
  protected void setRoundTripRequired() {
    roundTripRequired = (parent!=null? parent.roundTripRequired: true) 
        || !this.hasOriginalLoadingImp();
  }

  private boolean hasOriginalLoadingImp() {
    String signature = "(Ljava/lang/String;)Ljava/lang/Class;";
    MethodInfo loadClass = classInfo.getMethod("loadClass" + signature, true);
    MethodInfo findClass = classInfo.getMethod("findClass" + signature, true);
  
    return (loadClass.getClassName().equals("java.lang.ClassLoader") &&
        findClass.getClassName().equals("java.net.URLClassLoader"));
  }

  public boolean isSystemClassLoader() {
    return false;
  }
  
  public static ClassLoaderInfo getCurrentClassLoader() {
    return getCurrentClassLoader( ThreadInfo.getCurrentThread());
  }

  public static ClassLoaderInfo getCurrentClassLoader(ThreadInfo ti) {
    MethodInfo mi = ti.getTopFrameMethodInfo();
    if (mi != null) {
      ClassInfo ci = mi.getClassInfo();
      if(ci != null) {
        return ci.getClassLoaderInfo();
      } else {
        Iterator<StackFrame> it = ti.iterator();

        while(it.hasNext()) {
          StackFrame sf = it.next();
          ci = sf.getMethodInfo().getClassInfo();
          if(ci != null) {
            return ci.getClassLoaderInfo();
          }
        }
      }
    }

    return getCurrentSystemClassLoader(ti);
  }

  protected void updateCachedClassInfos (ClassInfo ci) {
    // nothing here, overridden by SystemClassLoaderInfo for standard classes such as java.lang.String
  }

  /**
   * This is useful when there are multiple systemClassLoaders created.
   */
  public static SystemClassLoaderInfo getCurrentSystemClassLoader() {
    return getCurrentSystemClassLoader( ThreadInfo.getCurrentThread()); 
  }

  /**
   * This is useful when there are multiple systemClassLoaders created.
   */
  public static SystemClassLoaderInfo getCurrentSystemClassLoader(ThreadInfo ti) {
    return VM.getVM().getSystemClassLoader(ti);
  }

  public ClassInfo getResolvedClassInfo (String className) throws ClassInfoException {
    if (className == null) {
      return null;
    }

    String typeName = Types.getClassNameFromTypeName(className);

    ClassInfo ci = getAlreadyResolvedClassInfo(typeName);

    if (ci == null) {
      ClassPath.Match match = getMatch(typeName);
      ci = ClassInfo.getResolvedClassInfo(className, this, match);
      if(ci.classLoader != this) {
        if(!ClassInfo.isBuiltinClass(typeName) || 
            (isSystemClassLoader() && ClassInfo.isBuiltinClass(typeName))) {
          // create a new instance from ci using this classloader
          ci = ci.getInstanceFor(this);
        }
      }

      // cache the defined class
      addResolvedClass(ci);
    }

    return ci;
  }

  public ClassInfo getResolvedClassInfo (String className, byte[] buffer, int offset, int length, ClassPath.Match match) throws ClassInfoException {
    if (className == null) {
      return null;   
    }

    String typeName = Types.getClassNameFromTypeName(className);

    ClassInfo ci = getAlreadyResolvedClassInfo(typeName);
    
    if (ci == null) {
      ci = ClassInfo.getResolvedClassInfo(typeName, buffer, offset, length, this, match);
      if(ci.classLoader != this) {
        if(!ClassInfo.isBuiltinClass(typeName) || 
            (isSystemClassLoader() && ClassInfo.isBuiltinClass(typeName))) {
          // create a new instance from ci using this classloader
          ci = ci.getInstanceFor(this);
        }
      }

      // cache the defined class
      addResolvedClass(ci);
    }

    return ci;
  }

  protected ClassInfo getAlreadyResolvedClassInfo(String cname) {
    return resolvedClasses.get(cname);
  }

  protected void addResolvedClass(ClassInfo ci) {
    resolvedClasses.put(ci.getName(), ci);
  }

  protected boolean hasResolved(String cname) {
    return (resolvedClasses.get(cname)!=null);
  }

  /**
   * this one is for clients that need to synchronously get an initialized classinfo.
   * NOTE: we don't handle clinits here. If there is one, this will throw
   * an exception. NO STATIC BLOCKS / FIELDS ALLOWED
   */
  public ClassInfo getInitializedClassInfo (String clsName, ThreadInfo ti){
    ClassInfo ci = getResolvedClassInfo(clsName);

    ci.registerClass(ti); // this is safe to call on already loaded classes

    if (!ci.isInitialized()) {
      if (ci.initializeClass(ti)) {
        throw new ClinitRequired(ci);
      }
    }

    return ci;
  }

  /**
   * obtain ClassInfo from context that does not care about resolution, i.e.
   * does not check for NoClassInfoExceptions
   *
   * @param className fully qualified classname to get a ClassInfo for
   * @return null if class was not found
   */
  public ClassInfo tryGetResolvedClassInfo (String className){
    try {
      return getResolvedClassInfo(className);
    } catch (ClassInfoException cx){
      return null;
    }
  }

  protected void resolveClass(ClassInfo ci) {
    if(ClassInfo.isBuiltinClass(ci.getName()) || ci.isObjectClassInfo) {
      return;
    }

    loadInterfaces(ci);
    // this is where we get recursive
    ci.superClass = loadSuperClass(ci, ci.superClassName);
  }

  
  public ClassInfo getClassInfo (int id) {
    ElementInfo ei = statics.get(id);
    if (ei != null) {
      return ei.getClassInfo();
    } else {
      return null;
    }
  }
  
  /**
   * This method is only used in the case of non-systemClassLoaders. SystemClassLoader 
   * overrides this method.
   */
  protected ClassInfo loadSuperClass (ClassInfo ci, String superName) throws ClassInfoException {
    if (ci.isObjectClassInfo()) {
      return null;
    }

    ClassInfo.logger.finer("resolving superclass: ", superName, " of ", ci.getName());

    // resolve the superclass
    ClassInfo superClass = ci.resolveReferencedClass(superName);

    return superClass;
  }

  protected void loadInterfaces (ClassInfo ci) throws ClassInfoException {
    for (String ifcName : ci.interfaceNames) {
      ClassInfo.logger.finer("resolving interface: ", ifcName, " of ", ci.getName());
      
      boolean loaded = false;
      for (ClassInfo ifc: ci.interfaces) {
        if(ifc.getName().equals(ifcName)) {
          loaded = true;
        }
      }

      if(!loaded) {
        // resolve the interface
        ClassInfo ifc = ci.resolveReferencedClass(ifcName);
        ci.interfaces.add(ifc);
      }
    }
  }

  // it acquires the resolvedClassInfo by executing the class loader loadClass() method
  public ClassInfo loadClass(String cname) {
    ClassInfo ci = null;
    if(roundTripRequired) {
      // loadClass bytecode needs to be executed by the JPF vm
      ci = loadClassOnJPF(cname);
    } else {
      // This class loader and the whole parent hierarchy use the standard class loading
      // mechanism, therefore the class is loaded natively
      ci = loadClassOnJVM(cname);
    }

    return ci;
  }

  protected ClassInfo loadClassOnJVM(String cname) {
    String className = Types.getClassNameFromTypeName(cname);
    // Check if the given class is already resolved by this loader
    ClassInfo ci = getAlreadyResolvedClassInfo(className);

    if (ci == null) {
      try {
        if(parent != null) {
          ci = parent.loadClassOnJVM(cname);
        } else {
          ClassLoaderInfo systemClassLoader = getCurrentSystemClassLoader();
          ci = systemClassLoader.getResolvedClassInfo(cname);
        }
      } catch(ClassInfoException cie) {
        if(cie.getExceptionClass().equals("java.lang.ClassNotFoundException")) {
          ci = getResolvedClassInfo(cname);
        } else {
          throw cie;
        }
      }
    }

    return ci;
  }

  protected ClassInfo loadClassOnJPF(String cname) {
    String className = Types.getClassNameFromTypeName(cname);
    // Check if the given class is already resolved by this loader
    ClassInfo ci = getAlreadyResolvedClassInfo(className);

    // If the class has not been resolved, do a round trip to execute the 
    // user code of loadClass(cname) 
    if(ci == null) {
      ThreadInfo ti = VM.getVM().getCurrentThread();
      StackFrame frame = ti.getReturnedDirectCall();

      if(frame != null && frame.getMethodName().equals("[loadClass(" + cname + ")]")) {
        // the round trip ends here. loadClass(cname) is already executed on JPF
        int clsObjRef = frame.pop();

        if (clsObjRef == MJIEnv.NULL){
          throw new ClassInfoException(cname + ", is not found in the classloader search path", 
                                       this, "java.lang.NoClassDefFoundError", cname);
          } else {
            return ti.getEnv().getReferredClassInfo(clsObjRef);
          }
      } else {
        // the round trip starts here
        pushloadClassFrame(cname);
        // bail out right away & re-execute the current instruction in JPF
        throw new LoadOnJPFRequired(cname);
      }
    }
    return ci;
  }

  protected void pushloadClassFrame(String superName) {
    ThreadInfo ti = VM.getVM().getCurrentThread();

    // obtain the class of this ClassLoader
    ClassInfo clClass = VM.getVM().getClassInfo(objRef);

    // retrieve the loadClass() method of this ClassLoader class
    MethodInfo mi = clClass.getMethod("loadClass(Ljava/lang/String;)Ljava/lang/Class;", true);

    // create a frame representing loadClass() & push it to the stack of the 
    // current thread 
    MethodInfo stub = mi.createDirectCallStub("[loadClass(" + superName + ")]");
    StackFrame frame = new DirectCallStackFrame(stub);

    int sRef = ti.getEnv().newString(superName.replace('/', '.'));
    frame.push(objRef, true);
    frame.pushRef(sRef);

    ti.pushFrame(frame);
  }

  protected ClassInfo getDefinedClassInfo(String typeName){
    ClassInfo ci = resolvedClasses.get(typeName);
    if(ci != null && ci.classLoader == this) {
      return ci;
    } else {
      return null;
    }
  }
  
  public ElementInfo getElementInfo (String typeName) {
    ClassInfo ci = resolvedClasses.get(typeName);
    if (ci != null) {
      ClassLoaderInfo cli = ci.classLoader;
      Statics st = cli.statics;
      return st.get(ci.getId());
      
    } else {
      return null; // not resolved
    }
  }

  public ElementInfo getModifiableElementInfo (String typeName) {
    ClassInfo ci = resolvedClasses.get(typeName);
    if (ci != null) {
      ClassLoaderInfo cli = ci.classLoader;
      Statics st = cli.statics;
      return st.getModifiable(ci.getId());
      
    } else {
      return null; // not resolved
    }
  }

  protected ClassPath.Match getMatch(String typeName) {
    if(ClassInfo.isBuiltinClass(typeName)) {
      return null;
    }

    ClassPath.Match match;
    try {
      match = cp.findMatch(typeName); 
    } catch (ClassFileException cfx){
      throw new JPFException("error reading class " + typeName, cfx);
    }

    return match;
  }

  /**
   * Finds the first Resource in the classpath which has the specified name. 
   * Returns null if no Resource is found.
   */
  public String findResource (String resourceName){
    for (String cpe : getClassPathElements()) {
      String URL = getResourceURL(cpe, resourceName);
      if(URL != null) {
        return URL;
      }
    }
    return null;
  }

  /**
   * Finds all resources in the classpath with the given name. Returns an 
   * enumeration of the URL objects.
   */
  public String[] findResources (String resourceName){
    ArrayList<String> resources = new ArrayList(0);
    for (String cpe : getClassPathElements()) {
      String URL = getResourceURL(cpe, resourceName);
      if(URL != null) {
        if(!resources.contains(URL)) {
          resources.add(URL);
        }
      }
    }
    return resources.toArray(new String[resources.size()]);
  }
  
  protected String getResourceURL(String path, String resource) {
    if(resource != null) {
      try {
        if (path.endsWith(".jar")){
          JarFile jar = new JarFile(path);
          JarEntry e = jar.getJarEntry(resource);
          if (e != null){
            File f = new File(path);
            return "jar:" + f.toURI().toURL().toString() + "!/" + resource;
          }
        } else {
          File f = new File(path, resource);
          if (f.exists()){
            return f.toURI().toURL().toString();
          }
        }
      } catch (MalformedURLException mfx){
        return null;
      } catch (IOException iox){
        return null;
      }
    }

    return null;
  }

  public Statics getStatics() {
    return statics;
  }

  public ClassPath getClassPath() {
    return cp;
  }

  public String[] getClassPathElements() {
    return cp.getPathNames();
  }

  /**
   * This is invoked by VM.initSubsystems()
   */
  static void init (Config config) {
    ClassLoaderInfo.config = config;

    //gidManager = new GlobalIdManager();
    globalCLids = new SparseIntVector();

    enabledAssertionPatterns = StringSetMatcher.getNonEmpty(config.getStringArray("vm.enable_assertions"));
    disabledAssertionPatterns = StringSetMatcher.getNonEmpty(config.getStringArray("vm.disable_assertions"));
  }

  /**
   * Comparison for sorting based on index.
   */
  public int compareTo (ClassLoaderInfo that) {
    return this.id - that.id;
  }

  /**
   * Returns an iterator over the classes that are defined (directly loaded) by this
   * classloader. 
   */
  public Iterator<ClassInfo> iterator () {
    return resolvedClasses.values().iterator();
  }

  /**
   * For now, this always returns true, and it used while the classloader is being
   * serialized. That is going to be changed if we ever consider unloading the
   * classes. For now, it is just added in analogy to ThreadInfo
   */
  public boolean isAlive () {
    return true;
  }

  public Map<String, ClassLoaderInfo> getPackages() {
    Map<String, ClassLoaderInfo> pkgs = new HashMap<String, ClassLoaderInfo>();
    for(String cname: resolvedClasses.keySet()) {
      if(!ClassInfo.isBuiltinClass(cname) && cname.indexOf('.')!=-1) {
        pkgs.put(cname.substring(0, cname.lastIndexOf('.')), this);
      }
    }

    Map<String, ClassLoaderInfo> parentPkgs = null;
    if(parent!=null) {
      parentPkgs = parent.getPackages();
    }

    if (parentPkgs != null) {
      for (String pName: parentPkgs.keySet()) {
        if (pkgs.get(pName) == null) {
          pkgs.put(pName, parentPkgs.get(pName));
        }
      }
    }
    return pkgs;
  }

  //-------- assertion management --------
  
  // set in the jpf.properties file
  static StringSetMatcher enabledAssertionPatterns;
  static StringSetMatcher disabledAssertionPatterns;

  protected Map<String, Boolean> classAssertionStatus = new HashMap<String, Boolean>();
  protected Map<String, Boolean> packageAssertionStatus = new HashMap<String, Boolean>();
  protected boolean defaultAssertionStatus = false;
  protected boolean isDefaultSet = false;

  protected boolean desiredAssertionStatus(String cname) {
    // class level assertion can override all their assertion settings
    Boolean result = classAssertionStatus.get(cname);
    if (result != null) {
      return result.booleanValue();
    }

    // package level assertion can override the default assertion settings
    int dotIndex = cname.lastIndexOf(".");
    if (dotIndex < 0) { // check for default package
      result = packageAssertionStatus.get(null);
      if (result != null) {
        return result.booleanValue();
      }
    }

    if(dotIndex > 0) {
      String pkgName = cname;
      while(dotIndex > 0) { // check for the class package and its upper level packages 
        pkgName = pkgName.substring(0, dotIndex);
        result = packageAssertionStatus.get(pkgName);
        if (result != null) {
          return result.booleanValue();
        }
        dotIndex = pkgName.lastIndexOf(".", dotIndex-1);
      }
    }

    // class loader default, if it has been set, can override the settings
    // specified by VM arguments
    if(isDefaultSet) {
      return defaultAssertionStatus;
    } else {
      return StringSetMatcher.isMatch(cname, enabledAssertionPatterns, disabledAssertionPatterns);
    }
  }

  public void setDefaultAssertionStatus(boolean enabled) {
    isDefaultSet = true;
    defaultAssertionStatus = enabled;
  }

  public void setClassAssertionStatus(String cname, boolean enabled) {
    classAssertionStatus.put(cname, enabled);
  }

  public void setPackageAssertionStatus(String pname, boolean enabled) {
    packageAssertionStatus.put(pname, enabled);
  }

  public void clearAssertionStatus() {
    classAssertionStatus = new HashMap<String, Boolean>();
    packageAssertionStatus = new HashMap<String, Boolean>();
    defaultAssertionStatus = false;
  }
}
