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
package gov.nasa.jpf.jvm.abstraction;

import java.util.LinkedList;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.abstraction.abstractor.AbstractorBasedBuilder;
import gov.nasa.jpf.jvm.AbstractSerializer;
import gov.nasa.jpf.jvm.JVM;

public class AbstractingSerializer extends AbstractSerializer {
  protected StateGraphBuilder builder;
  protected LinkedList<StateGraphTransform> transforms = new LinkedList<StateGraphTransform>();
  protected StateGraphSerializer serializer;
  protected boolean started = false;
  
  @Override
  public void attach(JVM jvm) throws Config.Exception {
    super.attach(jvm);
    Config config = jvm.getConfig();
    
    builder = config.getInstance("abstraction.builder.class", StateGraphBuilder.class);
    if (builder == null) {
      builder = new AbstractorBasedBuilder();
    }
    builder.attach(jvm);
    
    Iterable<StateGraphTransform> configTransforms = 
      config.getInstances("abstraction.transforms", StateGraphTransform.class);
    if (configTransforms != null) {
      for (StateGraphTransform t : configTransforms) {
        transforms.addLast(t);
        t.init(config);
      }
    }
    
    serializer = config.getInstance("abstraction.serializer.class", StateGraphSerializer.class);
    if (serializer == null) {
      serializer = new DefaultStateGraphSerializer();
    }
    serializer.init(config);
    // the serializer loads the linearizer if needed (probably)
  }

  public void appendTransform(StateGraphTransform t) {
    assert !started : "Attempt to change abstraction after state matching has started.";
    transforms.addLast(t);
  }
  
  protected int[] computeStoringData() {
    started = true;
    StateGraph g = builder.buildStateGraph();
    for (StateGraphTransform transform : transforms) {
      transform.transformStateGraph(g);
    }
    return serializer.serializeStateGraph(g);
  }
}