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
import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 * 
 * unit test for ClassLoaderInfo
 */
public class ClassLoaderInfoTest extends TestJPF {

  @Test
  public void testSystemClassLoader() {
    //--- Sets up the JPF environment
    // just use some dummy class to be able to initialize jpf & jvm 
    String[] args = {"+target=HelloWorld"};
    Config config = JPF.createConfig(args);
    JPF jpf = new JPF(config);
    ClassLoaderInfo.config = config;
    JVM vm = jpf.getVM();
    vm.initialize();


    //--- Gets systemClassLoaders
    // gets the systemClassLoader which already loaded the startup classes
    SystemClassLoader cl1 = vm.getSystemClassLoader();
    // create a new systemClassLoader
    SystemClassLoader cl2 = vm.createSystemClassLoader();
    // loades the startup classes
    cl2.loadStartUpClasses(vm);


    //--- Tests classloaders
    assert cl1.resolvedClasses.size() == ClassInfo.getLoadedClasses().length;
    assert cl1.getGlobalId() != cl2.getGlobalId();
    assert cl1.staticArea != cl2.staticArea;
    assert cl1.parent == null;
    assert cl2.parent == null;

    Heap heap = vm.getHeap();

    int cl1ObjRef = cl1.objRef;
    ElementInfo ei1 = heap.get(cl1ObjRef);
    assert ei1.getIntField("clRef") == cl1.getGlobalId();

    int cl2ObjRef = cl2.objRef;
    ElementInfo ei2 = heap.get(cl2ObjRef);
    assert ei2.getIntField("clRef") == cl2.getGlobalId();


    //--- Tests classes which are already loaded by both classLoaders
    ClassInfo ci1 = cl1.getResolvedClassInfo("java.lang.Class");
    ClassInfo ci2 = cl2.getResolvedClassInfo("java.lang.Class");

    assert ci1 != ci2;
    assert ci1.getName().equals(ci2.getName());
    assert ci1.getClassFileUrl().equals(ci2.getClassFileUrl());
    assert ci1.getUniqueId() != ci2.getUniqueId();
    // cl1 loaded java.lang.Class earlier than cl2, therefore 
    // ClassInfo.loadedClasses must contain ci1 and not ci2
    assert ci1 == ClassInfo.loadedClasses.get(ci2.getClassFileUrl());


    //--- Tests classes which are going to be loaded by both classLoaders
    ci2 = cl2.getResolvedClassInfo("java.util.ArrayList");
    ci1 = cl1.getResolvedClassInfo("java.util.ArrayList");

    assert ci1 != ci2;
    assert ci1.getName().equals(ci2.getName());
    assert ci1.getClassFileUrl().equals(ci2.getClassFileUrl());

    ThreadInfo ti = vm.getCurrentThread();
    // classes need to be registered before retrieving the uniqueIds
    cl1.registerClass(ti, ci1);
    cl2.registerClass(ti, ci2);
    assert ci1.getUniqueId() != ci2.getUniqueId();  
    // cl2 loaded java.util.ArrayList earlier than cl2, therefore 
    // ClassInfo.loadedClasses must contain ci2 and not ci1
    assert ci2 == ClassInfo.loadedClasses.get(ci1.getClassFileUrl());
  }
}
