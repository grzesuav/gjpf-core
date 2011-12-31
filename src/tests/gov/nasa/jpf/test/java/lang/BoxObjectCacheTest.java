//
// Copyright (C) 2011 United States Government as represented by the
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
package gov.nasa.jpf.test.java.lang;

import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

/**
 * regression test for java.lang.Integer
 */
public class BoxObjectCacheTest extends TestJPF {
  
  @Test
  public void testIntCache(){
    if (verifyNoPropertyViolation()){
      Integer i1 = Integer.valueOf( 1);        // should be cached
      assertTrue( i1.intValue() == 1);
      
      Integer i2 = Integer.parseInt("1");
      assertTrue( i1 == i2);
      
      i1 = Integer.valueOf(100000); // should be too large for cache
      assertTrue( i1.intValue() == 100000);
      
      i2 = Integer.parseInt("100000");
      assertTrue(i1 != i2);
    }
  }
  
  @Test
  public void testCharacterCache(){
    if (verifyNoPropertyViolation()){
      Character c1 = Character.valueOf( '?');        // should be cached
      assertTrue( c1.charValue() == '?');
      
      Character c2 = '?'; // compiler does the boxing
      assertTrue( c1 == c2);
      
      c1 = Character.valueOf( '\u2107' ); // epsilon, if I'm not mistaken
      assertTrue( c1.charValue() == '\u2107');
      
      c2 = '\u2107'; // compiler does the boxing
      assertTrue(c1 != c2);
    }
  }

  @Test
  public void testByteCache(){
    if (verifyNoPropertyViolation()){
      Byte b1 = Byte.valueOf( (byte)1);        // should be cached
      assertTrue( b1.byteValue() == 1);
      
      Byte b2 = Byte.parseByte("1");
      assertTrue( b1 == b2);
    }
  }

}
