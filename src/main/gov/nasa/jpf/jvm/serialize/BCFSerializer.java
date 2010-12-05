//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.jvm.serialize;

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.StaticArea;
import gov.nasa.jpf.jvm.StaticElementInfo;

/**
 * a bounded canonicalizing & filtering serializer.
 *
 * This came to bear by accidentally discovering that JPF finds the defects
 * of most known concurrency applications by just serializing the thread
 * stacks and the objects directly referenced from there. This seems too
 * aggressive if data CGs are involved, or if the application directly uses
 * sun.misc.Unsafe.park()/unpark(), but in all other cases there is a high
 * probability that relevant states have different stacks
 */
public class BCFSerializer extends CFSerializer {

  boolean traverseObjects;

  @Override
  protected void initReferenceQueue() {
    super.initReferenceQueue();

    traverseObjects = true;
  }


  @Override
  protected void queueReference(ElementInfo ei){
    if (traverseObjects){
      refQueue.add(ei);
    }
  }

  @Override
  protected void processReferenceQueue() {
    traverseObjects = false;
    refQueue.process(this);
  }

  //@Override
  protected void serializeStatics(){
    // only serialize class status and esp. class objects, because those might
    // not be on the stack when doing synchronized invoke_statics
    StaticArea statics = ks.getStaticArea();
    buf.add(statics.getLength());

    for (StaticElementInfo sei : statics) {
      ClassInfo ci = sei.getClassInfo();

      buf.add(ci.getUniqueId());
      buf.add(sei.getStatus());
      
      addObjRef( sei.getClassObjectRef());
    }
  }
}
