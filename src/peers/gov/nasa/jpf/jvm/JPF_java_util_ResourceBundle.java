//
// Copyright (C) 2009 United States Government as represented by the
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

package gov.nasa.jpf.jvm;

/**
 * native peer for ResourceBundle
 */
public class JPF_java_util_ResourceBundle {


  public static int getClassContext_____3Ljava_lang_Class_2 (MJIEnv env, int clsRef){
    ThreadInfo ti = env.getThreadInfo();
    int stackDepth = ti.countVisibleStackFrames();

    int aRef = env.newObjectArray("java.lang.Class", stackDepth);

    for (int i=ti.getStackDepth()-1, j=0; i>=0; i--){
      StackFrame frame = ti.getStackFrame(i);
      if (!frame.isDirectCallFrame()){
        MethodInfo mi = frame.getMethodInfo();
        ClassInfo ci = mi.getClassInfo();
        int clsObjRef = ci.getClassObjectRef();
        env.setReferenceArrayElement(aRef, j++, clsObjRef);
      }
    }

    return aRef;
  }
}
