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
package gov.nasa.jpf.util.test;

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DirectCallStackFrame;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import java.util.ArrayList;


/**
 * native peer for RawTest, the base class of tests to run under JPF
 *
 * we could have used more fancy mechanisms like InvokeCGs, but
 * (1) as a test harness, this should depend on as few mechanisms as possible
 * that are subject to testing
 * (2) it should be as simple as possible, so that we can minimize testing
 * the tests
 */

public class JPF_gov_nasa_jpf_util_test_RawTest {

  static ClassInfo testClass;
  static MethodInfo testClassCtor;

  static MethodInfo[] testMethods = null;
  static int index = 0;
  static int testObjRef = MJIEnv.NULL;


  private static void pushDirectCallFrame(MJIEnv env, MethodInfo mi, int objRef) {
    ThreadInfo ti = env.getThreadInfo();
    Instruction insn = ti.getPC();

    MethodInfo stub = mi.createDirectCallStub("[test]");
    DirectCallStackFrame frame = new DirectCallStackFrame(stub, insn);
    frame.pushRef(objRef);
    ti.pushFrame(frame);
  }

  private static boolean initializeTestMethods(MJIEnv env, String[] selectedTests) {
    if (selectedTests != null && selectedTests.length > 0) {
      testMethods = new MethodInfo[selectedTests.length];
      int i = 0;
      for (String test : selectedTests) {
        MethodInfo mi = testClass.getMethod(test + "()V", false);
        if (mi != null && mi.isPublic() && !mi.isStatic()) {
          testMethods[i++] = mi;
        } else {
          reset____V();
          env.throwException("gov.nasa.jpf.util.test.RawTest$Exception",
                  "no such test method: public void " + test + "()");
          return false;
        }
      }
    } else { // collect all public void test..() methods
      ArrayList<MethodInfo> list = new ArrayList<MethodInfo>();
      for (MethodInfo mi : testClass) {
        if (mi.getName().startsWith("test") && mi.isPublic() && !mi.isStatic() &&
                mi.getSignature().equals("()V")) {
          list.add(mi);
        }
      }
      testMethods = list.toArray(new MethodInfo[list.size()]);
    }

    return true;
  }

  //--- our exported native methods

  public static void reset____V(){
    index = 0;
    testObjRef = MJIEnv.NULL;
    testMethods = null;
    testClass = null;
    testClassCtor = null;
  }

  public static void runTestsOfThisClass___3Ljava_lang_String_2__V (MJIEnv env, int clsObjRef,
                                                                    int selectedTestsRef) {
    ThreadInfo ti = env.getThreadInfo();
    
    if (testMethods == null) {
      StackFrame frame = ti.getTopFrame(); // the runTestsOfThisClass() caller

      testClass = frame.getClassInfo();
      testClassCtor = testClass.getMethod("<init>()V", true);

      String[] selectedTests = env.getStringArrayObject(selectedTestsRef);
      if (initializeTestMethods(env,selectedTests)){
        env.repeatInvocation();
      }

    } else { // this is re-executed
      if (testObjRef == MJIEnv.NULL){ // create a new test object
        testObjRef = env.newObject(testClass);

        if (testClassCtor != null) {
          pushDirectCallFrame(env,testClassCtor,testObjRef);
          env.repeatInvocation();
        }

      } else { // execute the next test

        if (index < testMethods.length) {
          MethodInfo miTest = testMethods[index++];
          pushDirectCallFrame(env, miTest, testObjRef);

          testObjRef = MJIEnv.NULL;
          env.repeatInvocation();
          ti.getVM().print("--- running test: " + miTest.getName() + '\n');

        } else {
          reset____V();
        }
      }
    }
  }


}
