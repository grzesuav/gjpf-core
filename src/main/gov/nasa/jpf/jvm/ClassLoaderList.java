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
        ClassLoaderInfo cl = classLoaders.get(0);
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
}
