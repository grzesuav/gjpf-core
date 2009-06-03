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

import gov.nasa.jpf.util.ObjArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
 * <2do> need to make NumberFormatException handling consistent - should always
 * throw an Exception, not silently returning the default value
 * <2do> consistent expansion handling
 * <2do> type specific separator chars for set/array specs ? (the ':' issue)
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
  
  /**
   * this class wraps the various exceptions we might encounter esp. during
   * reflection instantiation
   */
  public class Exception extends java.lang.Exception {
    public Exception(String msg) {
      super(msg);
    }

    public Exception(String msg, Throwable cause) {
      super(msg, cause);
    }

    public Exception(String key, Class<?> cls, String failure) {
      super("error instantiating class " + cls.getName() + " for entry \""
          + key + "\":" + failure);
    }

    public Exception(String key, Class<?> cls, String failure, Throwable cause) {
      this(key, cls, failure);
      initCause(cause);
    }

    public String toString() {
      StringBuilder sb = new StringBuilder("JPF configuration error: ");
      sb.append(getMessage());

      return sb.toString();
    }

    public Config getConfig() {
      return Config.this;
    }
  }

  // where did we initialize from
  ArrayList<Object> sources = new ArrayList<Object>();
  
  List<ConfigChangeListener> changeListeners;
  
  // all arguments that are not <key>= <value>pairs
  String[] freeArgs; 

  // an [optional] hashmap to keep objects we want to be singletons
  HashMap<String,Object> singletons;
  
  final Object[] CONFIG_ARGS = { this }; 
  
  public Config(String[] args, String fileName, String alternatePath, Class<?> codeBase) {

    //--- load default.properties first
    loadFile("default.properties", alternatePath, codeBase);
    if (isEmpty()){
      // if we run JPF, we should throw a Config.Exception, but (1) this
      // could be used in a standalone program (like MethodTester) that doesn't
      // require defaults, (2) users could have complete mode property files
      // there are also a lot of clients out there which would have to be modified
      
      // throwException("no default properties found");
    }
    
    // maybe some of the keys depend on the mode property pathname
    put("config", fileName);
    int i = fileName.lastIndexOf(File.separatorChar);
    if (i>=0){
      put("config_path", fileName.substring(0,i));
    } else {
      put("config_path", ".");
    }

    // now load the key/value pairs from the mode property file
    loadFile(fileName, alternatePath, codeBase);

    //--- last are the command line args
    if (args != null){
      processArgs(args);
    } else {
      freeArgs = new String[0];
    }
  }

  //-------------------------------------------------------- internal functions

  
  /**
   * replace string constants with global static objects
   */
  protected String normalize (String v) {
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

  
  // we override this so that we can handle expansion
  @Override
  public Object put (Object key, Object value){
    // <2do> as long as we derive from Properties, we should check for String args
    
    String k = (String)key;
    String v = (String)value;
    
    if (k.charAt(k.length()-1) == '+'){ // the append hack
      return append(k.substring(0, k.length()-1), v, null);
      
    } else {
      v = normalize( expandString(k, v));
      Object oldValue = super.put(k, v);
      notifyPropertyChangeListeners(k, (String)oldValue, v);
      
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

  
  /**
   * extract all "+<key> [+]= <val>" parameters, store/overwrite them in our
   * dictionary, collect all other parameters in a String array
   *
   * @param args array of String parameters to process
   */
  protected void processArgs(String[] args) {
    int i;
    ArrayList<String> list = new ArrayList<String>();

    for (i = 0; i < args.length; i++) {
      String a = args[i];
      if (a != null) {
        if (a.charAt(0) == '+') {
          int idx = a.indexOf("=");
          if (idx > 0) {
            String key = a.substring(1, idx).trim();
            String val = a.substring(idx+1).trim();            
            setProperty(key, val);              
                        
          } else {
            setProperty(a.substring(1), null);
          }
        } else {
          list.add(a);
        }
      }
    }

    freeArgs = list.toArray(new String[list.size()]);
  }

  /**
   * return the index of the first free argument that does not start with an
   * hyphen
   */
  protected int getNonOptionArgIndex() {
    if ((freeArgs == null) || (freeArgs.length == 0))
      return -1;

    for (int i = 0; i < freeArgs.length; i++) {
      String a = freeArgs[i];
      if (a != null) {
        char c = a.charAt(0);
        if (c != '-') {
          return i;
        }
      }
    }

    return -1;
  }
  
  protected boolean loadFile(String fileName, String alternatePath, Class<?> codeBase) {
    InputStream is = null;

    try {
      // first, try to load from a file
      File f = new File(fileName);
      if (!f.exists()) {
        // Ok, try alternatePath, if fileName wasn't absolute
        if (!f.isAbsolute() && (alternatePath != null)) {
          f = new File(alternatePath, fileName);
        }
      }

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
        return true;
      }
    } catch (IOException iex) {
      return false;
    }

    return false;
  }

  
  //------------------------------ public methods - the Config API
  
  public void setCurrentClassLoader (ClassLoader loader) {
    this.loader = loader;
  }
  
  public ClassLoader getCurrentClassLoader () {
    return loader;
  }
  
  public String[] getArgs() {
    return freeArgs;
  }

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
  
  
  public Config.Exception exception (String msg) {
    return new Config.Exception(msg);
  }

  public void throwException(String msg) throws Exception {
    throw new Config.Exception(msg);
  }


  /**
   * return the first non-option freeArg, or 'null' if there is none (usually
   * denotes the application to start)
   */
  public String getTargetArg() {
    int i = getNonOptionArgIndex();
    if (i < 0) {
      return getString(TARGET_KEY);
    } else {
      return freeArgs[i];
    }
  }

  /**
   * return all args that follow the first non-option freeArgs (usually denotes
   * the parameters to pass to the application to start)
   */
  public String[] getTargetArgParameters() {
    int i = getNonOptionArgIndex();
        
    if (i < 0) {  // there are none in the command line, check 'target_args'
      String[] a = getStringArray(TARGET_ARGS_KEY);
      if (a != null) {
        return a;
      } else {
        return new String[0];
      }
      
    } else {
      int n = freeArgs.length - (i + 1);
      String[] a = new String[n];
      System.arraycopy(freeArgs, i + 1, a, 0, n);
      return a;
    }
  }

  public void setArgs (String[] args) {
    freeArgs = args;
  }
  
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


  public int[] getIntArray (String key) throws Exception {
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
        throw new Exception("illegal int[] element in '" + key + "' = \"" + sa[i] + '"');
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

  public long[] getLongArray (String key) throws Exception {
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
        throw new Exception("illegal long[] element in " + key + " = " + sa[i]);
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

  public double[] getDoubleArray (String key) throws Exception {
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
        throw new Exception("illegal double[] element in " + key + " = " + sa[i]);
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

  public Class<?> asClass (String v) throws Exception {
    if ((v != null) && (v.length() > 0)) {
      v = stripId(v);
      v = expandClassName(v);
      try {
        return loader.loadClass(v);
      } catch (ClassNotFoundException cfx) {
        throw new Exception("class not found " + v);
      } catch (ExceptionInInitializerError ix) {
        throw new Exception("class initialization of " + v + " failed: " + ix,
            ix);
      }
    }

    return null;    
  }
      
  public <T> Class<? extends T> getClass(String key, Class<T> type) throws Exception {
    Class<?> cls = asClass( getProperty(key));
    if (cls != null) {
      if (type.isAssignableFrom(cls)) {
        return cls.asSubclass(type);
      } else {
        throw new Exception("classname entry for: \"" + key + "\" not of type: " + type.getName());
      }
    }
    return null;
  }
  
    
  public Class<?> getClass(String key) throws Exception {
    return asClass( getProperty(key));
  }
  
  public Class<?> getEssentialClass(String key) throws Exception {
    Class<?> cls = getClass(key);
    if (cls == null) {
      throw new Exception("no classname entry for: \"" + key + "\"");
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

  
  public Class<?>[] getClasses(String key) throws Exception {
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
          throw new Exception("class not found " + v[i]);
        } catch (ExceptionInInitializerError ix) {
          throw new Exception("class initialization of " + v[i] + " failed: "
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

  public <T> ObjArray<T> getInstances(String key, Class<T> type) throws Exception {

    Class<?>[] argTypes = { Config.class };
    Object[] args = { this };

    return getInstances(key,type,argTypes,args);
  }
  
  public <T> ObjArray<T> getInstances(String key, Class<T> type, Class<?>[]argTypes, Object[] args)
                                                      throws Exception {
    Class<?>[] c = getClasses(key);

    if (c != null) {
      String[] ids = getIds(key);

      ObjArray<T> a = new ObjArray<T>(c.length);

      for (int i = 0; i < c.length; i++) {
        String id = (ids != null) ? ids[i] : null;
        T listener = getInstance(key, c[i], type, argTypes, args, id);
        if (listener != null) {
          a.set(i, listener);
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
  
  public <T> T getInstance(String key, Class<T> type, String defClsName) throws Exception {
    Class<?>[] argTypes = CONFIG_ARGTYPES;
    Object[] args = CONFIG_ARGS;

    Class<?> cls = getClass(key);
    String id = getIdPart(key);

    if (cls == null) {
      try {
        cls = loader.loadClass(defClsName);
      } catch (ClassNotFoundException cfx) {
        throw new Exception("class not found " + defClsName);
      } catch (ExceptionInInitializerError ix) {
        throw new Exception("class initialization of " + defClsName + " failed: " + ix, ix);
      }
    }
    
    return getInstance(key, cls, type, argTypes, args, id);
  }

  public <T> T getInstance(String key, Class<T> type) throws Exception {
    Class<?>[] argTypes = CONFIG_ARGTYPES;
    Object[] args = CONFIG_ARGS;

    return getInstance(key, type, argTypes, args);
  }
    
  public <T> T getInstance(String key, Class<T> type, Class<?>[] argTypes,
                            Object[] args) throws Exception {
    Class<?> cls = getClass(key);
    String id = getIdPart(key);

    if (cls != null) {
      return getInstance(key, cls, type, argTypes, args, id);
    } else {
      return null;
    }
  }
  
  public <T> T getInstance(String key, Class<T> type, Object arg1, Object arg2)  throws Exception {
    Class<?>[] argTypes = new Class<?>[2];
    argTypes[0] = arg1.getClass();
    argTypes[1] = arg2.getClass();

    Object[] args = new Object[2];
    args[0] = arg1;
    args[1] = arg2;

    return getInstance(key, type, argTypes, args);
  }


  public <T> T getEssentialInstance(String key, Class<T> type) throws Exception {
    Class<?>[] argTypes = { Config.class };
    Object[] args = { this };
    return getEssentialInstance(key, type, argTypes, args);
  }

  /**
   * just a convenience method for ctor calls that take two arguments
   */
  public <T> T getEssentialInstance(String key, Class<T> type, Object arg1, Object arg2)  throws Exception {
    Class<?>[] argTypes = new Class<?>[2];
    argTypes[0] = arg1.getClass();
    argTypes[1] = arg2.getClass();

    Object[] args = new Object[2];
    args[0] = arg1;
    args[1] = arg2;

    return getEssentialInstance(key, type, argTypes, args);
  }

  public <T> T getEssentialInstance(String key, Class<T> type, Class<?>[] argTypes, Object[] args) throws Exception {
    Class<?> cls = getEssentialClass(key);
    String id = getIdPart(key);

    return getInstance(key, cls, type, argTypes, args, id);
  }

  
  public <T> T getInstance (String id, String clsName, Class<T> type) throws Exception {
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
   * this is our private instantiation workhorse try to instantiate an object of
   * class 'cls' by using the following ordered set of ctors 1. <cls>(
   * <argTypes>) 2. <cls>(Config) 3. <cls>() if all of that fails, or there was
   * a 'type' provided the instantiated object does not comply with, return null
   */
  <T> T getInstance(String key, Class<?> cls, Class<T> type, Class<?>[] argTypes,
                     Object[] args, String id) throws Exception {
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
          throw new Exception(key, cls, "no suitable ctor found");
        }
      } catch (IllegalAccessException iacc) {
        throw new Exception(key, cls, "\n> ctor not accessible: "
            + getMethodSignature(ctor));
      } catch (IllegalArgumentException iarg) {
        throw new Exception(key, cls, "\n> illegal constructor arguments: "
            + getMethodSignature(ctor));
      } catch (InvocationTargetException ix) {
        Throwable tx = ix.getTargetException();
        if (tx instanceof Config.Exception) {
          throw new Exception(tx.getMessage() + "\n> used within \"" + key
              + "\" instantiation of " + cls);
        } else {
          throw new Exception(key, cls, "\n> exception in "
              + getMethodSignature(ctor) + ":\n>> " + tx, tx);
        }
      } catch (InstantiationException ivt) {
        throw new Exception(key, cls,
            "\n> abstract class cannot be instantiated");
      } catch (ExceptionInInitializerError eie) {
        throw new Exception(key, cls, "\n> static initialization failed:\n>> "
            + eie.getException(), eie.getException());
      }
    }

    // check type
    if (!type.isInstance(o)) {
      throw new Exception(key, cls, "\n> instance not of type: "
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

  /**
   * check if any of the freeArgs matches a regular expression
   *
   * @param regex regular expression to check for
   * @return true if found, false if not found or no freeArgs
   */
  public boolean hasArg(String regex) {
    if (freeArgs == null) {
      return false;
    }

    for (int i = 0; i < freeArgs.length; i++) {
      if (freeArgs[i].matches(regex)) {
        return true;
      }
    }

    return false;
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
      String[] pe =  v.split(PATH_SEPARATORS);
      
      if (pe != null && pe.length > 0) {
        File[] files = new File[pe.length];
        for (int i=0; i<files.length; i++) {
          String path = asPlatformPath(pe[i]);
          files[i] = new File(path);
        }
        return files;
      }      
    }

    return null;
  }

  public File getPath (String key) {
    String v = getProperty(key);
    if (v != null) {
      return new File(asPlatformPath(v));
    }
    
    return null;
  }
  
  public ClassLoader getClassLoader (String classpathKey) throws Exception {
    ClassLoader currentLoader = Config.class.getClassLoader();
    
    File[] pathElements = getPathArray(classpathKey);
    if (pathElements != null && pathElements.length > 0) {
      try {
        URL[] urls = new URL[pathElements.length];
        for (int i=0; i<pathElements.length; i++) {
          urls[i] = pathElements[i].toURI().toURL(); 
        }
        return new URLClassLoader(urls, currentLoader);
        
      } catch (MalformedURLException x) {
        throw new Exception("malformed classpath for " + classpathKey + " : " +  x.getMessage());
      }
    }
    
    return currentLoader; // whatever did define us should know
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
    map.putAll(defaults);
    map.putAll(this);
    return map;
  }

  public void print (PrintWriter pw) {
    pw.println("----------- dictionary contents");

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

    if ((freeArgs != null) && (freeArgs.length > 0)) {
      pw.println("----------- free arguments");
      for (int i = 0; i < freeArgs.length; i++) {
        pw.println(freeArgs[i]);
      }
    }

    pw.flush();
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

  /** test driver
  public static void main (String[] args){
    Config conf = JPF.createConfig(args);

    String[] a = conf.getStringEnumeration("a", 10);
    for (int i=0; i<a.length; i++){
      System.out.println("a." + i + "=" + a[i]);
    }
  }
  **/
}
