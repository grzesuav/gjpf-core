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
package gov.nasa.jpf;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;


/**
 * class that encapsulates property-based JPF configuration. This is mainly an
 * associative array with various typed accessors, and a structured
 * initialization process. This implementation has the design constraint that it
 * does not promote symbolic information to concrete types, which means that
 * frequently accessed data should be promoted and cached in client classes.
 * This in turn means we assume the data is not going to change at runtime.
 * Major motivation for this mechanism is to avoid 'Option' classes that have
 * concrete type fields, and hence are structural bottlenecks, i.e. every
 * parameterized user extension (Heuristics, Scheduler etc.) require to update
 * this single class. Note that Config is also not thread safe with respect to
 * retrieving exceptions that occurred during instantiation
 *
 * Another important caveat for both implementation and usage of Config is that
 * it is supposed to be our master configuration mechanism, i.e. it is also used
 * to configure other core services like logging. This means that Config
 * initialization should not depend on these services. Initialization has to
 * return at all times, recording potential problems for later handling. This is
 * why we have to keep the Config data model and initialization fairly simple
 * and robust.
 *
 * Except of JPF and Config itself, all JPF classes are loaded by a
 * Classloader that is constucted by Config (e.g. by collecting jars from
 * known/configured locations), i.e. we SHOULD NOT rely on any 3rd party
 * libraries within Config. Ideally, only JPF should have to be in the
 * platform classpath (or the jpf.jar manifest)
 *
 *
 * PROPERTY SOURCES
 * ----------------
 *
 * (1) default.properties - as the name implies, these are version/system specific
 * defaults that come with the jpf-core installation and should not be changed
 *
 * (2) site.properties - this file specifies the location of the jpf-core and
 * installed extensions, like:
 *
 *     jpf.core = /Users/pcmehlitz/projects/jpf-v5/jpf-core
 *     ...
 *     # numeric extension
 *     ext.numeric = /Users/pcmehlitz/projects/jpf-v5/jpf-numeric
 *     extensions+=,${ext.numeric}
 *
 * (3) application properties - (formerly called mode property file) specifies
 * all the settings for a specific SUT run, esp. listener and target/target_args.
 * app properties can be specified as the sole JPF argument, i.e. instead of
 * a SUT classname
 *     ..
 *     target = x.Y.MySystemUnderTest
 *     target_args = one,two
 *     ..
 *     listener = z.MyListener
 *
 * (4) commandline properties - all start with '+', they can override all other props
 *
 *
 * LOOKUP ORDER
 * ------------
 *                       property lookup
 *   property type   :      spec             :  default
 *   ----------------:-----------------------:----------
 * |  default        :   +default            : "default.properties" via codebase
 * |                 :                       :
 * |  site           :   +site               : "${user.home}/.jpf/site.properties"
 * |                 :                       :
 * |  app            :   +app                : -
 * |                 :                       :
 * v  cmdline        :   +<key>=<val>        : -
 *
 * (1) if there is an explicit spec and the pathname does not exist, throw a
 * JPFConfigException
 *
 * (2) if the system properties cannot be found, throw a JPFConfigException
 *
 *
 * <2do> need to make NumberFormatException handling consistent - should always
 * throw an JPFConfigException, not silently returning the default value
 *
 */


@SuppressWarnings("serial")
public class Config extends Properties {

  static final String TARGET_KEY = "target";

  static final String TARGET_ARGS_KEY = "target_args";
  public static final String LIST_SEPARATOR = ",";
  
  static final String DELIMS = "[,]+";  // for String arrays
  
  static final Class<?>[] CONFIG_ARGTYPES = { Config.class };
  
  static final Class<?>[] NO_ARGTYPES = new Class<?>[0];
  static final Object[] NO_ARGS = new Object[0];

  static final String TRUE = "true";
  static final String FALSE = "false";

  ClassLoader loader = Config.class.getClassLoader();
  
  // where did we initialize from
  ArrayList<Object> sources = new ArrayList<Object>();
  
  List<ConfigChangeListener> changeListeners;
  

  // an [optional] hashmap to keep objects we want to be singletons
  HashMap<String,Object> singletons;
  
  final Object[] CONFIG_ARGS = { this };




  /*                       property lookup
   *   property type   :      spec             :  default
   *   ----------------:-----------------------:----------
   * |  default        :   +default            : "default.properties" via codebase
   * |                 :                       :
   * |  site           :   +site               : "${user.home}/.jpf/site.properties"
   * |                 :                       :
   * |  project*       :      -                : <ext-dir>|<cur-dir>/jpf.properties
   * |                 :                       :
   * |  app            :   +app                : -
   * |                 :                       :
   * v  cmdline        :   +<key>=<val>        : -
   *
   * (1) if there is an explicit spec and the pathname does not exist, throw a
   * Config.JPFConfigException
   *
   * (2) if the system properties cannot be found, throw a Config.JPFConfigException
   *
   */

  public Config (String[] args, Class<?> codeBase)  {
    String[] a = args.clone(); // we might nullify some of them

    //--- the JPF system (default) properties
    // we need the codeBase to find default.properties, which is the reason why
    // the Config object cannot be created before we know where to get the core
    // classes from
    loadDefaultProperties( getPathArg(a,"default"), codeBase);

    //--- the site properties
    String siteProperties = getPathArg(a, "site");
    if (siteProperties == null){ // could be configured in defaults
      siteProperties = getString("site");
    }
    if (siteProperties != null){
      loadProperties( siteProperties);
    }

    //--- get the project properties from the configured extensions / current dir
    loadProjectProperties();

    //--- the application properties
    String appProperties = getPathArg(a, "app");
    if (appProperties == null){ // wasn't specified as a key=value arg
      appProperties = getAppArg(a); // but maybe it's the targetArg
    }
    if (appProperties != null){
      loadProperties( appProperties);
    }

    //--- at last, the (rest of the) command line properties
    loadArgs(a);
  }


