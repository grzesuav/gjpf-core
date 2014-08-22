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
package gov.nasa.jpf.test.mc.basic;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.choice.BreakGenerator;
import org.junit.Test;

/**
 * JPF regression test for breaking transitions when exceeding
 * vm.max_transition_length. While the program would not terminate outside
 * JPF, it will terminate when run under JPF because of state matching
 * 
 * the listener is purely informative - if the test fails it doesn't terminate.
 * It should report two registrations within the loop, the second one matching
 * the state that was stored after the first one. However, the number of 
 * transition breaks might change with more sophisticated loop detection
 */
public class TransitionLengthTest extends TestJPF {
    
  public static class Listener extends ListenerAdapter {
    @Override
    public void choiceGeneratorRegistered (VM vm, ChoiceGenerator<?> nextCG, ThreadInfo currentThread, Instruction executedInstruction) {
      if (nextCG instanceof BreakGenerator){
        System.out.println();
        System.out.println("registered: " + nextCG);
      }
    }
  }
  
  @Test
  public void testTermination(){
    if (verifyNoPropertyViolation("+vm.max_transition_length=500", 
                                  "+listener=" + TransitionLengthTest.class.getName() + "$Listener")){
      System.out.println("starting loop");
      while (true){
        // no program state change withing body - this should eventually run into state matching
        System.out.print(".");
      }
      // we can never get here outside of JPF
    }
  }
}
