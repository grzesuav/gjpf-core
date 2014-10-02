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
package gov.nasa.jpf.test.java.concurrent;

import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.Verify;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * regression test for java.util.concurrent.Exchanger
 */
public class ExchangerTest extends TestJPF  {

  static Exchanger<String> exchanger = new Exchanger<String>();
  
  static class ExTo extends Thread {
    public void run() {
      try {
        //interrupt();
        System.out.println("T now exchanging..");

        String response = exchanger.exchange("hi", 1000, TimeUnit.MILLISECONDS);

        System.out.print("T got: ");
        System.out.println(response);
        
        assertTrue(response.equals("there"));
        Verify.setBitInBitSet(0, 0, true);
        
      } catch (Throwable x) {
        System.out.print("T got exception: ");
        System.out.println(x);
        Verify.setBitInBitSet(0, 1, true);
      }
    }
  }
  
  @Test
  public void testTimeoutExchange() {
    
    if (verifyNoPropertyViolation()){
      Thread t = new ExTo();
      t.start();
    
      try {
        System.out.println("M now exchanging..");

        String response = exchanger.exchange("there", 100, TimeUnit.MILLISECONDS);

        System.out.print("M got: ");
        System.out.println(response);
        assertTrue(response.equals("hi"));
        Verify.setBitInBitSet(0, 2, true);

        
      } catch (Throwable x){
        System.out.print("M got exception: ");
        System.out.println( x);
        Verify.setBitInBitSet(0, 3, true);
      }     
      
    } else { // not executed under JPF
      // check if we saw each path at least once
      assertTrue( Verify.getBitInBitSet(0, 0));
      assertTrue( Verify.getBitInBitSet(0, 1));
      assertTrue( Verify.getBitInBitSet(0, 2));
      assertTrue( Verify.getBitInBitSet(0, 3));
    }
  }
}
