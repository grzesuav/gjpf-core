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

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * @author Nastaran Shafiei <nastaran.shafiei@gmail.com>
 * 
 * This is a native peer for multiprocess test class root
 */
public class JPF_gov_nasa_jpf_util_test_TestMultiProcessJPF 
  extends JPF_gov_nasa_jpf_util_test_TestJPF {

  @MJI
  public boolean verifyNoPropertyViolation__I_3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef, int n, int jpfArgsRef){
    return true;
  }

  @MJI
  public int getProcessId____I (MJIEnv env, int objRef) {
    return ThreadInfo.getCurrentThread().getApplicationContext().getId();
  }
}
