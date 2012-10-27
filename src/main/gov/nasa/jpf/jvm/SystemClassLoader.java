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

import gov.nasa.jpf.classfile.ClassPath;

import java.io.File;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 * 
 * Represents the JPF system classloader which models the following hierarchy.
 * 
 *            ----------------
 *            | Bootstrap CL |
 *            ----------------
 *                   |
 *            ----------------
 *            | Extension CL |
 *            ----------------
 *                   |
 *           ------------------
 *           | Application CL |
 *           ------------------
 *           
 * Since in the standard JVM user does not have any control over the built-in 
 * classloaders hierarchy, in JPF, we model all three by an instance of 
 * SystemClassLoader which is responsible to load classes from Java API, 
 * standard extensions packages, and the local file system.     
 */
public class SystemClassLoader extends ClassLoaderInfo {

  protected ClassInfo objectClassInfo;
  protected ClassInfo classClassInfo;
  protected ClassInfo stringClassInfo;
  protected ClassInfo weakRefClassInfo;
  protected ClassInfo refClassInfo;
  protected ClassInfo enumClassInfo;
  protected ClassInfo threadClassInfo;
  protected ClassInfo charArrayClassInfo;

  protected SystemClassLoader (JVM vm) {
    super(vm, MJIEnv.NULL, null, null);
    setSystemClassPath();
    isSystemClassLoader = true;
    classInfo = getResolvedClassInfo("java.lang.ClassLoader");
  }

  /**
   * Builds the classpath for our system class loaders which resemblances the 
   * location for classes within,
   *        - Java API ($JREHOME/Classes/classes.jar,...) 
   *        - standard extensions packages ($JREHOME/lib/ext/*.jar)
   *        - the local file system ($CLASSPATH)
   */
  protected void setSystemClassPath (){
    cp = new ClassPath();

    for (File f : config.getPathArray("boot_classpath")){
      cp.addPathName(f.getAbsolutePath());
    }

    for (File f : config.getPathArray("classpath")){
      cp.addPathName(f.getAbsolutePath());
    }

    // finally, we load from the standard Java libraries
    String v = System.getProperty("sun.boot.class.path");
    if (v != null) {
      for (String pn : v.split(File.pathSeparator)){
        cp.addPathName(pn);
      }
    }
  }

  @Override
  public ClassInfo loadClass(String cname) {
    return getResolvedClassInfo(cname);
  }

  private ArrayList<String> getStartupClasses(JVM vm) {
    ArrayList<String> startupClasses = new ArrayList<String>(128);

    // bare essentials
    startupClasses.add("java.lang.Object");
    startupClasses.add("java.lang.Class");
    startupClasses.add("java.lang.ClassLoader");

    // the builtin types (and their arrays)
    startupClasses.add("boolean");
    startupClasses.add("[Z");
    startupClasses.add("byte");
    startupClasses.add("[B");
    startupClasses.add("char");
    startupClasses.add("[C");
    startupClasses.add("short");
    startupClasses.add("[S");
    startupClasses.add("int");
    startupClasses.add("[I");
    startupClasses.add("long");
    startupClasses.add("[J");
    startupClasses.add("float");
    startupClasses.add("[F");
    startupClasses.add("double");
    startupClasses.add("[D");
    startupClasses.add("void");

    // the box types
    startupClasses.add("java.lang.Boolean");
    startupClasses.add("java.lang.Character");
    startupClasses.add("java.lang.Short");
    startupClasses.add("java.lang.Integer");
    startupClasses.add("java.lang.Long");
    startupClasses.add("java.lang.Float");
    startupClasses.add("java.lang.Double");
    startupClasses.add("java.lang.Byte");

    // the cache for box types
    startupClasses.add("gov.nasa.jpf.BoxObjectCaches");

    // standard system classes
    startupClasses.add("java.lang.String");
    startupClasses.add("java.lang.ThreadGroup");
    startupClasses.add("java.lang.Thread");
    startupClasses.add("java.lang.Thread$State");
    startupClasses.add("java.io.PrintStream");
    startupClasses.add("java.io.InputStream");
    startupClasses.add("java.lang.System");
    startupClasses.add("java.lang.ref.Reference");
    startupClasses.add("java.lang.ref.WeakReference");
    startupClasses.add("java.lang.Enum");

    // we could be more fancy and use wildcard patterns and the current classpath
    // to specify extra classes, but this could be VERY expensive. Projected use
    // is mostly to avoid static init of single classes during the search
    String[] extraStartupClasses = config.getStringArray("vm.extra_startup_classes");
    if (extraStartupClasses != null) {      
      for (String extraCls : extraStartupClasses) {
        startupClasses.add(extraCls);
      }
    }

    // last not least the application main class
    startupClasses.add(vm.getMainClassName());

    return startupClasses;
  }

