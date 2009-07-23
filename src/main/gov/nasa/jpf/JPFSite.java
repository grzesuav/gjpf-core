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

  static Pattern jarCmdPattern = Pattern.compile("Run.*\\.jar");
  static Pattern srcPattern = Pattern.compile(".*src[\\/]([^.\\/]*)[.\\/].*");

  private static JPFSite site;

  File bootEntry;
  File jpfCore;

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
    analyzeClasspath();
//printSite();
  }

  public void addExtensionDir (String extension){
    processJPFComponentDir(extension);
  }

  
  //--- the getters

  public File getBootEntry() {
    return bootEntry;
  }

  public File getJPFCore () {
    return jpfCore;
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
    System.out.println("  coreBootEntry: " + ((bootEntry != null) ? bootEntry.getAbsolutePath() : "null"));

    System.out.println("  jpfCore: " + ((jpfCore != null) ? jpfCore.getAbsolutePath() : "null"));

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

  /**
   * find the classpath component that loads JPF, which could be either
   *
   *  - the build/main dir that holds gov.nasa.jpf.JPF
   *  - jpf.jar - which is self contained, i.e. it doesn't matter where it is
   *  - Run*.jar - which can be either in the core dir or an extension dir
   *
   * based on the bootEntry, find the corresponding core dir (if any)
   */
  protected void analyzeClasspath() {
    String cp = System.getProperty("java.class.path");
    String[] cpEntries = cp.split(File.pathSeparator);

    for (String p : cpEntries) {
      File f = new File(p);
      String name = f.getName();

      if (name.endsWith(".jar")){

        if (name.equals("jpf.jar")){
          // that's easy - core dir in this case is the dir holding jpf.jar
          // we add all the jars we find in the same location, but we
          // DO NOT use anything outside this dir - using jpf.jar means
          // imperatively setting the core libs w/o external dependencies
          bootEntry = f;
          jpfCore = f;
          addJars(getParentFile(f));


        } else if (name.startsWith("Run")){
          // RunJPF, RunTest, RunExample,.. - our minimal command jars that
          // are distributed with extensions
          bootEntry = f;

          // this might be outside the core (in an extension) - we collect what
          // is in the containing dir (might override jpf-core classes)
          File jpfRoot = findJPFRootFromJar(f);
          if (jpfRoot != null){
            processJPFComponentDir(jpfRoot.getPath());

            if (isJPFCoreDir(jpfRoot)){
              jpfCore = jpfRoot;
            }
          }
        }
        
      } else if (f.isDirectory()){
                
        if (name.equals("main") || name.equals("peers")) {
          File parent = getParentFile(f);
          if (parent.getName().equals("build")){
            // .../build/main or .../build/peers dir, but we still have to check
            // if it's the one from jpf-core

            File jpfRoot = getParentFile(parent);
            if (isJPFCoreDir(jpfRoot)) {
              bootEntry = f;

              processJPFComponentDir(jpfRoot.getPath());
              jpfCore = jpfRoot;

            } else {
              // an extension, so we add this, but we still need the core
              processJPFComponentDir(jpfRoot.getPath());
            }
          }
        }
      }
    }

    if (jpfCore == null){
      File jpfRoot = getSitePropertyCoreLoc();
      if (jpfRoot != null){
        processJPFComponentDir(jpfRoot.getPath());
        jpfCore = jpfRoot;
      } else {
        // we are in trouble
      }
    }
  }


  // find out if a given jar resides in a JPF component project
  // return the root dir of this project or 'null' if this is not within
  // a JPF core or extension
  protected File findJPFRootFromJar (File cpEntry){
    File parent = getParentFile(cpEntry); // could be the root or dist/lib/tools/bin

    // first check if parent is already the root
    if (isJPFComponentDir(parent)){
      return parent;
    }

    // if not, look at our parent's parent, but no further
    parent = getParentFile(parent);
    if (isJPFComponentDir(parent)){
      return parent;
    }

    return null;
  }

  // look for any of the build/main, build/classes, build/peers, which are
  // the artifacts of JPF components (core or extension)
  protected boolean isJPFComponentDir (File dir){

    File buildDir = new File(dir,"build");
    if (buildDir.isDirectory()){
      File d = new File(buildDir, "main");
      if (d.isDirectory()){
        return true;
      }

      d = new File(buildDir, "classes");
      if (d.isDirectory()){
        return true;
      }

      d = new File(buildDir, "peers");
      if (d.isDirectory()){
        return true;
      }
    }

    return false;
  }

  static final char sc = File.separatorChar;
  static final String jpfClass = "build" + sc + "main" + sc + "gov" + sc +
            "nasa" + sc + "jpf" + sc + "JPF.class";

  // we consider this the core dir if we find the JPF class itself in
  // the build/main subdir
  protected boolean isJPFCoreDir (File dir){
    char sc = File.separatorChar;
    File jpfCls = new File( dir, jpfClass);

    return jpfCls.isFile();
  }

  /**
   * locate the jpf-core in use, which could be either
   *
   * - the current dir
   */
  protected File findCoreDir() {
    if (bootEntry != null) {
      File parent = bootEntry.getParentFile();
      if (parent == null){ // must be the current dir
        parent = new File(System.getProperty("user.dir"));
      }

      if (bootEntry.isDirectory()) {
        if (parent.getName().equals("build")) {
          parent = parent.getParentFile();
          return parent == null ? getCurrentDir() : parent;
        } else {
          return parent;
        }

      } else { // it was a jar
        if (haveJPFjar(bootEntry)){
          if (parent.getName().equals("dist")) {
            parent = parent.getParentFile();
            return parent == null ? getCurrentDir() : parent;
          } else {
            return parent;
          }
        }
      }
    }

    // the fallback is to get this from ~/.jpf/site.properties
    return getSitePropertyCoreLoc();
  }


  protected File getParentFile(File f){
    File parent = f.getParentFile();
    if (parent == null){
      parent = new File(System.getProperty("user.dir"));
    }
    return parent;
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

    if (siteProp.isFile()){
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

  protected boolean isJarCommand (String name){
    return jarCmdPattern.matcher(name).matches();
  }

  protected boolean haveJPFjar (File jarFile){
    String name = jarFile.getName();
    if ("jpf.jar".equals(name)){
      return true;

    } else if (isJarCommand(name)) {
      File jpfJar = new File(jarFile.getParent(), "jpf.jar");
      return jpfJar.exists();
    }

    return false;
  }


  File getCurrentDir() {
    return new File(System.getProperty("user.dir"));
  }

  /**
   * check for the standard classpath and vm.classpath locations within
   * the provided  dir.
   */
  protected void processJPFComponentDir(String dir) {
    boolean haveClassfileDirs = false;

    // first, look if we have a source distrib with
    //  - build/main, build/peers => native classpath
    //  - build/classes, build/annotations => classpath
    File buildDir = new File(dir, "build");
    if (buildDir.isDirectory()) {
      haveClassfileDirs |= addDir(nativeCpEntries, new File(buildDir, "main"));
      haveClassfileDirs |= addDir(nativeCpEntries, new File(buildDir, "peers"));

      haveClassfileDirs |= addDir(jpfCpEntries, new File(buildDir, "classes"));
      haveClassfileDirs |= addDir(jpfCpEntries, new File(buildDir, "annotations"));
    }

    // if we have a src dir, add it to sourcpath
    File srcDir = new File(dir,"src");
    if (srcDir.isDirectory()){
      addDir(jpfSpEntries, new File(srcDir, "classes"));
    }

    // if it's not a source distrib, collect jars from 'dist' (if exists)
    if (!haveClassfileDirs) {
      addJars(new File(dir, "dist"));
    }

    // add jars from the 'lib' dir
    addJars(new File(dir, "lib"));

    // lastly, add all jars that are in the dir itself
    addJars(new File(dir));
  }


  // add all the jars in this dir to the nativeCp, except of *-classes.jar,
  // which should be added to the jpfCp, and *-src.jar, which goes into jpfSp
  protected void addJars (File dir) {

    if (dir.isDirectory()) {
      for (File f : dir.listFiles()) {
        String name = f.getName();
        if (name.endsWith(".jar")) {
          if (name.endsWith("-classes.jar")) {
            addIfAbsent(jpfCpEntries,f);

          } else if (name.endsWith("-src.jar")) {
            addIfAbsent(jpfSpEntries,f);

          } else if (name.endsWith("-annotations.jar")){
            // we assume these are also in ..-classes.jar

          } else {
            if (!isJarCommand(name)) { // don't add the RunX command jars
              addIfAbsent(nativeCpEntries,f);
            }
          }
        }
      }
    }
  }

  protected boolean addDir(List<File> entries, File dir) {
    if (dir.isDirectory()) {
      addIfAbsent(entries,dir);
      return true;
    }

    return false;
  }

  protected void addIfAbsent (List<File> list, File element) {
    String absPath = element.getAbsolutePath();
    for (File f : list){
      if (f.getAbsolutePath().equals(absPath)){
        return;
      }
    }

    list.add(element);
  }
}
