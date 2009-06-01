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
package java.io;

/**
 * natively convert char output into byte output
 */
public class OutputStreamWriter extends Writer {

  static final int BUF_SIZE=128;
  private static Object lock = new Object();
  
  OutputStream out;
  
  byte[] buf = new byte[BUF_SIZE];
  
  public OutputStreamWriter(OutputStream os) {
    out = os;
  }
  
  public void close(){
    // nothing
  }
  
  public void flush() {
    // nothing
  }
  
  public native String getEncoding();
  
  native int encode (char[] cbuf, int off, int len, byte[] buf);
  
  public void write(char[] cbuf, int off, int len) throws IOException {
    int w=0;
    
    synchronized(lock){
      while (w < len){
        int n = encode(cbuf, off+w, len-w, buf);
        out.write(buf, 0, n);
        w += n;
      }
    }
  }
  
  private native int encode (char c, byte[] buf);

  public void write (int c) throws IOException {
    synchronized(lock){
      int n = encode((char)c, buf);
      out.write(buf,0,n);
    }
  }
  
  private native int encode (String s, int off, int len, byte[] buf);
  
  public void write(String s, int off, int len) throws IOException {
    int w=0;
    
    synchronized(lock){
      while (w < len){
        int n = encode(s, off+w, len-w, buf);
        out.write(buf, 0, n);
        w += n;
      }
    }
  }
}
