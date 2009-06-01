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
package gov.nasa.jpf.jvm.untracked;

import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.Fields;
import gov.nasa.jpf.jvm.MJIEnv;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * The instance of this class (it is a singleton) marks objects
 * as untracked and restores tracked fields of these objects.
 *
 * @author Milos Gligoric (milos.gligoric@gmail.com)
 * @author Tihomir Gvero (tihomir.gvero@gmail.com)
 *
 */
public class UntrackedManager {
  // the only instance of this class
  private static UntrackedManager manager = null;

  // "untracked" mode is on when "vm.untracked" is set to true
  private static boolean untrackedProperty = false;

  private UntrackedManager () { }

  public static UntrackedManager getInstance () {
    if (manager == null && untrackedProperty) {
      manager = new UntrackedManager();
    }
    return manager;
  }

  public static boolean getProperty () {
    return untrackedProperty;
  }

  public static void setProperty (boolean prop) {
    untrackedProperty = prop;
  }

  /*
   * This method is responsible for the traversal that affects all untracked
   * objects that are linked one to each other, starting from the object, whose
   * index is objref. After this traversal all of these objects are unmarked,
   * i.e. they are tracked, except those that are still connected to some other
   * untracked object. This is why we keep the number of untracked references
   * that point to one single object. This number we hold in field called
   * "untracked" in class Fields.
   */
  public void oldObjectsTraversal (int objref) {
    if (objref == MJIEnv.NULL)
      return;
    DynamicArea da = DynamicArea.getHeap();

    if (da.get(objref) == null)
      return;

    HashSet<Integer> visited = new HashSet<Integer>();     // pcm
    LinkedList<Integer> list = new LinkedList<Integer>();
    addRef(objref, visited, list);                       // pcm

    while (!list.isEmpty()) {
      int index = list.removeFirst();
      ElementInfo ei = da.get(index);
      Fields fields = ei.getFields();
      fields.decUntracked();

      if (!fields.isUntracked()) {
        if (fields.isReferenceArray()) {
          int numOfFields = fields.size();
          for (int i = 0; i < numOfFields; i++) {
            int childObjref = fields.getReferenceValue(i);
            if (childObjref != MJIEnv.NULL) {
              addRef(childObjref, visited, list);       // pcm
            }
          }

        } else {
          int numOfFields = fields.getNumberOfFields();
          for (int i = 0; i < numOfFields; i++) {
            FieldInfo fi = ei.getFieldInfo(i);

            if (fi.isReference()) {
              int childObjref = fields.getReferenceValue(fi.getStorageOffset());
              if (childObjref != MJIEnv.NULL) {
                addRef(childObjref, visited, list);       // pcm
              }
            }
          }
        }
      }
    }
  }

  /*
   * This method is responsible for traversal that affects all tracked objects
   * that are linked together, starting from the object, whose index is
   * objref. After this traversal all of these objects are marked as untracked.,
   * i.e. they are untracked.
   */
  public void newObjectsTraversal (int objref) {
    if (objref == MJIEnv.NULL)
      return;
    DynamicArea da = DynamicArea.getHeap();

    if (da.get(objref) == null)
      return;

    HashSet<Integer> visited = new HashSet<Integer>();     // pcm
    LinkedList<Integer> list = new LinkedList<Integer>();
    addRef(objref, visited, list);                       // pcm

    while (!list.isEmpty()) {
      int index = list.removeFirst();
      ElementInfo ei = da.get(index);
      Fields fields = ei.getFields();
      if (!fields.isUntracked()) {
        if (fields.isReferenceArray()) {
          int numOfFields = fields.size();
          for (int i = 0; i < numOfFields; i++) {
            int childObjref = fields.getReferenceValue(i);
            if (childObjref != MJIEnv.NULL) {
              addRef(childObjref, visited, list);       // pcm
            }
          }
        } else {
          int numOfFields = fields.getNumberOfFields();
          for (int i = 0; i < numOfFields; i++) {
            FieldInfo fi = ei.getFieldInfo(i);
            if (fi.isReference()) {
              int childObjref = fields.getReferenceValue(fi.getStorageOffset());
              if (childObjref != MJIEnv.NULL) {
                addRef(childObjref, visited, list);     // pcm
              }
            }
          }
        }
      }
      fields.incUntracked();
    }
  }

  // <2do> pcm - this is just a temporary workaround to avoid infinite loops
  // for cyclic references. fix it so that it becomes efficient
  private void addRef (int ref, HashSet<Integer> visited, LinkedList<Integer> queue) {
    Integer r = new Integer(ref);
    if (!visited.contains(r)) {
      visited.add(r);
      queue.add(r);
    }
  }

  /*
   * This method is responsible for restoring only "tracked" fields. A field is
   * "untracked" if it is marked as untracked by the annotation @UntrackedField
   * or it belongs to an object that is marked as untracked via the traversal of
   * untracked objects. If a field isn't "untracked" it is "tracked".
   */
  public void restoreTrackedFields (ElementInfo ei, Fields f) {
    Fields fields = ei.getFields();
    if (fields != null) {
      f.setUntracked(fields.getUntracked());
      if (fields.isUntracked())
        return;

      int numOfFields = ei.getNumberOfFields();
      for (int i = 0; i < numOfFields; i++) {
        FieldInfo fi = ei.getFieldInfo(i);
        if (fi.isUntracked()) {
          int storageSize = fi.getStorageSize();
          int storageOffset = fi.getStorageOffset();
          for (int j = 0; j < storageSize; j++)
            f.setIntValue(ei, storageOffset + j, fields.getIntValue(storageOffset
                + j));
        }
      }
    }

    ei.restoreFields(f);
  }

  public boolean isUntracked (ElementInfo ei) {
    Fields fields = ei.getFields();
    if (fields != null) {
      if (fields.isUntracked()) return true;

      int numOfFields = ei.getNumberOfFields();
      for (int i = 0; i < numOfFields; i++) {
        FieldInfo fi = ei.getFieldInfo(i);
        if (fi.isUntracked()) {
          return true;
        }
      }
    }
    return false;
  }

}