  /*
   * note that matching args are expanded and stored here, to avoid any
   * discrepancy whith value expansions (which are order-dependent)
   */
  protected String getPathArg (String[] args, String key){
    int keyLen = key.length();

    for (int i=0; i<args.length; i++){
      String a = args[i];
      if (a != null){
        int len = a.length();
        if (len > keyLen + 2){
          if (a.charAt(0) == '+' && a.charAt(keyLen+1) == '='){
            if (a.substring(1, keyLen+1).equals(key)){
              args[i] = null; // processed
              String val = expandString(key, a.substring(keyLen+2));
              setProperty(key, val);
              return val;
            }
          }
        }
      }
    }

    return null;
  }

  /*
   * if the first freeArg is a JPF application property filename, use this
   * as targetArg and set the "jpf.app" property accordingly
   */
  protected String getAppArg (String[] args){

    for (int i=0; i<args.length; i++){
      String a = args[i];
      if (a != null && a.length() > 0){
        switch (a.charAt(0)) {
          case '+': continue;
          case '-': continue;
          default:
            if (a.endsWith(".jpf")){
              String val = expandString("jpf.app", a);
              put("jpf.app", val);
              args[i] = null; // processed
              return val;
            }
        }
      }
    }

    return null;
  }



  protected void loadDefaultProperties (String fileName, Class<?> codeBase) {
    InputStream is = null;

    try {
      if (fileName == null){
        fileName = "default.properties";
      }

      // first, try to load from a file
      File f = new File(fileName);
      if (f.exists()) {
        sources.add(f);
        is = new FileInputStream(f);
      } else {
        // if there is no file, try to load as a resource (jar)
        Class<?> clazz = (codeBase != null) ? codeBase : Config.class;
        is = clazz.getResourceAsStream(fileName);
        if (is != null) {
          sources.add( clazz.getResource(fileName)); // a URL
        }
      }

      if (is != null) {
        load(is);
        is.close();
      } else {
        throw new JPFConfigException("default properties not found");
      }
    } catch (IOException iex) {
      throw new JPFConfigException("error loading default properties", iex);
    }

  }

  protected void setConfigPathProperties (String fileName){
    put("config", fileName);
    int i = fileName.lastIndexOf(File.separatorChar);
    if (i>=0){
      put("config_path", fileName.substring(0,i));
    } else {
      put("config_path", ".");
    }
  }


  protected boolean loadProperties (String fileName) {
    if (fileName != null && fileName.length() > 0) {
      try {
        // try to load from a file
        File f = new File(fileName);
        if (f.isFile()) {
          setConfigPathProperties(f.getAbsolutePath());
          sources.add(f);
          FileInputStream is = new FileInputStream(f);
          load(is);
          is.close();
          return true;
        }
      } catch (IOException iex) {
        throw new JPFConfigException("error loading properties: " + fileName, iex);
      }
    }

    return false;
  }

  protected void loadProjectProperties () {

    ArrayList<File> jpfDirs = new ArrayList<File>();

    // first, we check the current dir
    addJPFdirs(jpfDirs,new File(System.getProperty("user.dir")));

    // now we add everything we get from the current classpath
    // note that this contains the jpf-core in use
    addJPFdirsFromClasspath(jpfDirs);

    // and finally all the site configured extension dirs
    addJPFdirsFromSiteExtensions(jpfDirs);

    // now load all the jpf.property files we find in these dirs
    for (File dir : jpfDirs){
      loadProperties(new File(dir,"jpf.properties").getAbsolutePath());
    }
  }

  protected void addJPFdirs (ArrayList<File> jpfDirs, File dir){
    while (dir != null) {
      File jpfProp = new File(dir, "jpf.properties");
      if (jpfProp.isFile()) {
        addIfAbsent(jpfDirs, dir);
        return;       // we probably don't want recursion here
      }
      dir = getParentFile(dir);
    }
  }

  protected void addJPFdirsFromClasspath(ArrayList<File> jpfDirs) {
    String[] cpEntries = null;

    ClassLoader cl = Config.class.getClassLoader();
    if (cl instanceof JPFClassLoader){
      // in case it wasn't in the system classpath, this should now
      // contain the config classpath as the single element
      cpEntries = ((JPFClassLoader)cl).getClasspathEntries();
    }

    if (cpEntries == null || cpEntries.length == 0){ 
      String cp = System.getProperty("java.class.path");
      cpEntries = cp.split(File.pathSeparator);
    }

    for (String p : cpEntries) {
      File f = new File(p);
      File dir = f.isFile() ? getParentFile(f) : f;

      addJPFdirs(jpfDirs, dir);
    }
  }

  protected void addJPFdirsFromSiteExtensions (ArrayList<File> jpfDirs){
    String[] extensions = getCompactStringArray("extensions");
    if (extensions != null){
      for (String pn : extensions){
        addJPFdirs( jpfDirs, new File(pn));
      }
    }
  }

  protected boolean addIfAbsent(ArrayList<File> list, File f){
    String absPath = f.getAbsolutePath();
    for (File e : list){
      if (e.getAbsolutePath().equals(absPath)){
        return false;
      }
    }

    list.add(f);
    return true;
  }

