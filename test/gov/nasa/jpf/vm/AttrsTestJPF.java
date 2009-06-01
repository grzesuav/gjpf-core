//
// Copyright (C) 2007 United States Government as represented by the
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

import gov.nasa.jpf.jvm.*;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.DSTORE;
import gov.nasa.jpf.jvm.bytecode.ISTORE;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.util.test.TestJPF;

/**
 * the JPF test driver for field/operand/local attribute propagation
 */
public class AttrsTestJPF extends TestJPF {

  static final String ATTR = "\"the answer to the ultimate question of life, the universe, and everything\"";

  public static class IntListener extends ListenerAdapter {

    public IntListener () {}

    public void instructionExecuted (JVM vm){
      Instruction insn = vm.getLastInstruction();
      MethodInfo mi = insn.getMethodInfo();

      // not very efficient, but who cares - it's a small test
      if (insn instanceof ISTORE){
        if (mi.getName().equals("testIntPropagation")){
          ISTORE istore = (ISTORE)insn;
          ThreadInfo ti = vm.getLastThreadInfo();
          String localName = istore.getLocalVariableName();
          int localIndex = istore.getLocalVariableIndex();

          if (localName.equals("i")){
            ti.setLocalAttr(localIndex, ATTR);
            Object a = ti.getLocalAttr(localIndex);
            System.out.println("'i' attribute set to: " + a);

          } else if (localName.equals("j")){
            Object a = ti.getLocalAttr(localIndex);
            System.out.println("'j' attribute: " + a);

            /** get's overwritten in the model class
            if (a != ATTR){
              throw new JPFException("attribute propagation failed");
            }
            **/
          }
        }
      }
    }
  }

  public static class DoubleListener extends ListenerAdapter {

    public DoubleListener () {}

    public void instructionExecuted (JVM vm){
      Instruction insn = vm.getLastInstruction();
      MethodInfo mi = insn.getMethodInfo();

      if (insn instanceof DSTORE){
        if (mi.getName().equals("testDoublePropagation")){
          DSTORE dstore = (DSTORE)insn;
          ThreadInfo ti = vm.getLastThreadInfo();
          String localName = dstore.getLocalVariableName();
          int localIndex = dstore.getLocalVariableIndex();

          if (localName.equals("d")){
            ti.setLocalAttr(localIndex, ATTR);

          } else if (localName.equals("r")){
            Object a = ti.getLocalAttr(localIndex);
            System.out.println("'r' attribute: " + a);
            /** get's overwritten in the model class
            if (a != ATTR){
              throw new JPFException("attribute propagation failed");
            }
            **/
          }
        }

      }
    }
  }

  public static class InvokeListener extends ListenerAdapter {

    public void instructionExecuted (JVM vm){
      Instruction insn = vm.getLastInstruction();
      if (insn instanceof InvokeInstruction) {
        InvokeInstruction call = (InvokeInstruction)insn;
        ThreadInfo ti = vm.getLastThreadInfo();
        MethodInfo mi = call.getInvokedMethod();
        String mName = mi.getName();
        if (mName.equals("goModel") || mName.equals("goNative")) {
          Object[] a = call.getArgumentAttrs(ti);
          assert a != null & a.length == 3;

          System.out.println("listener notified of: " + mName + "(), attributes= "
                             + a[0] + ',' + a[1] + ',' + a[2]);

          assert a[0] instanceof Integer && a[1] instanceof Integer;
          assert (((Integer)a[0]).intValue() == 1) &&
                 (((Integer)a[1]).intValue() == 2) &&
                 (((Integer)a[2]).intValue() == 3);
        }
      }
    }
  }


  public static void main (String[] selectedMethods) {
    runTestsOfThisClass(selectedMethods);
  }

  /**************************** tests **********************************/

  @Test
  public void testIntPropagation () {
    noPropertyViolationThis("+jpf.listener=.vm.AttrsTestJPF$IntListener");
  }

  @Test
  public void testDoublePropagation () {
    noPropertyViolationThis("+jpf.listener=.vm.AttrsTestJPF$DoubleListener");
  }

  @Test
  public void testExplicitRef () {
    noPropertyViolationThis();
  }

  @Test
  public void testArrayPropagation () {
    noPropertyViolationThis();
  }

  @Test
  public void testNativeMethod () {
    noPropertyViolationThis();
  }

  @Test
  public void testInvokeListener () {
    noPropertyViolationThis("+jpf.listener=.vm.AttrsTestJPF$InvokeListener");
  }

}
