//
// Copyright (C) 2008 United States Government as represented by the
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
package gov.nasa.jpf.jvm.untracked;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.CollapsingRestorer;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.Fields;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.Monitor;

/**
 * CollapsingRestorer that support the @UntrackedField annotation
 *
 * @author Milos Gligoric (milos.gligoric@gmail.com)
 * @author Tihomir Gvero (tihomir.gvero@gmail.com)
 */

public class UntrackedCollapsingRestorer extends CollapsingRestorer {

  /* Overrides the method from AbstractRestorer to create an
   * UntrackedManager object, i.e., to set the "untracked" mode on,
   * if the property "vm.untracked" is true in the config file.
   */
  public void attach (JVM jvm) throws Config.Exception {
    super.attach(jvm);
    boolean untrackedProp = jvm.getConfig().getBoolean("vm.untracked", false);
    UntrackedManager.setProperty(untrackedProp);
  }

  protected void restoreFields (ElementInfo ei, Fields fields) {
    // is "untracked" mode on
    if (UntrackedManager.getProperty()) {
      UntrackedManager.getInstance().restoreTrackedFields(ei, fields);
    } else {
      ei.restoreFields(fields);
    }
  }

  protected Fields poolFields (ElementInfo ei) {
    if (UntrackedManager.getProperty() &&
        UntrackedManager.getInstance().isUntracked(ei)) {
      return ei.getFields();
    } else {
      return super.poolFields(ei);
    }
  }

  protected Monitor poolMonitor (ElementInfo ei) {
    if (UntrackedManager.getProperty() &&
        UntrackedManager.getInstance().isUntracked(ei)) {
      return ei.getMonitor();
    } else {
      return super.poolMonitor(ei);
    }
  }

}
