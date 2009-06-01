//
// Copyright (C) 2006 United States Government as represented by the
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


import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;


/**
 * utility class to access arbitrary source files by line number
 * sources can be files inside of root directories, or
 * can be entries in jars
 */
public class Source {

  static Logger logger = JPF.getLogger("gov.nasa.jpf.util.Source");

  static List<SourceRoot> sourceRoots;
  static Hashtable<String,Source> sources = new Hashtable<String,Source>();
  static Source noSource = new Source(null, null);

  static abstract class SourceRoot { // common base
    abstract InputStream getInputStream (String fname);
  }

  static class DirRoot extends SourceRoot {
    String path;

    DirRoot (String path){
      this.path = path;
    }

    InputStream getInputStream (String fname) {
      File f = new File(path, fname);
      if (f.exists()) {
        try {
          return new FileInputStream(f);
        } catch (FileNotFoundException fnfx) {
          return null;
        }
      } else {
        return null;
      }
    }

    public String toString() {
      return path;
    }
  }

  static class JarRoot extends SourceRoot {
    JarFile jar;
    String  entryPrefix;

    JarRoot (String path, String ep) throws IOException {
      jar = new JarFile(path);

      if (ep == null) {
        entryPrefix = null;
      } else {
        entryPrefix = ep;
        if (ep.charAt(ep.length()-1) != '/') {
          entryPrefix += '/';
        }
      }
    }

    InputStream getInputStream (String fname) {
      String en = (entryPrefix != null) ? entryPrefix + fname : fname;
      JarEntry entry = jar.getJarEntry(en);
      if (entry != null) {
        try {
          return jar.getInputStream(entry);
        } catch (IOException e) {
          return null;
        }
      } else {
        return null;
      }
    }

    public String toString() {
      return jar.getName();
    }
  }

  public static void init (Config config) {

    String[] specs = config.getStringArray("vm.sourcepath");    
    
    ArrayList<SourceRoot> roots = new ArrayList<SourceRoot>();
    for (String spec : specs) {
      SourceRoot sr = null;

      try {
        int i = spec.indexOf(".jar");
        if (i >= 0) {  // jar
          String pn = config.asPlatformPath(spec.substring(0,i+4));
          File jar = new File(pn);
          if (jar.exists()) {
            int i0 = i+5; // scrub the leading path separator
            // JarFile assumes Unix for archive-internal paths (also on Windows)
            String ep = (spec.length() > i0) ? config.asCanonicalUnixPath(spec.substring(i0)) : null;
            // we should probably check here if there is such a dir in the Jar
            sr = new JarRoot(pn,ep);
          }

        } else {       // directory
          String pn = config.asPlatformPath(spec);
          File dir = new File(pn);
          if (dir.exists()) {
            sr = new DirRoot(pn);
          }
        }
      } catch (IOException iox) {
        // we report this below
      }

      if (sr != null) {
        roots.add(sr);
      } else {
        logger.info("not a valid source root: " + spec);
      }
    }

    sourceRoots = roots;
    sources.clear();
  }


  public static Source getSource (String fname) {
    Source s = sources.get(fname);

    if (s == noSource) {
       return null;
    }

    if (s == null) {
      for (SourceRoot root : sourceRoots) {
        InputStream is = root.getInputStream(fname);
        if (is != null) {
          try {
          s = new Source(root,fname);
          s.loadLines(is);
          is.close();

          sources.put(fname, s);
          return s;
          } catch (IOException iox) {
            logger.warning("error reading " + fname + " from" + root);
            return null;
          }
        }
      }
    } else {
      return s;
    }

    sources.put(fname, noSource);

    return null;
  }

  //--- the Source instance data itself
  protected SourceRoot root;
  protected String     fname;
  protected String[]   lines;


  protected Source (SourceRoot root, String fname) {
    this.root = root;
    this.fname = fname;
  }

  protected void loadLines (InputStream is) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(is));

    ArrayList<String> l = new ArrayList<String>();
    for (String line = in.readLine(); line != null; line = in.readLine()) {
      l.add(line);
    }
    in.close();

    if (l.size() > 0) {
      lines = l.toArray(new String[l.size()]);
    }
  }


  /**
   * this is our sole purpose in life - answer line strings
   * line index is 1-based
   */
  public String getLine (int i) {
    if ((lines == null) || (i <= 0) || (i > lines.length)) {
      return null;
    } else {
      return lines[i-1];
    }
  }

  public int getLineCount()
  {
     return(lines.length);
  }

  public String getPath() {
    return root.toString() + File.separatorChar + fname;
  }
}
