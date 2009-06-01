//
// Copyright  (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
//  (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
//  (NOSA), version 1.3.  The NOSA has been approved by the Open Source
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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * class to turn string literals with regular expression like terms into
 * a list of expanded Strings, like
 *   "none|#[1-2][0-1]" -> {none, #10, #11, #20, #21}
 *   
 * alternatives ('|' terms) are greedy, and they don't nest (yet), even though
 * this could be added
 * 
 * the string literal handling is a bit awkward - alternatives and
 * char sets inside of string literals cause expansion, unless the
 * '[', ']' and '|' chars are quoted, and single quotes are
 * permitted to specify literals, but are replaced by double quotes,
 * e.g. 'ab|cd' -> {"ab","cd"}
 * This is a compromise to allow single quotes in double
 * quoted contexts (e.g. annotations, which would otherwise require
 * escaping double quotes, which doesn't improve readability), but it
 * cuts off char literals 
 */
public class StringExpander {
    
  ArrayList<String> expandWithSequence (StringBuilder seq, ArrayList<String> list) {
    if (seq.length() > 0) {
      int len = list.size();
      if (len > 0) {
        for (int i=0; i<len; i++) {
          list.set(i, list.get(i) + seq);
        }
      } else {
        list.add(seq.toString());
      }
    }
    
    return list;
  }
  
  ArrayList<String> expandWithSet (StringBuilder set, ArrayList<String> list) {
    int sl = set.length();
    if (sl > 0) {
      int len = list.size();
      if (len > 0) {
        ArrayList<String> newList = new ArrayList<String>(len * sl); 
        for (int j=0; j<len; j++) {
          String e = list.get(j);
          for (int i=0; i<sl; i++) {
            newList.add(e + set.charAt(i));
          }
        }
        list = newList;
      } else {
        for (int i=0; i<sl; i++) {
          list.add( Character.toString(set.charAt(i)));
        }
      }
      
    }
    
    return list;
  }

  StringBuilder getCharSet (ExpandableStringReader r) throws IOException {
    int c;
    StringBuilder set = new StringBuilder();

    while ((c = r.read()) != ExpandableStringReader.EOS) {
      if (c == ExpandableStringReader.CHAR_CHOICE_END) {
        break;
      } else if (c == '-') {
        int p = r.last+1;
        if ((c = r.read()) != ExpandableStringReader.EOS && 
            (c != ExpandableStringReader.CHAR_CHOICE_END)) {
          for (; p<=c; p++) {
            set.append((char)p);
          }
        }
      } else {
        set.append((char)c);
      }
    }
    
    return set;
  }
  
  List<String> expandCharSets (String s) {
    ArrayList<String> list = new ArrayList<String>();
    ExpandableStringReader r = new ExpandableStringReader(s);
    StringBuilder seq = new StringBuilder();
    int c;
    
    try {
      while ((c = r.read()) != ExpandableStringReader.EOS) {
        if (c == ExpandableStringReader.CHAR_CHOICE_START) {
          list = expandWithSequence( seq, list); // add the prefix to the current set
          StringBuilder set = getCharSet(r);
          list = expandWithSet( set, list);
          seq.setLength(0);
        } else {
          seq.append((char)c);
        }
      }
      list = expandWithSequence( seq, list);
      
    } catch (IOException x) {
      x.printStackTrace();
    }
    
    return list;
  }
  
  public List<String> expand (String s) {
    ArrayList<String> list = new ArrayList<String>();
    
    for (String sub : getAlternatives(s)){
      list.addAll( expandCharSets(sub));
    }
    
    return list;
  }
    
  List<String> getAlternatives (String s) {
    ExpandableStringReader r = new ExpandableStringReader(s);
    int c;
    int i, j;
    ArrayList<String> list = new ArrayList<String>();
    boolean inLiteral=false, inLiteral1=false;
    
    try {
      for (i=0, j=0, c=r.read(); c != ExpandableStringReader.EOS; c=r.read()) {
        inLiteral = inLiteral1;
        i = r.getPosition();
        if (c == ExpandableStringReader.OR){
          String ss = s.substring(j,i);
          list.add( inLiteral ? quote(ss) : ss);
          j = i+1;
        } else if (c == '"' || c == '\'') {
          inLiteral1 = !inLiteral1;
        }
      }
      
      if (i>=j) {
        String ss = s.substring(j); 
        list.add(inLiteral ? quote(ss) : ss);
      }
    } catch (IOException x) {   
    }
    
    return list;
  }
      
  String quote (String s) {
    char c = s.charAt(0);
    if (c == '"') {
      // nothing
    } else if (c == '\'') {
      s = "\"" + s.substring(1);
    } else {
      s = "\"" + s; 
    }
    
    int l = s.length() - 1;
    c = s.charAt(l);
    if (c == '"') {
      // nothing
    } else if (c == '\'') {
      s = s.substring(0,l) + '"';
    } else {
      s = s + '"'; 
    }    
    
    return s;
  }
  
  public static void main (String[] args) {
    StringExpander ex = new StringExpander();
    
/**/
    for (String s : ex.expand(args[0])) {
      System.out.println(s);
    }
/**/
/**
    for (String s : ex.getAlternatives(args[0])){
      System.out.println(s);
    }
**/
  }
}
