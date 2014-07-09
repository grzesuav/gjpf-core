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

package gov.nasa.jpf.test.mc.data;

import gov.nasa.jpf.EventProducer;
import gov.nasa.jpf.util.event.Event;
import gov.nasa.jpf.util.event.TestEventTree;
import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.Verify;
import org.junit.Test;

/**
 * regression test for EventGenerator based test drivers
 */
public class EventGeneratorTest extends TestJPF {

   
  //---------------------------------------------------------------------------------------
  public static class SimpleTree extends TestEventTree {
    public SimpleTree (){
      expected = new String[] {
        "a1b",
        "axxb"
      };
    }
    
    @Override
    public Event createEventTree() {
      return 
        sequence(
          event("a"),
          alternatives(
            event("1"),
            iteration(2,
              event("x")
            )
          ),
          event("b")
        );
    }
  }
  
  @Test
  public void testSimpleTree(){
    if (!isJPFRun()){
      Verify.resetCounter(0);
    }
    
    if (verifyNoPropertyViolation("+event.class=.test.mc.data.EventGeneratorTest$SimpleTree", "+log.info=event")){
      EventProducer producer = new EventProducer();
      StringBuilder sb = new StringBuilder();
      
      while (producer.processNextEvent()){
        sb.append(producer.getEventName());
      }
      
      String trace = sb.toString();
      System.out.print("got trace: ");
      System.out.println(trace);
      
      if (!producer.checkPath()){
        fail("unexpected trace failure");        
      }
      
      if (producer.isCompletelyCovered()){
        Verify.setCounter(0, 1);
      }
    }
    
    if (!isJPFRun()){
      if (Verify.getCounter(0) != 1){
        fail("unaccounted trace failure");
      }
    }
  }
    
  //-------------------------------------------------------------------------------------
  public static class CombinationTree extends TestEventTree {
    public CombinationTree (){
      printTree();
      printPaths();
    }
    
     @Override
    public Event createEventTree() {
       Event[] options = { event("A"), event("B"), event("C") };

       return anyCombination(options);
     }
  }
  
  //@Test
  public void testAnyCombination (){
    if (verifyNoPropertyViolation("+event.class=.test.mc.data.EventGeneratorTest$CombinationTree", "+log.info=event")){
      EventProducer producer = new EventProducer();
      StringBuilder sb = new StringBuilder();
      
      while (producer.processNextEvent()){
        sb.append(producer.getEventName());
      }
    }
  }
  
}
