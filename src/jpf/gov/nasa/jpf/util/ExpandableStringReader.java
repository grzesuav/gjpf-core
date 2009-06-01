//
// Copyright  (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
//  (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
//  (NOSA), version 1.3.  The NOSA has been approved by the Open Source
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

package gov.nasa.jpf.util;

import java.io.IOException;
import java.io.StringReader;

/**
 * simple StringReader that is aware of quoted chars and (some)
 * regular expression metachars, which are answered as tokens
 * with negative int values, i.e. don't interfere with char values.
 * 
 * current metachars are '[', ']', '|'
 * 
 * quotes itself are not returned, only the quoted char, i.e.
 * the quoting is transparent for the client (e.g. StringExpander)
 */
public class ExpandableStringReader extends StringReader {

  public static final int EOS = -1;
  public static final int CHAR_CHOICE_START = -2; // '['
  public static final int CHAR_CHOICE_END = -3;   // ']'
  public static final int OR = -4;                // '|'
    
  int pos;  // current string position  <2do> - that's not known to a reader! 
  int cur;  // current (returned) char value
  int last; // previously returned char value

  boolean isQuoted = false;
  
  public ExpandableStringReader(String arg) {
    super(arg);
    pos = -1;
  }

  public int read () throws IOException {
    last = cur;
    cur = super.read();
    pos++;
    isQuoted = false;
    
    switch (cur) {
    case '\\':
      pos++;
      isQuoted = true;
      return super.read();
    case '[':
      return CHAR_CHOICE_START;
    case ']':
      return CHAR_CHOICE_END;
    case '|':
      return OR;
    default:
      return cur;
    }
  }
  
  public int read(char[] cbuf, int off, int len)  {
    throw new UnsupportedOperationException("block reads for ExpandableStringReader not supported");
  }
  
  public boolean isQuoted() {
    return isQuoted;
  }
  
  public boolean isMetaChar(int c) {
    return (c < -1);
  }
  
  public int getPosition() {
    return pos;
  }
}
