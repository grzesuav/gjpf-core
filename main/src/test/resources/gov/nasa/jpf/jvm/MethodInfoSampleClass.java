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

class MethodInfoSampleClass {
  static double staticInt(int intArg){
    int a = 42;
    double b = 42.0;
    b += a;
    return b;
  }

  static double staticIntString(int intArg, String stringArg){
    int a = 42;
    double b = 42.0;
    b += a;
    return b;
  }

  static double staticNoArgs(){
    int a = 42;
    double b = 42.0;
    b += a;
    return b;
  }

  int instanceCycleMethod(int intArg, int int2Arg){
    for (int i = 0; i < int2Arg; ++i) {
      // it's important to have a for cycle because it breaks the instruction per line monotony
      intArg += intArg;
    }
    return intArg;
  }

  double instanceInt(int intArg){
    int a = 42;
    double b = 42.0;
    b += a;
    return b;
  }

  double instanceIntString(int intArg, String stringArg){
    int a = 42;
    double b = 42.0;
    b += a;
    return b;
  }

  double instanceNoArgs(){
    int a = 42;
    double b = 42.0;
    b += a;
    return b;
  }
}
