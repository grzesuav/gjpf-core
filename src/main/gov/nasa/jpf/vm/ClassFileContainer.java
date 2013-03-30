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

package gov.nasa.jpf.vm;

import java.io.IOException;
import java.io.InputStream;

/**
 * abstract class that represents the source of a classfile, such
 * as (root) directories and jars
 */
public abstract class ClassFileContainer {
  protected String name;
  protected String url;

  protected ClassFileContainer(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getURL() {
    return url;
  }

  public abstract byte[] getClassData (String clsName) throws ClassParseException;
  public abstract String getClassURL (String clsName);  
  
  public abstract ClassInfo createClassInfo (String typeName, ClassLoaderInfo classLoader, String url, byte[] data) throws ClassParseException;
  public abstract AnnotationInfo createAnnotationInfo (String typeName, ClassLoaderInfo classLoader, byte[] data) throws ClassParseException;
  
  protected void readFully (InputStream is, byte[] buf) throws ClassParseException {
    try {
      int nRead = 0;
      while (nRead < buf.length) {
        int n = is.read(buf, nRead, buf.length - nRead);
        if (n < 0) {
          error("premature end of classfile: " + buf.length + '/' + nRead);
        }
        nRead += n;
      }
    } catch (IOException iox) {
      error("failed to read classfile");
    }
  }

  protected static void error(String msg) throws ClassParseException {
    throw new ClassParseException(msg);
  }

}
