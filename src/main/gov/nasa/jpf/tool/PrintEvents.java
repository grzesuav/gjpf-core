//
// Copyright (C) 2014 United States Government as represented by the
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

package gov.nasa.jpf.tool;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFClassLoader;
import gov.nasa.jpf.util.FileUtils;
import gov.nasa.jpf.util.JPFSiteUtils;
import gov.nasa.jpf.util.event.EventTree;


/**
 * very simple tool to print .util.script.EventTrees
 * 
 * <2do> this should use native_classpath / JPFClassLoader to load the EventTree
 */
public class PrintEvents {

  static boolean printTree;
  static boolean printPaths;
  static String clsName;
  
  static void showUsage () {
    System.out.println("usage:   'PrintEvents [<option>..] <className>'");
    System.out.println("options:  -t  : print tree");
    System.out.println("          -p  : print paths");
  }

  static boolean readOptions (String[] args) {
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      if ("-t".equals(arg)) {
        printTree = true;
      } else if ("-p".equals(arg)) {
        printPaths = true;
      } else if (arg.charAt(0) != '-') {
          clsName = arg;
          if (clsName.charAt(0) == '.'){
            clsName = "gov.nasa.jpf" + clsName;
          }
      } else {
        System.err.println("unknown option: " + arg);
        showUsage();

        return false;
      }
    }

    return (clsName != null);
  }

  static String[] getTestPathElements (Config config){
    String projectId = JPFSiteUtils.getCurrentProjectId();
    
    if (projectId != null) {
      String testCpKey = projectId + ".test_classpath";
      return  config.getCompactTrimmedStringArray(testCpKey);
      
    } else {
      return new String[0];
    }    
  }
  
  static void addTestClassPath (JPFClassLoader cl, String[] testPathElements){
    if (testPathElements != null) {
      for (String pe : testPathElements) {
        try {
          cl.addURL(FileUtils.getURL(pe));
        } catch (Throwable x) {
          System.err.println("malformed test_classpath URL: " + pe);
        }
      }
    }
  }
  
  public static void main (String[] args){
    if ((args.length == 0) || !readOptions(args)) {
      showUsage();
    }
     
    Config config = new Config(args);
    String[] testPathElements = getTestPathElements(config);
    JPFClassLoader cl = config.initClassLoader(RunTest.class.getClassLoader());
    addTestClassPath( cl, testPathElements);

    try {     
      Class<EventTree> cls = (Class<EventTree>)cl.loadClass(clsName);
      EventTree et = cls.newInstance();
      
      if (printTree){
        System.out.println("---------------- event tree of " + clsName);
        et.printTree();
      }
      
      if (printPaths){
        System.out.println("---------------- event paths of " + clsName);
        et.printPaths();
      }
    } catch (ClassNotFoundException cnfx){
      System.err.println("class not found: " + clsName);
    } catch (NoClassDefFoundError ncdf){
      System.err.println("class does not load: " + ncdf.getMessage());      
    } catch (InstantiationException ex) {
      System.err.println("cannot instantiate: " + ex.getMessage());      
    } catch (IllegalAccessException ex) {
      System.err.println("cannot instantiate: " + ex.getMessage());      
    }
  }
}
