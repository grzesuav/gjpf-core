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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.classfile.ClassFileException;
import gov.nasa.jpf.classfile.ClassPath;
import gov.nasa.jpf.jvm.ThreadInfo.State;
import gov.nasa.jpf.jvm.bytecode.Instruction;

/**
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

  // The area containing static fields and  classes
  protected StaticArea staticArea;

  protected boolean isSystemClassLoader = false;

  // Search global id, which is the basis for canonical order of classloaders
  protected int gid;

  // The java.lang.ClassLoader object reference
  protected int objRef;

  protected ClassLoaderInfo parent;

  static Config config;

  static GlobalIdManager gidManager = new GlobalIdManager();

  static class ClMemento implements Memento<ClassLoaderInfo> {
    // note that we don't have to store the invariants (gid, cp, parent, isSystemClassLoader)
    ClassLoaderInfo cl;
    Memento<StaticArea> saMemento;

    ClMemento (ClassLoaderInfo cl){
      this.cl = cl;
      saMemento = cl.staticArea.getMemento();
    }

    public ClassLoaderInfo restore(ClassLoaderInfo ignored) {
      saMemento.restore(cl.staticArea);
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
      ei.setReferenceField("parent", parent.objRef);
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

  public ClassInfo getResolvedClassInfo (String className) throws NoClassInfoException {
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

  public ClassInfo getResolvedClassInfo (String className, byte[] buffer, int offset, int length) throws NoClassInfoException {
    if (className == null) {
      return null;   
    }
    
    String typeName = Types.getClassNameFromTypeName(className);

    
    ClassInfo ci = definedClasses.get(typeName);
    
    if (ci == null) {
      ci = ClassInfo.getResolvedClassInfo(className, buffer, offset, length, this);
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
    } catch (NoClassInfoException cx){
      return null;
    }
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

      for (String ifcName : ci.interfaceNames) {
        ClassInfo ici = ClassInfo.getResolvedClassInfo(ifcName); // already resolved at this point
        ici.registerClass(ti);
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

  public String findResource (String resourceName){
    // would have been nice to just delegate this to the BCEL ClassPath, but
    // unfortunately BCELs getPath() doesn't indicate at all if the resource
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
   * For now, this always returns true, and it used while the classloader is being
   * serialized. That is going to be changed if we ever consider unloading the
   * classes. For now, it is just added in analogy to ThreadInfo
   */
  public boolean isAlive () {
    return true;
  }
}
