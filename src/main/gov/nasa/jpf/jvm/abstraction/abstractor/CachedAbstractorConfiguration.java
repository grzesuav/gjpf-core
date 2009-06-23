package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.Config.Exception;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.util.ObjVector;

public class CachedAbstractorConfiguration implements AbstractorConfiguration {
  protected final AbstractorConfiguration config;
  protected final ObjVector<ObjectAbstractor<?>> objectCache  = new ObjVector<ObjectAbstractor<?>>();
  protected final ObjVector<StaticsAbstractor>   staticsCache = new ObjVector<StaticsAbstractor>();
  protected final ObjVector<StackTailAbstractor> tailCache    = new ObjVector<StackTailAbstractor>();
  protected final ObjVector<FrameLocalAbstractor> frameCache  = new ObjVector<FrameLocalAbstractor>();
  
  public CachedAbstractorConfiguration(AbstractorConfiguration config) {
    this.config = config;
  }
  
  // call this if it hasn't been called in `config', or don't if it has. :)
  public void attach(JVM jvm) throws Exception {
    config.attach(jvm);
  }

  public ObjectAbstractor<?> getObjectAbstractor(ClassInfo ci) {
    int cid = ci.getUniqueId();
    ObjectAbstractor<?> a = objectCache.get(cid);
    if (a == null) {
      a = config.getObjectAbstractor(ci);
      objectCache.set(cid, a); 
    }
    return a;
  }

  public StaticsAbstractor getStaticsAbstractor(ClassInfo ci) {
    int cid = ci.getUniqueId();
    StaticsAbstractor a = staticsCache.get(cid);
    if (a == null) {
      a = config.getStaticsAbstractor(ci);
      staticsCache.set(cid, a); 
    }
    return a;
  }

  public StackTailAbstractor getStackTailAbstractor(MethodInfo mi) {
    int mid = mi.getGlobalId();
    StackTailAbstractor a = tailCache.get(mid);
    if (a == null) {
      a = config.getStackTailAbstractor(mi);
      tailCache.set(mid, a); 
    }
    return a;
  }

  public FrameLocalAbstractor getFrameLocalAbstractor(MethodInfo mi) {
    int mid = mi.getGlobalId();
    FrameLocalAbstractor a = frameCache.get(mid);
    if (a == null) {
      a = config.getFrameLocalAbstractor(mi);
      frameCache.set(mid, a); 
    }
    return a;
  }

}
