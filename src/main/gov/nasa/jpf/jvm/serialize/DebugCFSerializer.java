package gov.nasa.jpf.jvm.serialize;

import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JPFOutputStream;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.util.FinalBitSet;

public class DebugCFSerializer extends CFSerializer {

  JPFOutputStream os;
  
  // this is for debugging purposes only
  public DebugCFSerializer() {
    os = new JPFOutputStream(System.out);
  }
  
  protected int[] computeStoringData() {
    
    os.println();
    os.printCommentLine("------------------------");
    return super.computeStoringData();
  }
  
  protected void processReferenceQueue(){
    os.println();
    os.printCommentLine("--- Heap");
    super.processReferenceQueue();
  }
  
  public void processElementInfo(ElementInfo ei) {
    super.processElementInfo(ei);
    
    FinalBitSet filtered = !ei.isArray() ? getInstanceFilterMask(ei.getClassInfo()) : null;
    os.print(ei, filtered);
    os.println();
  }
  
  protected void serializeStatics(){
    os.println();
    os.printCommentLine("--- classes");
    super.serializeStatics();
  }
  
  protected void serializeClass (StaticElementInfo sei){
    super.serializeClass(sei);
    
    FinalBitSet filtered = getStaticFilterMask(sei.getClassInfo());
    os.print(sei, filtered);
    os.println();    
  }
  
  protected void serializeStackFrames(){
    os.printCommentLine("--- threads");
    super.serializeStackFrames();
  }
  
  protected void serializeStackFrames(ThreadInfo ti){
    os.println();
    os.print(ti);
    os.println();
    super.serializeStackFrames(ti);
  }
  
  protected void serializeFrame(StackFrame frame){
    os.print(frame);
    os.println();
    super.serializeFrame(frame);
  }
}