  static File root = new File(File.separator);

  protected File getParentFile(File f){
    if (f == root){
      return null;
    } else {
      File parent = f.getParentFile();
      if (parent == null){
        parent = new File(f.getAbsolutePath());
        if (parent.getName().equals(root.getName())){
          return root;
        } else {
          return parent;
        }
      } else {
        return parent;
      }
    }
  }


  /*
   * argument syntax:
   *          {'+'<key>['='<val>'] | '-'<driver-arg>} {<free-arg>}
   *
   * (1) null args are ignored
   * (2) all config args start with '+'
   * (3) if '=' is ommitted, a 'true' value is assumed
   * (4) if <val> is ommitted, a 'null' value is assumed
   * (5) no spaces around '='
   * (6) all '-' driver-args are ignored
   * (7) if 'target' is already set (from 'jpf.app' property or
   *     "*.jpf" free-arg), all remaining <free-args> are 'target_args'
   *     otherwise 'target' is set to the first free-arg
   */

  protected void loadArgs (String[] args) {

    for (int i=0; i<args.length; i++){
      String a = args[i];

      if (a != null && a.length() > 0){
        switch (a.charAt(0)){
          case '+': // Config arg
            processArg(a.substring(1));
            break;

          case '-': // driver arg, ignore
            continue;

          default:  // target args to follow

            if (getString(TARGET_KEY) == null){ // no 'target' yet
              setTarget(a);
              i++;
            }

            int n = args.length - i;
            if (n > 0){ // we (might) have 'target_args'
              String[] targetArgs = new String[n];
              System.arraycopy(args, i, targetArgs, 0, n);
              setTargetArgs(targetArgs);
            }

            return;
        }
      }
    }
  }


  /*
   * this does not include the '+' prefix, just the 
   *     <key>[=[<value>]]
   */
  protected void processArg (String a) {

    int idx = a.indexOf("=");

    if (idx == 0){
      throw new JPFConfigException("illegal option: " + a);
    }

    if (idx > 0) {
      String key = a.substring(0, idx).trim();
      String val = a.substring(idx + 1).trim();
      if (val.length() == 0){
        val = null;
      }

      setProperty(key, val);

    } else {
      setProperty(a.trim(), "true");
    }

  }


