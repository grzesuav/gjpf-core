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

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.BitSet64;
import gov.nasa.jpf.util.Debug;
import gov.nasa.jpf.util.FixedBitSet;
import gov.nasa.jpf.util.HashData;

import java.io.PrintWriter;
import java.lang.ref.SoftReference;

/**
 * Describes an element of memory containing the field values of a class or an
 * object. In the case of a class, contains the values of the static fields. For
 * an object contains the values of the object fields.
 *
 * @see gov.nasa.jpf.jvm.FieldInfo
 */
public abstract class ElementInfo implements Cloneable, Restorable<ElementInfo> {

  //--- the lower 2 bytes of the attribute field are sticky (state stored)

  // object attribute flag values
  public static final int   ATTR_NONE          = 0x0000;

  // object doesn't change value
  public static final int   ATTR_IMMUTABLE     = 0x0001;

  // don't reycle this object as long as the flag is set
  public static final int   ATTR_PINDOWN       = 0x0002;

  // this is a identity-managed object
  public static final int   ATTR_INTERN        = 0x0004;

  // The constructor for the object has returned.  Final fields can no longer break POR
  // This attribute is set in gov.nasa.jpf.jvm.bytecode.RETURN.execute().
  // If ThreadInfo.usePorSyncDetection() is false, then this attribute is never set.
  public static final int   ATTR_CONSTRUCTED   = 0x0010;



  //--- the upper two bytes are for transient (heap internal) use only, and are not stored

  // BEWARE if you add or change values, make sure these are not used in derived classes !
  // <2do> this is efficient but fragile
  public static final int   ATTR_FIELDS_CHANGED     = 0x10000;
  public static final int   ATTR_MONITOR_CHANGED    = 0x20000;
  public static final int   ATTR_REFTID_CHANGED     = 0x40000;
  public static final int   ATTR_ATTRIBUTE_CHANGED  = 0x80000; // refers only to sticky bits

  //--- useful flag sets & masks

  static final int   ATTR_STORE_MASK = 0x0000ffff;

  static final int   ATTR_ANY_CHANGED = (ATTR_FIELDS_CHANGED | ATTR_MONITOR_CHANGED | ATTR_REFTID_CHANGED | ATTR_ATTRIBUTE_CHANGED);

  // transient flag set if object is reachable from root object, i.e. can't be recycled
  public static final int   ATTR_IS_MARKED       = 0x80000000;
  
  // this bit is set/unset by the heap in order to identify live objects that have
  // already been unmarked. This is to avoid additional passes over the whole heap in
  // order to clean up dangling references etc.
  // NOTE - this bit should never be state stored - restored ElementInfo should never have it set
  public static final int   ATTR_LIVE_BIT    = 0x40000000;

  public static final int   ATTR_MARKED_OR_LIVE_BIT = (ATTR_IS_MARKED | ATTR_LIVE_BIT);


  //--- instance fields

  protected Fields          fields;
  protected Monitor         monitor;
  
  // the set of referencing thread ids. Note that we need an implementation that
  // is not depending on order of order of reference, or we effectively shoot
  // heap symmetry
  protected FixedBitSet     refTid;

  protected int             index;

  // these are our state-stored object attributes
  // WATCH OUT! only include info that otherwise reflects a state change, so
  // that we don't introduce new changes. Its value is used to hash the state!
  // <2do> what a pity - 32 stored bits for (currently) only 2 bits of
  // information,but we might use this as a hash for more complex reference
  // info in the future.
  // We distinguish between propagates and private object attributes, the first
  // one stored in the lower 2 bytes
  protected int             attributes;


  //--- the following fields are transient or search global, i.e. their values
  // are not state-stored, but might be set during state restoration

  // FieldLockInfos are never state-stored/backtracked! They are set in the order of
  // field access during the search, so that we can detect potential
  // inconsistencies and re-run accordingly
  protected FieldLockInfo[] fLockInfo;

  // cache for unchanged ElementInfos, so that we don't have to re-create cachedMemento
  // objects all the time
  protected Memento<ElementInfo> cachedMemento;

  // cache for a serialized representation of the object, which can be used
  // by state-matching. Value interpretation depends on the configured Serializer
  protected int sid;


  /**
   * our default cachedMemento type
   */
  static public abstract class EIMemento<EI extends ElementInfo> extends SoftReference<EI> implements Memento<ElementInfo> {
    int ref;
    Fields fields;
    Monitor monitor;
    FixedBitSet refTid;
    int attributes;


    public EIMemento (EI ei){
      super(ei);

      ei.markUnchanged(); // we don't want any of the change flags

      this.ref = ei.index;
      this.attributes = ei.attributes;
      this.fields = ei.fields;
      this.monitor = ei.monitor;
      this.refTid = ei.refTid;

      ei.markUnchanged();
    }

    public ElementInfo restore (ElementInfo ei){
      ei.index = ref;
      ei.attributes = attributes;
      ei.fields = fields;
      ei.monitor = monitor;
      ei.refTid = refTid;

      ei.sid = 0;
      ei.updateLockingInfo();
      ei.markUnchanged();

      return ei;
    }

    /** for debugging purposes
    public boolean equals(Object o){
      if (o instanceof EIMemento){
        EIMemento other = (EIMemento)o;
        if (ref != other.ref) return false;
        if (fields != other.fields) return false;
        if (monitor != other.monitor) return false;
        if (refTid != other.refTid) return false;
        if (attributes != other.attributes) return false;
        return true;
      }
      return false;
    }
    public String toString() {
     return "EIMemento {ref="+ref+",attributes="+Integer.toHexString(attributes)+
             ",fields="+fields+",monitor="+monitor+",refTid="+refTid+"}";
    }
    **/
  }



  public ElementInfo(Fields f, Monitor m, int tid) {
    fields = f;
    monitor = m;

    // Ok, this is a hard limit, but for the time being a SUT with more
    // than 64 threads is blowing us out of the water state-space-wise anyways
    refTid = new BitSet64(tid);

    // attributes are set in the concrete type ctors
  }

  protected ElementInfo( Fields f, Monitor m, int ref, int a) {
    fields = f;
    monitor = m;
    index = ref;
    attributes = a;
  }

  protected ElementInfo() {
  }

  public abstract Memento<ElementInfo> getMemento();

