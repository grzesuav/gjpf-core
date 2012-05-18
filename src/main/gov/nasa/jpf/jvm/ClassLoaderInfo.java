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
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.classfile.ClassFileException;
import gov.nasa.jpf.classfile.ClassPath;
import gov.nasa.jpf.util.ObjVector;

/**
 * Represents the classloader construct in JVM which is responsible for loading
 * classes.
 */
public class ClassLoaderInfo {

  // Map from the name of classes defined (directly loaded) by this classloader to
  // the corresponding ClassInfos
  protected static Map<String,ClassInfo> definedClasses;

  // Represents the locations where this classloader can load classes form
  protected ClassPath cp;

  // The area containing static fields and  classes
  protected StaticArea staticArea;

  protected boolean isSysClassLoader = false;
  
  static Config config;

  protected ClassLoaderInfo(JVM vm, ClassPath cp) {
    definedClasses = new HashMap<String,ClassInfo>();
    this.cp = cp;

    Class<?>[] argTypes = { Config.class, KernelState.class };
    Object[] args = { config, vm.getKernelState() };

    this.staticArea = config.getEssentialInstance("vm.static.class", StaticArea.class, argTypes, args);

    vm.registerClassLoader(this);
  }

  public boolean isSysClassLoader() {
    return isSysClassLoader;
  }

  /**
   * Builds the classpath for our system classes loader which resemblances the 
   * location for classes within,
   *        - Java API ($JREHOME/Classes/classes.jar,...) 
   *        - standard extensions packages ($JREHOME/lib/ext/*.jar)
   *        - the local file system ($CLASSPATH)
   */
  protected static ClassPath buildSystemClassPath (Config config){
    ClassPath sysClassPath = new ClassPath();

    for (File f : config.getPathArray("boot_classpath")){
      sysClassPath.addPathName(f.getAbsolutePath());
    }

    for (File f : config.getPathArray("classpath")){
      sysClassPath.addPathName(f.getAbsolutePath());
    }

    // finally, we load from the standard Java libraries
    String v = System.getProperty("sun.boot.class.path");
    if (v != null) {
      for (String pn : v.split(File.pathSeparator)){
        sysClassPath.addPathName(pn);
      }
    }

    return sysClassPath;
  }

  public static ClassLoaderInfo getCurrentClassLoader() {
    try {
      ThreadInfo ti = ThreadInfo.getCurrentThread();

      MethodInfo mi = ti.getTopFrame().getMethodInfo();

      ClassInfo ci = mi.getClassInfo();

      if(ci != null) {
        return ci.classLoader;
      } else {
        List<StackFrame> stack = ti.getStack();

        for(StackFrame sf: stack) {
          ci = sf.getMethodInfo().getClassInfo();
          if(ci != null) {
            return ci.classLoader;
          }
        }
      }
    } catch(NullPointerException e) {
      return JVM.getSysClassLoader();
    }

    return JVM.getSysClassLoader();
  }

  public ClassInfo getResolvedClassInfo (String className) throws NoClassInfoException {
    if (className == null) {
      return null;
    }

    String typeName = Types.getClassNameFromTypeName(className);

    ClassInfo ci = definedClasses.get(typeName);

    if (ci == null) {
      ci = ClassInfo.getResolvedClassInfo(className, this);
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
      StaticArea sa = JVM.getVM().getStaticArea();
      sa.addClass(ci, ti);

      ci.createClassObject(ti);
    }
  }

  protected ClassPath.Match getMatch(String typeName) {
    ClassPath.Match match;
    try {
      match = cp.findMatch(typeName);
      if (match == null){
        throw new NoClassInfoException(typeName);
      } 
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
}
