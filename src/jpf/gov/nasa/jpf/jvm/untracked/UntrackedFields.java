//
// Copyright (C) 2008 United States Government as represented by the
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

package gov.nasa.jpf.jvm.untracked;

/**
 * unfortunately this is a case of base class variation - we very much
 * would like to refactor the 'untracked' handling into a Fields derived class
 * that is used as base for our three concrete Fields classes (Array/Dynamic/Static),
 * but then we would have to reimplement their non-untracked related interfaces,
 * which is actually more redundancy. Sometimes it sucks not to have multiple inheritance
 *
 * this interface at least enables UntrackedManager (the only user of this info) to
 * treat our concrete Fields classes uniformly
 */
public interface UntrackedFields {

  void setUntracked (int untracked);
  int getUntracked ();
  boolean isUntracked ();
  void incUntracked ();
  void decUntracked ();

}