  // it keeps the startup classes
  ArrayList<ClassInfo> startupQueue = new ArrayList<ClassInfo>(32);

  protected void registerStartupClasses (JVM vm, ThreadInfo ti) {
    ArrayList<String> startupClasses = getStartupClasses(vm);
    startupQueue = new ArrayList<ClassInfo>(32);

    // now resolve all the entries in the list and queue the corresponding ClassInfos
    for (String clsName : startupClasses) {
      ClassInfo ci = getResolvedClassInfo(clsName);
      if (ci != null) {
        ci.registerStartupClass( ti, startupQueue);
      } else {
        JVM.log.severe("can't find startup class ", clsName);
      }
    }    
  }

  protected ArrayList<ClassInfo> getStartupQueue() {
    return startupQueue;
  }
  
  protected void createStartupClassObjects (ThreadInfo ti){
    for (ClassInfo ci : startupQueue) {
      ci.createAndLinkClassObject(ti);
    }
  }

  protected void pushClinits (ThreadInfo ti) {
    // we have to traverse backwards, since what gets pushed last is executed first
    for (ListIterator<ClassInfo> it=startupQueue.listIterator(startupQueue.size()); it.hasPrevious(); ) {
      ClassInfo ci = it.previous();

      MethodInfo mi = ci.getMethod("<clinit>()V", false);
      if (mi != null) {
        MethodInfo stub = mi.createDirectCallStub("[clinit]");
        StackFrame frame = new DirectCallStackFrame(stub);
        ti.pushFrame(frame);
      } else {
        ci.setInitialized();
      }
    }
  }

  /**
   * This loads the startup classes. Loading includes the following steps:
   *   1. Defines
   *   2. Resolves
   *   3. Initializes
   */
  protected void loadStartUpClasses(JVM vm, ThreadInfo ti) {
    registerStartupClasses(vm, ti);
    createStartupClassObjects(vm.getCurrentThread());
    pushClinits(vm.getCurrentThread());
  }

  //-- ClassInfos cache management --

  protected void updateCachedClassInfos (ClassInfo ci) {
    String name = ci.name;      
    if ((objectClassInfo == null) && name.equals("java.lang.Object")) {
      objectClassInfo = ci;
    } else if ((classClassInfo == null) && name.equals("java.lang.Class")) {
      classClassInfo = ci;
    } else if ((stringClassInfo == null) && name.equals("java.lang.String")) {
      stringClassInfo = ci;
    } else if ((charArrayClassInfo == null) && name.equals("[C")) {
      charArrayClassInfo = ci;
    } else if ((weakRefClassInfo == null) && name.equals("java.lang.ref.WeakReference")) {
      weakRefClassInfo = ci;
    } else if ((refClassInfo == null) && name.equals("java.lang.ref.Reference")) {
      refClassInfo = ci;
    } else if ((enumClassInfo == null) && name.equals("java.lang.Enum")) {
      enumClassInfo = ci;
    } else if ((threadClassInfo == null) && name.equals("java.lang.Thread")) {
      threadClassInfo = ci;
    }
  }

  protected ClassInfo getObjectClassInfo() {
    return objectClassInfo;
  }

  protected ClassInfo getClassClassInfo() {
    return classClassInfo;
  }

  protected ClassInfo getStringClassInfo() {
    return stringClassInfo;
  }
  
  protected ClassInfo getCharArrayClassInfo() {
    return charArrayClassInfo;
  }
}
