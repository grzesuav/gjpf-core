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
import gov.nasa.jpf.jvm.bytecode.INVOKECLINIT;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.Debug;
import gov.nasa.jpf.util.IntTable;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.Misc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Vector;


/**
 * DynamicArea is the heap, i.e. the area were all objects created by NEW
 * insn live. Hence the garbage collection mechanism resides here
 */
public class DynamicArea extends Area<DynamicElementInfo> {

  static DynamicArea heap;

  /**
   * Used to store various mark phase infos
   */
  protected final BitSet isUsed = new BitSet();
  protected final BitSet isRoot = new BitSet();
  protected final IntVector refThread = new IntVector();
  protected final IntVector lastAttrs = new IntVector();

  protected boolean runFinalizer;
  protected boolean sweep;

  // just an internal helper
  protected int markLevel;

  protected boolean outOfMemory; // can be used by listeners to simulate outOfMemory conditions

  /** used to keep track of marked WeakRefs that might have to be updated */
  protected ArrayList<Fields> weakRefs;

  /**
   * DynamicMap is a mapping table used to achieve heap symmetry,
   * associating thread/pc specific DynamicMapIndex objects with their
   * corresponding DynamicArea elements[] index.
   */
  protected final IntTable<DynamicMapIndex> dynamicMap = new IntTable<DynamicMapIndex>();

  public static void init (Config config) {

  }

  /**
   * Creates a new empty dynamic area.
   */
  public DynamicArea (Config config, KernelState ks) {
    super(ks);

    runFinalizer = config.getBoolean("vm.finalize", true);
    sweep = config.getBoolean("vm.sweep",true);

    // beware - we store 'this' in a static field, which (a) makes it
    // effectively a singleton, (b) means the assignment should be the very last
    // insn to avoid handing out a ref to a partially initialized object (no
    // subclassing!)
    // <2do> - revisit during DynamicArea / Static redesign
    heap = this;
  }

  public static DynamicArea getHeap () {
    return heap;
  }

  /**
   * this is of course just a VM specific approximation
   */
  public int getHeapSize () {
    int n=0;
    for (DynamicElementInfo ei : this){
      n += ei.getHeapSize();
    }
    return n;
  }

  public boolean getOutOfMemory () {
    return outOfMemory;
  }

  public void setOutOfMemory (boolean isOutOfMemory) {
    outOfMemory = isOutOfMemory;
  }

  public void setSweep (boolean b){
    sweep = b;
  }

  public void gc () {
    analyzeHeap(sweep);
  }

  /**
   * Our precise mark & sweep garbage collector. Be aware of two things
   *
   * (1) it's called every transition (forward) we detect has changed a reference,
   *     to ensure heap symmetry (save states), but at the cost of huge
   *     gc loads, where we cannot perform all the nasty performance tricks of
   *     'normal' GCs
   * (2) we do even more - we keep track of reachability, i.e. if an object is
   *     reachable from a thread root object, to check if it is thread local
   *     (in which case we can ignore corresponding field accesses as potential
   *     scheduling relevant insns in our on-the-fly partial order reduction).
   *     Note that reachability does not mean accessibility, which is much harder
   */

