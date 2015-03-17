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
package gov.nasa.jpf.util;

public final class Permutation {
  public static final int defaultInitCap = 40;

  /** the backing array. */
  protected int[] data;
  
  /** growth strategy. */
  protected Growth growth;
  
  /** the size of the inverse vector == next range value to be assigned. */
  protected final IntVector inverse;
  
  public Permutation(Growth initGrowth, int initCap) {
    inverse = new IntVector(initGrowth,initCap);
    growth = initGrowth;
    data = new int[initCap];
  }
  
  public Permutation(Growth initGrowth) { this(initGrowth,defaultInitCap); }
  
  public Permutation(int initCap) { this(Growth.defaultGrowth, initCap); }
  
  public Permutation() { this(Growth.defaultGrowth,defaultInitCap); }
  
  
  public void add(int x) {
	if (x < 0) return;
	ensureCapacity(x+1);
    if (data[x] < 0) {
      data[x] = inverse.size;
      inverse.add(x);
    }
  }
  
  public int get(int x) {
    if (x >= data.length) {
      return -1;
    } else {
      return data[x];
    }
  }
  
  public int inverseGet(int v) {
    if (v >= inverse.size) {
      return -1;
    } else {
      return inverse.get(v);
    }
  }
  
  public void set(int x, int v) {
    ensureCapacity(x+1);
    data[x] = v;
  }

  public void clear() {
    for (int i = 0; i < data.length; i++) {
      data[i] = -1;
    }
    inverse.clear();
  }

  public int rangeSize() { return inverse.size; }
  
  public void ensureCapacity(int desiredCap) {
    if (data.length < desiredCap) {
      int[] newData = new int[growth.grow(data.length, desiredCap)];
      System.arraycopy(data, 0, newData, 0, data.length);
      for (int i = data.length; i < newData.length; i++) {
        newData[i] = -1;
      }
      data = newData;
    }
  }
}
