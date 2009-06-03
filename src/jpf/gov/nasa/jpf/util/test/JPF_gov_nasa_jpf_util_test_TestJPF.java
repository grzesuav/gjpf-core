
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
 *
 */
public class JPF_gov_nasa_jpf_util_test_TestJPF {

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
          env.throwException("gov.nasa.jpf.util.test.TestException",
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



  public static boolean isJUnitRun____Z (MJIEnv env, int clsObjRef){
    return false;
  }

  /**
   * if this is called, we know that we already run under JPF
   */
  public static boolean runJPF____Z (MJIEnv env, int clsObjRef){
    return false;
  }


  public static boolean verifyNoPropertyViolation___3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return true;
  }

  public static boolean verifyAssertionError___3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return true;
  }
}
