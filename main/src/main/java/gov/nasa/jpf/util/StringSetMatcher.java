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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * simple utility that can be used to check for string matches in
 * sets with '*' wildcards, e.g. to check for class name lists like
 *
 *   vm.halt_on_throw=java.lang.reflect.*:my.own.Exception
 *
 * Only meta chars in patterns are '*', i.e. '.' is a regular char to match
 */
public class StringSetMatcher {

  boolean hasAnyPattern; // do we have a universal '*' pattern?

  Pattern[] pattern;
  Matcher[] matcher;

  /**
   * convenience method for matcher pairs containing of explicit excludes and
   * includes
   */
  public static boolean isMatch (String s, StringSetMatcher includes, StringSetMatcher excludes){
    if (excludes != null) {
      if (excludes.matchesAny(s)){
        return false;
      }
    }

    if (includes != null) {
      if (!includes.matchesAny(s)){
        return false;
      }
    }

    return true;
  }

  public static StringSetMatcher getNonEmpty(String[] set){
    if (set != null && set.length > 0){
      return new StringSetMatcher(set);
    } else {
      return null;
    }
  }

  public StringSetMatcher (String... set){
    int n = set.length;
    pattern = new Pattern[n];
    matcher = new Matcher[n];

    for (int i=0; i<n; i++){
      String s = set[i];

      if (s.equals("*")) {
        hasAnyPattern = true;
        // no need to compile this

      } else {
        Pattern p =  createPattern(s);
        pattern[i] = p;
        matcher[i] = p.matcher(""); // gets reset upon use
      }
    }
  }

  @Override
  public String toString() {
    int n=0;
    StringBuilder sb = new StringBuilder(64);
    sb.append("StringSetMatcher [regex_patterns=");

    if (hasAnyPattern) {
      sb.append(".*");
      n++;
    }

    for (int i=0; i<pattern.length; i++) {
      if (pattern[i] != null) {
        if (n++>0) {
          sb.append(',');
        }
        sb.append(pattern[i]);
      }
    }
    sb.append(']');
    return sb.toString();
  }

  public void addPattern (String s){

    if (s.equals("*")) { // no need to compile
      hasAnyPattern = true;

    } else {
      int n = pattern.length;

      Pattern[] pNew = new Pattern[n+1];
      System.arraycopy(pattern, 0, pNew, 0, n);
      pNew[n] = createPattern(s);

      Matcher[] mNew = new Matcher[pNew.length];
      System.arraycopy(matcher, 0, mNew, 0, n);
      mNew[n] = pNew[n].matcher("");

      pattern = pNew;
      matcher = mNew;
    }
  }

  Pattern createPattern (String s){
    Pattern p;

    StringBuilder sb = new StringBuilder();

    int len = s.length();
    for (int j=0; j<len; j++){
      char c = s.charAt(j);
      switch (c){
      case '.' : sb.append("\\."); break;
      case '$' : sb.append("\\$"); break;
      case '[' : sb.append("\\["); break;
      case ']' : sb.append("\\]"); break;
      case '*' : sb.append(".*"); break;
      case '(' : sb.append("\\("); break;
      case ')' : sb.append("\\)"); break;
      // <2do> and probably more..
      default:   sb.append(c);
      }
    }

    p = Pattern.compile(sb.toString());
    return p;
  }

  /**
   * does 's' match at least one of our patterns
   */
  public boolean matchesAny (String s){
    if (s != null) {
      if (hasAnyPattern) {
        return true; // no need to check
      }

      for (int i=0; i<matcher.length; i++){
        Matcher m = matcher[i];
        m.reset(s);

        if (m.matches()){
          return true;
        }
      }
    }

    return false;
  }

  /**
   * does 's' match ALL of our patterns
   */
  public boolean matchesAll (String s){
    if (s != null) {
      if (hasAnyPattern && pattern.length == 1) {
        return true; // no need to check
      }

      for (int i=0; i<pattern.length; i++){
        Pattern p = pattern[i];
        Matcher m = matcher[i];
        m.reset(s);

        if (!m.matches()){
          return false;
        }
      }

      return true;

    } else {
      return false;
    }
  }

  /**
   * do all elements of 'set' match at least one of our patterns?
   */
  public boolean allMatch (String[] set){
    if (hasAnyPattern) {
      return true;
    }

    for (int i=0; i<set.length; i++){
      if (!matchesAny(set[i])){
        return false;
      }
    }
    return true;
  }


  public static void main (String[] args){
    String[] p = args[0].split(":");
    String[] s = args[1].split(":");

    StringSetMatcher sm = new StringSetMatcher(p);
    if (sm.matchesAny(s[0])){
      System.out.println("Bingo, \"" + s[0] + "\" matches " + sm);
    } else {
      System.out.println("nope, \"" + s[0] + "\" doesn't match " + sm);
    }
  }
}