  public boolean hasChanged() {
    return (attributes & ATTR_ANY_CHANGED) != 0;
  }

  public boolean hasMonitorChanged() {
    return (attributes & ATTR_MONITOR_CHANGED) != 0;
  }

  public boolean haveFieldsChanged() {
    return (attributes & ATTR_FIELDS_CHANGED) != 0;
  }

  public boolean hasRefTidChanged() {
    return (attributes & ATTR_REFTID_CHANGED) != 0;
  }

  public String toString() {
    return (getClassInfo().getName() + '@' + index);
  }

  public FieldLockInfo getFieldLockInfo (FieldInfo fi) {
    if (fLockInfo == null) {
      fLockInfo = new FieldLockInfo[getNumberOfFields()];
    }
    return fLockInfo[fi.getFieldIndex()];
  }

  public void setFieldLockInfo (FieldInfo fi, FieldLockInfo flInfo) {
    fLockInfo[fi.getFieldIndex()] = flInfo;
  }


  /**
   * update all non-fields references used by this object. This is only called
   * at the end of the gc, and recycled objects should be either null or
   * not marked
   */
  void cleanUp (Heap heap) {
    if (fLockInfo != null) {
      for (int i=0; i<fLockInfo.length; i++) {
        FieldLockInfo fli = fLockInfo[i];
        if (fli != null) {
          fLockInfo[i] = fli.cleanUp(heap);
        }
      }
    }
  }

  //--- sids are only supposed to be used by the Serializer
  public void setSid(int id){
    sid = id;
  }

  public int getSid() {
    return sid;
  }

  //--- cached mementos are only supposed to be used/set by the Restorer

  public Memento<ElementInfo> getCachedMemento(){
    return cachedMemento;
  }

  public void setCachedMemento (Memento<ElementInfo> memento){
    cachedMemento = memento;
  }

  /**
   * do we have a reference field with value objRef?
   */
  boolean hasRefField(int objRef) {
    return fields.hasRefField(objRef);
  }

  /**
   * BEWARE - never change the returned object without knowing about the
   * ElementInfo change status, this field is state managed!
   */
  public FixedBitSet getRefTid() {
    return refTid;
  }

  public boolean hasMultipleReferencingThreads() {
    return refTid.cardinality() > 1;
  }

  public void updateRefTidWith (int tid){
    FixedBitSet b = refTid;

    if (!b.get(tid)){
      if ((attributes & ATTR_REFTID_CHANGED) == 0){
        b = b.clone();
        refTid = b;
        attributes |= ATTR_REFTID_CHANGED;
      }
      b.set(tid);
    }
  }

  public void updateRefTidWithout (int tid){
    FixedBitSet b = refTid;

    if (b.get(tid)){
      if ((attributes & ATTR_REFTID_CHANGED) == 0){
        b = b.clone();
        refTid = b;
        attributes |= ATTR_REFTID_CHANGED;
      }
      b.clear(tid);
    }
  }


  /**
   * the recursive phase2 marker entry, which propagates the attributes set by a
   * previous phase1. This one is called on all 'root'-marked objects after
   * phase1 is completed. ElementInfo is not an ideal place for this method, as
   * it has to access some innards of both ClassInfo (FieldInfo container) and
   * Fields. But on the other hand, we want to keep the whole heap traversal
   * business as much centralized in ElementInfo and DynamicArea as possible
   */
  void markRecursive(Heap heap) {
    int i, n;

    if (isArray()) {
      if (fields.isReferenceArray()) {
        n = fields.arrayLength();
        for (i = 0; i < n; i++) {
          int objref = fields.getIntValue(i);
          if (objref != MJIEnv.NULL){
            heap.queueMark( objref);
          }
        }
      }

    } else { // not an array
      ClassInfo ci = getClassInfo();
      boolean isWeakRef = ci.isWeakReference();

      do {
        n = ci.getNumberOfDeclaredInstanceFields();
        boolean isRef = isWeakRef && ci.isReferenceClassInfo(); // is this the java.lang.ref.Reference part?

        for (i = 0; i < n; i++) {
          FieldInfo fi = ci.getDeclaredInstanceField(i);
          if (fi.isReference()) {
            if ((i == 0) && isRef) {
              // we need to reset the ref field once the referenced object goes away
              // NOTE: only the *first* WeakReference field is a weak ref
              // (this is why we have our own implementation)
              heap.registerWeakReference(this);
            } else {
              int objref = fields.getReferenceValue(fi.getStorageOffset());
              if (objref != MJIEnv.NULL){
                heap.queueMark( objref);
              }
            }
          }
        }
        ci = ci.getSuperClass();
      } while (ci != null);
    }
  }


  int getAttributes () {
    return attributes;
  }

  int getStoredAttributes() {
    return attributes & ATTR_STORE_MASK;
  }

  /**
   * note this was a prospective answer (reachability), but now it is
   * actual - only elements that already have been referenced from different threads
   * might return true. If the refTid set is cleaned from terminated threads, it
   * more precisely means this element has been referenced from different threads
   * that are still alive
   */
  public boolean isShared() {
    //return ((attributes & ATTR_TSHARED) != 0);
    return refTid.cardinality() > 1;
  }

  public boolean isImmutable() {
    return ((attributes & ATTR_IMMUTABLE) != 0);
  }

  public boolean checkUpdatedSchedulingRelevance (ThreadInfo ti) {
    // only mutable, shared objects are relevant
    //return ((attributes & (ATTR_TSHARED | ATTR_IMMUTABLE)) == ATTR_TSHARED);

    int tid = ti.getIndex();
    updateRefTidWith(tid);
    
    if ((attributes & ATTR_IMMUTABLE) != 0){
      return false;
    }

    int nThreadRefs = refTid.cardinality();
    if (nThreadRefs > 1){

      // check if we have to cleanup the refTid set, or if some other accessors are not runnable
      ThreadList tl = JVM.getVM().getThreadList();
      for (int i=refTid.nextSetBit(0); i>=0; i = refTid.nextSetBit(i+1)){
        ThreadInfo tiRef = tl.get(i);
        if (tiRef == null || tiRef.isTerminated()) {
          updateRefTidWithout(i);
          nThreadRefs--;
        } else if (!tiRef.isRunnable()){
          nThreadRefs--;
        }
      }
      return (nThreadRefs > 1);

    } else { // only one referencing thread
      return false;
    }
  }

