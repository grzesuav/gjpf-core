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

import java.util.ArrayList;
import java.util.Iterator;

public class ClassLoaderList implements Cloneable, Iterable<ClassLoaderInfo>, Restorable<ClassLoaderList> {

  /** the list of the class loaders */
  public ArrayList<ClassLoaderInfo> classLoaders;

  static class ClListMemento implements Memento<ClassLoaderList> {
    Memento<ClassLoaderInfo>[] clMementos;

    ClListMemento(ClassLoaderList cll) {
      ArrayList<ClassLoaderInfo> classLoaders = cll.classLoaders;
      int len = classLoaders.size();

      clMementos =  new Memento[len];
    
      for (int i=0; i<len; i++){
        ClassLoaderInfo cl = classLoaders.get(i);
        Memento<ClassLoaderInfo> m = cl.getMemento();
        clMementos[i] = m;
      }
    }

    public ClassLoaderList restore(ClassLoaderList cll){
      int len = clMementos.length;
      ArrayList<ClassLoaderInfo> classLoaders = new ArrayList<ClassLoaderInfo>();
      for (int i=0; i<len; i++){
        Memento<ClassLoaderInfo> m = clMementos[i];
        ClassLoaderInfo cl = m.restore(null);
        classLoaders.add(cl);
      }
      cll.classLoaders = classLoaders;

      return cll;
    }
  }

  public ClassLoaderList() {
    classLoaders = new ArrayList<ClassLoaderInfo>();
  }

  public Memento<ClassLoaderList> getMemento (MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<ClassLoaderList> getMemento(){
    return new ClListMemento(this);
  }

  public Iterator<ClassLoaderInfo> iterator () {
    return classLoaders.iterator();
  }

  public void add(ClassLoaderInfo cl) {
    classLoaders.add(cl);
  }

  public ClassLoaderInfo get(int i) {
    // <2do> - just to make it work for now
    return classLoaders.get(0);
  }

  public int size() {
    return classLoaders.size();
  }
  
  public void markRoots (Heap heap) {
    int len = classLoaders.size();
    for (int i=0; i<len; i++) {
      ClassLoaderInfo cli = classLoaders.get(i);
      cli.getStatics().markRoots(heap);
    }
  }
}
