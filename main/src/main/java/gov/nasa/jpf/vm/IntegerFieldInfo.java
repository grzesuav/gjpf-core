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
package gov.nasa.jpf.vm;

import gov.nasa.jpf.JPFException;


/**
 * type, name, mod info about integer fields
 */
public class IntegerFieldInfo extends SingleSlotFieldInfo {
  int init;

  public IntegerFieldInfo (String name, int modifiers) {
     super(name, "I", modifiers);
  }

  @Override
  public void initialize (ElementInfo ei, ThreadInfo ti) {
    ei.getFields().setIntValue( storageOffset, init);
  }

  @Override
  public void setConstantValue(Object constValue){
    if (constValue instanceof Integer){
      cv = constValue;
      init = (Integer)constValue;

    } else {
      throw new JPFException("illegal int ConstValue=" + constValue);
    }
  }


  @Override
  public Class<? extends ChoiceGenerator<?>> getChoiceGeneratorType() {
    return IntChoiceGenerator.class;
  }

  @Override
  public String valueToString (Fields f) {
    int i = f.getIntValue(storageOffset);
    return Integer.toString(i);
  }

  @Override
  public Object getValueObject (Fields f){
    int i = f.getIntValue(storageOffset);
    return new Integer(i);
  }

  @Override
  public boolean isIntField(){
    // booleans, byte, char and short are too
    return true;
  }

  @Override
  public boolean isNumericField(){
    return true;
  }
}
