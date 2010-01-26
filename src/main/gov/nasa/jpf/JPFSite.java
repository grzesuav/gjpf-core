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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
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

  static final String[] SITE_LOCATIONS = { "jpf", ".jpf" };
  static final String SITE_PROPERTIES = "site.properties";
  static final String PROJECT_PROPERTIES = "jpf.properties";

  private static JPFSite site;

  private File siteProps;
  private File siteCoreDir;

  //--- our public interface

  public static JPFSite getSite(String[] args) {
    if (site == null){
      site = new JPFSite(args);
    }

    return site;
  }

  private JPFSite(String[] args) {
    // first, check if we have an explicit +site=<file> argument
    File props = getSiteFromArgs(args);

    // if not, check if we have an application property file that sets a site property
    if (props == null){
      props = getSiteFromApp(args);
    }

    // if not, check if we have a project property that does
    if (props == null){
      props = getSiteFromProject();
    }

    // if all that fails, look at the known default locations
    if (props == null){
      props = getSiteFromDirs();
    }

    siteProps = props;
  }

  //////////////////////////////////////////////////////////////////////////////

  public File getSiteCoreDir (){
    if (siteCoreDir == null){
      if (siteProps != null){
        siteCoreDir = getMatchFromFile(siteProps, "jpf-core");
      }
    }

    return siteCoreDir;
  }

  File getSiteFromArgs(String[] args){
    if (args != null && args.length > 0){
      for (String a : args) {
        if ((a != null) && a.startsWith("+site=")) {
          return new File(a.substring(6));
        }
      }
    }

    return null;
  }

  File getSiteFromApp(String[] args){
    if (args != null && args.length > 0){
      String lastArg = args[args.length-1];
      if (lastArg != null && lastArg.endsWith(".jpf")){
        return getMatchFromFile(lastArg, "site");
      }
    }
    return null;
  }

  File getSiteFromProject() {
    File f = new File(PROJECT_PROPERTIES);
    if (f.isFile()){
      return getMatchFromFile(f, "site");
    }

    return null;
  }

  File getSiteFromDirs () {
    File userHome = new File(System.getProperty("user.home"));

    for (String dir : SITE_LOCATIONS) {
      File siteDir = new File(userHome, dir);
      if (siteDir.isDirectory()) {
        File siteProps = new File(siteDir, SITE_PROPERTIES);
        if (siteProps.isFile()) {
          return siteProps;
        }
      }
    }

    return null;
  }

  static Pattern pattern = Pattern.compile("^[ \t]*([^# \t][^ \t]*)[ \t]*=[ \t]*(.+?)[ \t]*$");


  File getMatchFromFile (String pathName, String key){
    File f = new File(pathName);
    if (f.isFile()){
      return getMatchFromFile(f, key);
    } else {
      return null;
    }
  }

  // minimal parsing - only local key and and config_path expansion
  File getMatchFromFile (File propFile, String lookupKey){
    String path = null;

    HashMap<String, String> map = new HashMap<String, String>();
    String dir = propFile.getParent();
    if (dir == null) {
      dir = ".";
    }
    map.put("config_path", dir);

    try {
      FileReader fr = new FileReader(propFile);
      BufferedReader br = new BufferedReader(fr);

      for (String line = br.readLine(); line != null; line = br.readLine()) {
        Matcher m = pattern.matcher(line);
        if (m.matches()) {
          String key = m.group(1);
          String val = m.group(2);

          val = expand(val, map);

          if ((key.length() > 0) && (val.length() > 0)) {
            // check for continuation lines
            if (val.charAt(val.length() - 1) == '\\') {
              val = val.substring(0, val.length() - 1).trim();
              for (line = br.readLine(); line != null; line = br.readLine()) {
                line = line.trim();
                int len = line.length();
                if ((len > 0) && (line.charAt(len - 1) == '\\')) {
                  line = line.substring(0, line.length() - 1).trim();
                  val += expand(line, map);
                } else {
                  val += expand(line, map);
                  break;
                }
              }
            }

            if (lookupKey.equals(key)) {
              path = val;
              break;
            } else {
              if (key.charAt(key.length() - 1) == '+') {
                key = key.substring(0, key.length() - 1);
                String v = map.get(key);
                if (v != null) {
                  val = v + val;
                }
              } else if (key.charAt(0) == '+') {
                key = key.substring(1);
                String v = map.get(key);
                if (v != null) {
                  val = val + v;
                }
              }
              map.put(key, val);
            }
          }
        }
      }
      br.close();

    } catch (FileNotFoundException fnfx) {
      return null;
    } catch (IOException iox) {
      return null;
    }

    if (path != null){
      return new File(path);
    } else {
      return null;
    }
  }

  /**
   * simple non-recursive, local key and system property expander
   */
  static String expand (String s, HashMap<String,String> map) {
    int i, j = 0;
    if (s == null || s.length() == 0) {
      return s;
    }

    while ((i = s.indexOf("${", j)) >= 0) {
      if ((j = s.indexOf('}', i)) > 0) {
        String k = s.substring(i + 2, j);
        String v = null;

        if (map != null){
          v = map.get(k);
        }
        if (v == null){
          v = System.getProperty(k);
        }

        if (v != null) {
          s = s.substring(0, i) + v + s.substring(j + 1, s.length());
          j = i + v.length();
        } else {
          s = s.substring(0, i) + s.substring(j + 1, s.length());
          j = i;
        }
      }
    }

    return s;
  }


  /**
  public static void main (String[] args){
    JPFSite site = new JPFSite();
    site.getSiteCoreDir();
  }
  **/
}
