//
// Copyright (C) 2013 United States Government as represented by the
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

package gov.nasa.jpf.util.test;

import gov.nasa.jpf.Property;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.TypeRef;
import gov.nasa.jpf.vm.NotDeadlockedProperty;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 * 
 * This is a root class for testing multi-processes code. This forces
 * JPF to use MultiProcessVM and DistributedSchedulerFactory
 */
public abstract class TestMultiProcessJPF extends TestJPF {
  int num_of_prc;

  protected String[] addMultiPrcEssentialArgs(String[] args) {
    String vm_class = "gov.nasa.jpf.vm.MultiProcessVM";
    String schedular_class = "gov.nasa.jpf.vm.DistributedSchedulerFactory";

    return Misc.appendArray(args, "+vm.class=" + vm_class, 
                "+vm.scheduler_factory.class=" + schedular_class,
                "+target.replicate=" + num_of_prc);
  }

  protected native int getProcessId();

  protected boolean verifyAssertionErrorDetails (int prcNum, String details, String... args){
    if (runDirectly) {
      return true;
    } else {
      num_of_prc = prcNum;
      args = addMultiPrcEssentialArgs(args);
      unhandledException( getCaller(), "java.lang.AssertionError", details, args);
      return false;
    }
  }

  protected boolean verifyAssertionError (int prcNum, String... args){
    if (runDirectly) {
      return true;
    } else {
      num_of_prc = prcNum;
      args = addMultiPrcEssentialArgs(args);
      unhandledException( getCaller(), "java.lang.AssertionError", null, args);
      return false;
    }
  }

  protected boolean verifyNoPropertyViolation (int prcNum, String...args){
    if (runDirectly) {
      return true;
    } else {
      num_of_prc = prcNum;
      args = addMultiPrcEssentialArgs(args);
      noPropertyViolation(getCaller(), args);
      return false;
    }
  }

  protected boolean verifyUnhandledExceptionDetails (int prcNum, String xClassName, String details, String... args){
    if (runDirectly) {
      return true;
    } else {
      num_of_prc = prcNum;
      args = addMultiPrcEssentialArgs(args);
      unhandledException( getCaller(), xClassName, details, args);
      return false;
    }
  }

  protected boolean verifyUnhandledException (int prcNum, String xClassName, String... args){
    if (runDirectly) {
      return true;
    } else {
      num_of_prc = prcNum;
      args = addMultiPrcEssentialArgs(args);
      unhandledException( getCaller(), xClassName, null, args);
      return false;
    }
  }

  protected boolean verifyJPFException (int prcNum, TypeRef xClsSpec, String... args){
    if (runDirectly) {
      return true;

    } else {
      num_of_prc = prcNum;
      args = addMultiPrcEssentialArgs(args);
      try {
        Class<? extends Throwable> xCls = xClsSpec.asSubclass(Throwable.class);

        jpfException( getCaller(), xCls, args);

      } catch (ClassCastException ccx){
        fail("not a property type: " + xClsSpec);
      } catch (ClassNotFoundException cnfx){
        fail("property class not found: " + xClsSpec);
      }
      return false;
    }
  }

  protected boolean verifyPropertyViolation (int prcNum, TypeRef propertyClsSpec, String... args){
    if (runDirectly) {
      return true;

    } else {
      num_of_prc = prcNum;
      args = addMultiPrcEssentialArgs(args);
      try {
        Class<? extends Property> propertyCls = propertyClsSpec.asSubclass(Property.class);
        propertyViolation( getCaller(), propertyCls, args);

      } catch (ClassCastException ccx){
        fail("not a property type: " + propertyClsSpec);
      } catch (ClassNotFoundException cnfx){
        fail("property class not found: " + propertyClsSpec);
      }
      return false;
    }
  }

  protected boolean verifyDeadlock (int prcNum, String... args){
    if (runDirectly) {
      return true;
    } else {
      num_of_prc = prcNum;
      args = addMultiPrcEssentialArgs(args);
      propertyViolation( getCaller(), NotDeadlockedProperty.class, args);
      return false;
    }
  }
}
