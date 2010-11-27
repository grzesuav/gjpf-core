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

import gov.nasa.jpf.jvm.ElementInfo;

/**
 * a bounded canonicalizing & filtering serializer.
 *
 * This came to bear by accidentally discovering that most of the known JPF
 * applications find targeted errors by just serializing the thread states,
 * statics, and the heap objects referenced from the thread states. This must
 * be caused by a high probability that threads more likely reference what
 * they change
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
}