  public void analyzeHeap (boolean sweep) {
    // <2do> pcm - we should refactor so that POR reachability (which is checked
    // on each ref PUTFIELD, PUTSTATIC) is more effective !!

    int i;
    int length = elements.size();
    ElementInfo ei;
    weakRefs = null;

    JVM.getVM().notifyGCBegin();
    initGc();

    // phase 0 - not awefully nice - we have to chache the attribute values
    // so that we can determine at the end of the gc if any life object has
    // changed only in its attributes. 'lastAttrs' could be a local.
    // Since we have this loop, we also use it to reset all the propagated
    // (i.e. re-computed) object attributes of live objects
    for (i=0; i<length; i++) {
      ei = elements.get(i);
      if (ei != null) {
        lastAttrs.set( i, ei.attributes);
        ei.attributes &= ~ElementInfo.ATTR_PROP_MASK;

        if ((ei.attributes & ElementInfo.ATTR_PINDOWN) != 0){
          markPinnedDown(i);
        }
      }
    }

    // phase 1 - mark our root sets.
    // After this phase, all directly root reachable objects have a 'lastGC'
    // value of '-1', but are NOT recursively processed yet (i.e. all other
    // ElementInfos still have the old 'lastGc'). However, all root marked objects
    // do have their proper reachability attribute set
    ks.tl.markRoots(); // mark thread stacks
    ks.sa.markRoots(); // mark objects referenced from StaticArea ElementInfos

    // phase 2 - walk through all the marked ones recursively
    // Now we traverse, and propagate the reachability attribute. After this
    // phase, all live objects should be marked with the 'curGc' value
    for (i=0; i<length; i++) {
      if (isRoot.get(i)) {
        markRecursive(i);
      }
    }

    // phase 3 - run finalization (slightly approximated, since it should be
    // done in a dedicated thread)
    // we need to do this in two passes, or otherwise we might end up
    // removing objects that are still referenced from within finalizers
    if (sweep && runFinalizer) {
      for (i = 0; i < length; i++) {
        ei = elements.get(i);
        if ((ei != null) && !isUsed.get(i)) {
          // <2do> here we have to add the object to the finalizer queue
          // and activate the FinalizerThread (which is kind of a root object too)
          // not sure yet how to handle this best to avoid more state space explosion
          // THIS IS NOT YET IMPLEMENTED
        }
      }
    }

    // phase 4 - all finalizations are done, reclaim all unmarked objects, i.e.
    // all objects with 'lastGc' != 'curGc', and check for attribute-only changes
    int count = 0;

    for (i = 0; i < length; i++) {
      ei = elements.get(i);
      if (ei != null) {
        if (isUsed.get(i)) {
          // Ok, it's live, BUT..
          // beware of the case where the only change we had was a attribute
          // change - the downside of our marvelous object attribute system is
          // that we have to store the attributes so that we can later-on backtrack

          // NOTE: even if the critical case is only the one where 'anyChanged'
          // is false (i.e. there was no other heap change), a high number of
          // attribute-only object changes (reachability) is bad because it means
          // the whole object has to be stored (we don't keep the attrs separate
          // from the other ElementInfo storage). On the other hand, we use
          // state collapsing, and hence the overhead should be bounded (<= 2x)
          if (lastAttrs.get(i) != ei.attributes) {
            /*
            if (!heapModified) {
              // that's BAD - an attribute-only change
              System.out.println("attr-only change of " + ei + " "
                                   + Integer.toHexString(lastAttrs.get(i)) + " -> "
                                   + Integer.toHexString(ei.attributes));
            }
            */

            markChanged(i);
          }

        } else if (sweep) {
          // this object is garbage, toast it
          count++;
          JVM.getVM().notifyObjectReleased(ei);
          remove(i,false);
        }
      }
    }

    if (sweep) {
      ks.tl.sweepTerminated(isUsed);
      checkWeakRefs(); // for potential nullification
    }

    JVM.getVM().notifyGCEnd();
  }

  protected void initGc () {
    isRoot.clear();
    isUsed.clear();
  }

  void logMark (FieldInfo fi, ElementInfo ei, int tid, int attrMask) {
    /**/
    for (int i=0; i<=markLevel; i++) System.out.print("    ");

    if (fi != null) {
      System.out.print('\'');
      System.out.print(fi.getName());
      System.out.print("': ");
    }

    System.out.print( ei);

    System.out.print( " ,attr:");
    System.out.print( Integer.toHexString(ei.attributes));

    System.out.print( " ,mask:");
    System.out.print( Integer.toHexString(attrMask));

    System.out.print( " ,thread:");
    System.out.print( tid);
    System.out.print( "/");
    System.out.print( refThread.get(ei.index));
    System.out.print( " ");

    if (isRoot.get(ei.index)) System.out.print( "R");
    if (isUsed.get(ei.index)) System.out.print( "V");

    System.out.println();
    /**/
  }

  /**
   * recursive attribute propagating marker, used to traverse the object graph
   * (it's here so that we can pass in gc-local data into the ElementInfo
   * methods). This method is called on all root objects, and starts the
   * traversal:
   *       DynamicArea.markRecursive(objref)   <-- tid, default attrMask
   *       ElementInfo.markRecursive(tid,attrMask)   <-- object attributes
   *       Fields.markRecursive(tid,attributes,attrMask)   <-- field info
   *       DynamicArea.markRecursive(objref,tid,refAttrs, attrMask, fieldInfo)
   * @aspects: gc
   */
  protected void markRecursive (int objref) {
    int tid = refThread.get(objref);
    ElementInfo ei = elements.get(objref);
    int attrMask = ElementInfo.ATTR_PROP_MASK;

    markLevel = 0;

    //logMark( null, ei, tid, attrMask);
    ei.markRecursive(tid, attrMask);
  }


