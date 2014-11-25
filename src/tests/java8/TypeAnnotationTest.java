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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.vm.AbstractTypeAnnotationInfo;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.TypeAnnotationInfo;
import gov.nasa.jpf.vm.VM;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import org.junit.Test;


/**
 * regression test for Java 8 type annotations (JSR 308)
 */
public class TypeAnnotationTest extends TestJPF {

  //--- test type annotations
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE_USE)
  @interface BaseFoo {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE_USE)
  @interface IfcFoo {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE_USE)
  @interface FieldFoo {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE_USE)
  @interface MethodFoo {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE_USE)
  @interface ArgFoo {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE_USE)
  @interface LocalFoo {}


  //--- test class hierarchy
  
  interface Ifc {}
  static class Base {}
  
  public class Anno8 extends @BaseFoo Base implements @IfcFoo Ifc {

    @FieldFoo int data;

    @MethodFoo int baz (@ArgFoo int a, int b){
      @LocalFoo int x = a + b;
      return x;
    }
  }
  
  //--- listener to check annotations are set
  public static class Listener extends ListenerAdapter {
    
    @Override
    public void classLoaded(VM vm, ClassInfo loadedClass) {
      if (loadedClass.getName().equals("java8.TypeAnnotationTest$Anno8")){
        System.out.println("checking loaded class " + loadedClass.getName() + " for type annotations..");
        
        System.out.println("--- super types");
        for (AbstractTypeAnnotationInfo tai : loadedClass.getTypeAnnotations()){
          System.out.println("  " + tai);
        }
        
        System.out.println("--- fields");
        FieldInfo fi = loadedClass.getDeclaredInstanceField("data");
        for (AbstractTypeAnnotationInfo tai : fi.getTypeAnnotations()){
          System.out.println("  " + tai);
        }
        
        System.out.println("--- methods");
        MethodInfo mi = loadedClass.getMethod("baz(II)I", false);
        for (AbstractTypeAnnotationInfo tai : mi.getTypeAnnotations()){
          System.out.println("  " + tai);
        }
        
        LocalVarInfo lv = mi.getLocalVar("x", 4);
        System.out.println("--- local var " + lv);
        for (AbstractTypeAnnotationInfo tai : lv.getTypeAnnotations()){
          System.out.println("  " + tai);
        }
        
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
