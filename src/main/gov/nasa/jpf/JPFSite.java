//
// Copyright (C) 2009 United States Government as represented by the
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
package gov.nasa.jpf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * class to analyze the local installation of JPF (core and extensions), to
 * determine required classpath entries for both the host VM and JPF itself.
 *
 * This is based on a couple of assumptions:
 *
 * (1) each JPF component project (core and extensions) is structured like
 *   <root>
 *     build
 *       main              => native_classpath
 *       peers             => native_classpath
 *       classes           => classpath
 *       annotations       => classpath
 *     src (same as build)
 *       classes           => sourcepath
 *     lib
 *       *.jar             => classpath
 *     dist
 *       *.jar             => native_classpath
 *       *-classes.jar     => classpath
 *       *-annotations.jar => classpath
 *
 * (2) any of the 'build' dirs are optional, i.e. might not be present in
 * a given extension
 *
 * (3) for a source distrib (project installation), 'build' dir contents
 * (*.class files) have precedence over dist/*.jar files
 * 
 * (4) binary distributions can be either slices (just containing dist and
 * lib dirs), or might just contain jars in a flat structure like:
 *   <root>
 *     *.jar               => native_classpath (either lib or JPF jar)
 *     *-classes.jar       => classpath
 *     *-annotations.jar   => classpath
 *
 *
 * NOTE! - this class is (partially) used by JPFClassLoader, i.e. it SHOULD NOT
 * contain any references to classes which might have to be loaded by it
 */
public class JPFSite {

  private static JPFSite site;

  File coreBootEntry;
  File coreDir;

  List<File> extensionRoots = new ArrayList<File>();

  List<File> nativeCpEntries = new ArrayList<File>();

  List<File> jpfCpEntries = new ArrayList<File>();
  List<File> jpfSpEntries = new ArrayList<File>();


  //--- our public interface

  public static JPFSite getSite() {
    if (site == null){
      site = new JPFSite();
    }

    return site;
  }

  private JPFSite() {
    coreBootEntry = findJPFCoreLib();
    coreDir = findCoreDir();

    processJPFComponentDir(coreDir.getAbsolutePath());
  }

  public void addExtensionDir (String extension){
    processJPFComponentDir(extension);
  }


  //--- the getters

  public File getCoreBootEntry() {
    return coreBootEntry;
  }

  public File getCoreDir() {
    return coreDir;
  }

  public File[] getNativeCpEntries() {
    return nativeCpEntries.toArray(new File[nativeCpEntries.size()]);
  }

  public URL[] getNativeCpURLs() {
    int i = 0;

    URL[] urls = new URL[nativeCpEntries.size()];
    for (File f : nativeCpEntries){
      try {
        urls[i++] = f.toURI().toURL();
      } catch (MalformedURLException x){
        // can't happen - it's an existing file
      }
    }

    return urls;
  }

  public File[] getJPFCpEntries() {
    return jpfCpEntries.toArray(new File[jpfCpEntries.size()]);
  }

  public File[] getJPFSpEntries() {
    return jpfSpEntries.toArray(new File[jpfSpEntries.size()]);
  }


  //--- internal stuff

  // for debugging purposes
  public void printSite() {
    System.out.println("JPFSite :");
    System.out.println("  coreBootEntry: " + ((coreBootEntry != null) ? coreBootEntry.getAbsolutePath() : "null"));
    System.out.println("  coreDir: " + ((coreDir != null) ? coreDir.getAbsolutePath() : "null"));

    System.out.println("  nativeCpEntries:");
    for (File f : nativeCpEntries){
      System.out.println("    " + f.getAbsolutePath());
    }

    System.out.println("  jpfCpEntries:");
    for (File f : jpfCpEntries){
      System.out.println("    " + f.getAbsolutePath());
    }

    System.out.println("  jpfSpEntries:");
    for (File f : jpfSpEntries){
      System.out.println("    " + f.getAbsolutePath());
    }
  }

  protected File findJPFCoreLib() {
    String cp = System.getProperty("java.class.path");
    String[] cpEntries = cp.split(File.pathSeparator);

    char sc = File.separatorChar;
    String jpfClass = "gov" + sc + "nasa" + sc + "jpf" + sc + "JPF.class";


    for (String p : cpEntries) {
      File f = new File(p);
      String name = f.getName();
      if (name.equals("jpf.jar")){
        return f;

      } else if (name.equals("jpf-launch.jar")){
        return f;
        
      } else if (name.equals("main")){
        if (f.getParentFile().getName().equals("build")) {
          // check if there is a gov/nasa/jpf/JPF class there
          File jpfClassfile = new File(f.getPath() + sc + jpfClass);
          if (jpfClassfile.exists()){
            return f;
          }
        }
      }
    }

    return null;
  }

