/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package gov.nasa.jpf.vm.choice;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.ChoiceGeneratorBase;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.ReferenceChoiceGenerator;

import java.util.ArrayList;

/**
 * a choice generator that enumerates the set of all objects of a certain type. This
 * is a replacement for the old 'Verify.randomObject'
 */
public class TypedObjectChoice extends ChoiceGeneratorBase<Integer> implements ReferenceChoiceGenerator {
  
  // the requested object type
  protected String type;
  
  // the object references
  protected int[] values;
  
  // our enumeration state
  protected int count;

  
  public TypedObjectChoice (Config conf, String id)  {
    super(id);
    
    Heap heap = VM.getVM().getHeap();
    
    type = conf.getString(id + ".type");
    if (type == null) {
      throw conf.exception("missing 'type' property for TypedObjectChoice " + id);
    }
    
    ArrayList<ElementInfo> list = new ArrayList<ElementInfo>();
    
    for ( ElementInfo ei : heap.liveObjects()) {
      ClassInfo ci = ei.getClassInfo();
      if (ci.isInstanceOf(type)) {
        list.add(ei);
      }
    }
    
    values = new int[list.size()];
    int i = 0;
    for ( ElementInfo ei : list) {
      values[i++] = ei.getObjectRef();
    }
    
    count = -1;
  }
  
  @Override
  public void advance () {
    count++;
  }

  @Override
  public int getProcessedNumberOfChoices () {
    return count+1;
  }

  @Override
  public int getTotalNumberOfChoices () {
    return values.length;
  }

  @Override
  public boolean hasMoreChoices () {
    return !isDone && (count < values.length-1);
  }

  @Override
  public void reset () {
    count = -1;

    isDone = false;
  }

  @Override
  public Integer getNextChoice () {
    if ((count >= 0) && (count < values.length)) {
      return new Integer(values[count]);
    } else {
      return new Integer(-1);
    }
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TypedObjectChoice [id=");
    sb.append(id);
    sb.append(",type=");
    sb.append(type);
    sb.append(",values=");
    for (int i=0; i< values.length; i++) {
      if (i>0) {
        sb.append(',');
      }
      if (i == count) {
        sb.append("=>");
      }
      sb.append(values[i]);
    }
    sb.append(']');
    
    return sb.toString();
  }
  
  @Override
  public TypedObjectChoice randomize() {
    for (int i = values.length - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      int tmp = values[i];
      values[i] = values[j];
      values[j] = tmp;
    }
    return this;
  }

  @Override
  public Class<Integer> getChoiceType() {
    return Integer.class;
  }
}
