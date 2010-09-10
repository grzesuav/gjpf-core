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

package gov.nasa.jpf.test.java.lang.ref;

import gov.nasa.jpf.jvm.*;
import org.junit.Test;
import gov.nasa.jpf.util.test.TestJPF;
import java.lang.ref.*;

public class WeakReferenceTest extends TestJPF
{
   public static void main(String[] args)
   {
      runTestsOfThisClass(args);
   }

   @Test
   public void testGCClearsRef()
   {
      if (verifyNoPropertyViolation())
      {
         WeakReference ref;
         
         ref = new WeakReference(new Object());
         
         System.gc();               // Mark that GC is needed
         Verify.getBoolean();       // Cause a state to be captured and hence GC to run
         
         assertNull(ref.get());
      }
   }
   
   @Test
   public void testStrongReferenceKeepsWeakReference()
   {
      WeakReference ref;
      Object obj;

      if (verifyNoPropertyViolation())
      {
         obj = new Object();
         ref = new WeakReference(obj);

         System.gc();                 // Mark that GC is needed
         Verify.getBoolean();         // Cause a state to be captured and hence GC to run

         assertSame(obj, ref.get());
      }
   }
}
