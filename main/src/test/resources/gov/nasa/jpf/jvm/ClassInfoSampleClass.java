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

package gov.nasa.jpf.jvm;

@ClassInfoSampleClass.X
public class ClassInfoSampleClass
    implements Cloneable {
  public static final int D = 42;

  @X("data")
  String s;

  public ClassInfoSampleClass(String s){
    this.s = s;
    foo();
  }

  public static int whatIsIt(){
    int d = D;
    switch (d) {
      case 41:
        d = -1;
        break;
      case 42:
        d = 0;
        break;
      case 43:
        d = 1;
        break;
      default:
        d = 2;
        break;
    }
    return d;
  }

  public boolean isItTheAnswer(boolean b, @X @Y({ 1, 2, 3 }) int d, String s){
    switch (d) {
      case 42:
        return true;
      default:
        return false;
    }
  }

  protected void foo()
      throws IndexOutOfBoundsException{
    @X int d = D;

    Object[] a = new Object[2];
    String s = "blah";
    a[0] = s;

    String x = (String)a[0];
    Object o = a;
    if (o instanceof Object[]) {
      o = x;
    }
    if (o instanceof String) {
      o = null;
    }

    Object[][] aa = new Object[2][2];

    try {
      char c = s.charAt(d);
    } catch (IndexOutOfBoundsException ioobx) {
      System.out.println("too big");
      throw ioobx;
    }
  }

  @X
  String getString(){
    return s;
  }

  @interface X {
    String value() default "nothing";
  }

  @interface Y {
    int[] value();
  }
}