  protected void markRecursive (int objref, int refTid, int refAttr, int attrMask, FieldInfo fi) {
    if (objref == -1) {
      return;
    }
    ElementInfo ei = elements.get(objref);

    if (fi != null) {
      attrMask &= fi.getAttributes();
    }

    markLevel++;

    // this is a bit tricky - (1) we have to recursively descend, and (2) we
    // have to make sure we do this only where needed (or we might get an infinite recursion
    // or at least get slow)

    if (isUsed.get(objref)) {
      // we have seen this before, and have to check for a change in attributes that
      // might require a re-recurse. That change could either be introduced at this
      // level (we hit a non-shared object referenced from another thread), or it could
      // be refAttr inflicted (i.e. passed in from a re-recurse). But in any way, we
      // have to check for these changes being masked out (attrMask)

      int attrs = ei.getAttributes();

      // Ok, gotcha - but be aware sharedness might be masked out (the ThreadGroup thing)
      if (!ei.isShared() && (refTid != refThread.get(objref))) {
        ei.setShared(attrMask);
      }

      // even if we didn't change sharedness here, we have to propagate attributes
      // (we might get here from the recursion of another object detected to be shared)
      ei.propagateAttributes(refAttr, attrMask);


      // only if the attributes have changed, we have to recurse
      if (ei.getAttributes() != attrs) {
        // make sure we don't traverse this again (note that root objects are marked 'isUsed')
        if (isRoot.get(objref)) {
          isRoot.clear(objref);
        }

        ei.markRecursive(refTid, attrMask);

      } else {
        // if attributes haven't changed, we still have to traverse this if it is a root object
      }

    } else {
      // first time around, mark used, record referencing thread, set attributes, and recurse
      isUsed.set(objref);
      refThread.set(objref, refTid);

      ei.propagateAttributes(refAttr, attrMask);
      ei.markRecursive(refTid, attrMask);
    }

    markLevel--;
  }


  /**
   * called during non-recursive phase1 marking of all objects reachable
   * from Thread roots
   * @aspects: gc
   */
  protected void markThreadRoot (int objref, int tid) {
    if (objref == -1) {
      return;
    }

    if (isRoot.get(objref)) {
      int rt = refThread.get(objref);
      if ((rt != tid) && (rt != -1)) {
        elements.get(objref).setShared();
        // <2do> - this would be the place to add a listener notification
      }
    } else {
      isRoot.set(objref);
      refThread.set(objref, tid);

      isUsed.set(objref);
    }
  }

  /**
   * called during non-recursive phase1 marking of all objects reachable
   * from static fields
   * @aspects: gc
   */
  protected void markStaticRoot (int objref) {
    if (objref == -1) {
      return;
    }

    isRoot.set(objref);
    refThread.set(objref, -1);

    isUsed.set(objref);

    elements.get(objref).setShared();
    // <2do> - this would be the place to add a listener notification
  }

  void markPinnedDown (int objref){
    isRoot.set(objref);
    refThread.set(objref, -1);
    isUsed.set(objref);
    // don't set shared yet
  }

  public boolean isSchedulingRelevantObject (int objRef) {
    if (objRef == -1) return false;

    return elements.get(objRef).isSchedulingRelevant();
  }

  public void log () {
    Debug.println(Debug.MESSAGE, "DA");

    for (int i = 0; i < elements.size(); i++) {
      if (elements.get(i) != null) {
        elements.get(i).log();
      }
    }
  }


