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

    for (int i = 0; i < size; i++) {
      elementReferences[i] = MJIEnv.NULL;
    }
    
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

  public static int toArray___3Ljava_lang_Object_2___3Ljava_lang_Object_2(MJIEnv env, int thisRef, int arrayRef) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    int size = alEI.getIntField("size");

    int elementsRef = alEI.getReferenceField("elementData");
    Fields fields = env.getHeap().get(elementsRef).getFields();
    int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();

    Fields arrayFields = env.getHeap().get(arrayRef).getFields();
    int arrayReferences[] = ((ReferenceArrayFields) arrayFields).asReferenceArray();

    if (size <= arrayReferences.length) {
      System.arraycopy(elementReferences, 0, arrayReferences, 0, size);

      return arrayRef;
    }


    ElementInfo arrEI = env.getElementInfo(arrayRef);
    String typeName = arrEI.getArrayType();

    int newArrayRef = env.newObjectArray(typeName, size);
    Fields newArrayFields = env.getHeap().get(newArrayRef).getFields();
    int newArrayReferences[] = ((ReferenceArrayFields) newArrayFields).asReferenceArray();

    System.arraycopy(elementReferences, 0, newArrayReferences, 0, size);

    return newArrayRef;
  }

  public static int get__I__Ljava_lang_Object_2(MJIEnv env, int thisRef, int index) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    int size = alEI.getIntField("size");

    int elementsRef = alEI.getReferenceField("elementData");
    Fields fields = env.getHeap().get(elementsRef).getFields();
    int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();

    if (checkRange(env, size, index)) {
      return elementReferences[index];
    }
    return MJIEnv.NULL;
  }

  public static int set__ILjava_lang_Object_2__Ljava_lang_Object_2(MJIEnv env, int thisRef, int index, int newObjRef) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    int size = alEI.getIntField("size");

    int elementsRef = alEI.getReferenceField("elementData");
    Fields fields = env.getHeap().get(elementsRef).getFields();
    int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();

    if (checkRange(env, size, index)) {
      int oldRef = elementReferences[index];
      elementReferences[index] = newObjRef;

      return oldRef;
    }
    return MJIEnv.NULL;
  }

  public static boolean add__Ljava_lang_Object_2__Z(MJIEnv env, int thisRef, int newObjRef) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    int size = alEI.getIntField("size");

    ensureCapacity__I__V(env, thisRef, size + 1);

    int elementsRef = alEI.getReferenceField("elementData");
    Fields fields = env.getHeap().get(elementsRef).getFields();
    int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();
    
    elementReferences[size++] = newObjRef;

    alEI.setIntField("size", size);

    return true;
  }

  public static void add__ILjava_lang_Object_2__V(MJIEnv env, int thisRef, int index, int newObjRef) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    int size = alEI.getIntField("size");

    if (checkRange(env, size + 1, index)) {
      incModCount(alEI);
      ensureCapacity__I__V(env, thisRef, size + 1);

      int elementsRef = alEI.getReferenceField("elementData");
      Fields fields = env.getHeap().get(elementsRef).getFields();
      int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();

      int toMove = size - index;
      if (toMove > 0) {
        System.arraycopy(elementReferences, index, elementReferences, index + 1, toMove);
      }
      elementReferences[index] = newObjRef;
      size++;

      alEI.setIntField("size", size);
    }
  }

  public static int remove__I__Ljava_lang_Object_2(MJIEnv env, int thisRef, int index) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    int size = alEI.getIntField("size");

    int elementsRef = alEI.getReferenceField("elementData");
    Fields fields = env.getHeap().get(elementsRef).getFields();
    int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();

    if (checkRange(env, size, index)) {
      int oldRef = elementReferences[index];
      internalRemove(env, alEI, size, elementReferences, index);

      return oldRef;
    }

    return MJIEnv.NULL;
  }

  private static void internalRemove(MJIEnv env, ElementInfo alEI, int size, int elementReferences[], int index) {
    incModCount(alEI);
    int numMoved = size - index - 1;

    if (numMoved > 0) {
      System.arraycopy(elementReferences, index + 1, elementReferences, index, numMoved);
    }
    
    elementReferences[--size] = MJIEnv.NULL;

    alEI.setIntField("size", size);
  }

  // modCount is used by list iterator to check if there were any modifications
  // of list between iterator methods calls
  private static void incModCount(ElementInfo alEI) {
    int modCount = alEI.getIntField("modCount");
    alEI.setIntField("modCount", modCount + 1);
  }

  private static boolean checkRange(MJIEnv env, int size, int index) {
    if (index < size && index >= 0) {
      return true;
    }
    env.throwException("java.lang.IndexOutOfBoundsException",
            "Index: " + index + ", Size: " + size);
    return false;
  }

}
