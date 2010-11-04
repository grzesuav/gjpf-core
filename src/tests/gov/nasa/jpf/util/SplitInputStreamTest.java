package gov.nasa.jpf.util;

import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.util.test.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import org.junit.*;

public class SplitInputStreamTest extends TestJPF
{
   private static final Random s_random = Verify.isRunningInJPF() ? null : new SecureRandom();

   private static final String s_jpfArgs[] = new String[]
   {
      //"+report.publisher=html",
      //"+report.html.finished+=,trace",
      "+listener+=,gov.nasa.jpf.listener.CoverageAnalyzer",
      "+coverage.include=gov.nasa.jpf.util.SplitInputStream*", 
      "+coverage.loaded_only=true",
      "+jpf-core.sourcepath+=,${jpf-core}/src/main,${jpf-core}/src/peers,${jpf-core}/src/tests,${jpf-core}/src/classes,${jpf-core}/src/annotations",
      "+listener+=,gov.nasa.jpf.listener.PreciseRaceDetector"
   };
   
   private SplitInputStream m_fixture;
   private InputStream      m_input;
   private byte             m_expect[];

   public static void main(String args[])
   {
      runTestsOfThisClass(args);
   }

   @Before
   public void before() throws IOException
   {
      initialize(10, SplitInputStream.INITIAL_BUFFER_SIZE);
   }

   public void initialize(int length, int bufferSize) throws IOException
   {
      ByteArrayInputStream source;
      int i;

      m_expect = new byte[length];

      if (s_random != null)
         s_random.nextBytes(m_expect);
      else
      {
         for (i = m_expect.length; --i >= 0; )
            m_expect[i] = (byte) i;
      }

      source    = new ByteArrayInputStream(m_expect);
      m_fixture = new SplitInputStream(source, 2, bufferSize);
      m_input   = m_fixture.getStream(0);
      
      Assert.assertEquals(2, m_fixture.getStreamCount());
   }

   @After
   public void after() throws IOException
   {
      InputStream input;
      int i, j;
      
      for (i = m_fixture.getStreamCount(); --i > 0; )
      {
         input = m_fixture.getStream(i);
         
         for (j = m_expect.length - input.available(); j < m_expect.length; j++)
            Assert.assertEquals(m_expect[j] & 0x0FF, input.read());
         
         Assert.assertEquals(-1, input.read());
         Assert.assertEquals(-1, input.read(new byte[1]));
      }
   }

