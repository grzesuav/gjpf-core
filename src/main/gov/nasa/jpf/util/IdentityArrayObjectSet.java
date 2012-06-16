//
// Copyright (C) 2012 United States Government as represented by the
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

/**
 * simple identity set for objects 
 */
public class IdentityArrayObjectSet<E> implements IdentityObjectSet<E> {

  static final int DEFAULT_CAPACITY = 4;
  
  protected int size;
  protected Object[] elements;
  
  public IdentityArrayObjectSet(){
    // nothing, elements allocated on demand
  }
  
  public IdentityArrayObjectSet (int initialCapacity){
    elements = new Object[initialCapacity];
  }
  
  public IdentityArrayObjectSet (E initialElement){
    elements = new Object[DEFAULT_CAPACITY];
    
    elements[0] = initialElement;
    size = 1;
  }
  
  public int size(){
    return size;
  }
  
  public boolean isEmpty(){
    return (size == 0);
  }
  
  public boolean add (E obj){
    for (int i=0; i<size; i++){
      if (elements[i] == obj){
        return false;
      }
    }
    
    if (size == 0){
      elements = new Object[DEFAULT_CAPACITY];
    } else if (size == elements.length){
      Object[] newElements = new Object[elements.length * 3 / 2];
      System.arraycopy(elements, 0, newElements, 0, size);
      elements = newElements;
    }
    
    elements[size] = obj;
    size++;
    return true;
  }
  
  public boolean contains (E obj){
    for (int i=0; i<size; i++){
      if (elements[i] == obj){
        return true;
      }
    }
    
    return false;
  }
  
  public boolean remove (E obj){
    int len = size;
    for (int i=0; i<len; i++){
      if (elements[i] == obj){
        len--;
        if (len == 0){
          size = 0;
          elements = null;
          
        } else if (i < len){
          System.arraycopy(elements, i+1, elements, i, len-i);
        } else {
          elements[len] = null; // avoid memory leak
        }
        
        size = len;
        return true;
      }
    }
    
    return false;    
  }
  
  public ObjectSet<E> clone(){
    try {
      return (ObjectSet<E>)super.clone();
    } catch (CloneNotSupportedException x){
      // can't happen
      return null;
    }
  }
  
  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder(/*getClass().getName()*/);
    sb.append('{');
    for (int i=0; i<size; i++){
      if (i>0){
        sb.append(',');
      }
      sb.append(elements[i]);
    }
    sb.append('}');
    return sb.toString();
  }
}
