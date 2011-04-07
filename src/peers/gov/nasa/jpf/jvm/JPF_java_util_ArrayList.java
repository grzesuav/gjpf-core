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

import java.util.Arrays;

/**
 *
 * @author Ivan Mushketik
 */
public class JPF_java_util_ArrayList {

  public static void $init__I__V(MJIEnv env, int thisRef, int initialCapacity) {
    if (initialCapacity < 0) {
      env.throwException("java.lang.IllegalArgumentException", "Illegal capacity + " + initialCapacity);
    }
    ElementInfo alEI = env.getElementInfo(thisRef);

    int elementsRef = env.newObjectArray("java.lang.Object", initialCapacity);
    alEI.setReferenceField("elementData", elementsRef);
  }

  public static void $init____V(MJIEnv env, int thisRef) {
    $init__I__V(env, thisRef, 10);
  }

  public static void trimToSize____V(MJIEnv env, int thisRef) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    incModCount(alEI);
    int size = alEI.getIntField("size");

    int elementsRef = alEI.getReferenceField("elementData");
    Fields fields = env.getHeap().get(elementsRef).getFields();
    int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();

    if (size < elementReferences.length) {
      int newElementsRef = env.newObjectArray("java.lang.Object", size);
      Fields newElementsFields = env.getHeap().get(newElementsRef).getFields();
      int newElementsReferences[] = ((ReferenceArrayFields) newElementsFields).asReferenceArray();

      System.arraycopy(elementReferences, 0, newElementsReferences, 0, size);

      alEI.setReferenceField("elementData", newElementsRef);
    }
  }

  public static void ensureCapacity__I__V(MJIEnv env, int thisRef, int minCapacity) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    incModCount(alEI);

    int elementsRef = alEI.getReferenceField("elementData");
    Fields fields = env.getHeap().get(elementsRef).getFields();
    int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();

    int oldCapacity = elementReferences.length;

    if (oldCapacity < minCapacity) {
      int newCapacity = (oldCapacity * 3) / 2 + 1;
      if (newCapacity < minCapacity)
        newCapacity = minCapacity;

      int newElementsRef = env.newObjectArray("java.lang.Object", newCapacity);
      Fields newElementsFields = env.getHeap().get(newElementsRef).getFields();
      int newElementsReferences[] = ((ReferenceArrayFields) newElementsFields).asReferenceArray();

      System.arraycopy(elementReferences, 0, newElementsReferences, 0, oldCapacity);

      alEI.setReferenceField("elementData", newElementsRef);
    }
  }

  public static int size____I(MJIEnv env, int thisRef) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    return alEI.getIntField("size");
  }

  public static boolean isEmpty____Z(MJIEnv env, int thisRef) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    int size = alEI.getIntField("size");
    return size == 0;
  }

  public static void clear____V(MJIEnv env, int thisRef) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    incModCount(alEI);
    int size = alEI.getIntField("size");

    int ref = alEI.getReferenceField("elementData");
    Fields fields = env.getHeap().get(ref).getFields();
    int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();

    for (int i = 0; i < size; i++)
      elementReferences[i] = MJIEnv.NULL;

    alEI.setIntField("size", 0);
  }

  public static int toArray_____3Ljava_lang_Object_2(MJIEnv env, int thisRef) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    int size = alEI.getIntField("size");

    int elementsRef = alEI.getReferenceField("elementData");
    Fields fields = env.getHeap().get(elementsRef).getFields();
    int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();

    int cloneElementsRef = env.newObjectArray("java.lang.Object", size);
    Fields cloneElementsFields = env.getHeap().get(cloneElementsRef).getFields();
    int newElementsReferences[] = ((ReferenceArrayFields) cloneElementsFields).asReferenceArray();

    System.arraycopy(elementReferences, 0, newElementsReferences, 0, size);

    return cloneElementsRef;
  }

  // modCount is used by list iterator to check if there were any modifications
  // of list between iterator methods calls
  private static void incModCount(ElementInfo alEI) {
    int modCount = alEI.getIntField("modCount");
    alEI.setIntField("modCount", modCount + 1);
  }

}