  /**
   * this is called before the system attempts to reclaim the object. If
   * we return 'false', the object will *not* be removed
   */
  protected boolean recycle () {

    // this is required to avoid loosing field lock assumptions
    // when the system sequentialized threads with conflicting assumptions,
    // but the offending object goes out of scope before the system backtracks
    if (hasVolatileFieldLockInfos()) {
      return false;
    }

    setIndex(-1);

    return true;
  }

  boolean hasVolatileFieldLockInfos() {
    if (fLockInfo != null) {
      for (int i=0; i<fLockInfo.length; i++) {
        FieldLockInfo fli = fLockInfo[i];
        if (fli != null) {
          if (fli.needsPindown(this)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  protected void resurrect (Area<?> area, int index) {
    setIndex(index);
  }

  
  public void hash(HashData hd) {
    fields.hash(hd);
    monitor.hash(hd);
    hd.add(refTid);
    
    // attributes ?
  }

  @Override
  public int hashCode() {
    HashData hd = new HashData();

    hash(hd);

    return hd.getValue();
  }

  @Override
  public boolean equals(Object o) {
    if (o != null && o instanceof ElementInfo) {
      ElementInfo other = (ElementInfo) o;

      if (attributes != other.attributes){ // ??
        return false;
      }
      if (!fields.equals(other.fields)) {
        return false;
      }
      if (!monitor.equals(other.monitor)){
        return false;
      }
      if (refTid != other.refTid){
        return false;
      }

      return true;

    } else {
      return false;
    }
  }

  public ClassInfo getClassInfo() {
    return fields.getClassInfo();
  }

  abstract protected FieldInfo getDeclaredFieldInfo(String clsBase, String fname);

  abstract protected ElementInfo getElementInfo(ClassInfo ci);

  abstract protected FieldInfo getFieldInfo(String fname);


  //--- attribute accessors

  public boolean hasObjectAttr(Class<?> attrType) {
    return fields.hasObjectAttr(attrType);
  }

  public <T> T getObjectAttr (Class<T> attrType) {
    return fields.getObjectAttr(attrType);
  }

  // supposed to return all
  public Object getObjectAttr () {
    return fields.getObjectAttr();
  }

  /**
   * this sets an attribute for the whole object
   */
  public void setObjectAttr (Object attr){
    cloneFields().setObjectAttr(attr);
  }

  public void setObjectAttrNoClone (Object attr){
    fields.setObjectAttr(attr);
  }

  public boolean hasFieldAttrs() {
    return fields.hasFieldAttrs();
  }

  public boolean hasFieldAttrs (Class<?> attrType){
    return fields.hasFieldAttrs(attrType);
  }

  /**
   * use this version if only the attr has changed, since we won't be able
   * to backtrack otherwise
   */
  public void setFieldAttr (FieldInfo fi, Object attr){
    ElementInfo ei = getElementInfo(fi.getClassInfo()); // might be static
    ei.cloneFields().setFieldAttr(fi.getFieldIndex(), attr);
  }

  /**
   * use this version if the concrete value is changed too, which means
   * the fields will be cloned anyways (no use to do this twice)
   */
  public void setFieldAttrNoClone (FieldInfo fi, Object attr){
    ElementInfo ei = getElementInfo(fi.getClassInfo()); // might be static
    ei.fields.setFieldAttr(fi.getFieldIndex(), attr);
  }

  public <T> T getFieldAttr (Class<T> attrType, FieldInfo fi){
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getFieldAttr(attrType,fi.getFieldIndex());
  }

  // supposed to return all
  public Object getFieldAttr (FieldInfo fi){
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getFieldAttr(fi.getFieldIndex());
  }


  /**
   * this sets an attribute for the field with index 'idx'
   */
  public void setElementAttr (int idx, Object attr){
    cloneFields().setFieldAttr(idx, attr);
  }

  public void setElementAttrNoClone (int idx, Object attr){
    fields.setFieldAttr(idx, attr);
  }

  public <T> T getElementAttr (Class<T> attrType, int idx){
    return fields.getFieldAttr(attrType, idx);
  }

  // this is supposed to return all attrs of the element
  public Object getElementAttr (int idx){
    return fields.getFieldAttr(idx);
  }

  public abstract void setIntField(FieldInfo fi, int value);

  public void setDeclaredIntField(String fname, String clsBase, int value) {
    setIntField(getDeclaredFieldInfo(clsBase, fname), value);
  }

  public void setBooleanField (String fname, boolean value) {
    setIntField( getFieldInfo(fname), value ? 1 : 0);
  }

  public void setIntField(String fname, int value) {
    setIntField(getFieldInfo(fname), value);
  }

  public void setDoubleField (String fname, double value) {
    setLongField(fname, Types.doubleToLong(value));
  }

  // <2do> we need to tell 'null' values apart from 'no such field'
  public Object getFieldValueObject (String fname) {
    Object ret = null;
    FieldInfo fi = getFieldInfo(fname);

    if (fi != null){
      ret = fi.getValueObject(fields);

    } else {
      // check if there is an enclosing class object
      ElementInfo eiEnclosing = getEnclosingElementInfo();
      if (eiEnclosing != null){
        ret = eiEnclosing.getFieldValueObject(fname);

      } else {
        // we should check static fields in enclosing scopes, but there is no
        // other way than to guess this from the name, and the outer
        // classes might not even be initialized yet
      }
    }

    return ret;
  }

  public ElementInfo getEnclosingElementInfo() {
    return null; // only for DynamicElementInfos
  }

/**
  public void setReferenceField(FieldInfo fi, int newValue) {
    ElementInfo ei = getElementInfo(fi.getClassInfo()); // might not be 'this'
                                                        // in case of a static
    Fields f = ei.cloneFields();
    int offset = fi.getStorageOffset();

    if (fi.isReference()) {
      int oldValue = f.getReferenceValue(offset);
      f.setReferenceValue(this, offset, newValue);

      Heap heap = JVM.getVM().getHeap();
      if (isShared()){

        if (newValue != MJIEnv.NULL){
          ElementInfo nei = heap.get(newValue);
          nei.addSharedReference(ei);
        }
        if (oldValue != MJIEnv.NULL){
          ElementInfo oei = heap.get(oldValue);
          oei.removeSharedReference(ei);
        }

        heap.updateReachability( true, oldValue, newValue);
      }

    } else {
      throw new JPFException("not a reference field: " + fi.getName());
    }
  }
**/

  public void setReferenceField(FieldInfo fi, int newValue) {
    ElementInfo ei = getElementInfo(fi.getClassInfo()); // might not be 'this'
                                                        // in case of a static
    Fields f = ei.cloneFields();
    int offset = fi.getStorageOffset();

    if (fi.isReference()) {
      f.setReferenceValue(this, offset, newValue);
    } else {
      throw new JPFException("not a reference field: " + fi.getName());
    }
  }

  protected void addSharedReference(ElementInfo refEi){
    // nothing here
  }
  protected void removeSharedReference(ElementInfo refEi){
    // nothing here
  }

  public void setDeclaredReferenceField(String fname, String clsBase, int value) {
    setReferenceField(getDeclaredFieldInfo(clsBase, fname), value);
  }

  public void setReferenceField(String fname, int value) {
    setReferenceField(getFieldInfo(fname), value);
  }

  public int getDeclaredReferenceField(String fname, String clsBase) {
    FieldInfo fi = getDeclaredFieldInfo(clsBase, fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());

    if (!fi.isReference()) {
      throw new JPFException("not a reference field: " + fi.getName());
    }
    return ei.fields.getIntValue(fi.getStorageOffset());
  }

  public int getReferenceField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());

    if (!fi.isReference()) {
      throw new JPFException("not a reference field: " + fi.getName());
    }
    return ei.fields.getIntValue(fi.getStorageOffset());
  }


  public int getDeclaredIntField(String fname, String clsBase) {
    // be aware of that static fields are not flattened (they are unique), i.e.
    // the FieldInfo might actually refer to another ClassInfo/StaticElementInfo
    FieldInfo fi = getDeclaredFieldInfo(clsBase, fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getIntValue(fi.getStorageOffset());
  }

  public int getIntField(String fname) {
    // be aware of that static fields are not flattened (they are unique), i.e.
    // the FieldInfo might actually refer to another ClassInfo/StaticElementInfo
    FieldInfo fi = getFieldInfo(fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getIntValue(fi.getStorageOffset());
  }

  public void setDeclaredLongField(String fname, String clsBase, long value) {
    FieldInfo fi = getDeclaredFieldInfo(clsBase, fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    ei.cloneFields().setLongValue(this,fi.getStorageOffset(), value);
  }

  public long getDeclaredLongField(String fname, String clsBase) {
    FieldInfo fi = getDeclaredFieldInfo(clsBase, fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getLongValue(fi.getStorageOffset());
  }

  public long getLongField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getLongValue(fi.getStorageOffset());
  }

  public boolean getDeclaredBooleanField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getBooleanValue(fi.getStorageOffset());
  }

  public boolean getBooleanField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getBooleanValue(fi.getStorageOffset());
  }

  public byte getDeclaredByteField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getByteValue(fi.getStorageOffset());
  }

  public byte getByteField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getByteValue(fi.getStorageOffset());
  }

  public char getDeclaredCharField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getCharValue(fi.getStorageOffset());
  }

  public char getCharField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getCharValue(fi.getStorageOffset());
  }