  /**
   * not terribly nice - we have to guess our site property location
   * since Config is not yet available. We can't just pull it up, because
   * it needs to locate defaults.properties, which we load via the CP of
   * gov.nasa.jpf.JPF, which in turn is what we want to locate here. Note that
   * it also means that the jpf.core value cannot use property expansion
   * (which standard Java properties don't know about)
   */
  protected File getSitePropertyCoreLoc(){
    char sc = File.separatorChar;
    String userHome = System.getProperty("user.home");
    File siteProp = new File(userHome + sc + ".jpf" + sc + "site.properties");

    if (siteProp.exists() && siteProp.isFile()){
      Properties sp = new Properties();
      try {
        sp.load(new FileInputStream(siteProp));
        String coreProp = sp.getProperty("jpf.core");
        if (coreProp != null && !coreProp.isEmpty()){
          File coreLoc = new File(coreProp);
          return coreLoc;
        }

      } catch (FileNotFoundException fnfx){
        // can't happen, we already checked
      } catch (IOException iox){
        // Hmm, not readable
      }
    }

    return null;
  }

  protected File findCoreDir() {
    if (coreBootEntry != null) {
      File parent = coreBootEntry.getParentFile();

      if (coreBootEntry.isDirectory()) {
        if (parent.getName().equals("build")) {
          parent = parent.getParentFile();
          return parent == null ? getCurrentDir() : parent;
        } else {
          return parent;
        }

      } else { // it was a jar, but which one, get core dir from site prop
        if (coreBootEntry.getName().equals("jpf-launch.jar")) {
          return getSitePropertyCoreLoc();

        } else { // must be jpf.jar, deduce core dir from path
          if (parent.getName().equals("dist")) {
            parent = parent.getParentFile();
            return parent == null ? getCurrentDir() : parent;
          } else {
            return parent;
          }
        }
      }
      
    } else {
      return getSitePropertyCoreLoc();
      // <2do> what about running within IDEs?
    }
  }

  File getCurrentDir() {
    return new File(System.getProperty("user.dir"));
  }

  /**
   * check for the standard classpath and vm.classpath locations within
   * the provided extension dir.
   */
  protected void processJPFComponentDir(String dir) {
    boolean haveClassDirs = false;

    // first, look if we have a source distrib with
    //  - build/main, build/peers => 'classpath'
    //  - build/classes, build/annotations => 'vm.classpath'
    File buildDir = new File(dir, "build");
    if (buildDir.exists() && buildDir.isDirectory()) {
      haveClassDirs |= addDir(nativeCpEntries, new File(buildDir, "main"));
      haveClassDirs |= addDir(nativeCpEntries, new File(buildDir, "peers"));

      haveClassDirs |= addDir(jpfCpEntries, new File(buildDir, "classes"));
      haveClassDirs |= addDir(jpfCpEntries, new File(buildDir, "annotations"));
    }

    File srcDir = new File(dir,"src");
    if (srcDir.exists() && srcDir.isDirectory()){
      addDir(jpfSpEntries, new File(srcDir, "classes"));
    }

    // if it's not a source distrib, collect jars from 'dist', or the dir itself
    if (!haveClassDirs) {
      addJars(new File(dir, "dist"));
    }

    // add jars from the 'lib' dir
    addJars(new File(dir, "lib"));

    // lastly, add all jars that are in the dir itself
    addJars(new File(dir));
  }

  static Pattern ignorePattern = Pattern.compile("(.+-annotations|.+-launch).jar");

  protected void addJars (File dir) {

    if (dir.exists() && dir.isDirectory()) {
      for (File f : dir.listFiles()) {
        String name = f.getName();
        if (name.endsWith(".jar")) {
          if (name.endsWith("-classes.jar")) {
            jpfCpEntries.add(f);

          } else {
            if (!ignorePattern.matcher(name).matches()) {

              // if this is not the first entry, we already have the core in the CP
              if (name.equals("jpf.jar") && !nativeCpEntries.isEmpty()){
                return;
              }

              // don't add the same jar twice
              for (File e : nativeCpEntries){
                if (name.equals(e.getName())){
                  return;
                }
              }

              nativeCpEntries.add(f);
            }
          }
        }
      }
    }
  }

  protected boolean addDir(List<File> entries, File dir) {
    if (dir.exists() && dir.isDirectory()) {
      entries.add(dir);
      return true;
    }

    return false;
  }
}
