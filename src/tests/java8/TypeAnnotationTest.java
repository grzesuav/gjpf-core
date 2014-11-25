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

package java8;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ClassFile;
import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.AbstractTypeAnnotationInfo;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.VM;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.Test;


/**
 * regression test for Java 8 type annotations (JSR 308)
 */
public class TypeAnnotationTest extends TestJPF {

  //--- test type annotations
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE_USE)
  @interface MyTA {}

  //--- test class hierarchy
  
  interface Ifc {}
  static class Base {}
  
  public class Anno8 extends @MyTA Base implements @MyTA Ifc {

    @MyTA int data;

    @MyTA int baz (@MyTA int a, int b){
      @MyTA int x = a + b;
      return x;
    }
  }
  
  //--- listener to check annotations are set
  public static class Listener extends ListenerAdapter {
    
    protected int numberOfTargetTypes (AbstractTypeAnnotationInfo[] annos, int targetType){
      int n = 0;
      for (AbstractTypeAnnotationInfo tai : annos){
        if (tai.getTargetType() == targetType){
          n++;
        }
      }
      return n;
    }
    
    @Override
    public void classLoaded(VM vm, ClassInfo loadedClass) {
      if (loadedClass.getName().equals("java8.TypeAnnotationTest$Anno8")){
        System.out.println("checking loaded class " + loadedClass.getName() + " for type annotations..");
        
        // <2do> - needs more tests..
        
        System.out.println("--- super types");
        AbstractTypeAnnotationInfo[] tais = loadedClass.getTypeAnnotations();
        for (AbstractTypeAnnotationInfo tai : tais){
          System.out.println("  " + tai);
        }
        assertTrue(tais.length == 2);
        assertTrue( numberOfTargetTypes(tais, ClassFile.CLASS_EXTENDS) == 2); // base and interface
        
        System.out.println("--- fields");
        FieldInfo fi = loadedClass.getDeclaredInstanceField("data");
        tais = fi.getTypeAnnotations();
        for (AbstractTypeAnnotationInfo tai : tais){
          System.out.println("  " + tai);
        }
        assertTrue(tais.length == 1);
        assertTrue( numberOfTargetTypes(tais, ClassFile.FIELD) == 1);
        
        System.out.println("--- methods");
        MethodInfo mi = loadedClass.getMethod("baz(II)I", false);
        tais = mi.getTypeAnnotations();
        for (AbstractTypeAnnotationInfo tai : tais){
          System.out.println("  " + tai);
        }
        assertTrue(tais.length == 3);
        assertTrue( numberOfTargetTypes(tais, ClassFile.METHOD_RETURN) == 1);
        assertTrue( numberOfTargetTypes(tais, ClassFile.METHOD_FORMAL_PARAMETER) == 1);
        assertTrue( numberOfTargetTypes(tais, ClassFile.LOCAL_VARIABLE) == 1);
        
        LocalVarInfo lv = mi.getLocalVar("x", 4);
        System.out.println("--- local var " + lv);
        tais = lv.getTypeAnnotations();
        for (AbstractTypeAnnotationInfo tai : tais){
          System.out.println("  " + tai);
        }
        assertTrue(tais.length == 1);
        assertTrue( numberOfTargetTypes(tais, ClassFile.LOCAL_VARIABLE) == 1);
        
      }
    }
  }
  
  @Test
  public void testBasicTypeAnnotations (){
    if (verifyNoPropertyViolation("+listener=java8.TypeAnnotationTest$Listener")){
      Anno8 anno8 = new Anno8();
    }
  }
  
}
