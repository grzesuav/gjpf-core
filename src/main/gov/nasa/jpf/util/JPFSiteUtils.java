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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * utility class for JPF site configuration related functions
 */
public class JPFSiteUtils {

  //--- preparse support - we need this if we use app properties to locat lower level property files

  static Pattern keyValPattern = Pattern.compile("^[ \t]*([^# \t][^ \t]*)[ \t]*=[ \t]*(.+?)[ \t]*$");

  /**
   * minimal parsing - only local key, system property and and config_path expansion
   * NOTE this stops after finding the key, and it doesn't add the file to the 'sources'
   */
  public static String getMatchFromFile (String pathName, String lookupKey){
    String value = null;
    Pattern lookupPattern = Pattern.compile(lookupKey);

    File propFile = new File(pathName);
    if (!propFile.isFile()){
      return null;
    }

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
        Matcher m = keyValPattern.matcher(line);
        if (m.matches()) {
          String key = m.group(1);
          String val = m.group(2);

          val = expandLocal(val, map);

          if ((key.length() > 0) && (val.length() > 0)) {
            // check for continuation lines
            if (val.charAt(val.length() - 1) == '\\') {
              val = val.substring(0, val.length() - 1).trim();
              for (line = br.readLine(); line != null; line = br.readLine()) {
                line = line.trim();
                int len = line.length();
                if ((len > 0) && (line.charAt(len - 1) == '\\')) {
                  line = line.substring(0, line.length() - 1).trim();
                  val += expandLocal(line, map);
                } else {
                  val += expandLocal(line, map);
                  break;
                }
              }
            }

            Matcher lookupMatcher = lookupPattern.matcher(key);
            if (lookupMatcher.matches()) {
              value = val;
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

    return value;
  }

  // simple non-recursive, local key and system property expander
  private static String expandLocal (String s, HashMap<String,String> map) {
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
   * get location of jpf-core from site.properties
   * @return null if it doesn't exist
   */
  public static File getSiteCoreDir() {
    String userHome = System.getProperty("user.home");
    File f = new File( userHome, "jpf/site.properties");
    if (!f.isFile()){
      f = new File( userHome, ".jpf/site.properties");
      if (!f.isFile()){
        return null;
      }
    }

    String path = getMatchFromFile(f.getAbsolutePath(), "jpf-core");
    if (path != null){
      File coreDir = new File(path);
      if (coreDir.isDirectory()){
        return coreDir;
      }
    }

    return null;
  }

  /**
   * find project properties (jpf.properties) from current dir
   */
  public static File getCurrentProjectProperties() {
    File d = new File(System.getProperty("user.dir"));
    do {
      File f = new File(d, "jpf.properties");
      if (f.isFile()){
        return f;
      }
      d = d.getParentFile();
    } while (d != null);

    return null;
  }


  static Pattern idPattern = Pattern.compile("^[ \t]*([^# \t][^ \t]*)[ \t]*=[ \t]*\\$\\{config_path\\}");

  static String projectId;

  /**
   * look for a "<id> = ${config_path}" entry in current dir/jpf.properties
   * this looks recursively upwards
   * @return null if no jpf.properties found
   */
  public static String getCurrentProjectId (){
    if (projectId == null) {
      File propFile = getCurrentProjectProperties();

      if (propFile != null) {
        try {
          FileReader fr = new FileReader(propFile);
          BufferedReader br = new BufferedReader(fr);

          for (String line = br.readLine(); line != null; line = br.readLine()) {
            Matcher m = idPattern.matcher(line);
            if (m.matches()) {
              projectId = m.group(1);
            }
          }
          br.close();

        } catch (FileNotFoundException fnfx) {
          return null;
        } catch (IOException iox) {
          return null;
        }
      }
    }

    return projectId;
  }
}
