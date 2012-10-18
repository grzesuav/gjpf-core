package gov.nasa.jpf.jvm;


public class JPF_gov_nasa_jpf_SerializationConstructor extends NativePeer {

  /**
   * create a new instance, but only call the ctor of the first
   * non-serializable superclass
   */
  public int newInstance___3Ljava_lang_Object_2__Ljava_lang_Object_2 (MJIEnv env, int mthRef,
                                                                             int argsRef) {
    ThreadInfo ti = env.getThreadInfo();
    StackFrame frame = ti.getReturnedDirectCall();
    String mid = "[serialization]";

    if (frame == null){ // first time
      int clsRef = env.getReferenceField(mthRef, "mdc");
      ClassInfo ci = env.getReferredClassInfo( clsRef);

      if (ci.isAbstract()){
        env.throwException("java.lang.InstantiationException");
        return MJIEnv.NULL;
      }
      
      if (ci.requiresClinitExecution(ti)) {
        // NOTE - this might cause cause another direct call for a <clinit.
        env.repeatInvocation();
        return MJIEnv.NULL;
      }
      
      int superCtorRef = env.getReferenceField(mthRef, "firstNonSerializableCtor");
      MethodInfo mi = JPF_java_lang_reflect_Constructor.getMethodInfo(env,superCtorRef);

      int objRef = env.newObject(ci);
      MethodInfo stub = mi.createDirectCallStub(mid);
      frame = new DirectCallStackFrame(stub, 2,0);
      frame.push(objRef, true);
      frame.dup(); // (1) we store the return object on the frame
      ti.pushFrame(frame);

      //env.repeatInvocation(); // we don't need this, direct calls don't advance their return frame
      return MJIEnv.NULL; // doesn't matter
      
    } else { // direct call returned
      while (!frame.getMethodInfo().getName().equals(mid)){
        // frame was the [clinit] direct call
        frame = frame.getPrevious();
      }
      
      int objRef = frame.pop(); // that's the object ref we pushed in (1)
      return objRef;
    }
  }
}
