//
// Copyright (C) 2014 United States Government as represented by the
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

package gov.nasa.jpf.util;

import java.util.BitSet;
import java.util.NoSuchElementException;

/**
 * a FixedBitSet implementation that is based on java.util.BitSet
 */
public class BitSetN extends BitSet implements FixedBitSet {
  
  class SetBitIterator implements IntIterator {
    int cur = 0;
    int nBits;
    int cardinality;  // <2do> this should be lifted since it makes the iterator brittle
    
    SetBitIterator (){
      cardinality = cardinality();
    }
    
    @Override
    public void remove() {
      if (cur >0){
        clear(cur-1);
      }
    }

    @Override
    public boolean hasNext() {
      return nBits < cardinality;
    }

    @Override
    public int next() {
      if (nBits < cardinality){
        int idx = nextSetBit(cur);
        if (idx >= 0){
          nBits++;
          cur = idx+1;
        }
        return idx;
        
      } else {
        throw new NoSuchElementException();
      }
    }
  }
  
  
  public BitSetN (int nBits){
    super(nBits);
  }
  
  @Override
  public FixedBitSet clone() {
    return (FixedBitSet) super.clone();
  }

  @Override
  public int capacity() {
    return size();
  }


  @Override
  public void hash (HashData hd){
    long[] data = toLongArray();
    for (int i=0; i<data.length; i++){
      hd.add(data[i]);
    }
  }

  
  //--- IntSet interface
  @Override
  public boolean add(int i) {
    if (get(i)) {
      return false;
    } else {
      set(i);
      return true;
    }
  }

  @Override
  public boolean remove(int i) {
    if (get(i)) {
      clear(i);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean contains(int i) {
    return get(i);
  }
  
  @Override
  public IntIterator intIterator() {
    return new SetBitIterator();
  }
  
}