   @Test(expected = NullPointerException.class)
   public void passNullPointerToConstructor() throws IOException
   {
      InputStream source;

      source = null;

      new SplitInputStream(source, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void passZeroStreamsToConstructor() throws IOException
   {
      InputStream source;
      
      source = new ByteArrayInputStream(m_expect);
      
      new SplitInputStream(source, 0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void passZeroBufferSizeToConstructor() throws IOException
   {
      InputStream source;

      source = new ByteArrayInputStream(m_expect);

      new SplitInputStream(source, 2, 0);
   }

   @Test
   public void readByte() throws IOException
   {
      Assert.assertEquals(m_expect[0] & 0x0FF, m_input.read());
   }
   
   @Test
   public void readEveryByteValue() throws IOException
   {
      ByteArrayInputStream source;
      int i;
      
      m_expect = new byte[256];
      
      for (i = 256; --i >= 0; )
         m_expect[i] = (byte) i;
      
      source    = new ByteArrayInputStream(m_expect);
      m_fixture = new SplitInputStream(source, 2, 256);
      m_input   = m_fixture.getStream(0);
   
      for (i = 0; i < 256; i++)
         Assert.assertEquals(i, m_input.read());
   }

   @Test(expected = NullPointerException.class)
   public void readNullBuffer() throws IOException
   {
      m_input.read(null, 0, 1);
   }

   @Test(expected = IndexOutOfBoundsException.class)
   public void readIndexNegOne() throws IOException
   {
      m_input.read(new byte[0], -1, 0);
   }

   @Test(expected = IndexOutOfBoundsException.class)
   public void readLengthNegOne() throws IOException
   {
      m_input.read(new byte[0], 0, -1);
   }

   @Test(expected = IndexOutOfBoundsException.class)
   public void readBeyondEnd() throws IOException
   {
      m_input.read(new byte[16], 8, 9);
   }

   @Test
   public void readLengthZero() throws IOException
   {
      m_input.read(new byte[1], 0, 0);
      Assert.assertEquals(m_expect[0] & 0x0FF, m_input.read());
   }

   @Test
   public void readArray() throws IOException
   {
      int offset, length, delta;
      byte buffer[];

      length = m_expect.length;
      buffer = new byte[length];

      for (offset = 0; offset < length; offset += delta)
      {
         delta = m_input.read(buffer);
         
         Assert.assertTrue(delta >= 0);
      }

      Assert.assertArrayEquals(m_expect, buffer);
   }

   @Test
   public void readArrayEveryByteValue() throws IOException
   {
      ByteArrayInputStream source;
      int i, delta;
      byte actual[];

      m_expect = new byte[256];
      actual   = new byte[256];

      for (i = 256; --i >= 0; )
         m_expect[i] = (byte) i;

      source    = new ByteArrayInputStream(m_expect);
      m_fixture = new SplitInputStream(source, 2);
      m_input   = m_fixture.getStream(0);
      
      for (i = 0; i < 256; i += delta)
      {
         delta = m_input.read(actual, i, 256 - i);
         
         Assert.assertTrue(delta >= 0);
      }
      
      Assert.assertArrayEquals(m_expect, actual);
   }

   @Test
   public void skipZero() throws IOException
   {
      Assert.assertEquals(0, m_input.skip(0));
   }

   @Test
   public void skipNegOne() throws IOException
   {
      Assert.assertEquals(0, m_input.skip(-1));
   }

   @Test
   public void skip() throws IOException
   {
      Assert.assertEquals(m_expect.length / 2, m_input.skip(m_expect.length / 2));
   }
   
   @Test
   public void skipToEnd() throws IOException
   {
      Assert.assertEquals(m_expect.length, m_input.skip(m_expect.length));
   }
   
   @Test
   public void skipBeyondEnd() throws IOException
   {
      Assert.assertEquals(m_expect.length, m_input.skip(m_expect.length + 1));
      Assert.assertEquals(0, m_input.skip(1));
   }
   
   @Test
   public void readByteAfterClose() throws IOException
   {
      m_input.close();
      Assert.assertEquals(-1, m_input.read());
   }

   @Test
   public void readBufferAfterClose() throws IOException
   {
      m_input.close();
      Assert.assertEquals(-1, m_input.read(new byte[1], 0, 1));
   }

   @Test
   public void skipAfterClose() throws IOException
   {
      m_input.close();
      Assert.assertEquals(0, m_input.skip(1));
   }

   @Test
   public void availableAfterClose() throws IOException
   {
      Assert.assertEquals(m_expect.length, m_input.available());
      m_input.close();
      Assert.assertEquals(0, m_input.available());
   }
   
   @Test
   public void availableAtEnd() throws IOException
   {
      int i;
      
      for (i = 0; i < m_expect.length; i++)
      {
         Assert.assertEquals(m_expect.length - i, m_input.available());
         Assert.assertEquals(m_expect[i] & 0x0FF, m_input.read());
      }
      
      Assert.assertEquals(0, m_input.available());
   }
   
   @Test
   public void availableNeverReads() throws IOException
   {
      SplitInputStream split;
      InputStream source, stream;
      
      source = new InputStream()
      {
         public int read()
         {
            Assert.fail();
            
            return(0);
         }
      };
      
      split  = new SplitInputStream(source, 1);
      stream = split.getStream(0);
      
      Assert.assertEquals(0, stream.available());
   }
   
   @Test
   public void closeSource() throws IOException
   {
      SplitInputStream split;
      CloseCountInputStream source;
      InputStream input;
      int i;
      
      source = new CloseCountInputStream();
      split  = new SplitInputStream(source, 2);
      input  = split.getStream(0);
      
      for (i = split.getStreamCount() + 5; --i >= 0; )
         input.close();
      
      Assert.assertEquals(0, source.getCloseCount());
      
      input = split.getStream(1);
      
      input.close();
      Assert.assertEquals(1, source.getCloseCount());

      for (i = split.getStreamCount() + 5; --i >= 0; )
         input.close();

      Assert.assertEquals(1, source.getCloseCount());
   }
   
   @Test
   public void overflowAvailable() throws IOException
   {
      SplitInputStream split;
      MaxAvailableInputStream source;
      InputStream input;

      source = new MaxAvailableInputStream();
      split  = new SplitInputStream(source, 1);
      input  = split.getStream(0);
      
      Assert.assertEquals(0, input.read());
      Assert.assertEquals(Integer.MAX_VALUE, input.available());
   }
   
   @Test
   public void expand() throws IOException
   {
      int i, length;
      
      length = 2 * SplitInputStream.INITIAL_BUFFER_SIZE;
      
      initialize(length, SplitInputStream.INITIAL_BUFFER_SIZE);
      
      for (i = 0; i < length; i++)
      {
         Assert.assertEquals(length - i, m_input.available());
         Assert.assertEquals(m_expect[i] & 0x0FF, m_input.read());
      }
   }
   
   @Test
   public void wrap() throws IOException
   {
      int i, length;
      
      length = 2 * SplitInputStream.INITIAL_BUFFER_SIZE;
      
      initialize(length, SplitInputStream.INITIAL_BUFFER_SIZE);
      
      for (i = 0; i < SplitInputStream.INITIAL_BUFFER_SIZE - 5; i++)
         Assert.assertEquals(m_expect[i] & 0x0FF, m_input.read());
      
      m_input = m_fixture.getStream(1);

      for (i = 0; i < SplitInputStream.INITIAL_BUFFER_SIZE; i++)
         Assert.assertEquals(m_expect[i] & 0x0FF, m_input.read());
      
      Assert.assertEquals(SplitInputStream.INITIAL_BUFFER_SIZE, m_input.available());
   }
   
   @Test
   public void ignoreClosedStream() throws IOException, NoSuchFieldException, IllegalAccessException
   {
      Field bufferField;
      int i, length;
      byte expect[], actual[];

      length = 2 * SplitInputStream.INITIAL_BUFFER_SIZE;

      initialize(length, SplitInputStream.INITIAL_BUFFER_SIZE);
      m_input.close();

      bufferField = SplitInputStream.class.getDeclaredField("m_buffer");
      
      bufferField.setAccessible(true);
      
      expect  = (byte[]) bufferField.get(m_fixture);
      m_input = m_fixture.getStream(1);

      for (i = 0; i < length; i++)
         Assert.assertEquals(m_expect[i] & 0x0FF, m_input.read());

      actual = (byte[]) bufferField.get(m_fixture);
      
      Assert.assertSame(expect, actual);
   }
   
   @Test
   public void bufferSize1() throws IOException
   {
      int i;
      
      initialize(10, 1);
      
      for (i = 0; i < m_expect.length; i++)
         Assert.assertEquals(m_expect[i] & 0x0FF, m_input.read());
   }

   @Ignore("This test takes too long for everyone to run all the time.  There are 101,174 states which take about 4 minutes to run.")
   @Test
   public void concurrentRead() throws IOException, InterruptedException
   {
      InputStream source;
      Thread thread1, thread2;
      Runnable task;
      
      if (verifyNoPropertyViolation(s_jpfArgs))
      {
         source = new InputStream()
         {
            private Thread m_reader;
            
            public int read()
            {
               if (m_reader == null)
                  m_reader = Thread.currentThread();    // JPF will catch the race condition if 2 threads call read concurrently.
               else
                  Assert.assertSame(m_reader, Thread.currentThread());
               
               return(0);
            }
         };
   
         m_fixture = new SplitInputStream(source, 2);
         
         task = new Runnable() 
         {
            public void run()
            {
               try
               {
                  m_fixture.getStream(0).read();
               }
               catch (IOException e)
               {
                  throw new RuntimeException(e);
               }
            }
         };
         
         thread1 = new Thread(task);

         task = new Runnable()
         {
            public void run()
            {
               try
               {
                  m_fixture.getStream(1).read();
               }
               catch (IOException e)
               {
                  throw new RuntimeException(e);
               }
            }
         };
         
         thread2 = new Thread(task);

         thread1.start();
         thread2.start();
         
         thread1.join();
         thread2.join();
      }
   }
   
   @Test
   public void concurrentAvailable() throws InterruptedException
   {
      InputStream source;
      Thread thread1, thread2;
      Runnable task;

      if (verifyNoPropertyViolation(s_jpfArgs))
      {
         source = new InputStream()
         {
            private Thread m_access;
            
            public int read()
            {
               Assert.fail();
               
               return(0);
            }
            
            public int available()
            {
               Assert.assertNull(m_access);
               
               m_access = Thread.currentThread();    // JPF will catch the race condition if 2 threads call concurrently.
               m_access = null;
               
               return(0);
            }
         };
      
         m_fixture = new SplitInputStream(source, 2);
         
         task = new Runnable() 
         {
            public void run()
            {
               try
               {
                  m_fixture.getStream(0).available();
               }
               catch (IOException e)
               {
                  throw new RuntimeException(e);
               }
            }
         };
         
         thread1 = new Thread(task);

         task = new Runnable()
         {
            public void run()
            {
               try
               {
                  m_fixture.getStream(1).available();
               }
               catch (IOException e)
               {
                  throw new RuntimeException(e);
               }
            }
         };

         thread2 = new Thread(task);
         
         thread1.start();
         thread2.start();
         
         thread1.join();
         thread2.join();
      }
   }

   @Ignore("This test takes too long for everyone to run all the time.  There are 230,360 states which take about 5 minutes to run.")
   @Test
   public void thoroughJPFTest() throws InterruptedException, IOException
   {
      Thread thread1, thread2;

      if (verifyNoPropertyViolation(s_jpfArgs))
      {
         initialize(4, 2);
         
         thread1 = new Thread(new JPFTask(0));
         thread2 = new Thread(new JPFTask(1));

         thread1.start();
         thread2.start();

         thread1.join();
         thread2.join();
      }
   }

   private static class CloseCountInputStream extends InputStream
   {
      private int m_closeCount;

      public int read()
      {
         return(0);
      }
      
      public int available()
      {
         return(m_closeCount == 0 ? 1 : 0);
      }
      
      public void close()
      {
         m_closeCount++;
      }
      
      public int getCloseCount()
      {
         return(m_closeCount);
      }
   }
   
   private static class MaxAvailableInputStream extends InputStream
   {
      private int m_data;
      
      public int read()
      {
         return(m_data++);
      }
      
      public int available()
      {
         return(Integer.MAX_VALUE);
      }
   }

   private class JPFTask implements Runnable
   {
      private final InputStream m_input;

      public JPFTask(int index)
      {
         m_input = m_fixture.getStream(index);
      }

      public void run()
      {
         try
         {
            unsafe();
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
      }

      private void unsafe() throws IOException
      {
         int i, expect, actual, test;
         byte buffer[];
         
         //System.out.print("#" + Thread.currentThread().getId() + " | Test ");
         test = Verify.getInt(0, 4);
         
         //System.out.println(test);
         
         switch (test)
         {
            case 0:
               m_input.close();
               break;
            
            case 1:
               Assert.assertEquals(4, m_input.available());
               break;
            
            case 2:
               expect = Verify.getInt(-1, 5);
               actual = (int) m_input.skip(expect);
               expect = Math.max(expect, 0);
               expect = Math.min(expect, 4);

               Assert.assertTrue(actual <= expect);
               break;
            
            case 3:
               for (i = 0; i < 4; i++)
                  Assert.assertEquals(m_expect[i], m_input.read());
            
               break;
            
            case 4:
               buffer = new byte[1];
               
               for (i = 0; i < 4; i++)
               {
                  Assert.assertEquals(1, m_input.read(buffer));
                  Assert.assertEquals(m_expect[i], buffer[0]);
               }
               break;
         }
      }
   }
}
