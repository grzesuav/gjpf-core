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
package gov.nasa.jpf.jvm;

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
import gov.nasa.jpf.classfile.ClassFileException;
import gov.nasa.jpf.classfile.ClassPath;
import gov.nasa.jpf.jvm.bytecode.Instruction;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 *  
 * Represents the classloader construct in JVM which is responsible for loading
 * classes.
 */
public class ClassLoaderInfo 
     implements Iterable<ClassInfo>, Comparable<ClassLoaderInfo>, Cloneable, Restorable<ClassLoaderInfo> {

  // Map from the name of classes defined (directly loaded) by this classloader to
  // the corresponding ClassInfos
  protected Map<String,ClassInfo> definedClasses;

  // Represents the locations where this classloader can load classes form
  protected ClassPath cp;

  // The type of the corresponding class loader object
  protected ClassInfo classInfo;

  // The area containing static fields and  classes
  protected StaticArea staticArea;

  protected boolean isSystemClassLoader = false;

  protected boolean roundTripRequired = false;

  // Search global id, which is the basis for canonical order of classloaders
  protected int gid;

  // The java.lang.ClassLoader object reference
  protected int objRef;

  protected ClassLoaderInfo parent;

  static Config config;

  static GlobalIdManager gidManager = new GlobalIdManager();

  static class ClMemento implements Memento<ClassLoaderInfo> {
    // note that we don't have to store the invariants (gid, parent, isSystemClassLoader)
    ClassLoaderInfo cl;
    Memento<StaticArea> saMemento;
    Memento<ClassPath> cpMemento;

    ClMemento (ClassLoaderInfo cl){
      this.cl = cl;
      saMemento = cl.staticArea.getMemento();
      cpMemento = cl.cp.getMemento();
    }

    public ClassLoaderInfo restore(ClassLoaderInfo ignored) {
      saMemento.restore(cl.staticArea);
      cpMemento.restore(null);
      return cl;
    }
  }

  protected ClassLoaderInfo(JVM vm, int objRef, ClassPath cp, ClassLoaderInfo parent) {
    definedClasses = new HashMap<String,ClassInfo>();

    this.cp = cp;
    this.parent = parent;
    this.gid = this.computeGlobalId(vm);
    this.objRef = objRef;

    Class<?>[] argTypes = { Config.class, KernelState.class };
    Object[] args = { config, vm.getKernelState() };

    this.staticArea = config.getEssentialInstance("vm.static.class", StaticArea.class, argTypes, args);

    vm.registerClassLoader(this);

    ElementInfo ei = vm.getElementInfo(objRef);

    // For systemClassLoaders, this object is still null, since the class java.lang.ClassLoader 
    // class cannot be loaded before creating the systemClassLoader
    if(ei!=null) {
      ei.setIntField("clRef", gid);
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
  public int getGlobalId() {
    return gid;
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

  protected int computeGlobalId (JVM vm){
    ThreadInfo tiExec = vm.getCurrentThread();
    Instruction insn = null;
    
    if (tiExec != null){
      insn = tiExec.getTopFrame().getPC();  
    }

    return gidManager.getNewId(vm.getSystemState(), tiExec, insn);
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
    return isSystemClassLoader;
  }

  public static ClassLoaderInfo getCurrentClassLoader() {
    try {
      ThreadInfo ti = ThreadInfo.getCurrentThread();

      MethodInfo mi = ti.getTopFrame().getMethodInfo();

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
    } catch(NullPointerException e) {
      return JVM.getVM().getSystemClassLoader();
    }

    return JVM.getVM().getSystemClassLoader();
  }

  /**
   * This is useful when there are multiple systemClassLoaders created.
   */
  public static ClassLoaderInfo getCurrentSystemClassLoader() {
    ClassLoaderInfo cl = getCurrentClassLoader();

    ClassInfo ci = cl.getClassInfo();

    while(!ci.getName().equals("java.lang.ClassLoader")) {
      ci = ci.getSuperClass();
    }

    return ci.getClassLoaderInfo();
  }

  public ClassInfo getResolvedClassInfo (String className) throws ClassInfoException {
    if (className == null) {
      return null;
    }

    String typeName = Types.getClassNameFromTypeName(className);

    ClassInfo ci = definedClasses.get(typeName);

    if (ci == null) {
      ClassPath.Match match = getMatch(typeName);
      ci = ClassInfo.getResolvedClassInfo(className, this, match);
      if(ci.classLoader != this) {
        // creates a new instance from ci using this classloader
        ci = ci.getInstanceFor(this);
      }

      // this class loader just defined the class ci.
      definedClasses.put(typeName, ci);
    }
    
    return ci;
  }

  public ClassInfo getResolvedClassInfo (String className, byte[] buffer, int offset, int length, ClassPath.Match match) throws ClassInfoException {
    if (className == null) {
      return null;   
    }

    String typeName = Types.getClassNameFromTypeName(className);

    ClassInfo ci = definedClasses.get(typeName);
    
    if (ci == null) {
      ci = ClassInfo.getResolvedClassInfo(typeName, buffer, offset, length, this, match);
      if(ci.classLoader != this) {
        // creates a new instance from ci using this classloader
        ci = ci.getInstanceFor(this);
      }

      // this class loader just defined the class ci.
      definedClasses.put(typeName, ci);
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

  /**
   * This method is only used in the case of non-systemClassLoaders. SystemClassLoader 
   * overrides this method.
   */
  protected ClassInfo loadSuperClass (ClassInfo ci, String superName) throws ClassInfoException {
    if (ci.isObjectClassInfo()) {
      return null;
    }

    ClassInfo.logger.finer("resolving superclass: ", superName, " of ", ci.getName());

    return loadClass(superName);
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
        ci.interfaces.add(loadClass(ifcName));
      }
    }
  }

  protected ClassInfo loadClass(String cname) {
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
    // Check if the given class is already defined by this classloader
    ClassInfo ci = getDefinedClassInfo(className);

    if (ci == null) {
      try {
        if(parent != null) {
          ci = parent.loadClassOnJVM(cname);
        } else {
          ClassLoaderInfo systemClassLoader = ClassLoaderInfo.getCurrentSystemClassLoader();
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
    // Check if the given class is already defined by this classloader
    ClassInfo ci = getDefinedClassInfo(className);

    // If the class is not among the defined classes of this classloader, then
    // do a round trip to execute the user code of loadClass(superName) 
    if(ci == null) {
      ThreadInfo ti = JVM.getVM().getCurrentThread();
      StackFrame frame = ti.getReturnedDirectCall();

      if(frame != null && frame.getMethodName().equals("[loadClass(" + cname + ")]")) {
        // the round trip ends here. loadClass(superName) is already executed on JPF
        int clsObjRef = frame.pop();

        if (clsObjRef == MJIEnv.NULL){
          throw new ClassInfoException(cname + ", is not found in the classloader search path", 
                                       "java.lang.NoClassDefFoundError", cname);
          } else {
            return ti.getEnv().getReferredClassInfo(clsObjRef);
          }
      } else {
        // the round trip starts here
        pushloadClassFrame(cname);
        // bail out right away & re-execute the current instruction in JPF
        throw new ResolveRequired(cname);
      }
    }
    return ci;
  }

  protected void pushloadClassFrame(String superName) {
    ThreadInfo ti = JVM.getVM().getCurrentThread();

    // obtain the class of this ClassLoader
    ClassInfo clClass = JVM.getVM().getClassInfo(objRef);

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
    return definedClasses.get(typeName);
  }

  // Note: JVM.registerStartupClass() must be kept in sync
  public void registerClass (ThreadInfo ti, ClassInfo ci){
    // ti might be null if we are still in main thread creation
    StaticElementInfo sei = ci.sei;
    ClassInfo superClass = ci.superClass;

    if (sei == null){
      // do this recursively for superclasses and interfaceNames
      if (superClass != null) {
        superClass.registerClass(ti);
      }

      for (ClassInfo ifc : ci.interfaces) {
        ifc.registerClass(ti);
      }

      ClassInfo.logger.finer("registering class: ", ci.name);

      // register ourself in the static area
      StaticArea sa = ci.getClassLoaderInfo().getStaticArea();
      sa.addClass(ci, ti);

      ci.createClassObject(ti);
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

  public StaticArea getStaticArea() {
    return staticArea;
  }

  public ClassPath getClassPath() {
    return cp;
  }

  public String[] getClassPathElements() {
    return cp.getPathNames();
  }

  /**
   * This is invoked by JVM.initSubsystems()
   */
  static void init (Config config) {
    ClassLoaderInfo.config = config;
  }

  /**
   * Comparison for sorting based on index.
   */
  public int compareTo (ClassLoaderInfo that) {
    return this.gid - that.gid;
  }

  /**
   * Returns an iterator over the classes that are defined (directly loaded) by this
   * classloader. 
   */
  public Iterator<ClassInfo> iterator () {
    return definedClasses.values().iterator();
  }

  /**
   * Creates a classLoader object in the heap
   */
  protected ElementInfo createClassLoaderObject(ClassInfo ci, ClassLoaderInfo parent, ThreadInfo ti) {
    Heap heap = JVM.getVM().getHeap();

    //--- create ClassLoader object of type ci which corresponds to this ClassLoader
    int objRef = heap.newObject( ci, ti);

    //--- make sure that the classloader object is not garbage collected 
    heap.registerPinDown(objRef);

    //--- initialize the systemClassLoader object
    ElementInfo ei = heap.get(objRef);
    ei.setIntField("clRef", gid);

    int parentRef;
    if(parent == null) {
      parentRef = MJIEnv.NULL;
    } else {
      parentRef = parent.objRef;
    }
    ei.setReferenceField("parent", parentRef);

    this.objRef = objRef;

    return ei;
  }

  /**
   * For now, this always returns true, and it used while the classloader is being
   * serialized. That is going to be changed if we ever consider unloading the
   * classes. For now, it is just added in analogy to ThreadInfo
   */
  public boolean isAlive () {
    return true;
  }
}
