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
package gov.nasa.jpf.verify;

/**
 * traces from execution of @Sequence annotated Java programs
 */
abstract public class SequenceOp {

  String sequenceId;
  
  public abstract void accept (SequenceProcessor proc);
  
  protected SequenceOp (String id){
    this.sequenceId = id;
  }

  public String getSequenceId() {
    return sequenceId;
  }
  
  public static class Start extends SequenceOp {    
    public Start (String sequenceId){
      super(sequenceId);
    }
    
    public void accept (SequenceProcessor proc) {
      proc.visit(this);
    }
  }
  
  
  //--- those represent operation on code sections (exec of a method)
  public static class Event extends SequenceOp {
    String src;
    String tgt;
    String scope;
    
    protected Event(String sequenceId, String src, String tgt, String scope) {
      super(sequenceId);
      
      this.src = src;
      this.tgt = tgt;
      this.scope = scope;
    }
    
    public String getScope() { return scope; }
    public String getSrc() { return src; }
    public String getTgt() { return tgt; }
    
    public void accept (SequenceProcessor proc) {
      proc.visit(this);
    }
  }
  
  public static class Enter extends Event {
    String res;
    
    public Enter(String sequenceId, String scope, String src, String tgt, String res) {
      super(sequenceId, src,tgt,scope);
      this.res = res;
    }

    public String getResult() {
      return res;
    }

    public String toString() {
      return "enter " + scope;
    }
    
    public void accept (SequenceProcessor proc) {
      proc.visit(this);
    }
  }
  
  public static class Exit extends Event {
    String res;
    
    public Exit(String sequenceId, String scope, String src, String tgt, String res) {
      super(sequenceId,src,tgt,scope);
      this.res = res;
    }
    
    public String getResult() {
      return res;
    }
    
    public String toString() {
      return "exit " + scope;
    }
  
    public void accept (SequenceProcessor proc) {
      proc.visit(this);
    }
  }
    
  public static class End extends SequenceOp {
    public End (String sequenceId) {
      super(sequenceId);
    }
    
    public void accept (SequenceProcessor proc) {
      proc.visit(this);
    }
  }

}
