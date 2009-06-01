//
// Copyright (C) 2007 United States Government as represented by the
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

package gov.nasa.jpf.verify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * abstraction for possible values for a call argument
 */
public class ValSet implements Iterable<Object> {
  ArrayList<Object> values = new ArrayList<Object>();
  
  public ValSet (Object... vals){
    for (Object o : vals){
      values.add(o);
    }
  }
  
  public void add (Object o){
    values.add(o);
  }
  
  public void addAll (Collection<?> set){
    values.addAll(set);
  }
  
  public Iterator<Object> iterator() {
    return values.iterator();
  }
  
  List<Object> getValues () {
    return values;
  }
  
  public int size() {
    return values.size();
  }
  
  public boolean hasHomogenousType() {
    return getType() != null;
  }
  
  public Class<?> getType() {
    int n = values.size();
    
    // very simple for now
    if (n == 0){
      return null;
      
    } else {
      Class<?> type = values.get(0).getClass();
      
      // check if the others are of the same type
      for (int i=1; i<n; i++){
        if (values.get(i).getClass() != type){
          return null;
        }
      }
      
      return type;
    }
  }
  
  public void printOn (PrintWriter pw){
    int i=0;
    
    pw.print('{');
    for (Object o : values){
      if (i++ > 0){
        pw.print(',');
      }
      pw.print(o);
    }
    pw.print('}');
  }
}
