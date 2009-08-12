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

  private static JPFSite site;


  private File siteCoreDir;

  //--- our public interface

  public static JPFSite getSite() {
    if (site == null){
      site = new JPFSite();
    }

    return site;
  }

  private JPFSite() {

  }



  //////////////////////////////////////////////////////////////////////////////



  /**
   * this should be more efficient than using Properties.load(), and it's
   * restricted parsing anyways (we only do system property expansion)
   */
  public File getSiteCoreDir (){

    if (siteCoreDir == null){
      char sc = File.separatorChar;
      File userHome = new File(System.getProperty("user.home"));
      String siteProps = userHome.getAbsolutePath() + sc + ".jpf" + sc + "site.properties";

      Pattern corePattern = Pattern.compile("^ *jpf.core *= *(.+?) *$");
      String coreDirPath = null;

      try {
        FileReader fr = new FileReader(siteProps);
        BufferedReader br = new BufferedReader(fr);

        for (String line = br.readLine(); line != null; line = br.readLine()) {
          Matcher m = corePattern.matcher(line);
          if (m.matches()) {
            coreDirPath = expand(m.group(1));
            break;
          }
        }
        br.close();

      } catch (FileNotFoundException fnfx) {
        return null;
      } catch (IOException iox) {
        return null;
      }

      siteCoreDir = new File(coreDirPath);
    }

    return siteCoreDir;
  }


  /**
   * simple non-recursive global system property expander
   */
  protected String expand (String s) {
    int i, j = 0;
    if (s == null || s.length() == 0) {
      return s;
    }

    while ((i = s.indexOf("${", j)) >= 0) {
      if ((j = s.indexOf('}', i)) > 0) {
        String k = s.substring(i + 2, j);
        String v = System.getProperty(k);

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
}
