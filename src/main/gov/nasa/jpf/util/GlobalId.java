package gov.nasa.jpf.util;

import gov.nasa.jpf.jvm.bytecode.Instruction;

public class GlobalId implements Comparable<GlobalId> {
  
  protected GlobalId tiGid;     // creating thread, null if this is for the main thread itself
  protected int count;          // running number created by thread
  protected Instruction insn;   // creating instruction
  
  protected int hc;             // hashCode cache 
  
  
  public GlobalId (GlobalId tiGid, int count, Instruction insn){
    this.tiGid = tiGid;
    this.count = count;
    this.insn = insn;
    
    HashData hd = new HashData();
    hd.add(tiGid);
    hd.add(count);
    hd.add(insn.getMethodInfo().getGlobalId());
    hd.add(insn.getInstructionIndex());
    hc = hd.getValue();
  }
  
  @Override
  public boolean equals (Object o){
    if (o instanceof GlobalId){
      GlobalId other = (GlobalId)o;
      
      if ((tiGid == other.tiGid) &&
          (count == other.count) &&
          (insn == other.insn)) {
        return true;
      }
    }
      
    return false;
  }
    
  @Override
  public int hashCode(){
    return hc;
  }

  @Override
  public int compareTo (GlobalId other) {
    return 0;
  }
}
