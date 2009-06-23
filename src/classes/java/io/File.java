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
package java.io;

import gov.nasa.jpf.annotations.FilterField;


/**
 * MJI model class for java.io.File
 *
 * NOTE - a number of methods are only stubbed out here to make Eclipse compile
 * JPF code that uses java.io.File (there is no way to tell Eclipse to exclude the
 * model classes from ths build-path)
 *
 * @author Owen O'Malley
 */
public class File
{
  public static final String separator = System.getProperty("file.separator");
  public static final char separatorChar = separator.charAt(0);
  public static final String pathSeparator = System.getProperty("path.separator");
  public static final char pathSeparatorChar = pathSeparator.charAt(0);

  @FilterField int id; // link to the real File object
  private String filename;

  public File(String filename) {
    this.filename = filename;
  }

  public File (String parent, String child) {
  	filename = parent + separator + child;
  }
  
  public File (File parent, String child) {
    filename = parent.filename + separator + child;
  }
  
  public File(java.net.URI uri) { throw new UnsupportedOperationException(); }
  
  public String getName() {
    int idx = filename.lastIndexOf(separatorChar);
    if (idx >= 0){
      return filename.substring(idx+1);
    } else {
      return filename;
    }
  }

  public String getParent() {
    int idx = filename.lastIndexOf(separatorChar);
    if (idx >= 0){
      return filename.substring(0,idx);
    } else {
      return null;
    }
  }

  
  public int compareTo(Object o) {
    return compareTo((File)o);
  }
  
  public int compareTo(File that) {
    return this.filename.compareTo(that.filename);
  }
  
  public boolean equals(Object o) {
    return filename.equals(((File)o).filename);
  }
  
  public int hashCode() {
    return filename.hashCode();
  }
  
  public String toString()  {
    return filename;
  }
  
  
  //--- native peer intercepted (hopefully)
  
  int getPrefixLength() { return 0; }
  public File getParentFile() { return null; }
  public String getPath() { return null; }
  public boolean isAbsolute() { return false; }
  public String getAbsolutePath() { return null; }
  public File getAbsoluteFile() { return null; }
  public String getCanonicalPath() throws java.io.IOException
   { return null; }
  public File getCanonicalFile() throws java.io.IOException
   { return null; }
  public java.net.URL toURL() throws java.net.MalformedURLException
   { return null; }
  public java.net.URI toURI() { return null; }
  public boolean canRead() { return false; }
  public boolean canWrite() { return false; }
  public boolean exists() { return false; }
  public boolean isDirectory() { return false; }
  public boolean isFile() { return false; }
  public boolean isHidden() { return false; }
  public long lastModified() { return -1L; }
  public long length() { return -1; }
  public boolean createNewFile() throws java.io.IOException
   { return false; }
  public boolean delete()  { return false; }
  public void deleteOnExit() {}
  public String[] list()  { return null; }
  public String[] list(FilenameFilter fnf)  { return null; }
  public File[] listFiles()  { return null; }
  public File[] listFiles(FilenameFilter fnf)  { return null; }
  public File[] listFiles(FileFilter ff)  { return null; }
  public boolean mkdir()  { return false; }
  public boolean mkdirs() { return false; }
  public boolean renameTo(File f)  { return false; }
  public boolean setLastModified(long t)  { return false; }
  public boolean setReadOnly()  { return false; }
  
  public static File[] listRoots()  { return null; }
  
  public static File createTempFile(String prefix, String suffix, File dir) throws IOException  {
    if (prefix == null){
      throw new NullPointerException();
    }
    
    String tmpDir;
    if (dir == null){
      tmpDir = System.getProperty("java.io.tmpdir");
      if (tmpDir == null){
        tmpDir = ".";
      }
      if (tmpDir.charAt(tmpDir.length()-1) != separatorChar){
        tmpDir += separatorChar;
      }
      
      if (suffix == null){
        suffix = ".tmp";
      }
    } else {
      tmpDir = dir.getPath();
    }
    
    return new File(tmpDir + prefix + suffix);
  }
  
  public static File createTempFile(String prefix, String suffix) throws IOException  {
    return createTempFile(prefix, suffix, null);
  }
}
