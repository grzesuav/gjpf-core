package gov.nasa.jpf.jvm;

import java.util.ArrayList;
import java.util.Iterator;

public class ClassLoaderList implements Cloneable, Iterable<ClassLoaderInfo> {

  /** the list of the class loaders */
  public ArrayList<ClassLoaderInfo> classLoaders;

  public ClassLoaderList() {
    classLoaders = new ArrayList<ClassLoaderInfo>();
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
