package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.Config.Exception;
import gov.nasa.jpf.jvm.abstraction.StateGraph;
import gov.nasa.jpf.jvm.abstraction.StateGraphBuilder;
import gov.nasa.jpf.jvm.abstraction.state.ClassesNode;
import gov.nasa.jpf.jvm.abstraction.state.FrameNode;
import gov.nasa.jpf.jvm.abstraction.state.ObjectNode;
import gov.nasa.jpf.jvm.abstraction.state.RootNode;
import gov.nasa.jpf.jvm.abstraction.state.StateNode;
import gov.nasa.jpf.jvm.abstraction.state.StaticsNode;
import gov.nasa.jpf.jvm.abstraction.state.ThreadNode;
import gov.nasa.jpf.jvm.abstraction.state.ThreadObject;
import gov.nasa.jpf.jvm.abstraction.state.ThreadsNode;
import gov.nasa.jpf.jvm.abstraction.symmetry.CollectionFactory;
import gov.nasa.jpf.jvm.abstraction.symmetry.EqSet;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.DynamicElementInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.util.ObjVector;

import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;

public class AbstractorBasedBuilder implements StateGraphBuilder {
  protected Process proc;
  protected KernelState ks;
  
  public void attach(JVM jvm) throws Exception {
    AbstractorConfiguration absConfig;
    absConfig = jvm.getConfig().getInstance("abstraction.abstractor.config.class",
        AbstractorConfiguration.class);
    if (absConfig == null) {
      absConfig = new DefaultAbstractorConfiguration();
    }
    absConfig.attach(jvm);
    proc = new Process(absConfig);
    proc.attach(jvm);
    ks = jvm.getKernelState();
  }

  public StateGraph buildStateGraph() {
    StateGraph g = new StateGraph(buildFromRoot());
    proc.fillData();
    proc.reset();
    return g;
  }

  protected RootNode buildFromRoot() {
    RootNode root = new RootNode();
    root.classes = buildClasses();
    root.threads = buildThreads();
    root.vmNodeId = StateNode.ROOT_VM_ID;
    return root;
  }
  
  protected ClassesNode buildClasses() {
    TreeMap<Integer,StaticsNode> tree = new TreeMap<Integer,StaticsNode>();
    for (StaticElementInfo sei : ks.sa) {
      if (sei != null) {
        StaticsNode n = proc.getStaticsAbstractor(sei.getClassInfo()).getStaticsNode(sei, proc);
        if (n != null) {
          int idx = sei.getIndex();
          n.vmNodeId = StateNode.STATIC_VM_ID_START + idx;
          tree.put(new Integer(idx), n);
        }
      }
    }
    ClassesNode classes = new ClassesNode();
    classes.statics = tree;
    classes.vmNodeId = StateNode.CLASSES_VM_ID;
    return classes;
  }
  
  protected ThreadsNode buildThreads() {
    EqSet<ThreadNode> set = CollectionFactory.newEqSet();
    for (ThreadInfo ti : ks.tl.getThreads()) {
      set.add(buildThread(ti));
    }
    ThreadsNode threads = new ThreadsNode();
    threads.threads = set;
    threads.vmNodeId = StateNode.THREADS_VM_ID;
    return threads;
  }
  
  protected ThreadNode buildThread(ThreadInfo ti) {
    StackFrame[] stack = ti.dumpStack(); 
    ThreadNode node = new ThreadNode();
    node.frames = new ObjVector<FrameNode>(stack.length);
    node.status = ti.getStatus();
    node.threadObj = (ThreadObject) proc.mapOldHeapRefSpecial(ti.getThreadObjectRef());
    node.vmNodeId = StateNode.THREAD_VM_ID_START + ti.getIndex();
    if (stack.length > 0) {
      proc.getStackTailAbstractor(stack[0].getMethodInfo()).addFrames(stack, 0, node, proc);
    }
    return node;
  }
  
  
  
  
  protected static class Process extends CachedAbstractorConfiguration
  implements AbstractorProcess {
    protected static class FillInfo {
      public ObjectNode node;
      public DynamicElementInfo dei;
      public ObjectAbstractor<?> abs;
      
      public FillInfo(ObjectNode node, DynamicElementInfo dei, ObjectAbstractor<?> abs) {
        this.node = node; this.dei = dei; this.abs = abs;
      }
      
      @SuppressWarnings("unchecked")
      public void fill(AbstractorProcess proc) {
        ObjectAbstractor<ObjectNode> abs2 = (ObjectAbstractor<ObjectNode>) abs;
        abs2.fillInstanceData(dei, node, proc);
      }
    }
    
    protected final ObjVector<ObjectNode> heapMap = new ObjVector<ObjectNode>();
    protected final Queue<FillInfo> needFilling = new LinkedList<FillInfo>(); 
    protected DynamicArea da;
    
    public Process(AbstractorConfiguration absConfig) {
      super(absConfig);
    }
    
    @Override
    public void attach(JVM jvm) throws Exception {
      super.attach(jvm);
      da = jvm.getDynamicArea();
    }

    public void reset() {
      heapMap.clear();
    }
    
    public ObjectNode mapOldHeapRef(int objref) {
      return doMap(objref, false);
    }
    
    public ObjectNode mapOldHeapRefSpecial(int objref) {
      return doMap(objref, true);
    }

    protected ObjectNode doMap(int objref, boolean nullOk) {
      if (objref < 0) return null;
      
      ObjectNode ret = heapMap.get(objref);
      if (ret == null) {
        DynamicElementInfo dei = da.get(objref);
        if (dei == null) {
          if (nullOk) {
            return null;
          } else {
            throw new IllegalStateException("Illegal vm reference");
          }
        }
        ObjectAbstractor<?> abs = getObjectAbstractor(dei.getClassInfo()); 
        ret = abs.createInstanceSkeleton(dei);
        if (ret != null) {
          ret.vmNodeId = StateNode.DYNAMIC_VM_ID_START + objref;
          heapMap.set(objref, ret);
          needFilling.add(new FillInfo(ret, dei, abs));
        } else {
          // not recommended, but OK
          // will end up asking for the instance skeleton again
        }
      }
      return ret;
    }
    
    public void fillData() {
      while (!needFilling.isEmpty()) {
        needFilling.remove().fill(this);
      }
    }
  }
}
