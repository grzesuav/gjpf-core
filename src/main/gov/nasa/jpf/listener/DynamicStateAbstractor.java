//
// Copyright (C) 2011 United States Government as represented by the
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
package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.serialize.Abstraction;
import gov.nasa.jpf.jvm.serialize.DynamicAbstractionSerializer;
import gov.nasa.jpf.jvm.serialize.Ignored;
import gov.nasa.jpf.util.FieldSpec;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.StringSetMatcher;
import java.util.LinkedList;
import java.util.List;

/**
 * listener that attaches state abstraction attributes to classes and fields, to
 * be used in combination with the DynamicAbstractionSerializer
 * 
 * <2do> extend towards stackframes
 */
public class DynamicStateAbstractor extends ListenerAdapter {
  
  static JPFLogger logger = JPF.getLogger("gov.nasa.jpf.DynamicStateAbstraction");
    
  public static class Serializer extends DynamicAbstractionSerializer {
    
    protected boolean isAbstractedClass(ClassInfo ci){
      // this is more efficient than checking for any Abstraction object set in all fields
      return ci.hasAttr(FieldAbstraction.class);
    }
  }

  static class FieldAbstraction {
    FieldSpec fspec;
    Abstraction abstraction;
    
    FieldAbstraction(FieldSpec f, Abstraction a){
      fspec = f;
      abstraction = a;
    }
  }

  StringSetMatcher includeClasses = null; //  means all
  StringSetMatcher excludeClasses = null; //  means none
  
  StringSetMatcher includeMethods = null;
  StringSetMatcher excludeMethods = null;
  
  List<FieldAbstraction> fieldAbstractions = new LinkedList<FieldAbstraction>();
  
  
  public DynamicStateAbstractor (Config conf){

    String[] fids = conf.getCompactTrimmedStringArray("dabs.fields");
    for (String id : fids){
      String keyPrefix = "dabs." + id;
      String fs = conf.getString(keyPrefix + ".field");
      if (fs != null){
        FieldSpec fspec = FieldSpec.createFieldSpec(fs);
        if (fspec != null){
          String aKey = keyPrefix + ".abstraction";
          Abstraction abstraction = conf.getInstance(aKey, Abstraction.class);
          
          fieldAbstractions.add(new FieldAbstraction(fspec, abstraction));
        }
      } else {
        logger.warning("no field spec for id: " + id);
      }
    }
    
    // <2do> add stackframe abstractors 
    
    includeClasses = StringSetMatcher.getNonEmpty(conf.getStringArray("dabs.classes.include"));
    excludeClasses = StringSetMatcher.getNonEmpty(conf.getStringArray("dabs.classes.exclude"));

    includeMethods = StringSetMatcher.getNonEmpty(conf.getStringArray("dabs.methods.include"));
    excludeMethods = StringSetMatcher.getNonEmpty(conf.getStringArray("dabs.methods.exclude"));
  }
  
  public void classLoaded (JVM vm){
    ClassInfo ci = vm.getLastClassInfo();
    String clsName = ci.getName();
    
    if (includeMethods != null || excludeMethods != null) {
      for (MethodInfo mi : ci.getDeclaredMethodInfos()) {
        if (!StringSetMatcher.isMatch(mi.getFullName(), includeMethods, excludeMethods)) {
          mi.addAttr(Ignored.IGNORED);
        }
      }
    }
    
    if (!StringSetMatcher.isMatch(clsName, includeClasses, excludeClasses)){
      ci.addAttr(Ignored.IGNORED);
      
    } else {
    
      // <2do> add stackframe abstractors 

      
      if (!fieldAbstractions.isEmpty()) {
        for (FieldInfo fi : ci.getDeclaredInstanceFields()) {
          for (FieldAbstraction fabs : fieldAbstractions) {
            if (fabs.fspec.matches(fi)) {
              logger.info("setting instance field abstraction ", fabs.abstraction.getClass().getName(),
                      " for field ", fi.getFullName());
              fi.addAttr(fabs.abstraction);
              if (!ci.hasAttr(FieldAbstraction.class)) {
                ci.addAttr(fabs);
              }
            }
          }
        }

        for (FieldInfo fi : ci.getDeclaredStaticFields()) {
          for (FieldAbstraction fabs : fieldAbstractions) {
            if (fabs.fspec.matches(fi)) {
              logger.info("setting static field abstraction ", fabs.abstraction.getClass().getName(),
                      " for field ", fi.getFullName());
              fi.addAttr(fabs.abstraction);
              if (!ci.hasAttr(FieldAbstraction.class)) {
                ci.addAttr(fabs);
              }
            }
          }
        }
      }
    }
  }
}