  /**
   * Creates a new array of the given type
   *
   * NOTE: The elementType has to be either a valid builtin typecode ("B', "C", ..)
   * or an "L-slash" name
   */
  public int newArray (String elementType, int nElements, ThreadInfo ti) {

    //if (!Types.isTypeCode(elementType)) {
    //  elementType = Types.getTypeCode(elementType, true);
    //}

    String type = "[" + elementType;
    ClassInfo ci = ClassInfo.getResolvedClassInfo(type);

    if (!ci.isInitialized()){
      // we do this explicitly here since there are no clinits for array classes
      ci.registerClass(ti);
      ci.setInitialized();
    }

    int idx = indexFor(ti);
    Fields  f = ci.createArrayFields(type, nElements,
                                     Types.getTypeSize(elementType),
                                     Types.isReference(elementType));
    Monitor  m = new Monitor();

    DynamicElementInfo e = createElementInfo(f, m);
    add(idx, e);

    if (ti != null) { // maybe we should report them all, and put the burden on the listener
      JVM.getVM().notifyObjectCreated(ti, elements.get(idx));
    }

    // see newObject for 'outOfMemory' handling

    return idx;
  }


  /**
   * Creates a new object of the given class.
   * NOTE - this does not ensure if the class is already loaded and/or
   * initialized, so that has to be checked in the caller
   *
   * <2do> this should return a DynamicElementInfo (most callers need it anyways,
   * and getting the ref out of the ElementInfo is more efficient than a get(ref)
   */
  public int newObject (ClassInfo ci, ThreadInfo th) {
    int index;

    // create the thing itself
    Fields             f = ci.createInstanceFields();
    Monitor            m = new Monitor();

    DynamicElementInfo dei = createElementInfo(f, m);

    // get the index where to store this sucker, but be aware of that the
    // returned index might be outside the current elements array (super.add
    // takes care of this <Hrmm>)
    index = indexFor(th);

    // store it on the heap
    add(index, dei);

    // and do the default (const) field initialization
    ci.initializeInstanceData(dei);

    if (th != null) { // maybe we should report them all, and put the burden on the listener
      JVM.getVM().notifyObjectCreated(th, dei);
    }

    // note that we don't return -1 if 'outOfMemory' (which is handled in
    // the NEWxx bytecode) because our allocs are used from within the
    // exception handling of the resulting OutOfMemoryError (and we would
    // have to override it, since the VM should guarantee proper exceptions)

    return index;
  }

  /**
   * Creates a new string.
   */
  public int newString (String str, ThreadInfo th) {
    if (str != null) {
      int length = str.length();
      int index = newObject(ClassInfo.stringClassInfo, th);
      int value = newArray("C", length, th);

      ElementInfo e = get(index);
      // <2do> pcm - this is BAD, we shouldn't depend on private impl of
      // external classes - replace with our own java.lang.String !
      e.setReferenceField("value", value);
      e.setIntField("offset", 0);
      e.setIntField("count", length);

      e = get(value);
      for (int i = 0; i < length; i++) {
        e.setElement(i, str.charAt(i));
      }

      return index;
    } else {
      return -1;
    }
  }


  // we are trying to save a backtracked HashMap here. The idea is that the string
  // value chars are constant and not attributed, so the Fields object of the
  // 'char[] value' should never change. The string Fields itself
  // can (e.g. if we set symbolic attributes on the value field), so we have
  // to go one level deeper. We can't just store the String ElementInfo in the
  // map because the new state branch might just have reused it for another string
  // (unlikely but possible)
  static class InternStringEntry {
    String str;
    int ref;
    Fields fValue;

    InternStringEntry (String str, int ref, Fields fValue){
      this.str = str;
      this.ref = ref;
      this.fValue = fValue;
    }
  }

  protected boolean checkInternStringEntry (InternStringEntry e) {
    ElementInfo ei = get(e.ref);
    if (ei != null && ei.getClassInfo() == ClassInfo.stringClassInfo) {
      // check if it was the interned string
      int vref = ei.getReferenceField("value");
      ei = get(vref);
      if (ei != null && ei.getFields() == e.fValue) {
        return true;
      }
    }

    return false;
  }

  protected HashMap<String,InternStringEntry> internStrings = new HashMap<String,InternStringEntry>();

  public int newInternString (String str, ThreadInfo ti) {
    int ref = -1;

    InternStringEntry e = internStrings.get(str);
    if (e == null || !checkInternStringEntry(e)) { // not seen or new state branch
      ref = newString(str,ti);
      ElementInfo ei = get(ref);
      ei.pinDown(true); // that's important, interns don't get recycled

      int vref = ei.getReferenceField("value");
      ei = get(vref);
      internStrings.put(str, new InternStringEntry(str,ref,ei.getFields()));
      return ref;

    } else {
      return e.ref;
    }
  }

