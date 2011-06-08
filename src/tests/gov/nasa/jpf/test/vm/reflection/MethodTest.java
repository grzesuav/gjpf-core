//
// Copyright (C) 2006 United States Government as represented by the
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
package gov.nasa.jpf.test.vm.reflection;

import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;
import java.lang.reflect.*;

public class MethodTest extends TestJPF
{
   private double m_data = 42.0;
   private Object m_arg;

   static class Boo
   {
      static int d = 42;
   }

   static class Faz
   {
      static int d = 4200;
   }

   static class SupC
   {
     private int privateMethod()
     {
       return -42;
     }
   }

   static class SubC extends SupC
   {
     public int privateMethod()
     {
       return 42;
     }
   }

   public Boo getBoo()
   {
      return null;
   }

   public double foo(int a, double d, String s)
   {
      assert m_data == 42.0 : "wrong object data";
      assert a == 3 : "wrong int parameter value";
      assert d == 3.33 : "wrong double parameter value";
      assert "Blah".equals(s) : "wrong String parameter value";

      return 123.456;
   }

   @Test
   public void testInstanceMethodInvoke()
   {
      if (verifyNoPropertyViolation())
      {
         MethodTest o = new MethodTest();

         try
         {
            Class<?> cls = o.getClass();
            Method   m   = cls.getMethod("foo", int.class, double.class, String.class);

            Object   res = m.invoke(o, new Integer(3), new Double(3.33), "Blah");
            double   d   = ((Double) res).doubleValue();

            assert d == 123.456 : "wrong return value";

         }
         catch (Throwable t)
         {
            t.printStackTrace();

            assert false : " unexpected exception: " + t;
         }
      }
   }

   @Test
   public void getPrivateMethod() throws NoSuchMethodException
   {
      if (verifyUnhandledException(NoSuchMethodException.class.getName()))
      {
         Integer.class.getMethod("toUnsignedString", int.class, int.class);   // Doesn't matter which class we use.  It just needs to be a different class and a private method.
      }
   }

   private static void privateStaticMethod()
   {
   }

   @Test
   public void invokePrivateSameClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
      privateStaticMethod();   // Get rid of IDE warning

