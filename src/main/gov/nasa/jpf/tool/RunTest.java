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

package gov.nasa.jpf.tool;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFClassLoader;
import gov.nasa.jpf.util.FileUtils;
import gov.nasa.jpf.util.JPFSiteUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * tool to run JPF test with configured classpath
 *
 * arguments are supposed to be of type
 *
 *   {<config-option>} <JPF-test-class> {<test-method>}
 *
 * all leading config options are used to create the initial Config, but be
 * aware of that each test (TestJPF.verifyX() invocation) uses its own
 * Config and JPF object, i.e. can have different path settings
 *
 * This automatically adds <project>.test_classpath to the startup classpath
 */
public class RunTest extends Run {

  public static final int HELP  = 0x1;
  public static final int SHOW  = 0x2;
  public static final int LOG   = 0x4;
  
  static Config config;

  public static Config getConfig(){
    return config;
  }

  public static class Failed extends RuntimeException {
    public Failed (){
    }
  }

  public static int getOptions (String[] args){
    int mask = 0;

    if (args != null){

      for (int i = 0; i < args.length; i++) {
        String a = args[i];
        if ("-help".equals(a)){
          args[i] = null;
          mask |= HELP;

        } else if ("-show".equals(a)) {
          args[i] = null;
          mask |= SHOW;

        } else if ("-log".equals(a)){
          args[i] = null;
          mask |= LOG;

        }
      }
    }

    return mask;
  }

  public static boolean isOptionEnabled (int option, int mask){
    return ((mask & option) != 0);
  }

  public static void showUsage() {
    System.out.println("Usage: \"java [<vm-option>..] -jar ...RunTest.jar [<jpf-option>..] [<app> [<app-arg>..]]");
    System.out.println("  <jpf-option> : -help : print usage information and exit");
    System.out.println("               | -log : print configuration initialization steps");
    System.out.println("               | -show : print configuration dictionary contents");    
    System.out.println("               | +<key>=<value>  : add or override <key>/<value> pair to global config");
    System.out.println("               | +test.<key>=<value>  : add or override <key>/<value> pair in test config");
    System.out.println("  <app>        : *.jpf application properties file pathname | fully qualified application class name");
    System.out.println("  <app-arg>    : arguments passed into main() method of application class");
  }
  
  public static void main(String[] args){
    int options = getOptions( args);
    
    if (isOptionEnabled(HELP, options)) {
      showUsage();
      return;
    }

    if (isOptionEnabled(LOG, options)) {
      Config.enableLogging(true);
    }

    config = new Config(args);

    if (isOptionEnabled(SHOW, options)) {
      config.printEntries();
    }
    
    args = removeConfigArgs( args);
    String testClsName = getTestClassName(args);

    if (testClsName != null) {
      testClsName = checkClassName(testClsName);

      try {
        JPFClassLoader cl = config.initClassLoader(RunTest.class.getClassLoader());

        addTestClassPath(cl, config);

        Class<?> testJpfCls = cl.loadClass("gov.nasa.jpf.util.test.TestJPF");
        Class<?> testCls = cl.loadClass(testClsName);

        if (testJpfCls.isAssignableFrom(testCls)) {
          String[] testArgs = getTestArgs(args);

          try {
            try { // check if there is a main(String[]) method
              Method mainEntry = testCls.getDeclaredMethod("main", String[].class);
              mainEntry.invoke(null, (Object)testArgs);
            
            } catch (NoSuchMethodException x){ // no main(String[]), call TestJPF.runTests(testCls,args) directly
              Method mainEntry = testJpfCls.getDeclaredMethod("runTests", Class.class, String[].class);
              mainEntry.invoke( null, new Object[]{testCls, testArgs});
            }
          } catch (NoSuchMethodException x){
            error("no suitable main() or runTests() in " + testCls.getName());
          } catch (IllegalAccessException iax){
            error( iax.getMessage());
          }

        } else {
          error("not a gov.nasa.jpf.util.test.TestJPF derived class: " + testClsName);
        }

      } catch (NoClassDefFoundError ncfx) {
        error("class did not resolve: " + ncfx.getMessage());

      } catch (ClassNotFoundException cnfx) {
        error("class not found " + cnfx.getMessage() + ", check <project>.test_classpath in jpf.properties");

      } catch (InvocationTargetException ix) {
        Throwable cause = ix.getCause();
        if (cause instanceof Failed){
          // no need to report - the test did run and reported why it failed
          System.exit(1);
        } else {
          error(ix.getCause().getMessage());
        }
      }

    } else {
      error("no test class specified");
    }
  }

  static boolean isPublicStatic (Method m){
    int mod = m.getModifiers();
    return ((mod & (Modifier.PUBLIC | Modifier.STATIC)) == (Modifier.PUBLIC | Modifier.STATIC));
  }
  
  static void addTestClassPath (JPFClassLoader cl, Config conf){
    // since test classes are executed by both the host VM and JPF, we have
    // to tell the JPFClassLoader where to find them
    String projectId = JPFSiteUtils.getCurrentProjectId();
    if (projectId != null) {
      String testCpKey = projectId + ".test_classpath";
      String[] tcp = config.getCompactTrimmedStringArray(testCpKey);
      if (tcp != null) {
        for (String pe : tcp) {
          try {
            cl.addURL(FileUtils.getURL(pe));
          } catch (Throwable x) {
            error("malformed test_classpath URL: " + pe);
          }
        }
      }
    }
  }

  static boolean isOptionArg(String a){
    if (a != null && !a.isEmpty()){
      char c = a.charAt(0);
      if ((c == '+') || (c == '-')){
        return true;
      }
    }
    return false;
  }
  
  static String getTestClassName(String[] args){
    for (int i=0; i<args.length; i++){
      String a = args[i];
      if (a != null && !isOptionArg(a)){
        return a;
      }
    }

    return null;
  }

  // return everything after the first free arg
  static String[] getTestArgs(String[] args){
    int i;

    for (i=0; i<args.length; i++){
      String a = args[i];
      if (a != null && !isOptionArg(a)){
        break;
      }
    }

    if (i >= args.length-1){
      return new String[0];
    } else {
      String[] testArgs = new String[args.length-i-1];
      System.arraycopy(args,i+1, testArgs, 0, testArgs.length);
      return testArgs;
    }
  }


}
