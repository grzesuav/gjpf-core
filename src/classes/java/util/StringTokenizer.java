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


package java.util;

/**
 *
 * @author Ivan Mushketik
 */
public class StringTokenizer implements Enumeration<Object> {

  // Current position in string during parsing
  private int currentPos;
  // Length of the string to tokenize. Save here for optimization
  private int strLength;
  // Max code point among delimeters symbols. For optimization
  private int maxCodePoint;
  // Array of code points of delimeters. We use int because delimeters can be
  // not from Basic Multilingual Plane
  private int delims[];

  private boolean returnDelims;
  // String to tokenize
  private String str;

  public StringTokenizer(String str, String delim, boolean returnDelims) {
    init(str, delim, returnDelims);
  }

  public StringTokenizer(String str, String delim) {
    init(str, delim);
  }

  public StringTokenizer(String str) {
    init(str);
  }

  public native void init(String str, String delim, boolean returnDelims);
  
  public native void init(String str, String delim);
  
  public native void init(String str);

  public native boolean hasMoreElements();

  public native Object nextElement();

  public native boolean hasMoreTokens();

  public native String nextToken();

  public native String nextToken(String delims);

  public native int countTokens();
}
