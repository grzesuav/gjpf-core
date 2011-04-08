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

import gov.nasa.jpf.util.InvocationDataStack;
import gov.nasa.jpf.util.InvocationData;

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

  private static InvocationDataStack ids = new InvocationDataStack();

  public static boolean remove__Ljava_lang_Object_2__Z(MJIEnv env, int thisRef, int objRef) {
    if (objRef == MJIEnv.NULL) {
      // No need to call equals method. Special case.
      return removeNULL(env, thisRef);
    }

    ThreadInfo ti = env.getThreadInfo();
    DirectCallStackFrame frame = ti.getReturnedDirectCall();

    InvocationData id = ids.get();
    int elementRef = -1;

    // We calculated equals and now can check it result value
    if (frame != null && id.isRepetetiveCall(frame.getPrevious())) {
      int result = frame.pop();
      RemoveInvocationData rid = (RemoveInvocationData) id;

      // Found object to delete
      if (result != 0) {
        internalRemove(env, rid.alEI, rid.size, rid.elementsRefs, rid.pos);
        ids.remove();
        return true;
      }

      if (rid.pos < rid.size) {
        rid.pos++;
        elementRef = rid.elementsRefs[rid.pos];
      }
      else {
        return false;
      }
    }
    // First call, or non-native method called remove
    else {
      ElementInfo alEI = env.getElementInfo(thisRef);
      int size = alEI.getIntField("size");

      // Empty array. Nothing to delete
      if (size == 0) {
        return false;
      }

      int elementsRef = alEI.getReferenceField("elementData");
      Fields fields = env.getHeap().get(elementsRef).getFields();
      int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();
      StackFrame topFrame = ti.getTopFrame();

      // Cache for higher performance
      RemoveInvocationData rid = new RemoveInvocationData(topFrame, alEI, elementReferences, size, 0);
      ids.add(rid);

      elementRef = elementReferences[0];
    }


    // Create equals method stub and call it
    ClassInfo objCI = env.getClassInfo(objRef);
    MethodInfo mi = objCI.getMethod("equals(Ljava/lang/Object;)Z", true);
    MethodInfo stub = mi.createReflectionCallStub();
    frame = new DirectCallStackFrame(stub, stub.getMaxStack(), stub.getMaxLocals());

    frame.push(objRef, true);
    frame.push(elementRef, true);

    ti.pushFrame(frame);

    return false;

  }

  private static boolean removeNULL(MJIEnv env, int thisRef) {
    ElementInfo alEI = env.getElementInfo(thisRef);
    int size = alEI.getIntField("size");

    int elementsRef = alEI.getReferenceField("elementData");
    Fields fields = env.getHeap().get(elementsRef).getFields();
    int elementReferences[] = ((ReferenceArrayFields) fields).asReferenceArray();

    for (int i = 0; i < size; i++) {
      if (elementReferences[i] == MJIEnv.NULL) {
        internalRemove(env, alEI, size, elementReferences, i);
        return true;
      }
    }

    return false;
  }

  private static void internalRemove(MJIEnv env, ElementInfo alEI, int size, int elementReferences[], int pos) {
    incModCount(alEI);
    int numMoved = size - pos - 1;

    if (numMoved > 0) {
      System.arraycopy(elementReferences, pos + 1, elementReferences, pos, numMoved);
    }

    --size;
    elementReferences[size] = MJIEnv.NULL;

    alEI.setIntField("size", size);
  }

  // modCount is used by list iterator to check if there were any modifications
  // of list between iterator methods calls
  private static void incModCount(ElementInfo alEI) {
    int modCount = alEI.getIntField("modCount");
    alEI.setIntField("modCount", modCount + 1);
  }

}

class RemoveInvocationData extends InvocationData {

  ElementInfo alEI;
  int elementsRefs[];
  int size;
  int pos;

  public RemoveInvocationData(StackFrame currentStackFrame, ElementInfo alEI, int elementsRefs[], int size, int pos) {
    super(currentStackFrame);

    this.alEI = alEI;
    this.elementsRefs = elementsRefs;
    this.size = size;
    this.pos = pos;
  }
}