  /**
   * reset all weak references that now point to collected objects to 'null'
   * NOTE: this implementation requires our own Reference/WeakReference implementation, to
   * make sure the 'ref' field is the first one
   */
  protected void checkWeakRefs () {
    if (weakRefs != null) {
      int len = weakRefs.size();

      for (int i = 0; i < len; i++) {
        Fields f = weakRefs.get(i);
        int    ref = f.getIntValue(0); // watch out, the 0 only works with our own WeakReference impl
        if (ref != -1) {
          ElementInfo ei = elements.get(ref);
          if ((ei == null) || (ei.isNull())) {
            f.setReferenceValue(ei, 0, -1);
          }
        }
      }

      weakRefs = null;
    }
  }

  //--- factory methods for creating associated ElementInfos
  protected DynamicElementInfo createElementInfo () {
    return new DynamicElementInfo();
  }

  protected DynamicElementInfo createElementInfo (Fields f, Monitor m){
    return new DynamicElementInfo(f,m);
  }


  protected void registerWeakReference (Fields f) {
    if (weakRefs == null) {
      weakRefs = new ArrayList<Fields>();
    }

    weakRefs.add(f);
  }

  public int getNext (ClassInfo ci, int idx){
    int n = elements.size();
    for (int i=idx; i<n; i++){
      ElementInfo ei = elements.get(i);
      if (ei != null){
        if (ei.getClassInfo().isInstanceOf(ci)){
          return i;
        }
      }
    }

    return -1;
  }

  protected int indexFor (ThreadInfo th) {
    Instruction pc = null;

    if (th != null) {
      int pos = th.countStackFrames();
      while (pc instanceof INVOKECLINIT && pos > 0) {
        pos--;
        pc = th.getPC(pos);
      }
    }

    int index;

    DynamicMapIndex dmi = new DynamicMapIndex(pc, (th == null) ? 0 : th.index, 0);

    for (;;) {
      int newIdx = dynamicMap.nextPoolVal();
      IntTable.Entry<DynamicMapIndex> e = dynamicMap.pool(dmi);
      index = e.val;
      if (index == newIdx || elements.get(index) == null) {
        // new or unoccupied
        break;
      }
      // else: occupied & seen before; also ok to modify dmi
      dmi.next();
    }

    return index;
  }

  /*
   * The following code is used to linearize a rooted structure in the heap
   * A PredicateMap is used to map states onto predicates as an additional key
   * used during the linearization
   */

  protected IntTable<String> object2Value;

  protected int lincount;

  protected PredicateMap pMap = null;

  public Vector<String> linearizeRoot(int objref) {
  	object2Value = new IntTable<String>();
  	lincount = 0;
  	Vector<String> result = new Vector<String>();
  	return linearize(objref,result);
  }

  public Vector<String> linearizeRoot(int objref, PredicateMap obj) {
  	object2Value = new IntTable<String>();
  	lincount = 0;
  	Vector<String> result = new Vector<String>();
  	pMap = obj;
  	return linearize(objref,result);
  }
  /*
   * A StructureMap is a special PredicateMap that is just an identity
   * function, i.e. only the structure is important no additional predicaes
   * are used
   */
  static class StructureMap extends PredicateMap {
  	public void evaluate() {
  	}
  	public String getRep() {
  		return "";
  	}
  }

  public Vector<String> linearize(int objref, Vector<String> result) {

  	PredicateMap mapObj = pMap;

  	if (objref == -1) {
    	result.addElement("-1");
  		return result;
    }

  	if (mapObj == null)
    	mapObj = new StructureMap();

  	mapObj.setRef(objref);
  	String refRep = "" + mapObj.getRef();

  	String objRep;

  	if (object2Value.hasEntry(refRep)) {
  		objRep = "" + object2Value.get(refRep).val;
  		result.addElement(objRep);
  		return result;
  	} else {
    	object2Value.add(refRep, lincount);
    	mapObj.evaluate();
    	objRep = "" + lincount + mapObj.getRep();
    	lincount++;
  	}

  	result.addElement(objRep);

    ElementInfo ei = elements.get(objref);
    return ei.linearize(result);
  }

  // for debugging
  protected void verifyLockInfo() {
    for (DynamicElementInfo dei : elements) {
      if (dei != null) {
        dei.verifyLockInfo(ks.tl);
      }
    }
  }
}


