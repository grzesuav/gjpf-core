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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * abstraction for a set of call argument lists
 */
public class ArgList implements Iterable<ValSet> {
  ArrayList<ValSet> args = new ArrayList<ValSet>();
  
  public void add (ValSet s){
    args.add(s);
  }

  public Iterator<ValSet> iterator () {
    return args.iterator();
  }
  
  public int size(){
    return args.size();
  }
  
  // internal helper
  void addArgCombinations (int i, Object[] combi, List<Object[]> list){
    int n1 = args.size()-1;
    
    for (Object v : args.get(i)){
      combi[i] = v;
        
      if (i == n1){
        list.add( combi.clone());
      } else if (i < n1){
        addArgCombinations(i+1, combi, list);
      }
    }
  }
  
  // flatten the args
  public List<Object[]> getArgCombinations () {
    List<Object[]> list = new ArrayList<Object[]>();
    Object[] combi = new Object[args.size()];

    if (args.isEmpty()){ // no args
      list.add( combi);
    } else {
      addArgCombinations(0, combi, list);
    }
    
    return list;
  }
  
  public Class<?>[] getArgTypes() {
    int n = args.size();
    Class<?>[] types = new Class<?>[n];
    for (int i=0; i<n; i++){
      types[i] = args.get(i).getType();
    }
    
    return types;
  }
  
  public ArgList (ValSet... valSets){
    for (ValSet s : valSets){
      add(s);
    }
  }
  
  public static void main (String[] args){
    ArgList a = new ArgList(  new ValSet( 42, 43)
                             ,new ValSet( "bla", "gna") 
                             ,new ValSet( .1));
    
    for (Object[] v : a.getArgCombinations()){
      for (int i=0; i<v.length; i++){
        if (i > 0){
          System.out.print(',');
        }
        System.out.print(v[i]);
      }
      System.out.println();
    }
  }
}
