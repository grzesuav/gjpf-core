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

package gov.nasa.jpf.vm;

import gov.nasa.jpf.Config;

/**
 * class that holds various settings and factory methods for the POR policy in use
 * 
 * <2do> this should probably be turned into a singleton
 * 
 * Note - this used to be in FieldInstruction and was initialized during VM subsystem initialization,
 * but the VM should not explicitly use machine specific instruction types
 */
public class POR {
  
  static FieldLockInfoFactory fliFactory;
  
  static boolean skipFinals; // do we ignore final fields for POR
  static boolean skipStaticFinals;  // do we ignore static final fields for POR
  static boolean skipConstructedFinals;  // do we ignore final fields for POR after the object's constructor has finished?
  

  static void init (Config config){
    if (config.getBoolean("vm.por")) {
       skipFinals = config.getBoolean("vm.por.skip_finals", true);
       skipStaticFinals = config.getBoolean("vm.por.skip_static_finals", false);
       skipConstructedFinals = config.getBoolean("vm.por.skip_constructed_finals", false);

      if (config.getBoolean("vm.por.sync_detection")) {
        fliFactory = config.getEssentialInstance("vm.por.fli_factory.class", FieldLockInfoFactory.class);
      }
     }
  }
  
  public static FieldLockInfo createFieldLockInfo (ThreadInfo ti, ElementInfo ei, FieldInfo fi){
    if (fliFactory != null){
      return fliFactory.createFieldLockInfo(ti, ei, fi);
    } else {
      return null;
    }
  }
  
  public static boolean skipFinals(){
    return skipFinals;
  }
  
  public static boolean skipStaticFinals(){
    return skipStaticFinals;
  }
  
  public static boolean skipConstructedFinals(){
    return skipConstructedFinals;
  }
  
}
