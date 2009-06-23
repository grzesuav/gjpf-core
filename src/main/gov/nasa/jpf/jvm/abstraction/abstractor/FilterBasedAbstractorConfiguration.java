package gov.nasa.jpf.jvm.abstraction.abstractor;

import gov.nasa.jpf.Config.Exception;
import gov.nasa.jpf.jvm.abstraction.state.ClassObject;
import gov.nasa.jpf.jvm.abstraction.state.InstanceObject;
import gov.nasa.jpf.jvm.abstraction.state.PrimArrayObject;
import gov.nasa.jpf.jvm.abstraction.state.RefArrayObject;
import gov.nasa.jpf.jvm.abstraction.state.ThreadObject;
import gov.nasa.jpf.jvm.abstraction.filter.DefaultFilterConfiguration;
import gov.nasa.jpf.jvm.abstraction.filter.FilterConfiguration;
import gov.nasa.jpf.jvm.abstraction.filter.FramePolicy;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;

public class FilterBasedAbstractorConfiguration implements AbstractorConfiguration {
  protected FilterConfiguration filter;
  
  public void attach(JVM jvm) throws Exception {
    filter = jvm.getConfig().getInstance("filter.class", FilterConfiguration.class);
    if (filter == null) filter = new DefaultFilterConfiguration(); 
    filter.init(jvm.getConfig());
  }

  public ObjectAbstractor<?> getObjectAbstractor(ClassInfo ci) {
    if (ci.isArray()) {
      if (ci.isReferenceArray()) {
        return getRefArrayAbstractor(ci);
      } else {
        return getPrimArrayAbstractor(ci);
      }
    } else if (ci.isInstanceOf("java.lang.Class")) {
      return getClassObjAbstractor(ci);
    } else {
      Iterable<FieldInfo> relevant = filter.getMatchedInstanceFields(ci);
      if (ci.isInstanceOf("java.lang.Thread")) {
        return getThreadAbstractor(ci, relevant);
      } else { 
        return getInstanceAbstractor(ci, relevant);
      }
    }
  }
  
  public StaticsAbstractor getStaticsAbstractor(ClassInfo ci) {
    return getStaticsAbstractor(ci,filter.getMatchedStaticFields(ci));
  }

  public StackTailAbstractor getStackTailAbstractor(MethodInfo mi) {
    return getStackTailAbstractor(mi,filter.getFramePolicy(mi));
  }

  public FrameLocalAbstractor getFrameLocalAbstractor(MethodInfo mi) {
    return getFrameLocalAbstractor(mi,filter.getFramePolicy(mi));
  }

  
  // ************************* Implementation *********************** //
  
  protected final FieldsMetaBuilder metaBuilder = new FieldsMetaBuilder();
  
  protected ObjectAbstractor<ClassObject>
  getClassObjAbstractor(ClassInfo ci) {
    return new ClassObjectAbstractor();
  }
  
  protected ObjectAbstractor<ThreadObject>
  getThreadAbstractor(ClassInfo ci, Iterable<FieldInfo> relevant) {
    ObjectAbstractor<ThreadObject> ret;
    metaBuilder.addAll(relevant);
    FieldsMeta meta = new FieldsMeta(metaBuilder);
    ret = meta.getThreadAbstractor();
    metaBuilder.reset();
    return ret;
  }
  
  protected ObjectAbstractor<InstanceObject>
  getInstanceAbstractor(ClassInfo ci, Iterable<FieldInfo> relevant) {
    ObjectAbstractor<InstanceObject> ret;
    metaBuilder.addAll(relevant);
    FieldsMeta meta = new FieldsMeta(metaBuilder);
    ret = meta.getInstanceAbstractor();
    metaBuilder.reset();
    return ret;
  }
  
  protected ObjectAbstractor<PrimArrayObject>
  getPrimArrayAbstractor(ClassInfo ci) {
    return ArrayAbstractors.defaultPrimsInstance;
  }

  protected ObjectAbstractor<RefArrayObject>
  getRefArrayAbstractor(ClassInfo ci) {
    return ArrayAbstractors.defaultRefsInstance;
  }


  protected StaticsAbstractor getStaticsAbstractor(ClassInfo ci, Iterable<FieldInfo> relevant) {
    metaBuilder.addAll(relevant);
    StaticsAbstractor ret = new FieldsMeta(metaBuilder).getStaticsAbstractor();
    metaBuilder.reset();
    return ret;
  }


  protected StackTailAbstractor getStackTailAbstractor(MethodInfo mi, FramePolicy policy) {
    return StackTailAbstractors.fromPolicy(policy);
  }

  protected FrameLocalAbstractor getFrameLocalAbstractor(MethodInfo mi, FramePolicy policy) {
    return FrameLocalAbstractors.fromPolicy(policy);
  }
}
