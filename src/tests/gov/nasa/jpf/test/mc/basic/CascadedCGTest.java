//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.test.mc.basic;


import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.choice.IntChoiceFromSet;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

/**
 * regression test for cascaded ChoiceGenerators
 */
public class CascadedCGTest extends TestJPF {

  public static void main (String[] args) {
    runTestsOfThisClass(args);
  }


  public static class IntChoiceCascader extends ListenerAdapter {
    static int result;

    public void instructionExecuted(JVM vm) {
      Instruction insn = vm.getLastInstruction();
      ThreadInfo ti = vm.getLastThreadInfo();
      SystemState ss = vm.getSystemState();

      if (insn instanceof InvokeInstruction) { // break on method call
        InvokeInstruction call = (InvokeInstruction) insn;

        if ("getInt(II)I".equals(call.getInvokedMethodName())){ // this insn did create a CG
          if (!ti.isFirstStepInsn()){
            result = 0;

            IntIntervalGenerator cg = new IntIntervalGenerator("listenerCG", 3,4);
            ss.setNextChoiceGenerator(cg);
            System.out.println("# listener registered " + cg);

          } else { // reexecution

            ChoiceGenerator<?>[] curCGs = ss.getCurrentChoiceGenerators();
            assert curCGs.length == 2;

            IntIntervalGenerator cg = ss.getCurrentChoiceGenerator("listenerCG", IntIntervalGenerator.class);
            assert cg != null : "no 'listenerCG' IntIntervalGenerator found";
            int i = cg.getNextChoice();
            System.out.println("# current listener CG choice: " + i);

            cg = ss.getCurrentChoiceGenerator("verifyGetInt", IntIntervalGenerator.class);
            assert cg != null : "no 'verifyGetInt' IntIntervalGenerator found";
            int j = cg.getNextChoice();
            System.out.println("# current insn CG choice: " + j);

            result += i * j;
          }
        }
      }
    }
  }

  @Test
  public void testCascadedIntIntervals () {
    if (verifyNoPropertyViolation("+listener=.test.mc.basic.CascadedCGTest$IntChoiceCascader")){
      int i = Verify.getInt( 1, 2);
      System.out.print("i=");
      System.out.println(i);
    } else {
      assert IntChoiceCascader.result == 21;
    }
  }


  //--- mixed data and thread CG

  public static class FieldAccessCascader extends ListenerAdapter {

/**
    public void executeInstruction(JVM vm) {
      Instruction insn = vm.getLastInstruction();
      ThreadInfo ti = vm.getLastThreadInfo();
      SystemState ss = vm.getSystemState();

      if (insn instanceof GETFIELD){
        FieldInfo fi = ((GETFIELD) insn).getFieldInfo();
        if (fi.getName().equals("mySharedField")){
          if (ti.isFirstStepInsn()){
            IntChoiceFromSet cg = ss.getCurrentChoiceGenerator("fieldReplace", IntChoiceFromSet.class);
            if (cg != null){
              int v = cg.getNextChoice();

System.out.println("@@ topFrame = " + ti.getTopFrame() + ", pc=" + ti.getPC() + ", next=" + ti.getNextPC());

              int n = ti.pop();
              ti.push(v);

              ti.skipInstruction();
              ti.setNextPC(insn.getNext());

              System.out.println("# listener replacing " + n + " with " + v);
            }
          }
        }
      }
    }
**/

    public void instructionExecuted(JVM vm) {
      Instruction insn = vm.getLastInstruction();
      ThreadInfo ti = vm.getLastThreadInfo();
      SystemState ss = vm.getSystemState();

      if (insn instanceof GETFIELD){
        FieldInfo fi = ((GETFIELD) insn).getFieldInfo();
        if (fi.getName().equals("mySharedField")){
          if (!ti.isFirstStepInsn()){
            IntChoiceFromSet cg = new IntChoiceFromSet("fieldReplace", 42, 43);
            ss.setNextChoiceGenerator(cg);
            System.out.println("# listener registering " + cg);
            ti.setNextPC(insn); // reexec
            
          } else {
            if (!ti.isReexecuted()){
              IntChoiceFromSet cg = ss.getCurrentChoiceGenerator("fieldReplace", IntChoiceFromSet.class);
              if (cg != null) {
                int v = cg.getNextChoice();

                System.out.println("@@ topFrame = " + ti.getTopFrame() + ", pc=" + ti.getPC() + ", next=" + ti.getNextPC());

                int n = ti.pop();
                ti.push(v);

                System.out.println("# listener replacing " + n + " with " + v);
              }
            }
          }
        }
      }
    }
  }

  int mySharedField = -1;

  //@Test
  public void testMixedThreadDataCGs () {
    if (verifyNoPropertyViolation("+listener=.test.mc.basic.CascadedCGTest$FieldAccessCascader,.listener.ExecTracker")){
      Thread t = new Thread(){
        public void run() {
          int n = mySharedField;
          System.out.print("<thread> mySharedField read: ");
          System.out.println(n);
        }
      };
      t.start();

      mySharedField = 7;
      System.out.println("<main> mySharedField write: 7");
    }
  }
}
