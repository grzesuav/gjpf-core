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

package gov.nasa.jpf.classfile;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.util.JPFLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * this is a lookup mechanism for class files that is based on an ordered
 * list of directory or jar entries
 */
public class ClassPath {

  static JPFLogger logger = JPF.getLogger("gov.nasa.jpf.classfile");

  ArrayList<PathElement> pathElements;


  static abstract class PathElement {
    String name;

    protected PathElement(String name){
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public abstract byte[] getClassData (String clsName) throws ClassFileException;

    protected void readFully(InputStream is, byte[] buf) throws ClassFileException {
      try {
        int nRead = 0;

        while (nRead < buf.length) {
          int n = is.read(buf, nRead, (buf.length - nRead));
          if (n < 0) {
            error("premature end of classfile: " + buf.length + '/' + nRead);
          }
          nRead += n;
        }

      } catch (IOException iox) {
        error("failed to read classfile");
      }
    }
  }

  static class DirElement extends PathElement {
    File dir;

    DirElement (File dir){
      super(dir.getPath());
      this.dir = dir;
    }

    public byte[] getClassData (String clsName) throws ClassFileException {
      String pn = clsName.replace('.', File.separatorChar) + ".class";
      File f = new File(dir,pn);

      if (f.isFile()){
        FileInputStream fis = null;

        try {
          fis = new FileInputStream(f);
          long len = f.length();
          if (len > Integer.MAX_VALUE){
            error("classfile too big: " + f.getPath());
          }
          byte[] data = new byte[(int)len];
          readFully(fis, data);

          return data;

        } catch (IOException iox){
          error("cannot read " + f.getPath());

        } finally {
          if (fis != null){
            try {
              fis.close();
            } catch (IOException iox){
              error("cannot close input stream for file " + f.getPath());
            }
          }
        }
      }

      return null;
    }
  }

  static class JarElement extends PathElement {
    JarFile jar;

    JarElement (File file) throws ClassFileException {
      super(file.getPath());

      try {
        jar = new JarFile(file);

      } catch (IOException iox){
        error("reading jar: " + name);
      }
    }


    public byte[] getClassData (String clsName) throws ClassFileException {
      String pn = clsName.replace('.', File.separatorChar) + ".class";
      JarEntry e = jar.getJarEntry(pn);

      if (e != null){
        InputStream is = null;
        try {
          long len = e.getSize();
          if (len > Integer.MAX_VALUE){
            error("classfile too big: " + e.getName());
          }

          is = jar.getInputStream(e);

          byte[] data = new byte[(int)len];
          readFully(is, data);

          return data;

        } catch (IOException iox){
          error("error reading jar entry " + e.getName());

        } finally {
          if (is != null){
            try {
              is.close();
            } catch (IOException iox){
              error("cannot close input stream for file " + e.getName());
            }
          }
        }
      }

      return null;
    }
  }


  public ClassPath (String[] pathNames) {
    pathElements = new ArrayList<PathElement>();

    for (String e : pathNames){
      File f = new File(e);
      PathElement pe = null;

      if (f.isDirectory()){
        pe = new DirElement(f);

      } else {
        if (f.isFile()){
          if (e.endsWith(".jar")){
            try {
              pe = new JarElement(f);
            } catch (ClassFileException cfx){
              // issue a warning
            }
          }
        }
      }

      if (pe != null){
        pathElements.add(pe);
      } else {
        logger.warning("illegal classpath element ", e);
      }
    }
  }

  protected static void error(String msg) throws ClassFileException {
    throw new ClassFileException(msg);
  }


  public byte[] getClassData(String clsName) throws ClassFileException {
    for (PathElement e : pathElements){
      byte[] data = e.getClassData(clsName);
      if (data != null){
        return data;
      }
    }

    return null;
  }

  public static void main(String[] args){
    String[] pe = args[0].split(":");

    long t1 = System.currentTimeMillis();
    ClassPath cp = new ClassPath(pe);

    for (int i=0; i<2000; i++){
      try {
        byte[] b = cp.getClassData(args[1]);
        if (b != null){
          //System.out.println("found classfile: " + b.length);
        }

      } catch (ClassFileException cfx) {
        cfx.printStackTrace();
      }
    }

    long t2 = System.currentTimeMillis();
    System.out.println("elapsed time: " + (t2 - t1));
  }

}