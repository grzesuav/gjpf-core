//
// Copyright (C) 2010 United States Government as represented by the
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

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * utility class to find all files matching (possibly hierarchical)
 * wildcard path specs
 *
 * we support single '*' wildcards as in filename matching, plus "**" patterns
 * that match all (recursive) subdirectories
 */
// example:  List<File> list = findMatches("/U*/p*/tmp/**/*.java");

public class FileFinder {

  public static boolean containsWildcards (String pattern) {
    return (pattern.indexOf('*') >= 0);
  }

  protected List<File> splitPath (String pattern) {
    ArrayList<File> list = new ArrayList<File>();

    for (File f = new File(pattern); f != null; f = f.getParentFile()) {
      list.add(f);
    }

    Collections.reverse(list);
    return list;
  }

  protected void addSubdirs (List<File> list, File dir){
    for (File f : dir.listFiles()) {
      if (f.isDirectory()){
        list.add(f);
        addSubdirs(list, f);
      }
    }
  }

  protected List<File> findMatches (File dir, String pattern) {
    ArrayList<File> list = new ArrayList<File>();

    if (dir.isDirectory()) {
      if ("**".equals(pattern)) { // recursively add all subdirectories
        addSubdirs(list, dir);

      } else {
        StringMatcher sm = new StringMatcher(pattern);
        for (File f : dir.listFiles()) {
          if (sm.matches(f.getName())) {
            list.add(f);
          }
        }
      }
    }

    return list;
  }

  public List<File> findMatches (String pattern) {
    List<File> pathComponents = splitPath(pattern);
    List<File> matches = null;

    for (File f : pathComponents) {
      String fname = f.getName();
      if (matches == null) { // first one
        if (fname.isEmpty()) { // filesystem root
          matches = new ArrayList<File>();
          matches.add(f);
        } else {
          matches = findMatches(new File(System.getProperty("user.dir")), fname);
        }

      } else {
        List<File> newMatches = new ArrayList<File>();
        for (File d : matches) {
          newMatches.addAll(findMatches(d, fname));
        }
        matches = newMatches;
      }

      if (matches.isEmpty()) {
        return matches;
      }
    }
    return matches;
  }

  public static void main (String[] args) {
    FileFinder finder = new FileFinder();
    for (File f : finder.findMatches(args[0])) {
      System.out.println(f);
    }
  }
}