  /**
   * replace string constants with global static objects
   */
  protected String normalize (String v) {
    if (v == null){
      return null; // ? maybe TRUE - check default loading of "key" or "key="
    }

    // trim leading and trailing blanks (at least Java 1.4.2 does not take care of trailing blanks)
    v = v.trim();
    
    // true/false
    if ("true".equalsIgnoreCase(v) || "t".equalsIgnoreCase(v)
        || "yes".equalsIgnoreCase(v) || "y".equalsIgnoreCase(v)
        || "on".equalsIgnoreCase(v)) {
      v = TRUE;
    } else if ("false".equalsIgnoreCase(v) || "f".equalsIgnoreCase(v)
        || "no".equalsIgnoreCase(v) || "n".equalsIgnoreCase(v)
        || "off".equalsIgnoreCase(v)) {
      v = FALSE;
    }

    // nil/null
    if ("nil".equalsIgnoreCase(v) || "null".equalsIgnoreCase(v)){
      v = null;
    }
    
    return v;
  }

  
  // our internal expander
  // Note that we need to know the key this came from, to handle recursive expansion
  protected String expandString (String key, String s) {
    int i, j = 0;
    if (s == null || s.length() == 0) {
      return s;
    }

    while ((i = s.indexOf("${", j)) >= 0) {
      if ((j = s.indexOf('}', i)) > 0) {
        String k = s.substring(i + 2, j);
        String v;
        
        if ((key != null) && key.equals(k)) {
          // that's expanding itself -> use what is there
          v = getProperty(key);
        } else {
          // refers to another key, which is already expanded, so this
          // can't get recursive (we expand during entry storage)
          v = getProperty(k);
        }
        
        if (v == null) { // if we don't have it, fall back to system properties
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


  // we override this so that we can handle expansion for both key and value
  // (value expansion can be recursive, i.e. refer to itself)
  @Override
  public Object put (Object key, Object value){

    if (key == null){
      throw new JPFConfigException("no null keys allowed");
    } else if (!(key instanceof String)){
      throw new JPFConfigException("only String keys allowed, got: " + key);
    }

    String k = expandString( null, (String) key);

    if (!(value == null)){
      if (!(value instanceof String)) {
        throw new JPFConfigException("only String values allowed, got: " + key + "=" + value);
      }

      String v = (String) value;

      if (k.charAt(k.length() - 1) == '+') { // the append hack
        k = k.substring( 0, k.length() - 1);
        return append( k, v, null);

      } else {
        v = normalize(expandString(k, v));
        Object oldValue = super.put(k, v);
        notifyPropertyChangeListeners(k, (String) oldValue, v);
        return oldValue;
      }

    } else {
        Object oldValue = super.get(k);
        remove(k);
        notifyPropertyChangeListeners(k, (String) oldValue, null);
        return oldValue;
    }
  }
    
  protected String append (String key, String value, String separator) {
    String oldValue = getProperty(key);
    String newValue;
    
    value = normalize( expandString(key, value));
    
    if (oldValue != null) { // append
      StringBuilder sb = new StringBuilder(oldValue);      
      if (separator != null) {
        sb.append(separator);
      }
      sb.append(value);
      newValue = sb.toString();
      
    } else { // nothing there yet
      newValue = value;
    }
    
    super.put(key,newValue);
    notifyPropertyChangeListeners(key, oldValue, newValue);
    
    return oldValue;
  }

  protected String append (String key, String value) {
    return append(key, value, LIST_SEPARATOR); // append with our standard list separator
  }


  void setClassLoader (ClassLoader newLoader){
    loader = newLoader;
  }

  public ClassLoader getClassLoader (){
    return loader;
  }

  //------------------------------ public methods - the Config API

  
  public void addChangeListener (ConfigChangeListener l) {
    if (changeListeners == null) {
      changeListeners = new ArrayList<ConfigChangeListener>();
      changeListeners.add(l);
    } else {
      if (!changeListeners.contains(l)) {
        changeListeners.add(l);
      }
    }
  }
  
  public void removeChangeListener (ConfigChangeListener l) {
    if (changeListeners != null) {
      changeListeners.remove(l);
      
      if (changeListeners.size() == 0) {
        changeListeners = null;
      }
    }
  }
  
  
  public JPFException exception (String msg) {
    return new JPFConfigException(msg);
  }

  public void throwException(String msg) {
    throw new JPFConfigException(msg);
  }

  //------------------------ special properties
  public String getTarget() {
    return getString(TARGET_KEY);
  }

  public void setTarget (String target){
    setProperty(TARGET_KEY,target);
  }

  public String[] getTargetArgs() {
    String[] args = getStringArray(TARGET_ARGS_KEY);
    if (args == null){
      args = new String[0];
    }
    return args;
  }

  public void setTargetArgs (String... args) {
    StringBuilder sb = new StringBuilder();
    for (int i=0, n = 0; i < args.length; i++) {
      String a = args[i];
      if (a != null) {
        if (n++ > 0) {
          sb.append(LIST_SEPARATOR);
        }
        // we expand to be consistent with an explicit 'target_args' spec
        sb.append(expandString(null, a));
      }
    }
    if (sb.length() > 0) {
      setProperty(TARGET_ARGS_KEY, sb.toString());
    }
  }

  //----------------------- type specific accessors

  public boolean getBoolean(String key) {
    String v = getProperty(key);
    return (v == TRUE);
  }

  public boolean getBoolean(String key, boolean def) {
    String v = getProperty(key);
    if (v != null) {
      return (v == TRUE);
    } else {
      return def;
    }
  }

  /**
   * for a given <baseKey>, check if there are corresponding
   * values for keys <baseKey>.0 ... <baseKey>.<maxSize>
   * If a value is found, store it in an array at the respective index
   *
   * @param baseKey String with base key without trailing '.'
   * @param maxSize maximum size of returned value array
   * @return trimmed array with String values found in dictionary
   */
  public String[] getStringEnumeration (String baseKey, int maxSize) {
    String[] arr = new String[maxSize];
    int max=-1;

    StringBuilder sb = new StringBuilder(baseKey);
    sb.append('.');
    int len = baseKey.length()+1;

    for (int i=0; i<maxSize; i++) {
      sb.setLength(len);
      sb.append(i);

      String v = getString(sb.toString());
      if (v != null) {
        arr[i] = v;
        max = i;
      }
    }

    if (max >= 0) {
      max++;
      if (max < maxSize) {
        String[] a = new String[max];
        System.arraycopy(arr,0,a,0,max);
        return a;
      } else {
        return arr;
      }
    } else {
      return null;
    }
  }


  public int[] getIntArray (String key) throws JPFConfigException {
    String v = getProperty(key);

    if (v != null) {
      String[] sa = v.split(DELIMS);
      int[] a = new int[sa.length];
      int i = 0;
      try {
        for (; i<sa.length; i++) {
          a[i] = Integer.parseInt(sa[i]);
        }
        return a;
      } catch (NumberFormatException nfx) {
        throw new JPFConfigException("illegal int[] element in '" + key + "' = \"" + sa[i] + '"');
      }
    } else {
      return null;
    }
  }

  public long getDuration (String key, long defValue) {
    String v = getProperty(key);
    if (v != null) {
      long d = 0;

      if (v.indexOf(':') > 0){
        String[] a = v.split(":");
        if (a.length > 3){
          //log.severe("illegal duration: " + key + "=" + v);
          return defValue;
        }
        int m = 1000;
        for (int i=a.length-1; i>=0; i--, m*=60){
          try {
            int n = Integer.parseInt(a[i]);
            d += m*n;
          } catch (NumberFormatException nfx) {
            return defValue;
          }
        }

      } else {
        try {
          d = Long.parseLong(v);
        } catch (NumberFormatException nfx) {
          return defValue;
        }
      }

      return d;
    }

    return defValue;
  }

  public int getInt(String key) {
    return getInt(key, 0);
  }

  public int getInt(String key, int defValue) {
    String v = getProperty(key);
    if (v != null) {
      try {
        return Integer.parseInt(v);
      } catch (NumberFormatException nfx) {
        return defValue;
      }
    }

    return defValue;
  }

  public long getLong(String key) {
    return getLong(key, 0L);
  }

  public long getLong(String key, long defValue) {
    String v = getProperty(key);
    if (v != null) {
      try {
        return Long.parseLong(v);
      } catch (NumberFormatException nfx) {
        return defValue;
      }
    }

    return defValue;
  }

  public long[] getLongArray (String key) throws JPFConfigException {
    String v = getProperty(key);

    if (v != null) {
      String[] sa = v.split(DELIMS);
      long[] a = new long[sa.length];
      int i = 0;
      try {
        for (; i<sa.length; i++) {
          a[i] = Long.parseLong(sa[i]);
        }
        return a;
      } catch (NumberFormatException nfx) {
        throw new JPFConfigException("illegal long[] element in " + key + " = " + sa[i]);
      }
    } else {
      return null;
    }
  }


  public double getDouble (String key) {
    return getDouble(key, 0.0);
  }

  public double getDouble (String key, double defValue) {
    String v = getProperty(key);
    if (v != null) {
      try {
        return Double.parseDouble(v);
      } catch (NumberFormatException nfx) {
        return defValue;
      }
    }

    return defValue;
  }

  public double[] getDoubleArray (String key) throws JPFConfigException {
    String v = getProperty(key);

    if (v != null) {
      String[] sa = v.split(DELIMS);
      double[] a = new double[sa.length];
      int i = 0;
      try {
        for (; i<sa.length; i++) {
          a[i] = Double.parseDouble(sa[i]);
        }
        return a;
      } catch (NumberFormatException nfx) {
        throw new JPFConfigException("illegal double[] element in " + key + " = " + sa[i]);
      }
    } else {
      return null;
    }
  }


  public String getString(String key) {
    return getProperty(key);
  }

  public String getString(String key, String defValue) {
    String s = getProperty(key);
    if (s != null) {
      return s;
    } else {
      return defValue;
    }
  }

  /**
   * return memory size in bytes, or 'defValue' if not in dictionary. Encoding
   * can have a 'M' or 'k' postfix, values have to be positive integers (decimal
   * notation)
   */
  public long getMemorySize(String key, long defValue) {
    String v = getProperty(key);
    long sz = defValue;

    if (v != null) {
      int n = v.length() - 1;
      try {
        char c = v.charAt(n);

        if ((c == 'M') || (c == 'm')) {
          sz = Long.parseLong(v.substring(0, n)) << 20;
        } else if ((c == 'K') || (c == 'k')) {
          sz = Long.parseLong(v.substring(0, n)) << 10;
        } else {
          sz = Long.parseLong(v);
        }

      } catch (NumberFormatException nfx) {
        return defValue;
      }
    }

    return sz;
  }

  public HashSet<String> getStringSet(String key){
    String v = getProperty(key);
    if (v != null && (v.length() > 0)) {
      HashSet<String> hs = new HashSet<String>();
      for (String s : v.split(DELIMS)) {
        hs.add(s);
      }
      return hs;
    }

    return null;
    
  }
  
  public HashSet<String> getNonEmptyStringSet(String key){
    HashSet<String> hs = getStringSet(key);
    if (hs != null && hs.isEmpty()) {
      return null;
    } else {
      return hs;
    }
  }
    
  public String[] getStringArray(String key) {
    String v = getProperty(key);
    if (v != null && (v.length() > 0)) {
      return v.split(DELIMS);
    }

    return null;
  }

  public String[] getCompactStringArray(String key){
    return removeEmptyStrings(getStringArray(key));
  }

  public static String[] removeEmptyStrings (String[] a){
    if (a != null) {
      int n = 0;
      for (int i=0; i<a.length; i++){
        if (a[i].length() > 0){
          n++;
        }
      }

      if (n < a.length){ // we have empty strings in the split
        String[] r = new String[n];
        for (int i=0, j=0; i<a.length; i++){
          if (a[i].length() > 0){
            r[j++] = a[i];
            if (j == n){
              break;
            }
          }
        }
        return r;

      } else {
        return a;
      }
    }

    return null;
  }
  
  public String[] getStringArray(String key, String[] def){
    String v = getProperty(key);
    if (v != null && (v.length() > 0)) {
      return v.split(DELIMS);
    } else {
      return def;
    }
  }


  /**
   * return an [optional] id part of a property value (all that follows the first '@')
   */
  String getIdPart (String key) {
    String v = getProperty(key);
    if ((v != null) && (v.length() > 0)) {
      int i = v.indexOf('@');
      if (i >= 0){
        return v.substring(i+1);
      }
    }

    return null;
  }

  public Class<?> asClass (String v) throws JPFConfigException {
    if ((v != null) && (v.length() > 0)) {
      v = stripId(v);
      v = expandClassName(v);
      try {
        return loader.loadClass(v);
      } catch (ClassNotFoundException cfx) {
        throw new JPFConfigException("class not found " + v);
      } catch (ExceptionInInitializerError ix) {
        throw new JPFConfigException("class initialization of " + v + " failed: " + ix,
            ix);
      }
    }

    return null;    
  }
      
  public <T> Class<? extends T> getClass(String key, Class<T> type) throws JPFConfigException {
    Class<?> cls = asClass( getProperty(key));
    if (cls != null) {
      if (type.isAssignableFrom(cls)) {
        return cls.asSubclass(type);
      } else {
        throw new JPFConfigException("classname entry for: \"" + key + "\" not of type: " + type.getName());
      }
    }
    return null;
  }
  
    
  public Class<?> getClass(String key) throws JPFConfigException {
    return asClass( getProperty(key));
  }
  
  public Class<?> getEssentialClass(String key) throws JPFConfigException {
    Class<?> cls = getClass(key);
    if (cls == null) {
      throw new JPFConfigException("no classname entry for: \"" + key + "\"");
    }

    return cls;
  }
  
  String stripId (String v) {
    int i = v.indexOf('@');
    if (i >= 0) {
      return v.substring(0,i);
    } else {
      return v;
    }
  }

  String getId (String v){
    int i = v.indexOf('@');
    if (i >= 0) {
      return v.substring(i+1);
    } else {
      return null;
    }
  }

  String expandClassName (String clsName) {
    if (clsName.charAt(0) == '.') {
      return "gov.nasa.jpf" + clsName;
    } else {
      return clsName;
    }
  }

  
  public Class<?>[] getClasses(String key) throws JPFConfigException {
    String[] v = getStringArray(key);
    if (v != null) {
      int n = v.length;
      Class<?>[] a = new Class[n];
      for (int i = 0; i < n; i++) {
        String clsName = expandClassName(v[i]);
        try {
          clsName = stripId(clsName);
          a[i] = loader.loadClass(clsName);
        } catch (ClassNotFoundException cnfx) {
          throw new JPFConfigException("class not found " + v[i]);
        } catch (ExceptionInInitializerError ix) {
          throw new JPFConfigException("class initialization of " + v[i] + " failed: "
              + ix, ix);
        }
      }

      return a;
    }

    return null;
  }
  
  // <2do> - that's kind of kludged together, not very efficient
  String[] getIds (String key) {
    String v = getProperty(key);

    if (v != null) {
      int i = v.indexOf('@');
      if (i >= 0) { // Ok, we have ids
        String[] a = v.split(DELIMS);
        String[] ids = new String[a.length];
        for (i = 0; i<a.length; i++) {
          ids[i] = getId(a[i]);
        }
        return ids;
      }
    }

    return null;
  }

  public <T> ArrayList<T> getInstances(String key, Class<T> type) throws JPFConfigException {

    Class<?>[] argTypes = { Config.class };
    Object[] args = { this };

    return getInstances(key,type,argTypes,args);
  }
  
  public <T> ArrayList<T> getInstances(String key, Class<T> type, Class<?>[]argTypes, Object[] args)
                                                      throws JPFConfigException {
    Class<?>[] c = getClasses(key);

    if (c != null) {
      String[] ids = getIds(key);

      ArrayList<T> a = new ArrayList<T>(c.length);

      for (int i = 0; i < c.length; i++) {
        String id = (ids != null) ? ids[i] : null;
        T listener = getInstance(key, c[i], type, argTypes, args, id);
        if (listener != null) {
          a.add( listener);
        } else {
          // should report here
        }
      }

      return a;
      
    } else {
      // should report here
    }

    return null;
  }
  
  public <T> T getInstance(String key, Class<T> type, String defClsName) throws JPFConfigException {
    Class<?>[] argTypes = CONFIG_ARGTYPES;
    Object[] args = CONFIG_ARGS;

    Class<?> cls = getClass(key);
    String id = getIdPart(key);

    if (cls == null) {
      try {
        cls = loader.loadClass(defClsName);
      } catch (ClassNotFoundException cfx) {
        throw new JPFConfigException("class not found " + defClsName);
      } catch (ExceptionInInitializerError ix) {
        throw new JPFConfigException("class initialization of " + defClsName + " failed: " + ix, ix);
      }
    }
    
    return getInstance(key, cls, type, argTypes, args, id);
  }

  public <T> T getInstance(String key, Class<T> type) throws JPFConfigException {
    Class<?>[] argTypes = CONFIG_ARGTYPES;
    Object[] args = CONFIG_ARGS;

    return getInstance(key, type, argTypes, args);
  }
    
  public <T> T getInstance(String key, Class<T> type, Class<?>[] argTypes,
                            Object[] args) throws JPFConfigException {
    Class<?> cls = getClass(key);
    String id = getIdPart(key);

    if (cls != null) {
      return getInstance(key, cls, type, argTypes, args, id);
    } else {
      return null;
    }
  }
  
  public <T> T getInstance(String key, Class<T> type, Object arg1, Object arg2)  throws JPFConfigException {
    Class<?>[] argTypes = new Class<?>[2];
    argTypes[0] = arg1.getClass();
    argTypes[1] = arg2.getClass();

    Object[] args = new Object[2];
    args[0] = arg1;
    args[1] = arg2;

    return getInstance(key, type, argTypes, args);
  }


  public <T> T getEssentialInstance(String key, Class<T> type) throws JPFConfigException {
    Class<?>[] argTypes = { Config.class };
    Object[] args = { this };
    return getEssentialInstance(key, type, argTypes, args);
  }

  /**
   * just a convenience method for ctor calls that take two arguments
   */
  public <T> T getEssentialInstance(String key, Class<T> type, Object arg1, Object arg2)  throws JPFConfigException {
    Class<?>[] argTypes = new Class<?>[2];
    argTypes[0] = arg1.getClass();
    argTypes[1] = arg2.getClass();

    Object[] args = new Object[2];
    args[0] = arg1;
    args[1] = arg2;

    return getEssentialInstance(key, type, argTypes, args);
  }

  public <T> T getEssentialInstance(String key, Class<T> type, Class<?>[] argTypes, Object[] args) throws JPFConfigException {
    Class<?> cls = getEssentialClass(key);
    String id = getIdPart(key);

    return getInstance(key, cls, type, argTypes, args, id);
  }

  
  public <T> T getInstance (String id, String clsName, Class<T> type) throws JPFConfigException {
    Class<?>[] argTypes = CONFIG_ARGTYPES;
    Object[] args = CONFIG_ARGS;

    Class<?> cls = asClass(clsName);
    
    if (cls != null) {
      return getInstance(id, cls, type, argTypes, args, id);
    } else {
      return null;
    }
  }
  
  /**
   * this is our private instantiation workhorse - try to instantiate an object of
   * class 'cls' by using the following ordered set of ctors 1. <cls>(
   * <argTypes>) 2. <cls>(Config) 3. <cls>() if all of that fails, or there was
   * a 'type' provided the instantiated object does not comply with, return null
   */
  <T> T getInstance(String key, Class<?> cls, Class<T> type, Class<?>[] argTypes,
                     Object[] args, String id) throws JPFConfigException {
    Object o = null;
    Constructor<?> ctor = null;

    if (cls == null) {
      return null;
    }

    if (id != null) { // check first if we already have this one instantiated as a singleton
      if (singletons == null) {
        singletons = new HashMap<String,Object>();
      } else {
        o = type.cast(singletons.get(id));
      }
    }

    while (o == null) {
      try {
        ctor = cls.getConstructor(argTypes);
        o = ctor.newInstance(args);
      } catch (NoSuchMethodException nmx) {

        if ((argTypes.length > 1) || ((argTypes.length == 1) && (argTypes[0] != Config.class))) {
          // fallback 1: try a single Config param
          argTypes = CONFIG_ARGTYPES;
          args = CONFIG_ARGS;

        } else if (argTypes.length > 0) {
          // fallback 2: try the default ctor
          argTypes = NO_ARGTYPES;
          args = NO_ARGS;

        } else {
          // Ok, there is no suitable ctor, bail out
          throw new JPFConfigException(key, cls, "no suitable ctor found");
        }
      } catch (IllegalAccessException iacc) {
        throw new JPFConfigException(key, cls, "\n> ctor not accessible: "
            + getMethodSignature(ctor));
      } catch (IllegalArgumentException iarg) {
        throw new JPFConfigException(key, cls, "\n> illegal constructor arguments: "
            + getMethodSignature(ctor));
      } catch (InvocationTargetException ix) {
        Throwable tx = ix.getTargetException();
        if (tx instanceof JPFConfigException) {
          throw new JPFConfigException(tx.getMessage() + "\n> used within \"" + key
              + "\" instantiation of " + cls);
        } else {
          throw new JPFConfigException(key, cls, "\n> exception in "
              + getMethodSignature(ctor) + ":\n>> " + tx, tx);
        }
      } catch (InstantiationException ivt) {
        throw new JPFConfigException(key, cls,
            "\n> abstract class cannot be instantiated");
      } catch (ExceptionInInitializerError eie) {
        throw new JPFConfigException(key, cls, "\n> static initialization failed:\n>> "
            + eie.getException(), eie.getException());
      }
    }

    // check type
    if (!type.isInstance(o)) {
      throw new JPFConfigException(key, cls, "\n> instance not of type: "
          + type.getName());
    }

    if (id != null) { // add to singletons (in case it's not already in there)
      singletons.put(id, o);
    }

    return type.cast(o); // safe according to above
  }

  String getMethodSignature(Constructor<?> ctor) {
    StringBuilder sb = new StringBuilder(ctor.getName());
    sb.append('(');
    Class<?>[] argTypes = ctor.getParameterTypes();
    for (int i = 0; i < argTypes.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(argTypes[i].getName());
    }
    sb.append(')');
    return sb.toString();
  }

  public boolean hasValue(String key) {
    String v = getProperty(key);
    return ((v != null) && (v.length() > 0));
  }

  public boolean hasValueIgnoreCase(String key, String value) {
    String v = getProperty(key);
    if (v != null) {
      return v.equalsIgnoreCase(value);
    }

    return false;
  }

  public int getChoiceIndexIgnoreCase(String key, String[] choices) {
    String v = getProperty(key);

    if ((v != null) && (choices != null)) {
      for (int i = 0; i < choices.length; i++) {
        if (v.equalsIgnoreCase(choices[i])) {
          return i;
        }
      }
    }

    return -1;
  }

  static final String PATH_SEPARATORS = "[,;]+"; // ' ', ':' can be part of path names

  /**
   * turn a mixed path list into a valid Windows path set with drive letters, 
   * and '\' and ';' separators. Also remove multiple consecutive separators
   * this assumes the path String to be already expanded
   */
  public String asCanonicalWindowsPath (String p) {
    boolean changed = false;
        
    int n = p.length();
    char[] buf = new char[n];
    p.getChars(0, n, buf, 0);
    
    for (int i=0; i<n; i++) {
      char c = buf[i];
      if (c == '/' || c == '\\') {
        if (c == '/'){
          buf[i] = '\\'; changed = true;
        }
        
        // remove multiple occurrences of dir separators
        int i1 = i+1;
        if (i1 < n) {
          for (c = buf[i1]; i1 < n && (c == '/' || c == '\\'); c = buf[i1]) {
            System.arraycopy(buf, i + 2, buf, i1, n - (i + 2));
            n--;
            changed = true;
          }
        }
        
      } else if (c == ':') {
        // is this part of a drive letter spec?
        int i1 = i+1;
        if (i1<n && (buf[i1] == '\\' || buf[i1] == '/')) {
          if (i>0) {
            if (i == 1 || (buf[i-2] == ';')){
              continue;
            }
          }
        }
        buf[i] = ';'; changed = true;
        
      } else if (c == ',') {
        buf[i] = ';'; changed = true;        
      }
      
      if (buf[i] == ';') { // remove multiple occurrences of path separators
        int i1 = i+1;
        if (i1<n) {
          for (c = buf[i1] ;(c == ':' || c == ';' || c == ','); c = buf[i1]){
            System.arraycopy(buf, i+2, buf, i1, n - (i+2));
            n--;
            changed = true;
          }
        }
      }
    }

    if (changed) {
      p = new String(buf, 0, n);
    }
    
    return p;
  }

  /**
   * turn a mixed path list into a valid Unix path set without drive letters, 
   * and with '/' and ':' separators. Also remove multiple consecutive separators
   * this assumes the path String to be already expanded
   */
  public String asCanonicalUnixPath (String p) {
    boolean changed = false;
    
    int n = p.length();
    char[] buf = new char[n];
    p.getChars(0, n, buf, 0);
    
    for (int i=0; i<n; i++) {
      char c = buf[i];
      if (c == '/' || c == '\\') {
        if (c == '\\'){
          buf[i] = '/'; changed = true;
        }
        
        // remove multiple occurrences of dir separators
        int i1 = i+1;
        if (i1 < n){
          for (c = buf[i1]; i1 < n && (c == '/' || c == '\\'); c = buf[i1]) {
            System.arraycopy(buf, i + 2, buf, i1, n - (i + 2));
            n--;
            changed = true;
          }
        }
        
      } else if (c == ':') {
        // strip drive letters - maybe this is trying to be too smart,
        // since we only do this for a "...:X:\..." but not a 
        // "...:X:/...", which could be a valid unix path list
        
        // is this part of a drive letter spec?
        int i1 = i+1;
        if (i1<n) {
          if (buf[i1] == '\\') {
            if (i>0) {
              if (i == 1 || (buf[i-2] == ':')){  // strip the drive letter
                System.arraycopy(buf, i1, buf, i-1, n - (i1));
                n-=2;
                changed = true;
              }
            }
          }
        }
        
      } else if (c == ';'){
        buf[i] = ':'; changed = true;
           
      } else if (c == ',') {
        buf[i] = ':'; changed = true;        
      }
      
      if (buf[i] == ':') {  // remove multiple occurrences of path separators
        int i1 = i+1;
        if (i1<n) {
          for (c = buf[i1] ;(c == ':' || c == ';' || c == ','); c = buf[i1]){
            System.arraycopy(buf, i+2, buf, i1, n - (i+2));
            n--;
            changed = true;
          }
        }
      }
    }
    
    if (changed) {
      p = new String(buf, 0, n);
    }
    
    return p;
  }
  
  public String asPlatformPath (String p) {
    if (File.separatorChar == '/') { // Unix'ish file system
      p = asCanonicalUnixPath(p);
    } else { // Windows'ish file system 
      p = asCanonicalWindowsPath(p);      
    }
    
    return p;
  }
  
  public File[] getPathArray (String key) {    
    String v = getProperty(key);
    if (v != null) {
      String[] pe = removeEmptyStrings(v.split(PATH_SEPARATORS));
      
      if (pe != null && pe.length > 0) {
        File[] files = new File[pe.length];
        for (int i=0; i<files.length; i++) {
          String path = asPlatformPath(pe[i]);
          files[i] = new File(path);
        }
        return files;
      }      
    }

    return new File[0];
  }

  public File getPath (String key) {
    String v = getProperty(key);
    if (v != null) {
      return new File(asPlatformPath(v));
    }
    
    return null;
  }
  
  
  //--- our modification interface
  
  @Override
  public Object setProperty (String key, String newValue) {    
    Object oldValue = put(key, newValue);    
    notifyPropertyChangeListeners(key, (String)oldValue, newValue);
    return oldValue;
  }

  public void parse (String s) {
    
    int i = s.indexOf("=");
    if (i > 0) {
      String key, val;
      
      if (i > 1 && s.charAt(i-1)=='+') { // append
        key = s.substring(0, i-1).trim();
        val = s.substring(i+1); // it's going to be normalized anyways
        append(key, val);
        
      } else { // put
        key = s.substring(0, i).trim();
        val = s.substring(i+1);
        setProperty(key, val);
      }
      
    }
  }
  
  protected void notifyPropertyChangeListeners (String key, String oldValue, String newValue) {
    if (changeListeners != null) {
      for (ConfigChangeListener l : changeListeners) {
        l.propertyChanged(this, key, oldValue, newValue);
      }
    }    
  }
  
  public String[] asStringArray (String s){
    return s.split(DELIMS);
  }
  
  public TreeMap<Object,Object> asOrderedMap() {
    TreeMap<Object,Object> map = new TreeMap<Object,Object>();
    map.putAll(this);
    return map;
  }

  public void print (PrintWriter pw) {
    pw.println("----------- Config contents");

    // just how much do you have to do to get a printout with keys in alphabetical order :<
    TreeSet<String> kset = new TreeSet<String>();
    for (Enumeration<?> e = propertyNames(); e.hasMoreElements();) {
      Object k = e.nextElement();
      if (k instanceof String) {
        kset.add( (String)k);
      }
    }

    for (String key : kset) {
      String val = getProperty(key);
      pw.print(key);
      pw.print(" = ");
      pw.println(val);
    }

    pw.flush();
  }

  /*
   * for debugging purposes
   */
  public void printEntries() {
    PrintWriter pw = new PrintWriter(System.out);
    print(pw);
  }

  public String getSourceName (Object src){
    if (src instanceof File){
      return ((File)src).getAbsolutePath();
    } else if (src instanceof URL){
      return ((URL)src).toString();
    } else {
      return src.toString();
    }
  }
  
  public List<Object> getSources() {
    return sources;
  }
  
  public void printStatus(Logger log) {
    int idx = 0;
    
    for (Object src : sources){
      if (src instanceof File){
        log.config("configuration source " + idx++ + " : " + getSourceName(src));
      }
    }
  }


}
