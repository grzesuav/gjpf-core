package gov.nasa.jpf.jvm;

import gov.nasa.jpf.jvm.bytecode.Instruction;

public class JPF_gov_nasa_jpf_SerializationConstructor {

  /**
   * create a new instance, but only call the ctor of the first
   * non-serializable superclass
   */
  public static int newInstance___3Ljava_lang_Object_2__Ljava_lang_Object_2 (MJIEnv env, int mthRef,
                                                                             int argsRef) {
    ThreadInfo ti = env.getThreadInfo();
    Instruction insn = ti.getPC();        
    DirectCallStackFrame frame;
    
    if (!ti.isResumedInstruction(insn)) { // make a direct call  
      
      int clsRef = env.getReferenceField(mthRef, "mdc");
      ClassInfo ci = JPF_java_lang_Class.getReferredClassInfo(env, clsRef);
            
      int superCtorRef = env.getReferenceField(mthRef, "firstNonSerializableCtor");
      MethodInfo mi = JPF_java_lang_reflect_Method.getMethodInfo(env,superCtorRef);

      if (ci.isAbstract()){
        env.throwException("java.lang.InstantiationException");
        return MJIEnv.NULL;
      }
      
      int objRef = env.newObject(ci);

      MethodInfo stub = mi.createDirectCallStub("[serialization]");
      frame = new DirectCallStackFrame(stub, insn);
        
      frame.push(objRef, true);
      frame.dup(); // we store the return object on the frame (don't do that with a normal frame)
      
      ti.pushFrame(frame);
      env.repeatInvocation();
      
      return MJIEnv.NULL; // doesn't matter
      
    } else { // direct call returned, unbox return type (if any)
      frame = ti.getReturnedDirectCall();
      return frame.pop();
    }
  }

}
