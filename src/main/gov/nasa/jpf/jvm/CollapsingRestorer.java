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
import gov.nasa.jpf.util.ObjVector;

import java.util.Arrays;
import java.util.BitSet;

/**
 * A backtracker that, by itself, does hash-collapsing to share structure
 * among states that can be backtracked to.  This has an advantage over
 * DefaultBacktracker+CollapseRestorer when CollapseRestorer is not used as
 * the serializer (for example, when state storage is not used or when another
 * serializer is used).
 * <br><br>
 * At present, this is chosen if no vm.backtracker.class is specified
 * (as in default.properties) and no restorer is loaded.
 *
 * @author peterd
 */
public class CollapsingRestorer extends AbstractRestorer<CollapsingRestorer.KState>
implements IncrementalChangeTracker {
  protected final CollapsePools.AllWeak pool = new CollapsePools.AllWeak();


  // *************** SUBCACHES ***************** //
  protected final ObjVector<TState>   tCaches = new ObjVector<TState>();
  protected final ObjVector<DEIState> dCaches = new ObjVector<DEIState>();
  protected final ObjVector<SEIState> sCaches = new ObjVector<SEIState>();


  // ************ SERIALIZATION STUFF ************ //

  protected KState computeRestorableData() {
    updateThreadListCache(ks.tl);
    updateStaticAreaCache(ks.sa);
    updateDynamicAreaCache(ks.da);

    TState[] tstates = tCaches.toArray(new TState[tCaches.size()]);
    DEIState[] dstates = dCaches.toArray(new DEIState[dCaches.size()]);
    SEIState[] sstates = sCaches.toArray(new SEIState[sCaches.size()]);
    return new KState(tstates, dstates, sstates);
  }

  protected void updateThreadListCache (ThreadList tl) {
    int     length = tl.length();
    tCaches.setSize(length);

    for (int i = 0; i < length; i++) {
      ThreadInfo ti = tl.get(i);
      tCaches.set(i, updateThreadCache(ti, tCaches.get(i)));
    }
  }

  protected transient final ObjVector<StackFrame> tmpFrames = new ObjVector<StackFrame>();

  protected TState updateThreadCache(ThreadInfo ti, TState entry) {
    int   length = ti.stack.size();

    ThreadData td;
    if (ti.tdChanged || entry == null || ti != entry.ti) { // cache not valid
      td = pool.poolThreadData(ti.threadData);
      ti.threadData = td;
    } else {
      td = entry.td;
    }

    int firstChanged;
    if (entry != null && ti == entry.ti) {
      if (ti.hasChanged.isEmpty()) {
        firstChanged = length;
      } else {
        firstChanged = ti.hasChanged.nextSetBit(0);
      }
    } else {  // cache not valid
      firstChanged = 0;
    }

    //tmpFrames.clear();  // invariant ouside this method
    if (entry != null) {
      tmpFrames.append(entry.frames, 0, firstChanged);
    }

    for (int i = firstChanged; i < length; i++) {
      tmpFrames.add(pool.poolStackFrame(ti.stack.get(i)));
    }

    ti.markUnchanged();

    StackFrame[] frames = tmpFrames.toArray(new StackFrame[tmpFrames.size()]);
    tmpFrames.clear();

    return new TState(ti,td,frames);
  }

  protected void updateDynamicAreaCache (DynamicArea area) {
    if (area.anyChanged()) {
      int length = area.getLength();
      dCaches.setSize(length);

      for (int i=0; (i=area.getNextChanged(i)) >= 0; i++) {
        DynamicElementInfo ei = area.get(i);
        if (ei != null) {
          Fields f = poolFields(ei);
          Monitor m = poolMonitor(ei);
          int a = ei.getAttributes();
          dCaches.set(i,new DEIState(f,m,a));
          ei.markUnchanged();
        } else {
          dCaches.set(i,null);
        }
      }

      area.markUnchanged();
    }
  }

  protected Fields poolFields (ElementInfo ei) {
    return pool.poolFields(ei.fields);
  }

  protected Monitor poolMonitor (ElementInfo ei) {
    return pool.poolMonitor(ei.monitor);
  }


  protected void updateStaticAreaCache (StaticArea area) {
    if (area.anyChanged()) {
      int length = area.getLength();
      sCaches.setSize(length);

      for (int i=0; (i=area.getNextChanged(i)) >= 0; i++) {
        StaticElementInfo ei = area.get(i);
        if (ei != null) {
          Fields f = poolFields(ei);
          Monitor m = poolMonitor(ei);
          int a = ei.getAttributes();
          int c = ei.getClassObjectRef();
          int s = ei.getStatus();
          sCaches.set(i, new SEIState(f,m,a,c,s));
          ei.markUnchanged();

        } else {
          sCaches.set(i, null);
        }
      }

      area.markUnchanged();
    }
  }



  // *********** DESERIALIZATION STUFF *********** //

  protected void doRestore (KState state) {
    // we need to restore the Thread list first, since objects (ElementInfos)
    // might refer to it (e.g. when re-computing volatiles)
    doRestore(ks.tl, state.tstates);
    doRestore(ks.sa, state.sstates);
    doRestore(ks.da, state.dstates);
  }

  protected void doRestore (ThreadList tl, TState[] tstates) {
    int newLength = tstates.length;
    ThreadInfo[] threads = new ThreadInfo[newLength];

    for (int i = 0; i < newLength; i++) {
      threads[i] = restoreThreadInfo(tstates[i]);
    }

    tl.setAll(threads);

    // restore cache
    tCaches.clear();
    tCaches.append(tstates);
  }


  protected ThreadInfo restoreThreadInfo (TState tstate) {
    ThreadData td = tstate.td;
    int objRef = td.objref;
    ThreadInfo ti = ThreadInfo.threadInfos.get(objRef);

    ti.resetVolatiles();
    ti.restoreThreadData(td);
    ti.replaceStackFrames(Arrays.asList(tstate.frames));
    ti.markUnchanged();

    return ti;
  }

  protected void restoreFields (ElementInfo ei, Fields fields) {
    ei.fields = fields;
  }

  protected void doRestore(DynamicArea area, DEIState[] dstates) {
    int length = dstates.length;

    area.resetVolatiles();
    area.removeAllFrom(length);

    for (int i = 0; i < length; i++) {
      DEIState estate = dstates[i];
      if (estate != null) {
        DynamicElementInfo ei = area.ensureAndGet(i);

        restoreFields(ei, estate.fields);
        ei.monitor = estate.monitor;
        ei.attributes = estate.attributes;
        
        ei.markUnchanged();
        ei.updateLockingInfo(); // monitor needs to be set before we call this
      } else {
        area.remove(i, true);
      }
    }

    area.restoreVolatiles();
    area.markUnchanged();

    // restore cache
    dCaches.clear();
    dCaches.append(dstates);
  }

  protected void doRestore(StaticArea area, SEIState[] sstates) {
    int length = sstates.length;

    area.resetVolatiles();
    area.removeAllFrom(length);

    for (int i = 0; i < length; i++) {
      SEIState estate = sstates[i];
      if (estate != null) {
        StaticElementInfo ei = area.ensureAndGet(i);

        restoreFields(ei, estate.fields);
        ei.monitor = estate.monitor;
        ei.attributes = estate.attributes;
        ei.classObjectRef = estate.classRef;
        ei.status = estate.status;

        ei.markUnchanged();
        ei.updateLockingInfo(); // monitor needs to be set before we call this

      } else {
        area.remove(i, true);
      }
    }

    area.restoreVolatiles();
    area.markUnchanged();

    // restore cache
    sCaches.clear();
    sCaches.append(sstates);
  }




  // ************** STATE DATA STRUCTURE *********** //
  protected static class KState {
    public final TState[] tstates;
    public final DEIState[] dstates;
    public final SEIState[] sstates;

    public KState(TState[] tstates, DEIState[] dstates, SEIState[] sstates) {
      this.tstates = tstates; this.dstates = dstates; this.sstates = sstates;
    }
  }

  protected static class TState {
    public final ThreadInfo ti; // not pooled; for cache rejection
    public final ThreadData td;
    public final StackFrame[] frames;

    public TState(ThreadInfo ti, ThreadData td, StackFrame[] frames) {
      this.ti = ti; this.td = td; this.frames = frames;
    }
  }

  protected static class DEIState {
    public final Fields fields;
    public final Monitor monitor;
    public final int attributes;

    public DEIState(Fields fields, Monitor monitor, int attributes) {
      this.fields = fields; this.monitor = monitor; this.attributes = attributes;
    }
  }

  protected static class SEIState extends DEIState {
    public final int classRef;
    public final int status;

    public SEIState(Fields fields, Monitor monitor, int attributes, int classRef, int status) {
      super(fields,monitor,attributes);
      this.classRef = classRef; this.status = status;
    }
  }

}