  public double getDeclaredDoubleField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getDoubleValue(fi.getStorageOffset());
  }

  public double getDoubleField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getDoubleValue(fi.getStorageOffset());
  }

  public float getDeclaredFloatField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getFloatValue(fi.getStorageOffset());
  }

  public float getFloatField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getFloatValue(fi.getStorageOffset());
  }

  public short getDeclaredShortField(String fname, String refType) {
    FieldInfo fi = getDeclaredFieldInfo(refType, fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getShortValue(fi.getStorageOffset());
  }

  public short getShortField(String fname) {
    FieldInfo fi = getFieldInfo(fname);
    ElementInfo ei = getElementInfo(fi.getClassInfo());
    return ei.fields.getShortValue(fi.getStorageOffset());
  }

  /**
   * note this only holds for instance fields, and hence the method has to
   * be overridden in StaticElementInfo
   */
  private void checkFieldInfo(FieldInfo fi) {
    if (!getClassInfo().isInstanceOf(fi.getClassInfo())) {
      throw new JPFException("wrong FieldInfo : " + fi.getName()
          + " , no such field in " + getClassInfo().getName());
    }
  }

  // those are the cached field value accessors. The caller is responsible
  // for assuring type compatibility
  public int getIntField(FieldInfo fi) {
    checkFieldInfo(fi);
    return fields.getIntValue(fi.getStorageOffset());
  }

  public int getReferenceField (FieldInfo fi) {
    // the reference is just an 'int' for us
    return getIntField(fi);
  }

  abstract ElementInfo getReferencedElementInfo (FieldInfo fi);

  public long getLongField(FieldInfo fi) {
    checkFieldInfo(fi);
    return fields.getLongValue(fi.getStorageOffset());
  }

  public void setLongField(FieldInfo fi, long value) {
    checkFieldInfo(fi);
    cloneFields().setLongValue(this,fi.getStorageOffset(), value);
  }

  public void setLongField(String fname, long value) {
    setLongField( getFieldInfo(fname), value);
  }


  public double getDoubleField (FieldInfo fi){
    checkFieldInfo(fi);
    return fields.getDoubleValue(fi.getStorageOffset());

  }

  public float getFloatField (FieldInfo fi){
    checkFieldInfo(fi);
    return fields.getFloatValue(fi.getStorageOffset());

  }

  public boolean getBooleanField (FieldInfo fi){
    checkFieldInfo(fi);
    return fields.getBooleanValue(fi.getStorageOffset());
  }

  protected void checkArray(int index) {
    if (!isArray()) { // <2do> should check for !long array
      throw new JPFException(
          "cannot access non array objects by index");
    }
    if ((index < 0) || (index >= fields.size())) {
      throw new JPFException("illegal array offset: " + index);
    }
  }

  protected void checkLongArray(int index) {
    if (!isArray()) { // <2do> should check for !int array
      throw new JPFException(
          "cannot access non array objects by index");
    }
    if ((index < 0) || (index >= (fields.size() - 1))) {
      throw new JPFException("illegal long array offset: " + index);
    }
  }

  public boolean isReferenceArray() {
    return getClassInfo().isReferenceArray();
  }

  // those are not really fields, so treat them differently!
  public void setElement(int fidx, int value) {
    checkArray(fidx);
    if (isReferenceArray()) {
      cloneFields().setReferenceValue(this, fidx, value);
    } else {
      cloneFields().setIntValue(this,fidx, value);
    }
  }

  public void setLongElement(int index, long value) {
    checkArray(index);
    cloneFields().setLongValue(this,index*2, value);
  }

  public int getElement(int index) {
    checkArray(index);
    return fields.getIntValue(index);
  }

  public long getLongElement(int index) {
    checkArray(index);
    return fields.getLongValue(index*2);
  }

  public void setIndex(int newIndex) {
    index = newIndex;
  }

  public int getIndex() {
    return index;
  }

  public int getThisReference() {
    return index;
  }

  public int getLockCount() {
    return monitor.getLockCount();
  }

  public ThreadInfo getLockingThread() {
    return monitor.getLockingThread();
  }

  public boolean isLocked() {
    return (monitor.getLockCount() > 0);
  }

  public boolean isArray() {
    return fields.isArray();
  }

  public String getArrayType() {
    if (!fields.isArray()) {
      throw new JPFException("object is not an array");
    }

    return Types.getArrayElementType(fields.getType());
  }

  public Object getBacktrackData() {
    return null;
  }

  public char getCharArrayElement(int index) {
    return (char) getElement(index);
  }

  public int getIntArrayElement(int findex) {
    return getElement(findex);
  }

  public long getLongArrayElement(int findex) {
    return getLongElement(findex);
  }

  public boolean[] asBooleanArray() {
    return fields.asBooleanArray();
  }

  public byte[] asByteArray() {
    return fields.asByteArray();
  }

  public short[] asShortArray() {
    return fields.asShortArray();
  }

  public char[] asCharArray() {
    return fields.asCharArray();
  }

  public int[] asIntArray() {
    return fields.asIntArray();
  }

  public long[] asLongArray() {
    return fields.asLongArray();
  }

  public float[] asFloatArray() {
    return fields.asFloatArray();
  }

  public double[] asDoubleArray() {
    return fields.asDoubleArray();
  }

  public boolean isNull() {
    return (index == -1);
  }

  public ElementInfo getDeclaredObjectField(String fname, String referenceType) {
    return JVM.getVM().getHeap().get(getDeclaredIntField(fname, referenceType));
  }

  public ElementInfo getObjectField(String fname) {
    return JVM.getVM().getHeap().get(getIntField(fname));
  }


  /**
   * answer an estimate of the heap size in bytes (this is of course VM
   * dependent, but we can give an upper bound for the fields/elements, and that
   * should be good in terms of application specific properties)
   */
  public int getHeapSize() {
    return fields.getHeapSize();
  }

  public String getStringField(String fname) {
    int ref = getIntField(fname);

    if (ref != -1) {
      ElementInfo ei = JVM.getVM().getHeap().get(ref);
      if (ei == null) {
        System.out.println("OUTCH: " + ref + ", this: " + index);
        throw new NullPointerException(); // gets rid of null warning -pcd
      }
      return ei.asString();
    } else {
      return "null";
    }
  }

  public String getType() {
    return fields.getType();
  }

  /**
   * get a cloned list of the waiters for this object
   */
  public ThreadInfo[] getWaitingThreads() {
    return monitor.getWaitingThreads();
  }

  public boolean hasWaitingThreads() {
    return monitor.hasWaitingThreads();
  }

  public ThreadInfo[] getBlockedThreads() {
    return monitor.getBlockedThreads();
  }

  public ThreadInfo[] getBlockedOrWaitingThreads() {
    return monitor.getBlockedOrWaitingThreads();
  }
    
  public int arrayLength() {
    return fields.arrayLength();
  }

  public boolean isStringObject() {
    return ClassInfo.isStringClassInfo(fields.getClassInfo());
  }

  public String asString() {
    throw new JPFException("not a String object: " + this);
  }

  /**
   * just a helper to avoid creating objects just for the sake of comparing
   */
  public boolean equalsString (String s) {
    throw new JPFException("not a String object: " + this);
  }

  void updateLockingInfo() {
    int i;

    ThreadInfo ti = monitor.getLockingThread();
    if (ti != null) {
      // here we can update ThreadInfo lock object info (so that we don't
      // have to store it separately)

      // NOTE - the threads need to be restored *before* the Areas, or this is
      // going to choke

      // note that we add only once, i.e. rely on the monitor lockCount to
      // determine when to remove an object from our lock set
      //assert area.ks.tl.get(ti.index) == ti;  // covered by verifyLockInfo
      ti.updateLockedObject(this);
    }

    if (monitor.hasLockedThreads()) {
      ThreadInfo[] lockedThreads = monitor.getLockedThreads();
      for (i=0; i<lockedThreads.length; i++) {
        ti = lockedThreads[i];
        //assert area.ks.tl.get(ti.index) == ti;  // covered by verifyLockInfo
        
        // note that the thread might still be runnable if we have several threads
        // competing for the same lock
        if (!ti.isRunnable()){
          ti.setLockRef(index);
        }
      }
    }
  }

  public boolean canLock(ThreadInfo th) {
    return monitor.canLock(th);
  }

  public void checkArrayBounds(int index)
      throws ArrayIndexOutOfBoundsExecutiveException {
    if (outOfBounds(index)) {
      throw new ArrayIndexOutOfBoundsExecutiveException(
          ThreadInfo.getCurrentThread().createAndThrowException(
              "java.lang.ArrayIndexOutOfBoundsException", Integer.toString(index)));
    }
  }

  public void checkLongArrayBounds(int index)
      throws ArrayIndexOutOfBoundsExecutiveException {
    checkArrayBounds(index);
    checkArrayBounds(index + 1);
  }

  public Object clone() {
    try {
      return (ElementInfo) super.clone();
      
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      throw new InternalError("should not happen");
    }
  }


  public boolean instanceOf(String type) {
    return Types.instanceOf(fields.getType(), type);
  }

  abstract public int getNumberOfFields();

  abstract public FieldInfo getFieldInfo(int i);

  public void log() { // <2do> replace this
    if (haveFieldsChanged()) {
      Debug.println(Debug.MESSAGE, "(fields have changed)");
    }

    int n = getNumberOfFields();
    for (int i = 0; i < n; i++) {
      FieldInfo fi = getFieldInfo(i);
      Debug.println(Debug.MESSAGE, fi.getName() + ": "
          + fi.valueToString(fields));
    }

    if (hasMonitorChanged()) {
      Debug.println(Debug.MESSAGE, "(monitor has changed)");
    }

    //monitor.log();
  }

  /**
   * threads that will grab our lock on their next execution have to be
   * registered, so that they can be blocked in case somebody else gets
   * scheduled
   */
  public void registerLockContender (ThreadInfo ti) {

    assert ti.lockRef == -1 || ti.lockRef == index :
      "thread " + ti + " trying to register for : " + this +
      " ,but already blocked on: " + ti.getElementInfo(ti.lockRef);

    // note that using the lockedThreads list is a bit counter-intuitive
    // since the thread is still in RUNNING or UNBLOCKED state, but it will
    // remove itself from there once it resumes: lock() calls setMonitorWithoutLocked(ti)
    setMonitorWithLocked(ti);

    // added to satisfy invariant implied by updateLockingInfo() -peterd
    //ti.setLockRef(index);
  }

  /**
   * somebody made up his mind and decided not to enter a synchronized section
   * of code it had registered before (e.g. INVOKECLINIT)
   */
  public void unregisterLockContender (ThreadInfo ti) {
    setMonitorWithoutLocked(ti);

    // moved from INVOKECLINIT -peterd
    //ti.resetLockRef();
  }

  void blockLockContenders () {
    // check if there are any other threads that have to change status because they
    // require to lock us in their next exec
    ThreadInfo[] lockedThreads = monitor.getLockedThreads();
    for (int i=0; i<lockedThreads.length; i++) {
      ThreadInfo ti = lockedThreads[i];
      if (ti.isRunnable()) {
        ti.setBlockedState(index);
      }
    }
  }

  /**
   * from a MONITOR_ENTER or sync INVOKExx if we cannot acquire the lock
   * note: this is not called from a NOTIFIED_UNBLOCKED state, so we don't have to restore NOTIFIED
   */
  public void block (ThreadInfo ti) {
    assert (monitor.getLockingThread() != null) && (monitor.getLockingThread() != ti) :
          "attempt to block " + ti.getName() + " on unlocked or own locked object: " + this;

    setMonitorWithLocked(ti);
    
    ti.setBlockedState(index);    
  }

  /**
   * from a MONITOR_ENTER or sync INVOKExx if we can acquire the lock
   */
  public void lock (ThreadInfo ti) {
    // if we do unlock consistency checks with JPFExceptions, we should do the same here
    if ((monitor.getLockingThread() != null) &&  (monitor.getLockingThread() != ti)){
      throw new JPFException("thread " + ti.getName() + " tries to lock object: "
              + this + " which is locked by: " + monitor.getLockingThread().getName());
    }

    // the thread might be still in the lockedThreads list if this is the
    // first step of a transition
    setMonitorWithoutLocked(ti);
    monitor.setLockingThread(ti);
    monitor.incLockCount();

    // before we execute anything else, mark this thread as not being blocked anymore
    ti.resetLockRef();

    ThreadInfo.State state = ti.getState();
    if (state == ThreadInfo.State.UNBLOCKED) {
      ti.setState(ThreadInfo.State.RUNNING);
    }

    // don't re-add if we are recursive - the lock count is avaliable in the monitor
    if (monitor.getLockCount() == 1) {
      ti.addLockedObject(this);
    }

    // this might set other threads blocked - make sure we lock first or the sequence
    // of notifications is a bit screwed (i.e. the lock would appear *after* the block)
    blockLockContenders();
  }

  /**
   * from a MONITOR_EXIT or sync method RETURN
   * release a possibly recursive lock if lockCount goes to zero
   */
  public void unlock (ThreadInfo ti) {

    /* If there is a compiler bug, we need to flag it.  Most compilers should 
     * generate balanced monitorenter and monitorexit instructions for all code 
     * paths.  The JVM is being used for more non-Java languages.  Some of these 
     * compilers might be experimental and might generate unbalanced 
     * instructions.  In a more likely case, dynamically generated bytecode is
     * more likely to make a mistake and miss a code path.
     */
    if ((monitor.getLockCount() <= 0) || (monitor.getLockingThread() != ti)){
      throw new JPFException("thread " + ti.getName() + " tries to release non-owned lock for object: " + this);
    }

    if (monitor.getLockCount() == 1) {
      ti.removeLockedObject(this);

      ThreadInfo[] lockedThreads = monitor.getLockedThreads();
      for (int i = 0; i < lockedThreads.length; i++) {
        ThreadInfo lti = lockedThreads[i];
        switch (lti.getState()) {

        case BLOCKED:
        case NOTIFIED:
        case TIMEDOUT:
        case INTERRUPTED:
          // Ok, this thread becomes runnable again
          lti.resetLockRef();
          lti.setState(ThreadInfo.State.UNBLOCKED);
          break;

        case WAITING:
        case TIMEOUT_WAITING:
          // nothing to do yet, thread has to timeout, get notified, or interrupted
          break;

        default:
          assert false : "Monitor.lockedThreads<->ThreadData.status inconsistency! " + lockedThreads[i].getStateName();
          // why is it in the list - when someone unlocks, all others should have been blocked
        }
      }

      // leave the contenders - we need to know whom to block on subsequent lock
      setMonitor();

      monitor.decLockCount();
      monitor.setLockingThread(null);

    } else { // recursive unlock
      setMonitor();
      monitor.decLockCount();
    }
  }

  /**
   * notifies one of the waiters. Note this is a potentially non-deterministic action
   * if we have several waiters, since we have to try all possible choices.
   * Note that even if we notify a thread here, it still remains in the lockedThreads
   * list until the lock is released (notified threads cannot run right away)
   */
  public void notifies(SystemState ss, ThreadInfo ti){
    notifies(ss, ti, true);
  }
  
  
  private void notifies0 (ThreadInfo tiWaiter){
    if (tiWaiter.isWaiting()){
      if (tiWaiter.getLockCount() > 0) {
        // waiter did hold the lock, but gave it up in the wait,  so it can't run yet
        tiWaiter.setNotifiedState();

      } else {
        // waiter didn't hold the lock, set it running
        setMonitorWithoutLocked(tiWaiter);
        tiWaiter.resetLockRef();
        tiWaiter.setRunning();
      }
    }
  }

  public void notifies (SystemState ss, ThreadInfo ti, boolean hasToHoldLock){
    if (hasToHoldLock){
      assert monitor.getLockingThread() != null : "notify on unlocked object: " + this;
    }

    ThreadInfo[] locked = monitor.getLockedThreads();
    int i, nWaiters=0, iWaiter=0;

    for (i=0; i<locked.length; i++) {
      if (locked[i].isWaiting()) {
        nWaiters++;
        iWaiter = i;
      }
    }

    if (nWaiters == 0) {
      // no waiters, nothing to do
    } else if (nWaiters == 1) {
      // only one waiter, no choice point
      notifies0(locked[iWaiter]);

    } else {
      // Ok, this is the non-deterministic case
      ThreadChoiceGenerator cg = ss.getCurrentSchedulingPoint();

      assert (cg != null) : "no ThreadChoiceGenerator in notify";

      notifies0(cg.getNextChoice());
    }

    ti.getVM().notifyObjectNotifies(ti, this);

  }

  /**
   * notify all waiters. This is a deterministic action
   * all waiters remain in the locked list, since they still have to be unblocked,
   * which happens in the unlock (monitor_exit or sync return) following the notifyAll()
   */
  public void notifiesAll() {
    assert monitor.getLockingThread() != null : "notifyAll on unlocked object: " + this;

    ThreadInfo[] locked = monitor.getLockedThreads();
    for (int i=0; i<locked.length; i++) {
      // !!!! if there is more than one BLOCKED thread (sync call or monitor enter), only one can be
      // unblocked
      notifies0(locked[i]);
    }

    JVM.getVM().notifyObjectNotifiesAll(ThreadInfo.currentThread, this);
  }


  /**
   * wait to be notified. thread has to hold the lock, but gives it up in the wait.
   * Make sure lockCount can be restored properly upon notification
   */
  public void wait(ThreadInfo ti, long timeout){
    wait(ti,timeout,true);
  }

  // this is used from a context where we don't require a lock, e.g. Unsafe.park()/unpark()
  public void wait (ThreadInfo ti, long timeout, boolean hasToHoldLock){
    boolean holdsLock = monitor.getLockingThread() == ti;

    if (hasToHoldLock){
      assert holdsLock : "wait on unlocked object: " + this;
    }

    setMonitorWithLocked(ti);
    ti.setLockRef(index);
    
    if (timeout == 0) {
      ti.setState(ThreadInfo.State.WAITING);
    } else {
      ti.setState(ThreadInfo.State.TIMEOUT_WAITING);
    }

    if (holdsLock) {
      ti.setLockCount(monitor.getLockCount());

      monitor.setLockingThread(null);
      monitor.setLockCount(0);

      ti.removeLockedObject(this);

      // unblock all runnable threads that are blocked on this lock
      ThreadInfo[] lockedThreads = monitor.getLockedThreads();
      for (int i = 0; i < lockedThreads.length; i++) {
        ThreadInfo lti = lockedThreads[i];
        switch (lti.getState()) {
          case NOTIFIED:
          case BLOCKED:
          case INTERRUPTED:
            lti.resetLockRef();
            lti.setState(ThreadInfo.State.UNBLOCKED);
            break;
        }
      }
    }

    // <2do> not sure if this is right if we don't hold the lock
    ti.getVM().notifyObjectWait(ti, this);
  }


  /**
   * re-acquire lock after being notified. This is the notified thread, i.e. the one
   * that will come out of a wait()
   */
  public void lockNotified (ThreadInfo ti) {
    assert ti.isUnblocked() : "resume waiting thread " + ti.getName() + " which is not unblocked";

    setMonitorWithoutLocked(ti);
    monitor.setLockingThread( ti);
    monitor.setLockCount( ti.getLockCount());

    ti.setLockCount(0);
    ti.resetLockRef();
    ti.setState( ThreadInfo.State.RUNNING);

    blockLockContenders();

    // this is important, if we later-on backtrack (reset the
    // ThreadInfo.lockedObjects set, and then restore from the saved heap), the
    // lock set would not include the lock when we continue to execute this thread
    ti.addLockedObject(this); //wv: add locked object back here
  }

  /**
   * this is for waiters that did not own the lock
   */
  public void resumeNonlockedWaiter (ThreadInfo ti){
    setMonitorWithoutLocked(ti);

    ti.setLockCount(0);
    ti.resetLockRef();
    ti.setRunning();
  }


  void dumpMonitor () {
    PrintWriter pw = new PrintWriter(System.out, true);
    pw.print( "monitor ");
    //pw.print( mIndex);
    monitor.printFields(pw);
    pw.flush();
  }

  public boolean outOfBounds(int index) {
    if (!fields.isArray()) {
      throw new JPFException("object is not an array");
    }

    return (index < 0 || index >= fields.size());
  }

  /**
   * object is kept alive regardless of references
   * NOTE - this is not a public method, pinning down an object is now
   * done through the Heap API, which sets this flag here, but might or might not
   * use it. Just setting this flag directly does not guarantee the Heap will
   * treat this object as pinned down
   */
  void pinDown(boolean keepAlive) {
    if (keepAlive) {
      attributes |= (ATTR_PINDOWN | ATTR_ATTRIBUTE_CHANGED);
    } else {
      attributes &= (~ATTR_PINDOWN | ATTR_ATTRIBUTE_CHANGED);
    }
  }

  public boolean isPinnedDown() {
    return (attributes & ATTR_PINDOWN) != 0;
  }

  /**
   * set the identity managed flag. Same as ATTR_PINDOWN - this has to be done
   * from the Heap in order to have an effect
   */
  void intern (){
    attributes |= (ATTR_INTERN | ATTR_ATTRIBUTE_CHANGED);
  }

  public boolean isIntern () {
    return (attributes & ATTR_INTERN) != 0;
  }


  public boolean isConstructed() {
    return (attributes & ATTR_CONSTRUCTED) != 0;
  }

  public void setConstructed() {
    attributes |= (ATTR_CONSTRUCTED | ATTR_ATTRIBUTE_CHANGED);
  }

  public void restoreFields(Fields f) {
    fields = f;
  }

  /**
   * BEWARE - never change the returned object without knowing about the
   * ElementInfo change status, this field is state managed!
   */
  public Fields getFields() {
    return fields;
  }

  public void restore(int index, int attributes, Fields fields, Monitor monitor){
    markUnchanged();
    
    this.index = index;
    this.attributes = attributes;
    this.fields = fields;
    this.monitor = monitor;
  }

  public void restoreMonitor(Monitor m) {
    monitor = m;
  }

  /**
   * BEWARE - never change the returned object without knowing about the
   * ElementInfo change status, this field is state managed!
   */
  public Monitor getMonitor() {
    return monitor;
  }

  public void restoreAttributes(int a) {
    attributes = a;
  }

  public boolean isAlive(boolean liveBitValue) {
    if (liveBitValue){
      return (attributes & ATTR_LIVE_BIT) != 0;
    } else {
      return (attributes & ATTR_LIVE_BIT) == 0;
    }
  }

  public void setAlive(boolean liveBitValue){
    if (liveBitValue){
      attributes |= ATTR_LIVE_BIT;
    } else {
      attributes &= ~ATTR_LIVE_BIT;
    }
  }

  public boolean isMarked() {
    return (attributes & ATTR_IS_MARKED) != 0;
  }

  public void setMarked() {
    attributes |= ATTR_IS_MARKED;
  }

  public boolean isMarkedOrAlive (boolean liveBitValue){
    if (liveBitValue) {
      // any of the bits are set
      return (attributes & ATTR_MARKED_OR_LIVE_BIT) != 0;
    } else {
      // only the mark bit is set
      return (attributes & ATTR_MARKED_OR_LIVE_BIT) == ATTR_IS_MARKED;
    }
  }

  protected abstract void markAreaChanged();

  public void markUnchanged() {
    attributes &= ~ATTR_ANY_CHANGED;
  }

  public void setUnmarked() {
    attributes &= ~ATTR_IS_MARKED;
  }

  /**
   * The various lock methods need access to a Ref object to do their work. The
   * subclass should return an appropriate type. This is a simple factory
   * method.
   *
   * @return the right kind of Ref object for the given ElementInfo
   */
  protected abstract Ref getRef();

  protected Fields cloneFields() {
    if ((attributes & ATTR_FIELDS_CHANGED) == 0) {
      fields = fields.clone();
      attributes |= ATTR_FIELDS_CHANGED;
      markAreaChanged();
    }

    return fields;
  }


  /**
   * this has to be called every time we create a new monitor, so that we know it's
   * not yet stored (mIndex = -1), and memory has changed (area)
   */
  void resetMonitorIndex () {
    attributes |= ATTR_MONITOR_CHANGED;
    markAreaChanged();
  }

  /**
   * the setMonitorXX() calls have to preclude any monitor modification (changing
   * lockingThread, lockCount, or blocked/waiter threads, since we have to make
   * sure we don't modify an already pool-stored Monitor object
   *
   * this is not very nice, but for the sake of consistency (ThreadData index,
   * Fields index etc.) we keep it. The plethora of setMonitorXX methods is due to
   * some optimization, since we don't want to first pushClinit, then clone,
   * and finally replace waiter/blocked arrays. Stupid thing is that is a Monitor
   * optimization which is just here because of the associated mIndex == -1 check
   */
  void setMonitor () {
    if ((attributes & ATTR_MONITOR_CHANGED) == 0) {
      resetMonitorIndex();
      monitor = monitor.clone();
    }
  }

  void setMonitorWithLocked( ThreadInfo ti) {
    if ((attributes & ATTR_MONITOR_CHANGED) != 0) {
      monitor.addLocked(ti);
    } else {
      resetMonitorIndex();
      monitor = monitor.cloneWithLocked(ti);
    }
  }

  void setMonitorWithoutLocked (ThreadInfo ti) {
    if ((attributes & ATTR_MONITOR_CHANGED) != 0) { // no need to clone, it hasn't been pooled yet
      monitor.removeLocked(ti);
    } else {
      resetMonitorIndex();
      monitor = monitor.cloneWithoutLocked(ti);
    }
  }

  public boolean isLockedBy(ThreadInfo ti) {
    return ((monitor != null) && (monitor.getLockingThread() == ti));
  }

  public boolean isLocking(ThreadInfo ti){
    return (monitor != null) && monitor.isLocking(ti);
  }
  
  void _printAttributes(String cls, String msg, int oldAttrs) {
    if (getClassInfo().getName().equals(cls)) {
      System.out.println(msg + " " + this + " attributes: "
          + Integer.toHexString(attributes) + " was: "
          + Integer.toHexString(oldAttrs));
    }
  }


    
  public void checkConsistency() {
    ThreadInfo ti = monitor.getLockingThread();
    if (ti != null) {
      // object has to be in the lockedObjects list of this thread
      checkAssertion( ti.getLockedObjects().contains(this), "locked object not in thread: " + ti);
    }

    if (monitor.hasLockedThreads()) {
      checkAssertion( refTid.cardinality() > 1, "locked threads without multiple referencing threads");

      for (ThreadInfo lti : monitor.getBlockedOrWaitingThreads()){
        checkAssertion( lti.lockRef == index, "blocked or waiting thread has invalid lockRef: " + lti);
      }

      // we can't check for having lock contenders without being shared, since this can happen
      // in case an object is behind a FieldInfo shared-ness firewall (e.g. ThreadGroup.threads), or
      // is kept/used in native code (listener, peer)
    }
  }
  
  protected void checkAssertion(boolean cond, String failMsg){
    if (!cond){
      System.out.println("!!!!!! failed ElementInfo consistency: "  + this + ": " + failMsg);

      System.out.println("object: " + this);
      System.out.println("refTid: " + refTid);
      
      ThreadInfo tiLock = getLockingThread();
      if (tiLock != null) System.out.println("locked by: " + tiLock);
      
      if (monitor.hasLockedThreads()){
        System.out.println("lock contenders:");
        for (ThreadInfo ti : monitor.getLockedThreads()){
          System.out.println("  " + ti + " = " + ti.getState());
        }
      }
      
      JVM.getVM().dumpThreadStates();
      assert false;
    }
  }

}