      if (verifyNoPropertyViolation())
      {
         Class c;
         
         Method m = getClass().getDeclaredMethod("privateStaticMethod");

         m.invoke(null);
      }
   }

   @Test
   public void invokePrivateOtherClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
      if (verifyUnhandledException(IllegalAccessException.class.getName()))
      {
         Method m = Integer.class.getDeclaredMethod("toUnsignedString", int.class, int.class);

         m.invoke(null, 5, 3);
      }
   }

   @Test
   public void invokePrivateOtherClassAccessible() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
      if (verifyNoPropertyViolation())
      {
         Method m = Integer.class.getDeclaredMethod("toUnsignedString", int.class, int.class);

         m.setAccessible(true);
         m.invoke(null, 5, 3);
      }
   }
   
   @Test
   public void invokePrivateSuperclass() throws SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
     if (verifyNoPropertyViolation())
     {
       Method aMethod = SupC.class.getDeclaredMethod("privateMethod");
       aMethod.setAccessible(true);
       assert ((Integer) aMethod.invoke(new SubC()) == -42) : "must call method from superclass";
     }
   }

   @Test
   public void getMethodCanFindNotify() throws NoSuchMethodException
   {
      if (verifyNoPropertyViolation())
      {
         Integer.class.getMethod("notify");
      }
   }

   @Test
   public void getDeclaredMethodCantFindNotify() throws NoSuchMethodException
   {
      if (verifyUnhandledException(NoSuchMethodException.class.getName()))
      {
         Integer.class.getDeclaredMethod("notify");
      }
   }

   public void publicMethod()
   {
   }

   @Test
   public void invokeWrongThisType() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
      if (verifyUnhandledException(IllegalArgumentException.class.getName()))
      {
         Method m = getClass().getMethod("publicMethod");

         m.invoke(new Object());
      }
   }

   @Test
   public void invokeNullObject() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
      if (verifyUnhandledException(NullPointerException.class.getName()))
      {
         Method m = getClass().getMethod("publicMethod");

         m.invoke(null);
      }
   }

   @Test
   public void invokeWrongNumberOfArguments() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
      if (verifyUnhandledException(IllegalArgumentException.class.getName()))
      {
         Method m = getClass().getMethod("publicMethod");

         m.invoke(this, 5);
      }
   }

   @Test
   public void invokeWrongArgumentTypeReference() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
      if (verifyUnhandledException(IllegalArgumentException.class.getName()))
      {
         Method m = getClass().getMethod("boofaz", Boo.class, Faz.class);

         m.invoke(this, new Faz(), new Boo());
      }
   }

   public void throwThrowable() throws Throwable
   {
      throw new Throwable("purposeful exception");
   }

   @Test
   public void invokeInvocationTargetException() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
      Class<?> clazz;
      Method method;
      
      if (verifyUnhandledException(InvocationTargetException.class.getName()))
      {
         clazz  = getClass();
         method = clazz.getMethod("throwThrowable");

         method.invoke(this);
      }
   }

   @Test
   public void testReturnType()
   {
      if (verifyNoPropertyViolation())
      {
         MethodTest o = new MethodTest();

         try
         {
            Class<?> cls = o.getClass();
            Method m = cls.getMethod("getBoo");
            Class<?> rt = m.getReturnType();
            String s = rt.getName();

            assert Boo.class.getName().equals(s) : "wrong return type: " + s;

         }
         catch (Throwable t)
         {
            t.printStackTrace();

            assert false : " unexpected exception in Method.getReturnType(): " + t;
         }
      }
   }

   public void boofaz(Boo b, Faz f)
   {
      b = null; // Get rid of IDE warning
      f = null;
   }

   @Test
   public void testParameterTypes()
   {
      if (verifyNoPropertyViolation())
      {
         MethodTest o = new MethodTest();

         try
         {
            Class<?> cls = o.getClass();

            for (Method m : cls.getMethods())
            {
               if (m.getName().equals("boofaz"))
               {
                  Class<?>[] pt = m.getParameterTypes();

                  assert Boo.class.getName().equals(pt[0].getName()) : "wrong parameter type 0: " + pt[0].getName();
                  assert Faz.class.getName().equals(pt[1].getName()) : "wrong parameter type 1: " + pt[1].getName();
               }
            }

         }
         catch (Throwable t)
         {
            t.printStackTrace();

            assert false : " unexpected exception in Method.getParameterTypes(): " + t;
         }
      }
   }

   public void argTestTarget(byte arg)
   {
      m_arg = arg;
   }

   public void argTestTarget(short arg)
   {
      m_arg = arg;
   }

   public void argTestTarget(int arg)
   {
      m_arg = arg;
   }

   public void argTestTarget(long arg)
   {
      m_arg = arg;
   }

   public void argTestTarget(float arg)
   {
      m_arg = arg;
   }

   public void argTestTarget(double arg)
   {
      m_arg = arg;
   }

   public void argTestTarget(boolean arg)
   {
      m_arg = arg;
   }

   public void argTestTarget(char arg)
   {
      m_arg = arg;
   }
   
   public void argTestTarget(String arg)
   {
      m_arg = arg;
   }

   public String runArgTest(Class argType) throws NoSuchMethodException
   {
      Class         clazz;
      Method        method;
      Object        args[];
      StringBuilder result;
      double        expected, actual;
      int           i;

      clazz  = MethodTest.class;
      method = clazz.getMethod("argTestTarget", argType);
      result = new StringBuilder();
      
      args   = new Object[]
      {
         Byte.valueOf((byte) 7), 
         Short.valueOf((short) 8), 
         Integer.valueOf(9), 
         Long.valueOf(10), 
         Float.valueOf(3.1415f), 
         Double.valueOf(3.14159), 
         Boolean.TRUE, 
         Character.valueOf('w'), 
         "hello", 
         null
      };

      for (i = args.length; --i >= 0; )
      {
         if (args[i] == null)
            result.append("null");
         else
            result.append(args[i].getClass().getName());
         
         result.append('=');
         
         try
         {
            m_arg = null;

            method.invoke(this, args[i]);

            actual   = decodeValue(m_arg);
            expected = decodeValue(args[i]);
            
            assert Math.abs(expected - actual) < 0.0000001 : "Expected = " + expected + ".  Actual = " + actual + ".  Result = " + result.toString();

            result.append("No Exception");
         }
         catch (AssertionError e)
         {
            throw e;
         }
         catch (Throwable t)
         {
            assert m_arg == null : "m_arg expected to be null.  It was " + m_arg.toString() + ".  Result = " + result.toString() + ".  Exception = " + t.toString();
            result.append(t.getClass().getName());
         }
         
         result.append(',');
      }

      return(result.toString());
   }
   
   private double decodeValue(Object value)
   {
      double result;
      
      if (value instanceof Number)
         result = ((Number) value).doubleValue();
      else if (value instanceof Character)
         result = ((Character) value).charValue();
      else if (value instanceof Boolean)
         result = ((Boolean) value).booleanValue() ? 1 : 0;
      else if (value instanceof String)
         result = -1.0;
      else if (value == null)
         result = 0.0;
      else
      {
         fail("Unknown type: " + value.getClass().getName());
         result = Double.NaN;
      }
      
      return(result);
   }
   
   @Test
   public void argTest() throws ClassNotFoundException, NoSuchMethodException // Method name must be the same as below in order to trick JPFTest
   {
      String actual, expected, argClassName;
      Class argClass;
      int i, length;
      
      if (!Verify.isRunningInJPF())
         return;
      
      argClassName = Verify.getProperty("argtest.argClassName");

      System.out.println("Arg Class = " + argClassName);

      argClass     = Class.forName(argClassName);
      actual       = runArgTest(argClass);
      expected     = Verify.getProperty("argtest.expected");

      System.out.println("Actual Length = " + actual.length());
      System.out.println("Actual =\n" + actual.replaceAll(",", "\n"));

      System.out.println("Expected Length = " + expected.length());
      System.out.println("Expected =\n" + expected.replaceAll(",", "\n"));
      
      if (expected.equals(actual))
         return;
      
      length = Math.min(expected.length(), actual.length());
         
      for (i = 0; i < length; i++)
         if (expected.charAt(i) != actual.charAt(i))
            break;
      
      System.out.println("Actual Diff =\n" + actual.substring(i));
      System.out.println("Expected Diff =\n" + expected.substring(i));
      
      fail("Expected != Actual");
   }
   
   private void argTest(Class argClass) throws NoSuchMethodException  // Method name must be the same as above in order to trick JPFTest
   {
      String expected;
      
      expected = runArgTest(argClass);
      
      verifyNoPropertyViolation("+argtest.argClassName=" + argClass.getName(), "+argtest.expected=" + expected);
   }

   @Test
   public void argTestByte() throws NoSuchMethodException
   {
      argTest(byte.class);
   }

   @Test
   public void argTestShort() throws NoSuchMethodException
   {
      argTest(short.class);
   }

   @Test
   public void argTestInt() throws NoSuchMethodException
   {
      argTest(int.class);
   }

   @Test
   public void argTestLong() throws NoSuchMethodException
   {
      argTest(long.class);
   }

   @Test
   public void argTestFloat() throws NoSuchMethodException
   {
      argTest(float.class);
   }

   @Test
   public void argTestDouble() throws NoSuchMethodException
   {
      argTest(double.class);
   }

   @Test
   public void argTestBoolean() throws NoSuchMethodException
   {
      argTest(boolean.class);
   }

   @Test
   public void argTestChar() throws NoSuchMethodException
   {
      argTest(char.class);
   }
   
   @Test
   public void argTestString() throws NoSuchMethodException
   {
      argTest(String.class);
   }
}
